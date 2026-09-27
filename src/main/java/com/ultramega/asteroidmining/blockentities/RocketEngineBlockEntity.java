package com.ultramega.asteroidmining.blockentities;

import com.ultramega.asteroidmining.blocks.RocketEngineBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class RocketEngineBlockEntity extends BlockEntity {
    private final RocketEngineBlock.Type type;
    private float soundVolume = 1.0F;

    public RocketEngineBlockEntity(final RocketEngineBlock.Type type, final BlockPos pos, final BlockState blockState) {
        super(type.getBlockEntity().get(), pos, blockState);
        this.type = type;
    }

    public RocketEngineBlock.Type getEngineType() {
        return this.type;
    }

    public float getSoundVolume() {
        return this.soundVolume;
    }

    public void setSoundVolume(final float soundVolume) {
        this.soundVolume = soundVolume;
    }
}
