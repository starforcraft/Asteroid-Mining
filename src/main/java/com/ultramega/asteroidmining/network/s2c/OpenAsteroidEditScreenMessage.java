package com.ultramega.asteroidmining.network.s2c;

import com.ultramega.asteroidmining.AsteroidMining;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenAsteroidEditScreenMessage() implements CustomPacketPayload {
    public static final Type<OpenAsteroidEditScreenMessage> TYPE = new Type<>(AsteroidMining.makeId("open_asteroid_edit_screen"));
    public static final StreamCodec<ByteBuf, OpenAsteroidEditScreenMessage> STREAM_CODEC = StreamCodec.unit(new OpenAsteroidEditScreenMessage());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final OpenAsteroidEditScreenMessage data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            //Minecraft.getInstance().setScreen(new AsteroidListScreen());
        });
    }
}
