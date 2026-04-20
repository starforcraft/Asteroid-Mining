package com.ultramega.asteroidmining.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class CameraHandler {
    public static float yawOffset;
    public static float pitchOffset;

    @Nullable
    public static Vec3 position;
    @Nullable
    public static Direction facing;

    private static final float MAX_TOTAL_INTENSITY = 6.0f;
    private static final List<ShakeSource> SOURCES = new ArrayList<>();

    private CameraHandler() {
    }

    public static void cameraTick(final Camera camera, final RandomSource random) {
        if (Minecraft.getInstance().isPaused()) {
            return;
        }

        // Set camera position
        if (position != null && facing != null) {
            camera.detached = true;
            camera.setPosition(position);
            camera.setRotation(facing.toYRot(), 45, 0);
        }

        if (SOURCES.isEmpty()) {
            return;
        }

        // Calculate intensity
        float totalIntensity = 0f;
        float maxPercentageToCenter = 0f;

        for (Iterator<ShakeSource> iterator = SOURCES.iterator(); iterator.hasNext();) {
            final ShakeSource source = iterator.next();
            totalIntensity += source.computeIntensity(camera.position());

            final float pct = source.getPercentageToCenter(camera.position());
            if (pct > maxPercentageToCenter) {
                maxPercentageToCenter = pct;
            }

            source.tick();
            if (source.isDead()) {
                iterator.remove();
            }
        }

        if (totalIntensity <= 0f) {
            return;
        }
        final float scaledMax = (float) Math.pow(maxPercentageToCenter, 3) * MAX_TOTAL_INTENSITY;
        totalIntensity = Math.min(totalIntensity, scaledMax);

        // Apply shake effect
        yawOffset = Utils.randomOffset(random, totalIntensity);
        pitchOffset = Utils.randomOffset(random, totalIntensity);
        camera.setRotation(camera.yRot() + yawOffset, camera.xRot() + pitchOffset, 0.0F);
    }

    public static void addScreenShake(@Nullable final UUID sourceId,
                                      @Nullable final Vec3 sourcePos,
                                      final float intensity,
                                      final boolean loseIntensity) {
        addOrReplaceShakeSource(sourceId, sourcePos, intensity, loseIntensity, 0);
    }

    public static void addScreenShake(@Nullable final UUID sourceId,
                                      @Nullable final Vec3 sourcePos,
                                      final float intensity,
                                      final int maxDistance) {
        addOrReplaceShakeSource(sourceId, sourcePos, intensity, false, maxDistance);
    }

    public static void removeScreenShake(final UUID sourceId) {
        SOURCES.stream()
            .filter(src -> src.sourceId != null && src.sourceId.equals(sourceId))
            .findFirst()
            .ifPresent(SOURCES::remove);
    }

    public static void removeScreenShake(final Vec3 sourcePos) {
        SOURCES.stream()
            .filter(src -> src.sourcePos != null && src.sourcePos.distanceToSqr(sourcePos) < 1e-6)
            .findFirst()
            .ifPresent(SOURCES::remove);
    }

    public static void clearScreenShakes() {
        SOURCES.clear();
    }

    private static void addOrReplaceShakeSource(@Nullable final UUID sourceId, @Nullable final Vec3 sourcePos,
                                                final float intensity, final boolean loseIntensity, final int maxDistance) {
        final ShakeSource newSource = new ShakeSource(sourceId, sourcePos, intensity, 3, intensity, loseIntensity, maxDistance);

        if (sourcePos != null) {
            final Optional<ShakeSource> existingShake = SOURCES.stream()
                .filter(src -> (src.sourceId != null && src.sourceId.equals(sourceId))
                    || (src.sourcePos != null && src.sourcePos.distanceToSqr(sourcePos) < 1e-6))
                .findFirst();

            if (existingShake.isPresent()) {
                final ShakeSource existingSource = existingShake.get();
                final boolean sameId = existingSource.sourceId != null && existingSource.sourceId.equals(sourceId);
                final boolean samePos = existingSource.sourcePos != null && existingSource.sourcePos.distanceToSqr(sourcePos) < 1e-6;

                if (!sameId || !samePos) {
                    SOURCES.remove(existingSource);
                    SOURCES.add(newSource);
                }
            } else {
                SOURCES.add(newSource);
            }
        } else {
            SOURCES.add(newSource);
        }
    }

    public static void setPosition(final BlockPos position, final Direction facing) {
        CameraHandler.position = position.getCenter();
        CameraHandler.facing = facing;
    }

    public static void resetPosition() {
        CameraHandler.position = null;
        CameraHandler.facing = null;
    }

    public static boolean isCameraOutsideOfPlayer() {
        return CameraHandler.position != null && CameraHandler.facing != null;
    }

    private static class ShakeSource {
        @Nullable
        public final UUID sourceId;
        @Nullable
        public final Vec3 sourcePos;
        public final float baseIntensity;
        public final float maxIntensity;
        public float currentIntensity;
        public final boolean loseIntensity;
        public final int maxDistance;

        ShakeSource(final @Nullable UUID sourceId,
                    final @Nullable Vec3 sourcePos,
                    final float baseIntensity,
                    final float maxIntensity,
                    final float currentIntensity,
                    final boolean loseIntensity,
                    final int maxDistance) {
            this.sourceId = sourceId;
            this.sourcePos = sourcePos;
            this.baseIntensity = baseIntensity;
            this.maxIntensity = maxIntensity;
            this.currentIntensity = currentIntensity;
            this.loseIntensity = loseIntensity;
            this.maxDistance = maxDistance;
        }

        public float computeIntensity(final Vec3 cameraPos) {
            if (this.currentIntensity <= 0.0f) {
                return 0.0f;
            }

            final float percentageToCenter = this.getPercentageToCenter(cameraPos);
            if (percentageToCenter <= 0f) {
                return 0f;
            }

            final float intensity = 0.4f * (float) Math.pow(percentageToCenter, 3) * this.currentIntensity;
            return Math.min(intensity, this.maxIntensity);
        }

        public float getPercentageToCenter(final Vec3 cameraPos) {
            if (this.maxDistance == 0 || this.sourcePos == null) {
                return 1f;
            }

            final double distance = this.sourcePos.distanceTo(cameraPos);
            if (distance > this.maxDistance) {
                return 0f;
            }

            return (float) (1 - (distance / this.maxDistance));
        }

        public void tick() {
            if (this.loseIntensity) {
                this.currentIntensity = Math.max(0f, this.currentIntensity - 0.05f);
            }
        }

        public boolean isDead() {
            return this.currentIntensity <= 0;
        }
    }
}
