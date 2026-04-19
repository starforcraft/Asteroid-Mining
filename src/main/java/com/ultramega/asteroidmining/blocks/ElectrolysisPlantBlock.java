package com.ultramega.asteroidmining.blocks;

import com.ultramega.asteroidmining.blockentities.ElectrolysisPlantBlockEntity;
import com.ultramega.asteroidmining.registry.ModBlockEntityTypes;
import com.ultramega.asteroidmining.utils.Utils;

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

public class ElectrolysisPlantBlock extends AbstractDataPreservingBlock implements EntityBlock {
    public ElectrolysisPlantBlock(final Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state,
                                               final Level level,
                                               final BlockPos pos,
                                               final Player player,
                                               final BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof ElectrolysisPlantBlockEntity blockEntity) {
            if (!level.isClientSide()) {
                player.openMenu(blockEntity, pos);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new ElectrolysisPlantBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> blockEntityType) {
        return !level.isClientSide() ? Utils.createTickerHelper(
            blockEntityType, ModBlockEntityTypes.ELECTROLYSIS_PLANT.get(), ElectrolysisPlantBlockEntity::serverTick) : null;
    }
}
