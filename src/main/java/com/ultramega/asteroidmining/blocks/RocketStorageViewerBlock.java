package com.ultramega.asteroidmining.blocks;

import com.ultramega.asteroidmining.blockentities.RocketStorageViewerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class RocketStorageViewerBlock extends AbstractModuleBlock implements EntityBlock {
    public RocketStorageViewerBlock(final Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof RocketStorageViewerBlockEntity blockEntity) {
            if (!level.isClientSide()) {
                player.openMenu(blockEntity, pos);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new RocketStorageViewerBlockEntity(pos, state);
    }

    // TODO: test if stuff is inside dropped and this isn't needed
//    @Override
//    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean isMoving) {
//        Containers.dropContentsOnDestroy(state, newState, level, pos);
//        super.onRemove(state, level, pos, newState, isMoving);
//    }
}
