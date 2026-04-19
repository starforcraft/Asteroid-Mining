package com.ultramega.asteroidmining.gui.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import guideme.color.LightDarkMode;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public class ScenePictureInPictureRenderer extends PictureInPictureRenderer<ScenePictureInPictureRenderer.State> {
    public ScenePictureInPictureRenderer(final MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected void renderToTexture(final State state, final PoseStack pose) {
        state.renderer.render(state.lightDarkMode, pose, this.bufferSource);
    }

    @Override
    protected String getTextureLabel() {
        return "Asteroid Mining game scene";
    }

    //TODO: some of these variables aren't being used at all, delete?
    public record State(
        LightDarkMode lightDarkMode,
        Matrix3x2f pose,
        int x0, int y0,
        int x1, int y1,
        ScreenRectangle bounds,
        @Nullable ScreenRectangle scissorArea,
        Renderer renderer) implements PictureInPictureRenderState {
        @Override
        public float scale() {
            return 1;
        }
    }

    @FunctionalInterface
    public interface Renderer {
        void render(LightDarkMode lightDarkMode, PoseStack pose, MultiBufferSource.BufferSource buffers);
    }
}
