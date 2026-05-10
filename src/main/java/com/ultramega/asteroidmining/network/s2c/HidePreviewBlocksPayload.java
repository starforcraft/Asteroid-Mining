package com.ultramega.asteroidmining.network.s2c;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.events.PreviewClientEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HidePreviewBlocksPayload(BlockPos pos, boolean shouldHide) implements CustomPacketPayload {
    public static final Type<HidePreviewBlocksPayload> TYPE = new Type<>(AsteroidMining.makeId("hide_preview_blocks"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HidePreviewBlocksPayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, HidePreviewBlocksPayload::pos,
        ByteBufCodecs.BOOL, HidePreviewBlocksPayload::shouldHide,
        HidePreviewBlocksPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final HidePreviewBlocksPayload data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (data.shouldHide()) {
                PreviewClientEvents.HIDE_PREVIEW_BLOCKS.add(data.pos());
            } else {
                PreviewClientEvents.HIDE_PREVIEW_BLOCKS.remove(data.pos());
            }
        });
    }
}
