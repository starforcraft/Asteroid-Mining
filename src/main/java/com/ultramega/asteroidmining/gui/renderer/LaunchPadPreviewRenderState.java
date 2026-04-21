package com.ultramega.asteroidmining.gui.renderer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

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

            // TODO: make it translucent (waiting for camol or cable facades for code)
//            collector.submitMovingBlock(poseStack, entry.movingRenderState());
            var minecraft = Minecraft.getInstance();
            boolean ambientOcclusion = minecraft.options.ambientOcclusion().get();
            var blockRenderer = new ModelBlockRenderer(ambientOcclusion, false, minecraft.getBlockColors());

            BlockQuadOutput quadOutput = (x, y, z, quad, instance) -> {
                var layer = quad.materialInfo().layer();
                if (layer.translucent() == translucent) {
                    var builder = buffers.getBuffer(getEntityRenderType(layer));
                    builder.putBakedQuad(poseStack.last(), quad, instance);
                }
            };

            blockRenderer.tesselateBlock(quadOutput, 0, 0, 0, level, pos, blockState, model, blockState.getSeed(pos));

            poseStack.popPose();
        }
    }

    private static RenderType getEntityRenderType(ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID, CUTOUT -> Sheets.cutoutBlockSheet();
            case TRANSLUCENT -> Sheets.translucentBlockSheet();
        };
    }


    public record Entry(BlockPos pos,
                        boolean occupied,
                        MovingBlockRenderState movingRenderState) {
    }
}
