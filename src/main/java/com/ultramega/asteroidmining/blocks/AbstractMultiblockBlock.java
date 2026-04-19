package com.ultramega.asteroidmining.blocks;

import com.ultramega.asteroidmining.blockentities.BoundingBoxBlockEntity;
import com.ultramega.asteroidmining.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

//TODO: custom bounding boxes
public class AbstractMultiblockBlock extends Block {
    private final int width;
    private final int height;

    public AbstractMultiblockBlock(final Properties properties, final int width, final int height) {
        super(properties);
        this.width = width;
        this.height = height;
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, @Nullable final LivingEntity placer, final ItemStack stack) {
        //TODO: when looking at the bottom of a block move everything down
        for (int dx = 0; dx < this.width; dx++) {
            for (int dz = 0; dz < this.width; dz++) {
                for (int dy = 0; dy < this.height; dy++) {
                    if (dx == 0 && dz == 0 && dy == 0) {
                        continue;
                    }

                    final BlockPos boundingBoxPos = pos.offset(-dx, dy, -dz);
                    //TODO: check if air before replacing block (and if it isn't air cancel the placement)
                    level.setBlock(boundingBoxPos, ModBlocks.BOUNDING_BOX.get().defaultBlockState(), Block.UPDATE_ALL);
                    final BlockEntity blockEntity = level.getBlockEntity(boundingBoxPos);
                    if (blockEntity instanceof BoundingBoxBlockEntity boundingBoxBlockEntity) {
                        boundingBoxBlockEntity.setMainBlockPos(pos);
                    }
                }
            }
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos, final boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);

        for (int dx = 0; dx < this.width; dx++) {
            for (int dz = 0; dz < this.width; dz++) {
                for (int dy = 0; dy < this.height; dy++) {
                    if (dx == 0 && dz == 0 && dy == 0) {
                        continue;
                    }

                    final BlockPos boundingBoxPos = pos.offset(-dx, dy, -dz);
                    if (level.getBlockState(boundingBoxPos).is(ModBlocks.BOUNDING_BOX.get())) {
                        level.setBlock(boundingBoxPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }
}
