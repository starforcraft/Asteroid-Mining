package com.ultramega.asteroidmining.gui.renderer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class LaunchPadPreviewRenderState {
    public final List<Entry> previewBlocks = new ArrayList<>();
    public final Set<BlockPos> rocketPositions = new HashSet<>();

    public record Entry(BlockPos pos, BlockState previewState, boolean occupied) {
    }
}
