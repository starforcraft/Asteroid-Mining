package com.ultramega.asteroidmining.blockentities.renderer;

import com.ultramega.asteroidmining.gui.renderer.LaunchPadPreviewRenderState;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class LaunchPadBuilderBlockEntityRenderState extends BlockEntityRenderState {
    public final LaunchPadPreviewRenderState previewState = new LaunchPadPreviewRenderState();
}
