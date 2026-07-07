package com.ultramega.asteroidmining.asteroids;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record OrbitData(String centralBodyName,
                        float semiMajorAxis,
                        float semiMinorAxis,
                        float orbitalSpeed,
                        boolean clockwise,
                        float startingAngleDegrees,
                        boolean orbitVisible,
                        boolean rotateAroundItself) {
    public static final OrbitData DEFAULT = new OrbitData(
        "",
        0.0F,
        0.0F,
        0.0F,
        true,
        0.0F,
        false,
        false
    );

    public static final StreamCodec<ByteBuf, OrbitData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(512), OrbitData::centralBodyName,
        ByteBufCodecs.FLOAT, OrbitData::semiMajorAxis,
        ByteBufCodecs.FLOAT, OrbitData::semiMinorAxis,
        ByteBufCodecs.FLOAT, OrbitData::orbitalSpeed,
        ByteBufCodecs.BOOL, OrbitData::clockwise,
        ByteBufCodecs.FLOAT, OrbitData::startingAngleDegrees,
        ByteBufCodecs.BOOL, OrbitData::orbitVisible,
        ByteBufCodecs.BOOL, OrbitData::rotateAroundItself,
        OrbitData::new
    );

    public static final Codec<OrbitData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.optionalFieldOf("centralBodyName", "").forGetter(OrbitData::centralBodyName),
        Codec.FLOAT.optionalFieldOf("semiMajorAxis", 0.0F).forGetter(OrbitData::semiMajorAxis),
        Codec.FLOAT.optionalFieldOf("semiMinorAxis", 0.0F).forGetter(OrbitData::semiMinorAxis),
        Codec.FLOAT.optionalFieldOf("orbitalSpeed", 0.0F).forGetter(OrbitData::orbitalSpeed),
        Codec.BOOL.optionalFieldOf("isClockwise", true).forGetter(OrbitData::clockwise),
        Codec.FLOAT.optionalFieldOf("startingAngleDegrees", 0.0F).forGetter(OrbitData::startingAngleDegrees),
        Codec.BOOL.optionalFieldOf("isOrbitVisible", false).forGetter(OrbitData::orbitVisible),
        Codec.BOOL.optionalFieldOf("rotateAroundItself", false).forGetter(OrbitData::rotateAroundItself)
    ).apply(instance, OrbitData::new));

    public OrbitData withSemiMinorAxis(final float value) {
        return new OrbitData(
            this.centralBodyName,
            this.semiMajorAxis,
            value,
            this.orbitalSpeed,
            this.clockwise,
            this.startingAngleDegrees,
            this.orbitVisible,
            this.rotateAroundItself
        );
    }

    public OrbitData withRotateAroundItself(final boolean value) {
        return new OrbitData(
            this.centralBodyName,
            this.semiMajorAxis,
            this.semiMinorAxis,
            this.orbitalSpeed,
            this.clockwise,
            this.startingAngleDegrees,
            this.orbitVisible,
            value
        );
    }
}
