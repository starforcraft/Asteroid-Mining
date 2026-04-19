package com.ultramega.asteroidmining.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

public record LaunchPadConfiguration(String name, BlockPos mainPos, int width, int height, Direction facing) {
    public static final Codec<LaunchPadConfiguration> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("name").forGetter(LaunchPadConfiguration::name),
            BlockPos.CODEC.fieldOf("mainPos").forGetter(LaunchPadConfiguration::mainPos),
            ExtraCodecs.intRange(9, 21).fieldOf("width").forGetter(LaunchPadConfiguration::width),
            ExtraCodecs.intRange(9, 21).fieldOf("height").forGetter(LaunchPadConfiguration::height),
            Direction.CODEC.fieldOf("facing").forGetter(LaunchPadConfiguration::facing)
        ).apply(instance, LaunchPadConfiguration::new)
    );

    public static final StreamCodec<ByteBuf, LaunchPadConfiguration> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, LaunchPadConfiguration::name,
        BlockPos.STREAM_CODEC, LaunchPadConfiguration::mainPos,
        ByteBufCodecs.INT, LaunchPadConfiguration::width,
        ByteBufCodecs.INT, LaunchPadConfiguration::height,
        Direction.STREAM_CODEC, LaunchPadConfiguration::facing,
        LaunchPadConfiguration::new
    );
}
