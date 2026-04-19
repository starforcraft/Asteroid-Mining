package com.ultramega.asteroidmining.blockentities;

import com.ultramega.asteroidmining.container.RocketStorageViewerContainerMenu;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class RocketStorageViewerBlockEntity extends AbstractModuleBlockEntity implements MenuProvider, Nameable {
//    public final UnlimitedItemFluidStackHandler itemFluidHandler = new UnlimitedItemFluidStackHandler(this) {
//        @Override
//        protected void onContentsChanged() {
//            RocketStorageViewerBlockEntity.this.setChanged();
//        }
//    };

    public RocketStorageViewerBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntityTypes.ROCKET_STORAGE_VIEWER.get(), pos, blockState);
    }

    @Override
    public Component getName() {
        return Component.translatable(ModBlocks.ROCKET_STORAGE_VIEWER.get().getDescriptionId());
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Override
    public void setChanged() {
        super.setChanged();

        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        final var menu = new RocketStorageViewerContainerMenu(containerId, inventory, this, ContainerLevelAccess.create(this.level, this.getBlockPos()));
        menu.setOverwriteStillValid(this.overwriteStillValid);
        return menu;
    }
}
