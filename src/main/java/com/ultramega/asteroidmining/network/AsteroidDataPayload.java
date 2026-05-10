package com.ultramega.asteroidmining.network;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.asteroids.AsteroidConfig;
import com.ultramega.asteroidmining.events.AsteroidReloadListener;

import java.util.Map;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AsteroidDataPayload(Map<Identifier, AsteroidConfig> data) implements CustomPacketPayload {
    public static final Type<AsteroidDataPayload> TYPE = new Type<>(AsteroidMining.makeId("asteroid_data"));
    public static final StreamCodec<ByteBuf, AsteroidDataPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.fromCodec(Codec.unboundedMap(Identifier.CODEC, AsteroidConfig.CODEC)), AsteroidDataPayload::data,
        AsteroidDataPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final AsteroidDataPayload data, final IPayloadContext context) {
        context.enqueueWork(() ->
            AsteroidReloadListener.INSTANCE.setData(data.data)
        );
    }
}
