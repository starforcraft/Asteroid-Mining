package com.ultramega.asteroidmining.network.s2c;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.events.PreviewClientEvents;
import com.ultramega.asteroidmining.utils.PreviewInfo;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SendLaunchPreviewDataPayload(BlockPos pos, UUID uuid, List<PreviewInfo> previewInfos) implements CustomPacketPayload {
    public static final Type<SendLaunchPreviewDataPayload> TYPE = new Type<>(AsteroidMining.makeId("send_launch_preview_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SendLaunchPreviewDataPayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SendLaunchPreviewDataPayload::pos,
        UUIDUtil.STREAM_CODEC, SendLaunchPreviewDataPayload::uuid,
        PreviewInfo.STREAM_CODEC.apply(ByteBufCodecs.list()), SendLaunchPreviewDataPayload::previewInfos,
        SendLaunchPreviewDataPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SendLaunchPreviewDataPayload data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            PreviewClientEvents.LAUNCH_PAD_BUILDER_POS.put(data.pos(), data.uuid());
            PreviewClientEvents.LAUNCH_PAD_PREVIEW_BLOCKS.put(data.pos(), data.previewInfos());
        });
    }
}
