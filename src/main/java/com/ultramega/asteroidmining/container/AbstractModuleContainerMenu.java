package com.ultramega.asteroidmining.container;

import com.ultramega.asteroidmining.blockentities.AbstractModuleBlockEntity;
import com.ultramega.asteroidmining.blockentities.RocketControllerBlockEntity;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;

public abstract class AbstractModuleContainerMenu extends AbstractContainerMenu {
    private final AbstractModuleBlockEntity blockEntity;
    private final Player player;
    private final ContainerLevelAccess access;
    private final Block block;

    private boolean overwriteStillValid;

    public AbstractModuleContainerMenu(final MenuType<?> menuType,
                                       final int containerId,
                                       final Inventory playerInv,
                                       final AbstractModuleBlockEntity blockEntity,
                                       final ContainerLevelAccess access,
                                       final Block block) {
        super(menuType, containerId);
        this.blockEntity = blockEntity;
        this.player = playerInv.player;
        this.access = access;
        this.block = block;
    }

    public Set<BlockPos> getConnectedModules() {
        if (this.blockEntity.getLevel() == null) {
            return Set.of();
        }

        if (this.blockEntity.getControllerPos() != null) {
            if (this.blockEntity.getLevel().getBlockEntity(this.blockEntity.getControllerPos()) instanceof RocketControllerBlockEntity controller) { // For modules
                return controller.getConnectedModules();
            }
        } else if (this.blockEntity.getLevel().getBlockEntity(this.blockEntity.getBlockPos()) instanceof RocketControllerBlockEntity controller) { // For controller
            return controller.getConnectedModules();
        }

        return Set.of();
    }

    @Override
    public boolean stillValid(final Player player) {
        return AbstractContainerMenu.stillValid(this.access, player, this.block) || this.overwriteStillValid;
    }

    public AbstractModuleBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public Player getPlayer() {
        return this.player;
    }

    public void setOverwriteStillValid(final boolean overwriteStillValid) {
        this.overwriteStillValid = overwriteStillValid;
    }
}
