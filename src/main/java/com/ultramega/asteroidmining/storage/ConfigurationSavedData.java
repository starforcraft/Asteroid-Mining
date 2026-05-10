package com.ultramega.asteroidmining.storage;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.network.s2c.UpdateClientConfigurationDataPayload;
import com.ultramega.asteroidmining.utils.CoreValidations;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

//TODO: sync to client when joining
// ^ isn't this already done?
public class ConfigurationSavedData extends SavedData {
    public static final SavedDataType<ConfigurationSavedData> TYPE = new SavedDataType<>(
        AsteroidMining.makeId("configurations"),
        ConfigurationSavedData::new,
        RecordCodecBuilder.create(instance -> instance.group(
            NetworkConfiguration.MAP_CODEC.fieldOf("entries").forGetter(sd -> sd.entries)
        ).apply(instance, ConfigurationSavedData::new)));

    private final Map<UUID, NetworkConfiguration> entries;

    public ConfigurationSavedData() {
        this(new HashMap<>());
    }

    public ConfigurationSavedData(final Map<UUID, NetworkConfiguration> entries) {
        this.entries = new HashMap<>(entries);
    }

    // TODO: add an easy way to overwrite existing data instead of doing addConfiguration() everytime
    public void set(final UUID uuid, final NetworkConfiguration configuration) {
        CoreValidations.validateNotNull(uuid, "uuid cannot be null");
        CoreValidations.validateNotNull(configuration, "rocket details cannot be null");

        this.entries.put(uuid, configuration);
        this.setDirty();
    }

    // TODO: change this to Optional<> instead
    @Nullable
    public NetworkConfiguration get(final UUID uuid) {
        return this.entries.getOrDefault(uuid, null);
    }

    @Override
    public void setDirty() {
        super.setDirty();

        //TODO: switch to request response system, sending the map everytime is too expensive (Check refinedstorage2 ClientStorageRepository)
        PacketDistributor.sendToAllPlayers(new UpdateClientConfigurationDataPayload(Map.copyOf(this.entries)));
    }

    public static ConfigurationSavedData getConfigurationData(final ServerLevel level) {
        final ServerLevel serverLevel = requireNonNull(level.getServer().getLevel(Level.OVERWORLD));
        return serverLevel.getDataStorage().computeIfAbsent(ConfigurationSavedData.TYPE);
    }
}
