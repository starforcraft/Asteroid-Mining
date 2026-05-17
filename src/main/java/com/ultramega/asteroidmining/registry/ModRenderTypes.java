package com.ultramega.asteroidmining.registry;

import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

import static com.ultramega.asteroidmining.AsteroidMining.MOD_ID;

public final class ModRenderTypes {
    public static final RenderType ROCKET_FLAME = RenderType.create(MOD_ID + "_rocket_flame",
        RenderSetup.builder(ModRenderPipelines.ROCKET_FLAME)
            .setOutputTarget(OutputTarget.WEATHER_TARGET)
            .sortOnUpload()
            .createRenderSetup());

    private ModRenderTypes() {
    }
}
