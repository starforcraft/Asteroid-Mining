package com.ultramega.asteroidmining.container;

import com.ultramega.asteroidmining.blockentities.BiogasPlantBlockEntity;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.registry.ModMenuTypes;

import java.util.Objects;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public class BiogasPlantContainerMenu extends AbstractContainerMenu {
    public final BiogasPlantBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public BiogasPlantContainerMenu(final int containerId, final Inventory playerInv, final FriendlyByteBuf data) {
        this(containerId, playerInv, getBlockEntity(playerInv, data), ContainerLevelAccess.NULL, new SimpleContainerData(3));
    }

    public BiogasPlantContainerMenu(final int containerId,
                                    final Inventory playerInv,
                                    final BiogasPlantBlockEntity blockEntity,
                                    final ContainerLevelAccess access,
                                    final ContainerData data) {
        super(ModMenuTypes.BIOGAS_PLANT.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = access;
        this.data = data;

        this.addSlot(new ResourceHandlerSlot(blockEntity.inventoryHandler, blockEntity.inventoryHandler::set, 0, 62, 35) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                if (!stack.is(Tags.Items.FOODS)) {
                    return false;
                }
                final FoodProperties foodProperties = stack.get(DataComponents.FOOD);
                return foodProperties != null;
            }
        });

        this.addStandardInventorySlots(playerInv, 8, 84);

        this.addDataSlots(data);
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
        return AbstractContainerMenu.stillValid(this.access, player, ModBlocks.BIOGAS_PLANT.get());
    }

    private static BiogasPlantBlockEntity getBlockEntity(final Inventory playerInventory, final FriendlyByteBuf data) {
        Objects.requireNonNull(playerInventory, "playerInventory cannot be null!");
        Objects.requireNonNull(data, "data cannot be null!");
        final BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(data.readBlockPos());
        if (blockEntity instanceof BiogasPlantBlockEntity block) {
            return block;
        }
        throw new IllegalStateException("Block entityType is not correct! " + blockEntity);
    }

    public int getEnergyStored() {
        return this.data.get(0);
    }

    public int getMaxEnergyStored() {
        return this.data.get(1);
    }

    public int getRecipeProgress() {
        return this.data.get(2);
    }
}
