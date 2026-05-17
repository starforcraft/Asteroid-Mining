package com.ultramega.asteroidmining.blockentities.renderer;

import com.ultramega.asteroidmining.blockentities.RocketEngineBlockEntity;
import com.ultramega.asteroidmining.blocks.RocketEngineBlock;
import com.ultramega.asteroidmining.registry.ModRenderTypes;
import com.ultramega.asteroidmining.utils.CommonUtils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
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

    public RocketEngineBlockEntityRenderer() {
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

        collector.order(10).submitCustomGeometry(poseStack, ModRenderTypes.ROCKET_FLAME, (pose, vertexConsumer) ->
            buildFlames(blockRenderState, pose.pose(), vertexConsumer));
    }

    private static void buildFlames(final RocketEngineBlockEntityRenderState state,
                                    final Matrix4f matrix,
                                    final VertexConsumer vertexConsumer) {
        final RocketEngineBlock.Type type = state.type;

        final int flameCount = 50;
        final float strayWidth = 0.03f;

        final float radius = type.getWidth() * 0.25f;
        final float offset = type.getWidth() == 2 ? 0.0f : 0.5f;

        final long frameSeed = state.seed ^ Float.floatToIntBits(state.animationTime);

        for (int i = 0; i < flameCount; i++) {
            final float height = 2.2f + (random01(frameSeed, i, 0) - 0.5f) * 0.7f + (type.getHeight() - 1);
            final float startY = -height;

            final float angleRad = (i / (float) flameCount) * Mth.TWO_PI;
            final float baseX = offset + Mth.cos(angleRad) * radius;
            final float baseZ = offset + Mth.sin(angleRad) * radius;

            final float angle = random01(frameSeed, i, 1) * 360.0f;

            final Matrix4f flameMatrix = new Matrix4f(matrix);
            flameMatrix.translate(baseX, startY + height / 2.0f, baseZ);
            flameMatrix.rotateY(angle * Mth.DEG_TO_RAD);
            flameMatrix.translate(-baseX, -(startY + height / 2.0f), -baseZ);

            final int steps = 10;
            for (int j = 0; j < steps; j++) {
                final float y1 = startY + (j / (float) steps) * height;
                final float y2 = startY + ((j + 1) / (float) steps) * height;

                final float t1 = (y1 - startY) / height;
                final float t2 = (y2 - startY) / height;

                final float widthFactor1 = Mth.sin(t1 * Mth.PI);
                final float widthFactor2 = Mth.sin(t2 * Mth.PI);

                final float w1 = strayWidth + 0.25f * widthFactor1;
                final float w2 = strayWidth + 0.25f * widthFactor2;

                final float midT = j / (float) steps;
                final float midT2 = (j + 1) / (float) steps;

                final int[] color1 = CommonUtils.interpolateGradient(midT, YELLOW, ORANGE, BLUE);
                final int[] color2 = CommonUtils.interpolateGradient(midT2, YELLOW, ORANGE, BLUE);

                vertexConsumer.addVertex(flameMatrix, baseX - w1 / 2.0f, y1, baseZ)
                    .setColor(color1[0], color1[1], color1[2], 200)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(state.lightCoords);

                vertexConsumer.addVertex(flameMatrix, baseX - w2 / 2.0f, y2, baseZ)
                    .setColor(color2[0], color2[1], color2[2], 200)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(state.lightCoords);

                vertexConsumer.addVertex(flameMatrix, baseX + w2 / 2.0f, y2, baseZ)
                    .setColor(color2[0], color2[1], color2[2], 200)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(state.lightCoords);

                vertexConsumer.addVertex(flameMatrix, baseX + w1 / 2.0f, y1, baseZ)
                    .setColor(color1[0], color1[1], color1[2], 200)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(state.lightCoords);
            }
        }
    }

    private static float random01(final long seed, final int a, final int b) {
        long x = seed;
        x ^= 0x9E3779B97F4A7C15L * (a + 1L);
        x ^= 0xC2B2AE3D27D4EB4FL * (b + 1L);
        x ^= (x >>> 33);
        x *= 0xff51afd7ed558ccdL;
        x ^= (x >>> 33);
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= (x >>> 33);

        // Keep only positive bits and map to [0, 1)
        return (float) ((x >>> 40) & 0xFFFFFF) / (float) 0x1000000;
    }

    @Override
    public AABB getRenderBoundingBox(final RocketEngineBlockEntity blockEntity) {
        return BlockEntityRenderer.super.getRenderBoundingBox(blockEntity).inflate(0, 2.5f, 0);
    }

    @Override
    public boolean shouldRender(final RocketEngineBlockEntity blockEntity, final Vec3 cameraPosition) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public RocketEngineBlockEntityRenderState createRenderState() {
        return new RocketEngineBlockEntityRenderState();
    }
}
