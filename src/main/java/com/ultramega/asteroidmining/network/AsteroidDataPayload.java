package com.ultramega.asteroidmining.network;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.asteroids.AsteroidConfig;
import com.ultramega.asteroidmining.events.AsteroidReloadListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
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

    private static final int MAX_ASTEROIDS_PER_PACKET = 128;
    private static final AtomicInteger NEXT_SYNC_ID = new AtomicInteger();
    // Accessed only on the server thread. Repeated reloads replace each player's pending sync.
    private static final Map<UUID, PendingSync> PENDING = new LinkedHashMap<>();

    public static void handle(final AsteroidDataPayload data, final IPayloadContext context) {
        context.enqueueWork(() ->
            AsteroidReloadListener.INSTANCE.acceptAsteroidDataChunk(data.syncId(), data.chunkIndex(), data.chunkCount(), data.asteroids()));
    }

    public static void sendToPlayer(final ServerPlayer player, final Map<Identifier, AsteroidConfig> data) {
        final int syncId = NEXT_SYNC_ID.incrementAndGet();
        PENDING.put(player.getUUID(), new PendingSync(player, createChunks(syncId, data).iterator()));
    }

    public static void tick() {
        final Iterator<PendingSync> pending = PENDING.values().iterator();
        while (pending.hasNext()) {
            final PendingSync sync = pending.next();
            // Spread encoding and transport over ticks instead of queueing the whole catalog at login.
            for (int packet = 0; packet < 2 && sync.chunks.hasNext(); packet++) {
                PacketDistributor.sendToPlayer(sync.player, sync.chunks.next());
            }
            if (!sync.chunks.hasNext()) {
                pending.remove();
            }
        }
    }

    public static void cancel(final UUID playerId) {
        PENDING.remove(playerId);
    }

    public static void clearPending() {
        PENDING.clear();
    }

    private record PendingSync(ServerPlayer player, Iterator<AsteroidDataPayload> chunks) {
    }

    public static Iterable<AsteroidDataPayload> createChunks(final int syncId, final Map<Identifier, AsteroidConfig> data) {
        // The published data map is immutable. Materialize only the packet currently being sent.
        final int chunkCount = Math.max(1, (data.size() + MAX_ASTEROIDS_PER_PACKET - 1) / MAX_ASTEROIDS_PER_PACKET);
        return () -> new Iterator<>() {
            private final Iterator<AsteroidConfig> values = data.values().iterator();
            private int chunkIndex;

            @Override
            public boolean hasNext() {
                return this.chunkIndex < chunkCount;
            }

            @Override
            public AsteroidDataPayload next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                }
                final List<AsteroidConfig> chunk = new ArrayList<>(MAX_ASTEROIDS_PER_PACKET);
                while (chunk.size() < MAX_ASTEROIDS_PER_PACKET && this.values.hasNext()) {
                    chunk.add(this.values.next());
                }
                return new AsteroidDataPayload(syncId, this.chunkIndex++, chunkCount, chunk);
            }
        };
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
