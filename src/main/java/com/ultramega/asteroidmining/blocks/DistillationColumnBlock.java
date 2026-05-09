package com.ultramega.asteroidmining.blocks;

import com.ultramega.asteroidmining.blockentities.DistillationColumnBlockEntity;
import com.ultramega.asteroidmining.registry.ModBlockEntityTypes;
import com.ultramega.asteroidmining.utils.CommonUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class DistillationColumnBlock extends AbstractMultiblockBlock implements EntityBlock {
    public DistillationColumnBlock(final Properties properties) {
        super(properties, 1, 2);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state,
                                               final Level level,
                                               final BlockPos pos,
                                               final Player player,
                                               final BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof DistillationColumnBlockEntity blockEntity) {
            if (!level.isClientSide()) {
                player.openMenu(blockEntity, pos);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new DistillationColumnBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> blockEntityType) {
        return !level.isClientSide() ? CommonUtils.createTickerHelper(
            blockEntityType, ModBlockEntityTypes.DISTILLATION_COLUMN.get(), DistillationColumnBlockEntity::serverTick) : null;
    }
}
