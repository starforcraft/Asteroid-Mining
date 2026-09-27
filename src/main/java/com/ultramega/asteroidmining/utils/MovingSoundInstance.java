package com.ultramega.asteroidmining.utils;

import com.ultramega.asteroidmining.blockentities.RocketEngineBlockEntity;
import com.ultramega.asteroidmining.blocks.RocketEngineBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class MovingSoundInstance extends AbstractTickableSoundInstance {
    private static final int FADE_IN_TICKS = 10;

    private final RocketEngineBlockEntity blockEntity;
    private Vec3 sourcePos;
    private int age;

    public MovingSoundInstance(final SoundEvent soundEvent,
                               final SoundSource source,
                               final float volume,
                               final float pitch,
                               final RocketEngineBlockEntity blockEntity,
                               final Vec3 sourcePos,
                               final long seed) {
        super(soundEvent, source, RandomSource.create(seed));
        this.volume = volume / FADE_IN_TICKS * blockEntity.getSoundVolume();
        this.pitch = pitch;
        this.looping = true;
        this.delay = 0;
        this.blockEntity = blockEntity;
        this.sourcePos = sourcePos;

        this.x = (float) this.sourcePos.x();
        this.y = (float) this.sourcePos.y();
        this.z = (float) this.sourcePos.z();
    }

    @Override
    public void tick() {
        this.age = Math.min(this.age + 1, FADE_IN_TICKS);
        this.x = (float) this.sourcePos.x();
        this.y = (float) this.sourcePos.y();
        this.z = (float) this.sourcePos.z();

        if (Minecraft.getInstance().player != null) { //TODO: update calculation to make y less 'important'
            final double distance = Minecraft.getInstance().player.position().distanceTo(this.sourcePos);
            final double maxDistance = 64.0;
            final float fadeIn = (float) this.age / FADE_IN_TICKS;
            this.volume = distance > maxDistance ? 0.0f : 4.0f * (float) ((maxDistance - distance) / maxDistance) * fadeIn * this.blockEntity.getSoundVolume();
        }

        if (this.blockEntity.isRemoved() || !this.blockEntity.getBlockState().getValue(RocketEngineBlock.RUNNING)) {
            this.stop();
        }
    }

    public void setSourcePos(final Vec3 sourcePos) {
        this.sourcePos = sourcePos;
    }
}
