package com.ultramega.asteroidmining.blockentities;

import com.ultramega.asteroidmining.blocks.RocketEngineBlock;
import com.ultramega.asteroidmining.registry.ModSounds;
import com.ultramega.asteroidmining.utils.CameraHandler;
import com.ultramega.asteroidmining.utils.MovingSoundInstance;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

public class RocketEngineBlockEntity extends BlockEntity {
    @Nullable
    public UUID shakeUUID;
    @OnlyIn(Dist.CLIENT) //TODO: remove this shit
    @Nullable
    public MovingSoundInstance rocketEngineSound;

    private final RocketEngineBlock.Type type;

    public RocketEngineBlockEntity(final RocketEngineBlock.Type type, final BlockPos pos, final BlockState blockState) {
        super(type.getBlockEntity().get(), pos, blockState);
        this.type = type;
    }

    public static void clientTick(final Level level, final BlockPos pos, final BlockState state, final RocketEngineBlockEntity blockEntity) {
        if (!blockEntity.getBlockState().getValue(RocketEngineBlock.RUNNING)) {
            return;
        }
        if (blockEntity.shakeUUID == null) {
            blockEntity.shakeUUID = UUID.randomUUID();
        }

        final Vec3 centerPos = new Vec3(pos.getX(), pos.getY(), pos.getZ()).add(0.5D, 0, 0.5D);

        CameraHandler.addScreenShake(blockEntity.shakeUUID, centerPos, 5f, 70);

        final SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        if (blockEntity.rocketEngineSound == null || !soundManager.isActive(blockEntity.rocketEngineSound)) {
            final MovingSoundInstance instance = new MovingSoundInstance(ModSounds.ROCKET_START.value(),
                SoundSource.BLOCKS, 1.0f, 1.0f,
                blockEntity, centerPos,
                level.getRandom().nextLong());
            soundManager.play(instance);
            blockEntity.rocketEngineSound = instance;
        } else {
            blockEntity.rocketEngineSound.setSourcePos(centerPos);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();

        if (this.level != null && this.level.isClientSide() && this.shakeUUID != null) {
            CameraHandler.removeScreenShake(this.shakeUUID);
        }
    }

    public RocketEngineBlock.Type getEngineType() {
        return type;
    }
}
