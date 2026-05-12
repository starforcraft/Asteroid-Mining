package com.ultramega.asteroidmining.blocks;

import com.ultramega.asteroidmining.blockentities.RocketControllerBlockEntity;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

public abstract class AbstractModuleBlock extends AbstractDataPreservingBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active"); //TODO: make gui gray if not active for all blocks

    public AbstractModuleBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, final LivingEntity placer, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.getBlockEntity(pos) instanceof RocketControllerBlockEntity blockEntity) {
            blockEntity.updateConnectedModules();
        }

        if (state.getBlock() instanceof RocketControllerBlock) {
            return;
        }

        this.updateConnectedModules(level, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos, final boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);

        if (state.getBlock() instanceof RocketControllerBlock) {
            return;
        }

        this.updateConnectedModules(level, pos);
    }

    private void updateConnectedModules(final Level level, final BlockPos pos) {
        for (final Direction direction : Direction.values()) {
            final BlockPos adjacentPos = pos.relative(direction);
            final BlockState adjacentState = level.getBlockState(adjacentPos);
            final Block block = adjacentState.getBlock();

            if (block instanceof RocketControllerBlock) {
                final BlockEntity entity = level.getBlockEntity(adjacentPos);
                if (entity instanceof RocketControllerBlockEntity controller) {
                    controller.updateConnectedModules();
                }
                return;
            }

            if (block instanceof AbstractModuleBlock moduleBlock) {
                final BlockPos controllerPos = moduleBlock.searchForController(level, adjacentPos, pos, new HashSet<>());
                if (controllerPos != null) {
                    final BlockEntity entity = level.getBlockEntity(controllerPos);
                    if (entity instanceof RocketControllerBlockEntity controller) {
                        controller.updateConnectedModules();
                    }
                }
            }
        }
    }

    @Nullable
    public BlockPos searchForController(final Level level, final BlockPos pos, final BlockPos ignorePos, final Set<BlockPos> visited) {
        if (!visited.add(pos)) {
            return null;
        }

        for (final Direction direction : Direction.values()) {
            final BlockPos adjacentPos = pos.relative(direction);
            if (adjacentPos.equals(ignorePos)) {
                continue;
            }

            final BlockState adjacentState = level.getBlockState(adjacentPos);
            final Block block = adjacentState.getBlock();

            if (block instanceof RocketControllerBlock) {
                return level.getBlockEntity(adjacentPos) instanceof RocketControllerBlockEntity ? adjacentPos : null;
            }

            if (block instanceof AbstractModuleBlock moduleBlock) {
                final BlockPos controllerPos = moduleBlock.searchForController(level, adjacentPos, pos, visited);
                if (controllerPos != null) {
                    return controllerPos;
                }
            }
        }

        return null;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }
}
