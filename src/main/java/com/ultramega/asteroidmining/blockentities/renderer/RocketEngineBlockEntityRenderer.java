package com.ultramega.asteroidmining.blockentities.renderer;

import com.ultramega.asteroidmining.blockentities.RocketEngineBlockEntity;
import com.ultramega.asteroidmining.blocks.RocketEngineBlock;
import com.ultramega.asteroidmining.utils.Utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public class RocketEngineBlockEntityRenderer implements BlockEntityRenderer<RocketEngineBlockEntity, RocketEngineBlockEntityRenderState> {
    private static final int[] YELLOW = {247, 223, 37};
    private static final int[] ORANGE = {255, 140, 0};
    private static final int[] BLUE = {42, 105, 209};

    public RocketEngineBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void extractRenderState(final RocketEngineBlockEntity blockEntity,
                                   final RocketEngineBlockEntityRenderState state,
                                   final float partialTicks,
                                   final Vec3 cameraPosition,
                                   final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

        state.running = blockEntity.getBlockState().getValue(RocketEngineBlock.RUNNING);
        state.type = blockEntity.getEngineType();
        state.partialTicks = partialTicks;
        state.animationTime = (blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0L) + partialTicks;
        state.seed = blockEntity.getBlockPos().asLong();
    }

    @Override
    public void submit(final RocketEngineBlockEntityRenderState blockRenderState,
                       final PoseStack poseStack,
                       final SubmitNodeCollector collector,
                       final CameraRenderState cameraRenderState) {
        //TODO: Update this exhaust effect once you start getting smarter in rendering or leave this to a smarter guy
        //TODO: also once someone makes better models for the rockets, move the exhaust effect into the valve
        if (!blockRenderState.running) {
            return;
        }

        collector.submitCustomGeometry(
            poseStack,
            RenderTypes.debugQuads(),
            (pose, vertexConsumer) -> buildFlames(blockRenderState, pose.pose(), vertexConsumer)
        );
    }

    private static void buildFlames(final RocketEngineBlockEntityRenderState state,
                                    final Matrix4f matrix,
                                    final VertexConsumer vertexConsumer) {
        final RocketEngineBlock.Type type = state.type;

        final int flameCount = 50;
        final int steps = 10;
        final float strayWidth = 0.03f;

        final float radius = type.getWidth() * 0.25f;
        final float offset = type.getWidth() == 2 ? 0.0f : 0.5f;

        for (int i = 0; i < flameCount; i++) {
            // Deterministic pseudo-random values based on block position + flame index,
            // so the flame is stable instead of sampling level.random during submit().
            final float randHeight = random01(state.seed, i, 0);
            final float randAngle = random01(state.seed, i, 1);

            // Optional subtle animation
            final float pulse = 0.08f * Mth.sin(state.animationTime * 0.35f + i * 0.6f);

            final float height = 2.2f
                + (randHeight - 0.5f) * 0.7f
                + pulse
                + (type.getHeight() - 1);

            final float startY = -height;

            final float angleRad = (i / (float) flameCount) * (Mth.TWO_PI);
            final float baseX = offset + Mth.cos(angleRad) * radius;
            final float baseZ = offset + Mth.sin(angleRad) * radius;

            final float rotationDeg = randAngle * 360.0f;
            final float rotationRad = rotationDeg * Mth.DEG_TO_RAD;

            final float cos = Mth.cos(rotationRad);
            final float sin = Mth.sin(rotationRad);

            for (int j = 0; j < steps; j++) {
                final float y1 = startY + (j / (float) steps) * height;
                final float y2 = startY + ((j + 1) / (float) steps) * height;

                final float t1 = (y1 - startY) / height;
                final float t2 = (y2 - startY) / height;

                final float widthFactor1 = Mth.sin(t1 * Mth.PI);
                final float widthFactor2 = Mth.sin(t2 * Mth.PI);

                final float w1 = strayWidth + 0.25f * widthFactor1;
                final float w2 = strayWidth + 0.25f * widthFactor2;

                final float midT1 = j / (float) steps;
                final float midT2 = (j + 1) / (float) steps;

                final int[] color1 = Utils.interpolateGradient(midT1, YELLOW, ORANGE, BLUE);
                final int[] color2 = Utils.interpolateGradient(midT2, YELLOW, ORANGE, BLUE);

                // Rotate the quad around its center on the Y axis.
                final float x1l = -w1 / 2.0f;
                final float x1r = w1 / 2.0f;
                final float x2l = -w2 / 2.0f;
                final float x2r = w2 / 2.0f;

                final float v1x1 = baseX + x1l * cos;
                final float v1z1 = baseZ - x1l * sin;

                final float v2x1 = baseX + x2l * cos;
                final float v2z1 = baseZ - x2l * sin;

                final float v2x2 = baseX + x2r * cos;
                final float v2z2 = baseZ - x2r * sin;

                final float v1x2 = baseX + x1r * cos;
                final float v1z2 = baseZ - x1r * sin;

                vertexConsumer.addVertex(matrix, v1x1, y1, v1z1)
                    .setColor(color1[0], color1[1], color1[2], 200)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(state.lightCoords);

                vertexConsumer.addVertex(matrix, v2x1, y2, v2z1)
                    .setColor(color2[0], color2[1], color2[2], 200)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(state.lightCoords);

                vertexConsumer.addVertex(matrix, v2x2, y2, v2z2)
                    .setColor(color2[0], color2[1], color2[2], 200)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(state.lightCoords);

                vertexConsumer.addVertex(matrix, v1x2, y1, v1z2)
                    .setColor(color1[0], color1[1], color1[2], 200)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(state.lightCoords);
            }
        }
    }

    //TODO: I don't like this at all
    private static float random01(final long seed, final int a, final int b) {
        long x = seed;
        x ^= 0x9E3779B97F4A7C15L * (a + 1L);
        x ^= 0xC2B2AE3D27D4EB4FL * (b + 1L);
        x ^= (x >>> 33);
        x *= 0xff51afd7ed558ccdL;
        x ^= (x >>> 33);
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= (x >>> 33);

        // Keep only positive bits and map to [0, 1).
        return (float) ((x >>> 40) & 0xFFFFFF) / (float) 0x1000000;
    }

    @Override
    public AABB getRenderBoundingBox(final RocketEngineBlockEntity blockEntity) {
        return BlockEntityRenderer.super.getRenderBoundingBox(blockEntity).inflate(0, 2.5f, 0);
    }

    @Override
    public RocketEngineBlockEntityRenderState createRenderState() {
        return new RocketEngineBlockEntityRenderState();
    }
}
