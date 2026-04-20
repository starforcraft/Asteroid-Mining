package com.ultramega.asteroidmining.gui.renderer;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public class OrbitRenderState implements GuiElementRenderState {
    private final Matrix3x2f pose;
    private final float[] points;
    private final int color;
    private final ScreenRectangle bounds;
    @Nullable
    private final ScreenRectangle scissorArea;

    public OrbitRenderState(final Matrix3x2f pose,
                            final float[] points,
                            final int color,
                            final ScreenRectangle bounds,
                            @Nullable final ScreenRectangle scissorArea) {
        this.pose = pose;
        this.points = points;
        this.color = color;
        this.bounds = bounds;
        this.scissorArea = scissorArea;
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
        return TextureSetup.noTexture();
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }

    @Override
    public void buildVertices(final VertexConsumer consumer) {
        final int a = (this.color >>> 24) & 0xFF;
        final int r = (this.color >>> 16) & 0xFF;
        final int g = (this.color >>> 8) & 0xFF;
        final int b = this.color & 0xFF;

        for (int i = 0; i < this.points.length; i += 2) {
            consumer.addVertexWith2DPose(this.pose, this.points[i], this.points[i + 1])
                .setColor(r, g, b, a);
        }
    }
}
