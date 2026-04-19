package com.ultramega.asteroidmining.network.s2c;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.events.ClientEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HidePreviewBlocksMessage(BlockPos pos, boolean shouldHide) implements CustomPacketPayload {
    public static final Type<HidePreviewBlocksMessage> TYPE = new Type<>(AsteroidMining.makeId("hide_preview_blocks"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HidePreviewBlocksMessage> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, HidePreviewBlocksMessage::pos,
        ByteBufCodecs.BOOL, HidePreviewBlocksMessage::shouldHide,
        HidePreviewBlocksMessage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final HidePreviewBlocksMessage data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (data.shouldHide()) {
                ClientEvents.HIDE_PREVIEW_BLOCKS.add(data.pos());
            } else {
                ClientEvents.HIDE_PREVIEW_BLOCKS.remove(data.pos());
            }
        });
    }
}
