package com.ultramega.asteroidmining.utils;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public record LaunchError(LaunchErrors type,
                          @Nullable BlockPos pos,
                          @Nullable BlockState expectedState,
                          @Nullable BlockState actualState,
                          List<BlockPos> relatedPositions) {
    public LaunchError {
        relatedPositions = List.copyOf(relatedPositions);
    }

    public static LaunchError simple(final LaunchErrors type) {
        return new LaunchError(type, null, null, null, List.of());
    }

    public static LaunchError at(final LaunchErrors type,
                                 final BlockPos pos,
                                 @Nullable final BlockState expectedState,
                                 @Nullable final BlockState actualState) {
        return new LaunchError(type, pos, expectedState, actualState, List.of());
    }

    public static LaunchError positions(final LaunchErrors type, final List<BlockPos> relatedPositions) {
        return new LaunchError(type, null, null, null, relatedPositions);
    }

    public enum LaunchErrors {
        WRONG_BLOCK,
        UNMOVABLE_BLOCK,
        NO_DESTINATION_SELECTED,
        ROCKET_HAS_AIR_GAP,
        NOT_ENOUGH_THRUST_FORCE, // or TOO_HEAVY
        NOT_ENOUGH_FUEL,
        MISSING_ENGINE,
        MISSING_ITEM_STORAGE_OR_FLUID_TANK,
        BLOCKS_ABOVE_LAUNCH_PAD
    }
}
