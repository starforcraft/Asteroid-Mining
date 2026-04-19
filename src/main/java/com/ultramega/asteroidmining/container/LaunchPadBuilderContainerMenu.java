package com.ultramega.asteroidmining.container;

import com.ultramega.asteroidmining.blockentities.LaunchPadBuilderBlockEntity;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.registry.ModItems;
import com.ultramega.asteroidmining.registry.ModMenuTypes;

import java.util.Objects;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public class LaunchPadBuilderContainerMenu extends AbstractContainerMenu {
    private final LaunchPadBuilderBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public LaunchPadBuilderContainerMenu(final int containerId, final Inventory playerInv, final FriendlyByteBuf data) {
        this(containerId, playerInv, getBlockEntity(playerInv, data), ContainerLevelAccess.NULL);
    }

    public LaunchPadBuilderContainerMenu(final int containerId,
                                         final Inventory playerInv,
                                         final LaunchPadBuilderBlockEntity blockEntity,
                                         final ContainerLevelAccess access) {
        super(ModMenuTypes.LAUNCH_PAD_BUILDER.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = access;

        this.addSlot(new ResourceHandlerSlot(blockEntity.inventoryHandler, blockEntity.inventoryHandler::set, 0, 80, 17) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return stack.getItem() == ModItems.CONFIGURATION_CARD.get();
            }
        });

        this.addStandardInventorySlots(playerInv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack quickMovedStack = ItemStack.EMPTY;
        final Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            final ItemStack rawStack = slot.getItem();
            quickMovedStack = rawStack.copy();

            if (index == 0) {
                if (!this.moveItemStackTo(rawStack, 1, 36, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(rawStack, quickMovedStack);
            } else if (index >= 1 && index < 37) {
                if (!this.moveItemStackTo(rawStack, 0, 1, false)) {
                    if (index < 28) {
                        if (!this.moveItemStackTo(rawStack, 28, 37, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(rawStack, 1, 28, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(rawStack, 1, 37, false)) {
                return ItemStack.EMPTY;
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
    public boolean stillValid(final Player player) {
        return AbstractContainerMenu.stillValid(this.access, player, ModBlocks.LAUNCH_PAD_BUILDER.get());
    }

    public LaunchPadBuilderBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    private static LaunchPadBuilderBlockEntity getBlockEntity(final Inventory playerInventory, final FriendlyByteBuf data) {
        Objects.requireNonNull(playerInventory, "playerInventory cannot be null!");
        Objects.requireNonNull(data, "data cannot be null!");
        final BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(data.readBlockPos());
        if (blockEntity instanceof LaunchPadBuilderBlockEntity block) {
            return block;
        }
        throw new IllegalStateException("Block entityType is not correct! " + blockEntity);
    }
}
