package com.ultramega.asteroidmining.network;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.asteroids.AsteroidConfig;
import com.ultramega.asteroidmining.events.AsteroidReloadListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AsteroidDataPayload(int syncId, int chunkIndex, int chunkCount, List<AsteroidConfig> asteroids) implements CustomPacketPayload {
    public static final Type<AsteroidDataPayload> TYPE = new Type<>(AsteroidMining.makeId("asteroid_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AsteroidDataPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, AsteroidDataPayload::syncId,
        ByteBufCodecs.VAR_INT, AsteroidDataPayload::chunkIndex,
        ByteBufCodecs.VAR_INT, AsteroidDataPayload::chunkCount,
        AsteroidConfig.LIST_STREAM_CODEC, AsteroidDataPayload::asteroids,
        AsteroidDataPayload::new
    );

    private static final int MAX_ASTEROIDS_PER_PACKET = 512;
    private static final AtomicInteger NEXT_SYNC_ID = new AtomicInteger();

    public static void handle(final AsteroidDataPayload data, final IPayloadContext context) {
        context.enqueueWork(() ->
            AsteroidReloadListener.INSTANCE.acceptAsteroidDataChunk(data.syncId(), data.chunkIndex(), data.chunkCount(), data.asteroids()));
    }

    public static void sendToAllPlayers(final Map<Identifier, AsteroidConfig> data) {
        final int syncId = NEXT_SYNC_ID.incrementAndGet();
        for (final AsteroidDataPayload payload : AsteroidDataPayload.createChunks(syncId, data)) {
            PacketDistributor.sendToAllPlayers(payload);
        }
    }

    public static void sendToPlayer(final ServerPlayer player, final Map<Identifier, AsteroidConfig> data) {
        final int syncId = NEXT_SYNC_ID.incrementAndGet();
        for (final AsteroidDataPayload payload : AsteroidDataPayload.createChunks(syncId, data)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public static List<AsteroidDataPayload> createChunks(final int syncId, final Map<Identifier, AsteroidConfig> data) {
        final List<AsteroidConfig> values = List.copyOf(data.values());
        if (values.isEmpty()) {
            return List.of(new AsteroidDataPayload(syncId, 0, 1, List.of()));
        }

        final int chunkCount = (values.size() + MAX_ASTEROIDS_PER_PACKET - 1) / MAX_ASTEROIDS_PER_PACKET;
        final List<AsteroidDataPayload> chunks = new ArrayList<>(chunkCount);
        for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
            final int fromIndex = chunkIndex * MAX_ASTEROIDS_PER_PACKET;
            final int toIndex = Math.min(values.size(), fromIndex + MAX_ASTEROIDS_PER_PACKET);
            chunks.add(new AsteroidDataPayload(syncId, chunkIndex, chunkCount, List.copyOf(values.subList(fromIndex, toIndex))));
        }
        return chunks;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
