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

public record AsteroidDataMessage(Map<Identifier, AsteroidConfig> data) implements CustomPacketPayload {
    public static final Type<AsteroidDataMessage> TYPE = new Type<>(AsteroidMining.makeId("asteroid_data"));
    public static final StreamCodec<ByteBuf, AsteroidDataMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.fromCodec(Codec.unboundedMap(Identifier.CODEC, AsteroidConfig.CODEC)), AsteroidDataMessage::data,
        AsteroidDataMessage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final AsteroidDataMessage data, final IPayloadContext context) {
        context.enqueueWork(() ->
            AsteroidReloadListener.INSTANCE.setData(data.data)
        );
    }
}
