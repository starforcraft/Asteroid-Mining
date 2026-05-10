package com.ultramega.asteroidmining.network.c2s;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.container.AbstractSideConfigContainerMenu;
import com.ultramega.asteroidmining.utils.sides.SideConfigType;
import com.ultramega.asteroidmining.utils.sides.SideIoMode;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetSideConfigPayload(BlockPos pos, SideConfigType configType, Direction side, SideIoMode mode) implements CustomPacketPayload {
    public static final Type<SetSideConfigPayload> TYPE = new Type<>(AsteroidMining.makeId("set_side_config"));
    public static final StreamCodec<FriendlyByteBuf, SetSideConfigPayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SetSideConfigPayload::pos,
        NeoForgeStreamCodecs.enumCodec(SideConfigType.class), SetSideConfigPayload::configType,
        Direction.STREAM_CODEC, SetSideConfigPayload::side,
        NeoForgeStreamCodecs.enumCodec(SideIoMode.class), SetSideConfigPayload::mode,
        SetSideConfigPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SetSideConfigPayload data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            if (!(player.containerMenu instanceof AbstractSideConfigContainerMenu<?> menu)) {
                return;
            }

            if (!menu.blockEntity.getBlockPos().equals(data.pos())) {
                return;
            }

            menu.blockEntity.setSideConfig(data.configType(), data.side(), data.mode());
        });
    }
}
