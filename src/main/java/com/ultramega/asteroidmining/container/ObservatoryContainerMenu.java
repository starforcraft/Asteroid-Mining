package com.ultramega.asteroidmining.container;

import com.ultramega.asteroidmining.blockentities.ObservatoryBlockEntity;
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

public class ObservatoryContainerMenu extends AbstractModuleContainerMenu {
    private final ObservatoryBlockEntity blockEntity;

    public ObservatoryContainerMenu(final int containerId, final Inventory playerInv, final FriendlyByteBuf data) {
        this(containerId, playerInv, getBlockEntity(playerInv, data), ContainerLevelAccess.NULL);
    }

    public ObservatoryContainerMenu(final int containerId,
                                    final Inventory playerInv,
                                    final ObservatoryBlockEntity blockEntity,
                                    final ContainerLevelAccess access) {
        super(ModMenuTypes.SMALL_OBSERVATORY.get(), containerId, playerInv, blockEntity, access, ModBlocks.SMALL_OBSERVATORY.get());
        this.blockEntity = blockEntity;

        //TODO: decide if we want to delete the inventory (there's no use for it)
        this.addStandardInventorySlots(playerInv, 8, 100);
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

    @Override
    public ObservatoryBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    private static ObservatoryBlockEntity getBlockEntity(final Inventory playerInventory, final FriendlyByteBuf data) {
        Objects.requireNonNull(playerInventory, "playerInventory cannot be null!");
        Objects.requireNonNull(data, "data cannot be null!");
        final BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(data.readBlockPos());
        if (blockEntity instanceof ObservatoryBlockEntity block) {
            return block;
        }
        throw new IllegalStateException("Block entityType is not correct! " + blockEntity);
    }
}
