package com.ultramega.asteroidmining.network.c2s;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.blockentities.RocketControllerBlockEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LaunchRocketMessage(BlockPos controllerPos) implements CustomPacketPayload {
    public static final Type<LaunchRocketMessage> TYPE = new Type<>(AsteroidMining.makeId("launch_rocket"));
    public static final StreamCodec<ByteBuf, LaunchRocketMessage> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, LaunchRocketMessage::controllerPos,
        LaunchRocketMessage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final LaunchRocketMessage data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(data.controllerPos()) instanceof RocketControllerBlockEntity blockEntity) {
                blockEntity.setLaunchingRocket(true);
            }
        });
    }
}
