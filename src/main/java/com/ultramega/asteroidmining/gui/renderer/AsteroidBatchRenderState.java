package com.ultramega.asteroidmining.gui.renderer;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/**
 * Renders multiple GUI-atlas sprites in one GuiElementRenderState.
 *
 * <p>All quads in one instance must use the same atlas texture.</p>
 */
public final class AsteroidBatchRenderState implements GuiElementRenderState {
    private final Matrix3x2f pose;
    private final TextureSetup textureSetup;
    private final Quad[] quads;
    private final int quadCount;
    private final ScreenRectangle bounds;

    @Nullable
    private final ScreenRectangle scissorArea;

    public AsteroidBatchRenderState(
        final Matrix3x2f pose,
        final TextureSetup textureSetup,
        final Quad[] quads,
        final int quadCount,
        @Nullable final ScreenRectangle scissorArea
    ) {
        if (quadCount <= 0 || quadCount > quads.length) {
            throw new IllegalArgumentException(
                "Invalid quad count " + quadCount
                    + " for array length " + quads.length
            );
        }

        this.pose = pose;
        this.textureSetup = textureSetup;
        this.quads = quads;
        this.quadCount = quadCount;
        this.scissorArea = scissorArea;
        this.bounds = calculateBounds(quads, quadCount);
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
        for (int i = 0; i < this.quadCount; i++) {
            this.emitQuad(consumer, this.quads[i]);
        }
    }

    private void emitQuad(
        final VertexConsumer consumer,
        final Quad quad
    ) {
        final float halfSize = quad.size() * 0.5F;
        final float centerX = quad.x() + halfSize;
        final float centerY = quad.y() + halfSize;

        final float rotation = quad.rotationRadians();
        final float cos;
        final float sin;

        if (rotation == 0.0F) {
            cos = 1.0F;
            sin = 0.0F;
        } else {
            cos = (float) Math.cos(rotation);
            sin = (float) Math.sin(rotation);
        }

        final float topLeftX =
            centerX + rotateX(-halfSize, -halfSize, cos, sin);
        final float topLeftY =
            centerY + rotateY(-halfSize, -halfSize, cos, sin);

        final float bottomLeftX =
            centerX + rotateX(-halfSize, halfSize, cos, sin);
        final float bottomLeftY =
            centerY + rotateY(-halfSize, halfSize, cos, sin);

        final float bottomRightX =
            centerX + rotateX(halfSize, halfSize, cos, sin);
        final float bottomRightY =
            centerY + rotateY(halfSize, halfSize, cos, sin);

        final float topRightX =
            centerX + rotateX(halfSize, -halfSize, cos, sin);
        final float topRightY =
            centerY + rotateY(halfSize, -halfSize, cos, sin);

        final int color = quad.color();
        final int alpha = color >>> 24 & 0xFF;
        final int red = color >>> 16 & 0xFF;
        final int green = color >>> 8 & 0xFF;
        final int blue = color & 0xFF;

        consumer.addVertexWith2DPose(
            this.pose,
            topLeftX,
            topLeftY
        ).setUv(
            quad.u0(),
            quad.v0()
        ).setColor(
            red,
            green,
            blue,
            alpha
        );

        consumer.addVertexWith2DPose(
            this.pose,
            bottomLeftX,
            bottomLeftY
        ).setUv(
            quad.u0(),
            quad.v1()
        ).setColor(
            red,
            green,
            blue,
            alpha
        );

        consumer.addVertexWith2DPose(
            this.pose,
            bottomRightX,
            bottomRightY
        ).setUv(
            quad.u1(),
            quad.v1()
        ).setColor(
            red,
            green,
            blue,
            alpha
        );

        consumer.addVertexWith2DPose(
            this.pose,
            topRightX,
            topRightY
        ).setUv(
            quad.u1(),
            quad.v0()
        ).setColor(
            red,
            green,
            blue,
            alpha
        );
    }

    private static float rotateX(
        final float x,
        final float y,
        final float cos,
        final float sin
    ) {
        return x * cos - y * sin;
    }

    private static float rotateY(
        final float x,
        final float y,
        final float cos,
        final float sin
    ) {
        return x * sin + y * cos;
    }

    private static ScreenRectangle calculateBounds(
        final Quad[] quads,
        final int quadCount
    ) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < quadCount; i++) {
            final Quad quad = quads[i];
            final float halfSize = quad.size() * 0.5F;
            final float centerX = quad.x() + halfSize;
            final float centerY = quad.y() + halfSize;

            /*
             * Axis-aligned half extent of a rotated square.
             *
             * extent = halfSize * (abs(cos) + abs(sin))
             */
            final float rotation = quad.rotationRadians();
            final float extent;

            if (rotation == 0.0F) {
                extent = halfSize;
            } else {
                final float cos = Math.abs((float) Math.cos(rotation));
                final float sin = Math.abs((float) Math.sin(rotation));
                extent = halfSize * (cos + sin);
            }

            minX = Math.min(minX, centerX - extent);
            minY = Math.min(minY, centerY - extent);
            maxX = Math.max(maxX, centerX + extent);
            maxY = Math.max(maxY, centerY + extent);
        }

        final int left = (int) Math.floor(minX);
        final int top = (int) Math.floor(minY);
        final int right = (int) Math.ceil(maxX);
        final int bottom = (int) Math.ceil(maxY);

        return new ScreenRectangle(
            left,
            top,
            Math.max(1, right - left),
            Math.max(1, bottom - top)
        );
    }

    /**
     * One textured GUI quad.
     *
     * @param x top-left X before rotation
     * @param y top-left Y before rotation
     * @param size width and height
     * @param rotationRadians rotation around the quad center
     * @param u0 minimum atlas U
     * @param v0 minimum atlas V
     * @param u1 maximum atlas U
     * @param v1 maximum atlas V
     * @param color ARGB color
     */
    public record Quad(
        float x,
        float y,
        float size,
        float rotationRadians,
        float u0,
        float v0,
        float u1,
        float v1,
        int color
    ) {
    }
}
