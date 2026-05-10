package com.ultramega.asteroidmining.network.s2c;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.storage.ClientConfigurationSavedData;
import com.ultramega.asteroidmining.storage.NetworkConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateClientConfigurationDataPayload(Map<UUID, NetworkConfiguration> entries) implements CustomPacketPayload {
    public static final Type<UpdateClientConfigurationDataPayload> TYPE = new Type<>(AsteroidMining.makeId("update_client_configuration_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateClientConfigurationDataPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, NetworkConfiguration.STREAM_CODEC), UpdateClientConfigurationDataPayload::entries,
        UpdateClientConfigurationDataPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final UpdateClientConfigurationDataPayload data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientConfigurationSavedData.INSTANCE.set(data.entries());
        });
    }
}
