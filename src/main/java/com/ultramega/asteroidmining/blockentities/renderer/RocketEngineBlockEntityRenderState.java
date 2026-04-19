package com.ultramega.asteroidmining.blockentities.renderer;

import com.ultramega.asteroidmining.blocks.RocketEngineBlock;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class RocketEngineBlockEntityRenderState extends BlockEntityRenderState {
    public boolean running;
    public RocketEngineBlock.Type type;
    public float partialTicks;
    public float animationTime;
    public long seed;
}
