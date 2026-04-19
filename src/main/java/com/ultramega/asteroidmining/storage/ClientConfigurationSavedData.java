package com.ultramega.asteroidmining.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

public class ClientConfigurationSavedData {
    public static final ClientConfigurationSavedData INSTANCE = new ClientConfigurationSavedData();

    private Map<UUID, NetworkConfiguration> entries = new HashMap<>();

    public void set(final Map<UUID, NetworkConfiguration> entries) {
        this.entries = entries;
    }

    public void put(final UUID uuid, final NetworkConfiguration configuration) {
        this.entries.put(uuid, configuration);
    }

    // TODO: change to Optional
    @Nullable
    public NetworkConfiguration get(final UUID uuid) {
        return this.entries.getOrDefault(uuid, null);
    }
}
