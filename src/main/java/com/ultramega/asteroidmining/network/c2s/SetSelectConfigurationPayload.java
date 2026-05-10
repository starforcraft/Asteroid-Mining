package com.ultramega.asteroidmining.network.c2s;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.blockentities.RocketControllerBlockEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetSelectConfigurationPayload(BlockPos controllerPos, int selectedConfiguration) implements CustomPacketPayload {
    public static final Type<SetSelectConfigurationPayload> TYPE = new Type<>(AsteroidMining.makeId("set_select_configuration"));
    public static final StreamCodec<ByteBuf, SetSelectConfigurationPayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SetSelectConfigurationPayload::controllerPos,
        ByteBufCodecs.INT, SetSelectConfigurationPayload::selectedConfiguration,
        SetSelectConfigurationPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SetSelectConfigurationPayload data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(data.controllerPos()) instanceof RocketControllerBlockEntity blockEntity) {
                blockEntity.setSelectedConfigurationIndex(data.selectedConfiguration());
                blockEntity.inventoryHandler.triggerContentsChanged();
            }
        });
    }
}
