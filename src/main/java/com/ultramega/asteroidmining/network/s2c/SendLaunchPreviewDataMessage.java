package com.ultramega.asteroidmining.network.s2c;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.events.ClientEvents;
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

public record SendLaunchPreviewDataMessage(BlockPos pos, UUID uuid, List<PreviewInfo> previewInfos) implements CustomPacketPayload {
    public static final Type<SendLaunchPreviewDataMessage> TYPE = new Type<>(AsteroidMining.makeId("send_launch_preview_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SendLaunchPreviewDataMessage> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SendLaunchPreviewDataMessage::pos,
        UUIDUtil.STREAM_CODEC, SendLaunchPreviewDataMessage::uuid,
        PreviewInfo.STREAM_CODEC.apply(ByteBufCodecs.list()), SendLaunchPreviewDataMessage::previewInfos,
        SendLaunchPreviewDataMessage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SendLaunchPreviewDataMessage data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientEvents.LAUNCH_PAD_BUILDER_POS.put(data.pos(), data.uuid());
            ClientEvents.LAUNCH_PAD_PREVIEW_BLOCKS.put(data.pos(), data.previewInfos());
        });
    }
}
