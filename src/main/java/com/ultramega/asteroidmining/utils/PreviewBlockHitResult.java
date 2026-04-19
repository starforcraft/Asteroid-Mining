package com.ultramega.asteroidmining.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PreviewBlockHitResult extends BlockHitResult {
    @Nullable
    private final PreviewInfo previewInfo;

    public PreviewBlockHitResult(final Vec3 location,
                                 final Direction direction,
                                 final BlockPos blockPos,
                                 final boolean inside,
                                 final boolean worldBorderHit,
                                 final PreviewInfo previewInfo) {
        this(false, location, direction, blockPos, inside, worldBorderHit, previewInfo);
    }

    public PreviewBlockHitResult(final boolean miss,
                                 final Vec3 location,
                                 final Direction direction,
                                 final BlockPos blockPos,
                                 final boolean inside,
                                 final boolean worldBorderHit,
                                 @Nullable final PreviewInfo previewInfo) {
        super(miss, location, direction, blockPos, worldBorderHit, inside);
        this.previewInfo = previewInfo;
    }

    @Nullable
    public PreviewInfo getPreviewInfo() {
        return this.previewInfo;
    }
}
