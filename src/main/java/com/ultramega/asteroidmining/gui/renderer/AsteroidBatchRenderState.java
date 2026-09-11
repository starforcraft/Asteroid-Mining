package com.ultramega.asteroidmining.gui.renderer;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/** One immutable frame-owned primitive buffer instead of thousands of temporary Quad objects. */
public final class AsteroidBatchRenderState implements GuiElementRenderState {
    public static final int STRIDE = 10;
    private final Matrix3x2f pose;
    private final TextureSetup textureSetup;
    private final float[] quads;
    private final int quadCount;
    private final ScreenRectangle bounds;
    @Nullable
    private final ScreenRectangle scissorArea;

    public AsteroidBatchRenderState(final Matrix3x2f pose, final TextureSetup textureSetup,
                                    final float[] quads, final int quadCount,
                                    @Nullable final ScreenRectangle scissorArea) {
        if (quadCount <= 0 || quadCount > quads.length / STRIDE) {
            throw new IllegalArgumentException("Invalid asteroid quad count: " + quadCount);
        }
        this.pose = pose;
        this.textureSetup = textureSetup;
        this.quads = quads;
        this.quadCount = quadCount;
        this.scissorArea = scissorArea;
        this.bounds = this.calculateBounds();
    }

    public static void writeQuad(final float[] data,
                                 final int index,
                                 final float x,
                                 final float y,
                                 final float size,
                                 final float rotation,
                                 final float u0,
                                 final float v0,
                                 final float u1,
                                 final float v1,
                                 final float opacity) {
        final int offset = index * STRIDE;
        data[offset] = x + size * 0.5F;
        data[offset + 1] = y + size * 0.5F;
        data[offset + 2] = size * 0.5F;
        data[offset + 3] = rotation == 0.0F ? 1.0F : (float) Math.cos(rotation);
        data[offset + 4] = rotation == 0.0F ? 0.0F : (float) Math.sin(rotation);
        data[offset + 5] = u0;
        data[offset + 6] = v0;
        data[offset + 7] = u1;
        data[offset + 8] = v1;
        data[offset + 9] = Math.clamp(opacity, 0.0F, 1.0F);
    }

    @Override
    public ScreenRectangle bounds() {
        return this.bounds;
    }

    @Override
    public @Nullable ScreenRectangle scissorArea() {
        return this.scissorArea;
    }

    @Override
    public TextureSetup textureSetup() {
        return this.textureSetup;
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI_TEXTURED;
    }

    @Override
    public void buildVertices(final VertexConsumer consumer) {
        for (int i = 0; i < this.quadCount * STRIDE; i += STRIDE) {
            final float half = this.quads[i + 2];
            this.vertex(consumer, i, -half, -half, this.quads[i + 5], this.quads[i + 6]);
            this.vertex(consumer, i, -half, half, this.quads[i + 5], this.quads[i + 8]);
            this.vertex(consumer, i, half, half, this.quads[i + 7], this.quads[i + 8]);
            this.vertex(consumer, i, half, -half, this.quads[i + 7], this.quads[i + 6]);
        }
    }

    private void vertex(final VertexConsumer consumer, final int offset, final float x, final float y,
                        final float u, final float v) {
        final float cos = this.quads[offset + 3];
        final float sin = this.quads[offset + 4];
        consumer.addVertexWith2DPose(this.pose,
            this.quads[offset] + x * cos - y * sin,
            this.quads[offset + 1] + x * sin + y * cos).setUv(u, v).setColor(255, 255, 255, Math.round(this.quads[offset + 9] * 255.0F));
    }

    private ScreenRectangle calculateBounds() {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < this.quadCount * STRIDE; i += STRIDE) {
            final float extent = this.quads[i + 2] * (Math.abs(this.quads[i + 3]) + Math.abs(this.quads[i + 4]));
            // Include the GUI pose: bounds participate in GUI ordering as well as clipping.
            for (int corner = 0; corner < 4; corner++) {
                final float x = this.quads[i] + ((corner & 1) == 0 ? -extent : extent);
                final float y = this.quads[i + 1] + ((corner & 2) == 0 ? -extent : extent);
                final float transformedX = this.pose.m00() * x + this.pose.m10() * y + this.pose.m20();
                final float transformedY = this.pose.m01() * x + this.pose.m11() * y + this.pose.m21();
                minX = Math.min(minX, transformedX);
                minY = Math.min(minY, transformedY);
                maxX = Math.max(maxX, transformedX);
                maxY = Math.max(maxY, transformedY);
            }
        }
        final int left = (int) Math.floor(minX);
        final int top = (int) Math.floor(minY);
        return new ScreenRectangle(left, top, Math.max(1, (int) Math.ceil(maxX) - left),
            Math.max(1, (int) Math.ceil(maxY) - top));
    }
}
