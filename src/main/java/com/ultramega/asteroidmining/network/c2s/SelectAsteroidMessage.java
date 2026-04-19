package com.ultramega.asteroidmining.network.c2s;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.storage.ConfigurationSavedData;
import com.ultramega.asteroidmining.storage.ModuleProperties;
import com.ultramega.asteroidmining.storage.NetworkConfiguration;
import com.ultramega.asteroidmining.storage.RocketProperties;

import java.util.Optional;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SelectAsteroidMessage(Optional<Identifier> selectedAsteroid, UUID configurationUUID) implements CustomPacketPayload {
    public static final Type<SelectAsteroidMessage> TYPE = new Type<>(AsteroidMining.makeId("select_asteroid"));
    public static final StreamCodec<ByteBuf, SelectAsteroidMessage> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC.apply(ByteBufCodecs::optional), SelectAsteroidMessage::selectedAsteroid,
        UUIDUtil.STREAM_CODEC, SelectAsteroidMessage::configurationUUID,
        SelectAsteroidMessage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SelectAsteroidMessage data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            //TODO: add util methods so these if statement abominations aren't everywhere
            if (!(context.player().level() instanceof ServerLevel serverLevel)) {
                return;
            }

            final ConfigurationSavedData savedData = ConfigurationSavedData.getConfigurationData(serverLevel);
            final NetworkConfiguration configuration = savedData.get(data.configurationUUID());
            if (configuration == null) {
                return;
            }

            Optional<RocketProperties> stats = configuration.rocketProperties();
            stats = stats.map(rocketStats ->
                    new RocketProperties(rocketStats.weight(), rocketStats.trustForce(), rocketStats.fuelUsage()))
                .or(() -> Optional.of(new RocketProperties(-1, -1, -1)));

            ModuleProperties moduleProperties = configuration.moduleProperties();
            if (data.selectedAsteroid().isPresent()) {
                moduleProperties = new ModuleProperties(data.selectedAsteroid(), moduleProperties.inventory());
            }
            savedData.set(data.configurationUUID(),
                new NetworkConfiguration(configuration.launchPadConfiguration(), stats, moduleProperties));
        });
    }
}
