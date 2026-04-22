package com.ultramega.asteroidmining.gui.renderer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;

public final class LaunchPadPreviewRenderState {
    public final List<Entry> previewBlocks = new ArrayList<>();
    public final Set<BlockPos> rocketPositions = new HashSet<>();

    public static void submitPreviewBlocks(final LaunchPadPreviewRenderState state,
                                           final PoseStack poseStack,
                                           final SubmitNodeCollector collector,
                                           final BlockPos origin) {
        for (final LaunchPadPreviewRenderState.Entry entry : state.previewBlocks) {
            poseStack.pushPose();

            final BlockPos relativePos = entry.pos().subtract(origin);
            poseStack.translate(relativePos.getX(), relativePos.getY(), relativePos.getZ());

            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(1.005F, 1.005F, 1.005F);
            poseStack.translate(-0.5, -0.5, -0.5);

            // TODO: make it translucent (waiting for camol or cable facades for code), also fix it getting dark
            collector.submitMovingBlock(poseStack, entry.movingRenderState());

            poseStack.popPose();
        }
    }

    public record Entry(BlockPos pos,
                        boolean occupied,
                        MovingBlockRenderState movingRenderState) {
    }
}
