package com.ultramega.asteroidmining.registry;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;

import static com.ultramega.asteroidmining.AsteroidMining.makeId;

public final class ModRenderPipelines {
    public static final RenderPipeline ORBIT_LINES = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
        .withLocation(makeId("orbit"))
        .withVertexShader("core/position_color")
        .withFragmentShader("core/position_color")
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINE_STRIP)
        .build();

    public static final RenderPipeline ROCKET_FLAME = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
        .withLocation(makeId("pipeline/rocket_flame"))
        .withVertexShader("core/rendertype_lightning")
        .withFragmentShader("core/rendertype_lightning")
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .withCull(false)
        .build();

    private ModRenderPipelines() {
    }
}
