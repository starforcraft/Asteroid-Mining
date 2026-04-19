package com.ultramega.asteroidmining.blocks;

import com.ultramega.asteroidmining.blockentities.AirAbsorberBlockEntity;
import com.ultramega.asteroidmining.registry.ModBlockEntityTypes;
import com.ultramega.asteroidmining.utils.Utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class AirAbsorberBlock extends AbstractDataPreservingBlock implements EntityBlock {
    public AirAbsorberBlock(final Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state,
                                               final Level level,
                                               final BlockPos pos,
                                               final Player player,
                                               final BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof AirAbsorberBlockEntity blockEntity) {
            if (!level.isClientSide()) {
                player.openMenu(blockEntity, pos);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, @Nullable final LivingEntity placer, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.getBlockEntity(pos) instanceof AirAbsorberBlockEntity blockEntity) {
            blockEntity.neighborChanged();
        }
    }

    @Override
    protected void neighborChanged(final BlockState state,
                                   final Level level,
                                   final BlockPos pos,
                                   final Block block,
                                   @Nullable final Orientation orientation,
                                   final boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);

        if (level.getBlockEntity(pos) instanceof AirAbsorberBlockEntity blockEntity) {
            blockEntity.neighborChanged();
        }
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new AirAbsorberBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> blockEntityType) {
        return !level.isClientSide() ? Utils.createTickerHelper(
            blockEntityType, ModBlockEntityTypes.AIR_ABSORBER.get(), AirAbsorberBlockEntity::serverTick) : null;
    }
}
