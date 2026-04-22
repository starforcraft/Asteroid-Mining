package com.ultramega.asteroidmining.blockentities;

import com.ultramega.asteroidmining.blocks.RocketEngineBlock;
import com.ultramega.asteroidmining.registry.ModSounds;
import com.ultramega.asteroidmining.utils.CameraHandler;
import com.ultramega.asteroidmining.utils.MovingSoundInstance;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class RocketEngineBlockEntityClient {
    private static final Map<BlockPos, UUID> SHAKES = new WeakHashMap<>();
    private static final Map<BlockPos, MovingSoundInstance> SOUNDS = new WeakHashMap<>();

    private RocketEngineBlockEntityClient() {
    }

    public static void clientTick(final Level level, final BlockPos pos, final BlockState state, final RocketEngineBlockEntity blockEntity) {
        if (!level.isClientSide()) {
            return;
        }

        if (!state.getValue(RocketEngineBlock.RUNNING)) {
            stopEffects(blockEntity.getBlockPos());
            return;
        }

        final UUID shakeUUID = SHAKES.computeIfAbsent(blockEntity.getBlockPos(), _ -> UUID.randomUUID());
        final Vec3 centerPos = new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);

        CameraHandler.addScreenShake(shakeUUID, centerPos, 5f, 70);

        final SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        final MovingSoundInstance current = SOUNDS.get(blockEntity.getBlockPos());

        // TODO: too many rocket engines next to each other break this. So just make a single rocket engine sound for all and just increase the volume then
        if (current == null || !soundManager.isActive(current)) {
            final MovingSoundInstance instance = new MovingSoundInstance(
                ModSounds.ROCKET_START.value(),
                SoundSource.BLOCKS,
                1.0f,
                1.0f,
                blockEntity,
                centerPos,
                level.getRandom().nextLong()
            );
            soundManager.play(instance);
            SOUNDS.put(blockEntity.getBlockPos(), instance);
        } else {
            current.setSourcePos(centerPos);
        }
    }

    private static void stopEffects(final BlockPos pos) {
        final UUID shakeUUID = SHAKES.remove(pos);
        if (shakeUUID != null) {
            CameraHandler.removeScreenShake(shakeUUID);
        }
        SOUNDS.remove(pos);
    }
}
