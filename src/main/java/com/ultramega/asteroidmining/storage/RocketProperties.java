package com.ultramega.asteroidmining.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record RocketProperties(int weight, int trustForce, int fuelUsage) {
    public static final Codec<RocketProperties> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.INT.fieldOf("weight").forGetter(RocketProperties::weight),
            Codec.INT.fieldOf("trustForce").forGetter(RocketProperties::trustForce),
            Codec.INT.fieldOf("fuelUsage").forGetter(RocketProperties::fuelUsage)
        ).apply(instance, RocketProperties::new)
    );

    public static final StreamCodec<ByteBuf, RocketProperties> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, RocketProperties::weight,
        ByteBufCodecs.INT, RocketProperties::trustForce,
        ByteBufCodecs.INT, RocketProperties::fuelUsage,
        RocketProperties::new
    );
}
