package com.ultramega.asteroidmining.blocks;

import com.ultramega.asteroidmining.blockentities.AbstractDataPreservingBlockEntity;
import com.ultramega.asteroidmining.utils.PreserveData;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public abstract class AbstractDataPreservingBlock extends Block {
    public AbstractDataPreservingBlock(final Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, @Nullable final LivingEntity placer, final ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof AbstractDataPreservingBlockEntity blockEntity && blockEntity instanceof PreserveData) {
            blockEntity.setPlacedBy(stack);
        }
    }

    @Override
    public void playerDestroy(final Level level, final Player player, final BlockPos pos, final BlockState state, final BlockEntity blockEntity, final ItemStack tool) {
        if (blockEntity instanceof AbstractDataPreservingBlockEntity preservingBlockEntity && blockEntity instanceof PreserveData) {
            final ItemStack stack = preservingBlockEntity.storeToStack(new ItemStack(this));
            popResource(level, pos, stack);
        } else {
            super.playerDestroy(level, player, pos, state, blockEntity, tool);
        }
    }
}
