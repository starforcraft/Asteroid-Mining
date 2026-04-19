package com.ultramega.asteroidmining.utils;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.neoforged.neoforge.client.model.pipeline.VertexConsumerWrapper;

public class AlphaColorWrapper extends VertexConsumerWrapper {
    private static final int RED_TINT_MASK = 0x99FF0000;
    private static final int DEFAULT_ALPHA_MASK = 0x99FFFFFF;

    private final boolean redTint;

    public AlphaColorWrapper(final VertexConsumer consumer, final boolean redTint) {
        super(consumer);
        this.redTint = redTint;
    }

    @Override
    public VertexConsumer setColor(final int color) {
        super.setColor(color & (this.redTint ? RED_TINT_MASK : DEFAULT_ALPHA_MASK));
        return this;
    }
}
