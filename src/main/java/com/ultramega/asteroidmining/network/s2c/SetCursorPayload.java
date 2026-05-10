package com.ultramega.asteroidmining.network.s2c;

import com.ultramega.asteroidmining.AsteroidMining;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.lwjgl.glfw.GLFW;

public record SetCursorPayload(int cursorX, int cursorY) implements CustomPacketPayload {
    public static final Type<SetCursorPayload> TYPE = new Type<>(AsteroidMining.makeId("set_cursor"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetCursorPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, SetCursorPayload::cursorX,
        ByteBufCodecs.INT, SetCursorPayload::cursorY,
        SetCursorPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SetCursorPayload data, final IPayloadContext context) {
        context.enqueueWork(() ->
            GLFW.glfwSetCursorPos(Minecraft.getInstance().getWindow().handle(), data.cursorX(), data.cursorY())
        );
    }
}
