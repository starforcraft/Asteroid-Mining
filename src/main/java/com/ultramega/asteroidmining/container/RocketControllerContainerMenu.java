package com.ultramega.asteroidmining.container;

import com.ultramega.asteroidmining.blockentities.RocketControllerBlockEntity;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.registry.ModMenuTypes;

import java.util.Objects;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RocketControllerContainerMenu extends AbstractModuleContainerMenu {
    private final RocketControllerBlockEntity blockEntity;

    public RocketControllerContainerMenu(final int containerId, final Inventory playerInv, final FriendlyByteBuf data) {
        this(containerId, playerInv, getBlockEntity(playerInv, data), ContainerLevelAccess.NULL);
    }

    public RocketControllerContainerMenu(final int containerId,
                                         final Inventory playerInv,
                                         final RocketControllerBlockEntity blockEntity,
                                         final ContainerLevelAccess access) {
        super(ModMenuTypes.ROCKET_CONTROLLER.get(), containerId, playerInv, blockEntity, access, ModBlocks.ROCKET_CONTROLLER.get());
        this.blockEntity = blockEntity;
        blockEntity.updateRocketStats();
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public RocketControllerBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    private static RocketControllerBlockEntity getBlockEntity(final Inventory playerInventory, final FriendlyByteBuf data) {
        Objects.requireNonNull(playerInventory, "playerInventory cannot be null!");
        Objects.requireNonNull(data, "data cannot be null!");
        final BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(data.readBlockPos());
        if (blockEntity instanceof RocketControllerBlockEntity block) {
            return block;
        }
        throw new IllegalStateException("Block entityType is not correct! " + blockEntity);
    }
}
