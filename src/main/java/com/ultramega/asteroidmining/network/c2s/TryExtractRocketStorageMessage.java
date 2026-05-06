package com.ultramega.asteroidmining.network.c2s;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.container.RocketStorageViewerContainerMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.CarriedSlotWrapper;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public record TryExtractRocketStorageMessage(boolean fluid,
                                             int handlerIndex,
                                             ItemResource itemResource,
                                             FluidResource fluidResource,
                                             int amount,
                                             boolean shiftDown) implements CustomPacketPayload {
    public static final Type<TryExtractRocketStorageMessage> TYPE = new Type<>(AsteroidMining.makeId("try_carry_rocket_storage"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TryExtractRocketStorageMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, TryExtractRocketStorageMessage::fluid,
        ByteBufCodecs.INT, TryExtractRocketStorageMessage::handlerIndex,
        ItemResource.STREAM_CODEC, TryExtractRocketStorageMessage::itemResource,
        FluidResource.STREAM_CODEC, TryExtractRocketStorageMessage::fluidResource,
        ByteBufCodecs.INT, TryExtractRocketStorageMessage::amount,
        ByteBufCodecs.BOOL, TryExtractRocketStorageMessage::shiftDown,
        TryExtractRocketStorageMessage::new
    );

    public static TryExtractRocketStorageMessage item(final int handlerIndex, final ItemResource resource, final int amount, final boolean shiftDown) {
        return new TryExtractRocketStorageMessage(false, handlerIndex, resource, FluidResource.EMPTY, amount, shiftDown);
    }

    public static TryExtractRocketStorageMessage fluid(final int handlerIndex, final FluidResource resource, final int amount, final boolean shiftDown) {
        return new TryExtractRocketStorageMessage(true, handlerIndex, ItemResource.EMPTY, resource, amount, shiftDown);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final TryExtractRocketStorageMessage data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().containerMenu instanceof RocketStorageViewerContainerMenu containerMenu) || data.amount() <= 0 || data.handlerIndex() < 0) {
                return;
            }

            if (data.fluid()) {
                handleFluidExtraction(data, context.player(), containerMenu);
            } else {
                handleItemExtraction(data, context.player(), containerMenu);
            }

            containerMenu.broadcastChanges();
        });
    }

    private static void handleItemExtraction(final TryExtractRocketStorageMessage data, final Player player, final RocketStorageViewerContainerMenu containerMenu) {
        final ResourceHandler<ItemResource> source = containerMenu.getItemHandler();
        if (data.handlerIndex() >= source.size()) {
            return;
        }

        final ItemResource currentResource = source.getResource(data.handlerIndex());
        if (currentResource.isEmpty() || !currentResource.equals(data.itemResource())) {
            return;
        }

        final int amount = boundedTransferAmount(source.getAmountAsLong(data.handlerIndex()), data.amount());
        if (amount <= 0) {
            return;
        }

        final ResourceHandler<ItemResource> target = data.shiftDown()
            ? PlayerInventoryWrapper.of(player)
            : CarriedSlotWrapper.of(containerMenu);

        try (Transaction tx = Transaction.openRoot()) {
            final int extracted = source.extract(data.handlerIndex(), currentResource, amount, tx);
            if (extracted <= 0) {
                return;
            }

            final int inserted = target.insert(currentResource, extracted, tx);
            if (inserted == extracted) {
                tx.commit();
            }
        }
    }

    private static void handleFluidExtraction(final TryExtractRocketStorageMessage data, final Player player, final RocketStorageViewerContainerMenu containerMenu) {
        final ResourceHandler<FluidResource> source = containerMenu.getFluidHandler();
        if (data.handlerIndex() >= source.size()) {
            return;
        }

        final FluidResource currentResource = source.getResource(data.handlerIndex());
        if (currentResource.isEmpty() || !currentResource.equals(data.fluidResource())) {
            return;
        }

        final int amount = Math.min(FluidType.BUCKET_VOLUME, boundedTransferAmount(source.getAmountAsLong(data.handlerIndex()), data.amount()));
        if (amount <= 0) {
            return;
        }

        //TODO: rework this (currently a bucket in your inventory will just be filled, however the user should instead drag the item over the fluid)
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            final ItemAccess access = ItemAccess.forPlayerSlot(player, slot).oneByOne();
            final ResourceHandler<FluidResource> target = access.getCapability(Capabilities.Fluid.ITEM);
            if (target == null) {
                continue;
            }

            try (Transaction tx = Transaction.openRoot()) {
                final int extracted = source.extract(data.handlerIndex(), currentResource, amount, tx);
                if (extracted != amount) {
                    continue;
                }

                final int inserted = target.insert(currentResource, amount, tx);
                if (inserted == amount) {
                    tx.commit();
                    return;
                }
            }
        }
    }

    private static int boundedTransferAmount(final long available, final int requested) {
        return Math.clamp(requested, 0, Math.clamp(available, 0, Integer.MAX_VALUE));
    }
}
