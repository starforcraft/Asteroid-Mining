package com.ultramega.asteroidmining.blocks;

import com.ultramega.asteroidmining.blockentities.BoundingBoxBlockEntity;

import java.util.function.BiConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

// TODO: breaking with a pickaxe in survival is completely broken since porting
public class BoundingBoxBlock extends Block implements EntityBlock {
    public BoundingBoxBlock(final Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state,
                                               final Level level,
                                               final BlockPos pos,
                                               final Player player,
                                               final BlockHitResult hitResult) {
        final BlockPos mainPos = getMainBlockPos(level, pos);
        if (mainPos == null) {
            return InteractionResult.FAIL;
        }

        final BlockState mainState = level.getBlockState(mainPos);
        return mainState.useWithoutItem(level, player, hitResult.withPosition(mainPos));
    }

    @Override
    public boolean onDestroyedByPlayer(final BlockState state,
                                       final Level level,
                                       final BlockPos pos,
                                       final Player player,
                                       final ItemStack toolStack,
                                       final boolean willHarvest,
                                       final FluidState fluid) {
        if (willHarvest) {
            return true;
        }

        final BlockPos mainPos = getMainBlockPos(level, pos);
        if (mainPos != null) {
            final BlockState mainState = level.getBlockState(mainPos);
            if (!mainState.isAir()) {
                return mainState.onDestroyedByPlayer(level, mainPos, player, toolStack, false, mainState.getFluidState());
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, toolStack, false, fluid);
    }

    @Override
    public BlockState playerWillDestroy(final Level level, final BlockPos pos, final BlockState state, final Player player) {
        final BlockPos mainPos = getMainBlockPos(level, pos);
        if (mainPos != null) {
            final BlockState mainState = level.getBlockState(mainPos);
            if (!mainState.isAir()) {
                level.destroyBlock(mainPos, true, player);
                return state;
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onExplosionHit(final BlockState state,
                                  final ServerLevel level,
                                  final BlockPos pos,
                                  final Explosion explosion,
                                  final BiConsumer<ItemStack, BlockPos> onHit) {
        final BlockPos mainPos = getMainBlockPos(level, pos);
        if (mainPos == null) {
            super.onExplosionHit(state, level, pos, explosion, onHit);
        } else {
            level.getBlockState(mainPos).onExplosionHit(level, mainPos, explosion, onHit);
        }
    }

    @Override
    protected void spawnAfterBreak(final BlockState state, final ServerLevel level, final BlockPos pos, final ItemStack stack, final boolean dropExperience) {
        final BlockPos mainPos = getMainBlockPos(level, pos);
        if (mainPos != null) {
            final BlockState mainState = level.getBlockState(mainPos);
            if (!mainState.isAir()) {
                mainState.spawnAfterBreak(level, mainPos, stack, dropExperience);
            }
        }
        super.spawnAfterBreak(state, level, pos, stack, dropExperience);
    }

    @Override
    public void playerDestroy(final Level level,
                              final Player player,
                              final BlockPos pos,
                              final BlockState state,
                              @Nullable final BlockEntity blockEntity,
                              final ItemStack tool) {
        final BlockPos mainPos = getMainBlockPos(level, pos);
        if (mainPos != null) {
            final BlockState mainState = level.getBlockState(mainPos);
            mainState.getBlock().playerDestroy(level, player, mainPos, mainState, blockEntity, tool);
        } else {
            super.playerDestroy(level, player, pos, state, blockEntity, tool);
        }
        level.removeBlock(pos, false);
    }

    @Override
    protected void neighborChanged(final BlockState state,
                                   final Level level,
                                   final BlockPos pos,
                                   final Block block,
                                   @Nullable final Orientation orientation,
                                   final boolean movedByPiston) {
        final BlockPos mainPos = getMainBlockPos(level, pos);
        if (mainPos != null) {
            level.getBlockState(mainPos).handleNeighborChanged(level, mainPos, state.getBlock(), orientation, movedByPiston);
        }
    }

    @Override
    protected float getDestroyProgress(final BlockState state,
                                       final Player player,
                                       final BlockGetter level,
                                       final BlockPos pos) {
        final BlockPos mainPos = getMainBlockPos(level, pos);
        if (mainPos == null) {
            return super.getDestroyProgress(state, player, level, pos);
        }
        return level.getBlockState(mainPos).getDestroyProgress(player, level, mainPos);
    }

    @Override
    public float getExplosionResistance(final BlockState state,
                                        final BlockGetter level,
                                        final BlockPos pos,
                                        final Explosion explosion) {
        final BlockPos mainPos = getMainBlockPos(level, pos);
        if (mainPos == null) {
            return super.getExplosionResistance(state, level, pos, explosion);
        }

        return level.getBlockState(mainPos).getExplosionResistance(level, mainPos, explosion);
    }

    @Override
    public ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state, final boolean includeData, final Player player) {
        final BlockPos mainPos = getMainBlockPos(level, pos);
        if (mainPos == null) {
            return ItemStack.EMPTY;
        }

        final BlockState mainState = level.getBlockState(mainPos);
        return mainState.getBlock().getCloneItemStack(level, mainPos, mainState, includeData, player);
    }

    @Nullable
    public static BlockPos getMainBlockPos(final BlockGetter level, final BlockPos pos) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BoundingBoxBlockEntity boundingBoxBlockEntity) {
            return boundingBoxBlockEntity.getMainBlockPos();
        }

        return null;
    }

    @Override
    protected float getShadeBrightness(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new BoundingBoxBlockEntity(pos, state);
    }
}
