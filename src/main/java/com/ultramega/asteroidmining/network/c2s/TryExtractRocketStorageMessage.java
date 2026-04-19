package com.ultramega.asteroidmining.network.c2s;

import com.ultramega.asteroidmining.AsteroidMining;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TryExtractRocketStorageMessage(int slotIndex, int amount, boolean shiftDown) implements CustomPacketPayload {
    public static final Type<TryExtractRocketStorageMessage> TYPE = new Type<>(AsteroidMining.makeId("try_carry_rocket_storage"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TryExtractRocketStorageMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, TryExtractRocketStorageMessage::slotIndex,
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
//            if (context.player().containerMenu instanceof RocketStorageViewerContainerMenu containerMenu) {
//                final ItemFluidStack itemFluidStack = containerMenu.getInventoryHandler().getItemFluidStackInSlot(data.slotIndex());
//                if (itemFluidStack.getItemStack() != null) {
//                    // Item Extraction
//                    ItemStack stack;
//                    if (data.shiftDown()) {
//                        stack = containerMenu.getInventoryHandler().extractItem(data.slotIndex(), data.amount(), true);
//                        final ItemStack quickMovedStack = containerMenu.quickMoveStackFromInventory(stack);
//
//                        if (!quickMovedStack.isEmpty()) {
//                            containerMenu.getInventoryHandler().extractItem(data.slotIndex(), data.amount(), false);
//                        }
//                    } else {
//                        stack = containerMenu.getCarried();
//
//                        if (stack.isEmpty()) {
//                            stack = containerMenu.getInventoryHandler().extractItem(data.slotIndex(), data.amount(), false);
//                            containerMenu.setCarried(stack);
//                        }
//                    }
//                } else if (itemFluidStack.getFluidStack() != null) {
//                    // Fluid Extraction
//                    final FluidStack toDrainStack = new FluidStack(itemFluidStack.getFluidStack().getFluid(), data.amount());
//                    final Inventory playerInventory = context.player().getInventory();
//                    for (int i = 0; i < playerInventory.getContainerSize(); i++) {
//                        final ItemStack stack = playerInventory.getItem(i);
//
//                        if (!stack.isEmpty() && stack.is(Items.BUCKET)) {
//                            final FluidStack fluidStack = containerMenu.getInventoryHandler().drain(toDrainStack, IFluidHandler.FluidAction.SIMULATE);
//                            if (!fluidStack.isEmpty()) {
//                                final IFluidHandlerItem cap = stack.copyWithCount(1).getCapability(Capabilities.FluidHandler.ITEM);
//                                if (cap != null) {
//                                    playerInventory.removeItem(i, 1);
//
//                                    containerMenu.getInventoryHandler().drain(toDrainStack, IFluidHandler.FluidAction.EXECUTE);
//                                    cap.fill(fluidStack, IFluidHandler.FluidAction.EXECUTE);
//                                    final ItemStack resultStack = cap.getContainer();
//                                    if (!playerInventory.add(resultStack)) {
//                                        playerInventory.player.drop(resultStack, false);
//                                    }
//
//                                    break;
//                                }
//                            }
//                        }
//                    }
//                }
//
//                containerMenu.broadcastChanges();
//            }
        });
    }
}
