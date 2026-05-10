package com.ultramega.asteroidmining.network.c2s;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.blockentities.RocketControllerBlockEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LaunchRocketPayload(BlockPos controllerPos) implements CustomPacketPayload {
    public static final Type<LaunchRocketPayload> TYPE = new Type<>(AsteroidMining.makeId("launch_rocket"));
    public static final StreamCodec<ByteBuf, LaunchRocketPayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, LaunchRocketPayload::controllerPos,
        LaunchRocketPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final LaunchRocketPayload data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(data.controllerPos()) instanceof RocketControllerBlockEntity blockEntity) {
                blockEntity.setLaunchingRocket(true);
            }
        });
    }
}
