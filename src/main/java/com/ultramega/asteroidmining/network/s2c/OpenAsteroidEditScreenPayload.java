package com.ultramega.asteroidmining.network.s2c;

import com.ultramega.asteroidmining.AsteroidMining;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenAsteroidEditScreenPayload() implements CustomPacketPayload {
    public static final Type<OpenAsteroidEditScreenPayload> TYPE = new Type<>(AsteroidMining.makeId("open_asteroid_edit_screen"));
    public static final StreamCodec<ByteBuf, OpenAsteroidEditScreenPayload> STREAM_CODEC = StreamCodec.unit(new OpenAsteroidEditScreenPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final OpenAsteroidEditScreenPayload data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            //Minecraft.getInstance().setScreen(new AsteroidListScreen());
        });
    }
}
