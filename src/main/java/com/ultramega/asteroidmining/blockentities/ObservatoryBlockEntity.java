package com.ultramega.asteroidmining.blockentities;

import com.ultramega.asteroidmining.container.ObservatoryContainerMenu;
import com.ultramega.asteroidmining.registry.ModBlockEntityTypes;
import com.ultramega.asteroidmining.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class ObservatoryBlockEntity extends AbstractModuleBlockEntity implements MenuProvider, Nameable {
    public ObservatoryBlockEntity(final BlockPos pos,
                                  final BlockState blockState) {
        super(ModBlockEntityTypes.SMALL_OBSERVATORY.get(), pos, blockState);
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Override
    public Component getName() {
        return Component.translatable(ModBlocks.SMALL_OBSERVATORY.get().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        if (this.level == null) {
            return null;
        }
        final var menu = new ObservatoryContainerMenu(containerId, inventory, this, ContainerLevelAccess.create(this.level, this.getBlockPos()));
        menu.setOverwriteStillValid(this.overwriteStillValid);
        return menu;
    }
}
