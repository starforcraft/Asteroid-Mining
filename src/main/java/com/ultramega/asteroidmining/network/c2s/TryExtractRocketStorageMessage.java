package com.ultramega.asteroidmining.network.c2s;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.asteroids.AsteroidResource;
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

// TODO: make this easily extensible instead of only item + fluid
public record TryExtractRocketStorageMessage(int handlerIndex,
                                             AsteroidResource resource,
                                             int amount,
                                             boolean shiftDown) implements CustomPacketPayload {
    public static final Type<TryExtractRocketStorageMessage> TYPE = new Type<>(AsteroidMining.makeId("try_carry_rocket_storage"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TryExtractRocketStorageMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, TryExtractRocketStorageMessage::handlerIndex,
        AsteroidResource.STREAM_CODEC, TryExtractRocketStorageMessage::resource,
        ByteBufCodecs.INT, TryExtractRocketStorageMessage::amount,
        ByteBufCodecs.BOOL, TryExtractRocketStorageMessage::shiftDown,
        TryExtractRocketStorageMessage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final TryExtractRocketStorageMessage data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().containerMenu instanceof RocketStorageViewerContainerMenu containerMenu) || data.amount() <= 0 || data.handlerIndex() < 0) {
                return;
            }

            switch (data.resource()) {
                case AsteroidResource.ItemEntry item -> handleItemExtraction(item, data, context.player(), containerMenu);
                case AsteroidResource.FluidEntry fluid -> handleFluidExtraction(fluid, data, context.player(), containerMenu);
            }

            containerMenu.broadcastChanges();
        });
    }

    private static void handleItemExtraction(final AsteroidResource.ItemEntry item, final TryExtractRocketStorageMessage data, final Player player, final RocketStorageViewerContainerMenu containerMenu) {
        final ResourceHandler<ItemResource> source = containerMenu.getItemHandler();
        if (data.handlerIndex() >= source.size()) {
            return;
        }

        final ItemResource currentResource = source.getResource(data.handlerIndex());
        if (currentResource.isEmpty() || !currentResource.equals(item.resource())) {
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

    private static void handleFluidExtraction(final AsteroidResource.FluidEntry fluid, final TryExtractRocketStorageMessage data, final Player player, final RocketStorageViewerContainerMenu containerMenu) {
        final ResourceHandler<FluidResource> source = containerMenu.getFluidHandler();
        if (data.handlerIndex() >= source.size()) {
            return;
        }

        final FluidResource currentResource = source.getResource(data.handlerIndex());
        if (currentResource.isEmpty() || !currentResource.equals(fluid.resource())) {
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
