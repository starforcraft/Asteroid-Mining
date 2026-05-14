package com.ultramega.asteroidmining.container;

import com.ultramega.asteroidmining.blockentities.DistillationColumnBlockEntity;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.registry.ModMenuTypes;

import java.util.Objects;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public class DistillationColumnContainerMenu extends AbstractSideConfigContainerMenu<DistillationColumnBlockEntity> {
    private static final int MACHINE_DATA_COUNT = DistillationColumnBlockEntity.MACHINE_DATA_COUNT;

    public final DistillationColumnBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public DistillationColumnContainerMenu(final int containerId, final Inventory playerInv, final FriendlyByteBuf data) {
        this(containerId, playerInv, getBlockEntity(playerInv, data), ContainerLevelAccess.NULL,
            AbstractSideConfigContainerMenu.createClientData(MACHINE_DATA_COUNT), new ItemStacksResourceHandler(1));
    }

    public DistillationColumnContainerMenu(final int containerId,
                                           final Inventory playerInv,
                                           final DistillationColumnBlockEntity blockEntity,
                                           final ContainerLevelAccess access,
                                           final ContainerData data) {
        this(containerId, playerInv, blockEntity, access, data, blockEntity.inventoryHandler);
    }

    public DistillationColumnContainerMenu(final int containerId,
                                           final Inventory playerInv,
                                           final DistillationColumnBlockEntity blockEntity,
                                           final ContainerLevelAccess access,
                                           final ContainerData data,
                                           final ItemStacksResourceHandler slotHandler) {
        super(ModMenuTypes.DISTILLATION_COLUMN.get(), containerId, playerInv, blockEntity, access, data, MACHINE_DATA_COUNT);

        this.blockEntity = blockEntity;
        this.access = access;
        this.data = data;

        this.addSlot(new ResourceHandlerSlot(slotHandler, slotHandler::set, 0, 56, 58));

        this.addStandardInventorySlots(playerInv, 8, 84);

        this.addSideConfigDataSlots();
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
        return AbstractContainerMenu.stillValid(this.access, player, ModBlocks.DISTILLATION_COLUMN.get());
    }

    private static DistillationColumnBlockEntity getBlockEntity(final Inventory playerInventory, final FriendlyByteBuf data) {
        Objects.requireNonNull(playerInventory, "playerInventory cannot be null!");
        Objects.requireNonNull(data, "data cannot be null!");
        final BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(data.readBlockPos());
        if (blockEntity instanceof DistillationColumnBlockEntity block) {
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

    public int getLitTime() {
        return this.data.get(3);
    }

    public float getLitDuration() {
        int duration = this.data.get(4);
        if (duration == 0) {
            duration = DistillationColumnBlockEntity.RECIPE_DURATION;
        }

        return Mth.clamp((float) this.getLitTime() / (float) duration, 0.0F, 1.0F);
    }

    public int getCoolingTime() {
        return this.data.get(5);
    }

    public float getCoolingDuration() {
        int duration = this.data.get(6);
        if (duration == 0) {
            duration = DistillationColumnBlockEntity.RECIPE_DURATION;
        }

        return Mth.clamp((float) this.getCoolingTime() / (float) duration, 0.0F, 1.0F);
    }

    public int getTemperature() {
        return this.data.get(7);
    }
}
