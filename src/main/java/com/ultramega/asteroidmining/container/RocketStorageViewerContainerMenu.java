package com.ultramega.asteroidmining.container;

import com.ultramega.asteroidmining.blockentities.RocketStorageViewerBlockEntity;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.registry.ModMenuTypes;

import java.util.Objects;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

public class RocketStorageViewerContainerMenu extends AbstractModuleContainerMenu {
    private final RocketStorageViewerBlockEntity blockEntity;

    public RocketStorageViewerContainerMenu(final int containerId, final Inventory playerInv, final FriendlyByteBuf data) {
        this(containerId, playerInv, getBlockEntity(playerInv, data), ContainerLevelAccess.NULL);
    }

    public RocketStorageViewerContainerMenu(final int containerId,
                                            final Inventory playerInv,
                                            final RocketStorageViewerBlockEntity blockEntity,
                                            final ContainerLevelAccess access) {
        super(ModMenuTypes.ROCKET_STORAGE_VIEWER.get(), containerId, playerInv, blockEntity, access, ModBlocks.ROCKET_STORAGE_VIEWER.get());
        this.blockEntity = blockEntity;

        this.addStandardInventorySlots(playerInv, 8, 144);
    }

    public ItemStack quickMoveStackFromInventory(final ItemStack stack) {
        if (!this.moveItemStackTo(stack.copy(), 0, 36, true)) {
            return ItemStack.EMPTY;
        }

        return stack.copy();
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack quickMovedStack = ItemStack.EMPTY;
        final Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            final ItemStack rawStack = slot.getItem();
            quickMovedStack = rawStack.copy();

            if (index >= 0 && index < 36) {
                if (index < 27) {
                    if (!this.moveItemStackTo(rawStack, 27, 36, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(rawStack, 0, 27, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (rawStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (rawStack.getCount() == quickMovedStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, rawStack);
        }

        return quickMovedStack;
    }

    private static RocketStorageViewerBlockEntity getBlockEntity(final Inventory playerInventory, final FriendlyByteBuf data) {
        Objects.requireNonNull(playerInventory, "playerInventory cannot be null!");
        Objects.requireNonNull(data, "data cannot be null!");
        final BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(data.readBlockPos());
        if (blockEntity instanceof RocketStorageViewerBlockEntity block) {
            return block;
        }
        throw new IllegalStateException("Block entityType is not correct! " + blockEntity);
    }

    @Override
    public RocketStorageViewerBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public ResourceHandler<ItemResource> getItemHandler() {
        return this.getBlockEntity().getItemHandler(null);
    }

    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.getBlockEntity().getFluidHandler(null);
    }
}
