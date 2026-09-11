package com.ultramega.asteroidmining.events;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.asteroids.AsteroidConfig;
import com.ultramega.asteroidmining.asteroids.AsteroidJsonReader;
import com.ultramega.asteroidmining.asteroids.AsteroidSearchIndex;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jspecify.annotations.Nullable;

public final class AsteroidReloadListener extends SimpleJsonResourceReloadListener<AsteroidConfig> {
    // Integrated servers share a JVM with the client, but must never share catalog lifecycle.
    public static final AsteroidReloadListener SERVER_INSTANCE = new AsteroidReloadListener();
    public static final AsteroidReloadListener INSTANCE = new AsteroidReloadListener();

    private static final FileToIdConverter FILES = FileToIdConverter.json("asteroids");

    public ICondition.IContext context;

    // One publication prevents readers from observing indices from a different reload.
    private volatile Snapshot snapshot = new Snapshot(Map.of());

    private final Map<Identifier, AsteroidConfig> asteroidDataConfig = new HashMap<>();

    private Map<Identifier, AsteroidConfig> pendingNetworkAsteroidData = new LinkedHashMap<>();
    private final BitSet receivedNetworkChunks = new BitSet();
    private int activeNetworkSyncId = Integer.MIN_VALUE;
    private int expectedNetworkChunks;
    private int receivedNetworkChunkCount;

    public AsteroidReloadListener() {
        super(AsteroidConfig.CODEC, FileToIdConverter.json("asteroids"));
    }

    @Override
    protected Map<Identifier, AsteroidConfig> prepare(final ResourceManager resourceManager, final ProfilerFiller profiler) {
        final RegistryOps<JsonElement> registryOps = this.makeConditionalOps();
        final Map<Identifier, AsteroidConfig> data = new LinkedHashMap<>();
        // Stable file order also makes duplicate-ID overrides and search result order deterministic.
        final var resources = new ArrayList<>(FILES.listMatchingResources(resourceManager).entrySet());
        resources.sort(Map.Entry.comparingByKey());
        for (final var entry : resources) {
            final Map<Identifier, AsteroidConfig> fileData = new LinkedHashMap<>();
            try {
                AsteroidJsonReader.read(entry.getValue()::openAsReader,
                    json -> this.shouldLoad(json, registryOps), json -> {
                        try {
                            if (json.isJsonObject()) {
                                this.filterAsteroidComposition(json.getAsJsonObject(), registryOps);
                            }
                            final AsteroidConfig asteroid = AsteroidConfig.fromJson(json);
                            fileData.put(asteroid.getId(), asteroid);
                        } catch (RuntimeException e) {
                            AsteroidMining.LOGGER.error("Invalid asteroid in {}", entry.getKey(), e);
                        }
                    });
                data.putAll(fileData);
            } catch (Exception e) {
                AsteroidMining.LOGGER.error("Skipping invalid asteroid file {}", entry.getKey(), e);
            }
        }
        return data;
    }

    @Override
    protected void apply(final Map<Identifier, AsteroidConfig> data, final ResourceManager resourceManager, final ProfilerFiller profiler) {
        data.putAll(this.asteroidDataConfig);
        this.publishData(data);
    }

    private boolean shouldLoad(final JsonElement jsonValue, final RegistryOps<JsonElement> registryOps) {
        if (!jsonValue.isJsonObject()) {
            return true;
        }

        final JsonObject object = jsonValue.getAsJsonObject();
        if (!object.has("neoforge:conditions")) {
            return true;
        }

        final List<ICondition> conditions = ICondition.LIST_CODEC.parse(registryOps, object.get("neoforge:conditions")).getOrThrow();
        for (final ICondition condition : conditions) {
            if (!condition.test(this.context)) {
                return false;
            }
        }
        return true;
    }

    private void filterAsteroidComposition(final JsonObject asteroid, final RegistryOps<JsonElement> registryOps) {
        if (!asteroid.has("composition") || !asteroid.get("composition").isJsonArray()) {
            return;
        }

        // This tree belongs to just one streamed asteroid, so filtering can happen in place.
        final var entries = asteroid.getAsJsonArray("composition").iterator();
        while (entries.hasNext()) {
            final JsonElement element = entries.next();
            if (!this.shouldLoad(element, registryOps)) {
                entries.remove();
            } else if (element.isJsonObject()) {
                element.getAsJsonObject().remove("neoforge:conditions");
            }
        }
    }

    public void setData(final Map<Identifier, AsteroidConfig> data) {
        final Map<Identifier, AsteroidConfig> mutableData = new LinkedHashMap<>(Math.max(16, (int) ((data.size() + this.asteroidDataConfig.size()) / 0.75F) + 1));
        mutableData.putAll(data);
        // Config has priority over datapack.
        mutableData.putAll(this.asteroidDataConfig);
        this.publishData(mutableData);
    }

    private void publishData(final Map<Identifier, AsteroidConfig> ownedData) {
        AsteroidConfig.resolveCentralBodies(ownedData);
        this.snapshot = new Snapshot(ownedData);
        AsteroidMining.LOGGER.info("Loaded {} asteroid configs", ownedData.size());
    }

    public Map<Identifier, AsteroidConfig> getData() {
        return this.snapshot.data;
    }

    public List<AsteroidConfig> getAsteroids() {
        return this.snapshot.asteroids;
    }

    public RenderIndex getRenderIndex() {
        return this.snapshot.renderIndex();
    }

    public List<AsteroidConfig> findAsteroids(@Nullable final String query, final int maxResults) {
        if (query == null || query.isBlank() || maxResults <= 0) {
            return List.of();
        }
        return this.snapshot.searchIndex().find(query, maxResults);
    }

    public synchronized void acceptAsteroidDataChunk(final int syncId,
                                                     final int chunkIndex,
                                                     final int chunkCount,
                                                     final List<AsteroidConfig> asteroids) {
        if (chunkCount <= 0 || chunkCount > 1_000_000 || chunkIndex < 0 || chunkIndex >= chunkCount) {
            AsteroidMining.LOGGER.warn("Ignoring invalid asteroid sync chunk {}/{} for sync {}", chunkIndex, chunkCount, syncId);
            return;
        }

        if (syncId != this.activeNetworkSyncId) {
            this.activeNetworkSyncId = syncId;
            this.expectedNetworkChunks = chunkCount;
            this.receivedNetworkChunkCount = 0;
            this.receivedNetworkChunks.clear();
            this.pendingNetworkAsteroidData.clear();
        }

        if (chunkCount != this.expectedNetworkChunks) {
            AsteroidMining.LOGGER.warn("Ignoring asteroid sync chunk {}/{} for sync {} because this sync expected {} chunks",
                chunkIndex, chunkCount, syncId, this.expectedNetworkChunks);
            return;
        }

        if (this.receivedNetworkChunks.get(chunkIndex)) {
            return;
        }

        for (final AsteroidConfig asteroid : asteroids) {
            this.pendingNetworkAsteroidData.put(asteroid.getId(), asteroid);
        }

        this.receivedNetworkChunks.set(chunkIndex);
        this.receivedNetworkChunkCount++;

        if (this.receivedNetworkChunkCount >= this.expectedNetworkChunks) {
            final Map<Identifier, AsteroidConfig> completed = this.pendingNetworkAsteroidData;
            this.pendingNetworkAsteroidData = new LinkedHashMap<>();
            completed.putAll(this.asteroidDataConfig);
            this.publishData(completed);
            this.receivedNetworkChunks.clear();
            this.expectedNetworkChunks = 0;
            this.receivedNetworkChunkCount = 0;
        }
    }

    public synchronized int getSyncProgress() {
        return this.expectedNetworkChunks == 0 ? -1
            : (int) (100L * this.receivedNetworkChunkCount / this.expectedNetworkChunks);
    }

    public synchronized void clearData() {
        this.pendingNetworkAsteroidData.clear();
        this.receivedNetworkChunks.clear();
        this.activeNetworkSyncId = Integer.MIN_VALUE;
        this.expectedNetworkChunks = 0;
        this.receivedNetworkChunkCount = 0;
        this.snapshot = new Snapshot(Map.of());
    }

    private static final class Snapshot {
        private final Map<Identifier, AsteroidConfig> data;
        private final List<AsteroidConfig> asteroids;
        @Nullable
        private RenderIndex renderIndex;
        @Nullable
        private AsteroidSearchIndex searchIndex;

        private Snapshot(final Map<Identifier, AsteroidConfig> ownedData) {
            this.data = Collections.unmodifiableMap(ownedData);
            this.asteroids = List.copyOf(ownedData.values());
        }

        private synchronized RenderIndex renderIndex() {
            if (this.renderIndex == null) {
                this.renderIndex = RenderIndex.create(this.asteroids);
            }
            return this.renderIndex;
        }

        private synchronized AsteroidSearchIndex searchIndex() {
            if (this.searchIndex == null) {
                this.searchIndex = new AsteroidSearchIndex(this.asteroids);
            }
            return this.searchIndex;
        }
    }

    /**
     * Compact render lookup used by SolarSystemViewScreen.
     *
     * Asteroids are grouped by central body and sorted by orbit radius. The screen can then query the
     * radius range that intersects the current viewport instead of scanning every asteroid whenever
     * the camera moves.
     */
    public static final class RenderIndex {
        public static final RenderIndex EMPTY = new RenderIndex(List.of(), List.of());

        private final List<AsteroidConfig> rootAsteroids;
        private final List<OrbitGroup> orbitGroups;

        private RenderIndex(final List<AsteroidConfig> rootAsteroids, final List<OrbitGroup> orbitGroups) {
            this.rootAsteroids = rootAsteroids;
            this.orbitGroups = orbitGroups;
        }

        public static RenderIndex create(final List<AsteroidConfig> asteroids) {
            if (asteroids.isEmpty()) {
                return EMPTY;
            }

            final List<AsteroidConfig> roots = new ArrayList<>();
            final Map<AsteroidConfig, List<AsteroidConfig>> grouped = new HashMap<>();

            for (final AsteroidConfig asteroid : asteroids) {
                final AsteroidConfig centralBody = asteroid.getCentralBody();
                if (centralBody == null || asteroid.getSemiMajorAxis() <= 0.0F) {
                    roots.add(asteroid);
                    continue;
                }

                if (asteroid.isOrbitVisible()) {
                    roots.add(asteroid);
                }
                grouped.computeIfAbsent(centralBody, ignored -> new ArrayList<>()).add(asteroid);
            }

            roots.sort((left, right) -> {
                final int diameterCompare = Integer.compare(right.getDiameter(), left.getDiameter());
                return diameterCompare != 0 ? diameterCompare : left.getName().compareToIgnoreCase(right.getName());
            });

            final List<OrbitGroup> groups = new ArrayList<>(grouped.size());
            for (final Map.Entry<AsteroidConfig, List<AsteroidConfig>> entry : grouped.entrySet()) {
                final List<AsteroidConfig> entries = entry.getValue();
                entries.sort((left, right) -> {
                    final int radiusCompare = Double.compare(OrbitGroup.outerRadius(left), OrbitGroup.outerRadius(right));
                    return radiusCompare != 0 ? radiusCompare : Long.compareUnsigned(stableHash(left.getId()), stableHash(right.getId()));
                });
                groups.add(new OrbitGroup(entry.getKey(), entries));
            }

            groups.sort((left, right) -> left.getCentralBody().getName().compareToIgnoreCase(right.getCentralBody().getName()));
            return new RenderIndex(List.copyOf(roots), List.copyOf(groups));
        }

        public List<AsteroidConfig> getRootAsteroids() {
            return this.rootAsteroids;
        }

        public List<OrbitGroup> getOrbitGroups() {
            return this.orbitGroups;
        }

        private static long stableHash(final Identifier id) {
            return Integer.toUnsignedLong(id.toString().hashCode());
        }
    }

    public static final class OrbitGroup {
        private final AsteroidConfig centralBody;
        private final AsteroidConfig[] asteroids;
        private final double[] radii;
        private final double minOrbitRadius;
        private final double maxOrbitRadius;
        private final double minimumAxisRatio;
        private final double maxSpriteRadius;

        private OrbitGroup(final AsteroidConfig centralBody, final List<AsteroidConfig> entries) {
            this.centralBody = centralBody;
            this.asteroids = entries.toArray(AsteroidConfig[]::new);
            this.radii = new double[entries.size()];
            double minRadius = Double.POSITIVE_INFINITY;
            double ratio = 1.0D;
            double spriteRadius = 0.0D;
            for (int i = 0; i < entries.size(); i++) {
                final AsteroidConfig asteroid = entries.get(i);
                final double outer = outerRadius(asteroid);
                final double inner = Math.max(0.0D, asteroid.getOrbitMajorRadius() - asteroid.getOrbitFocusOffset());
                this.radii[i] = outer;
                minRadius = Math.min(minRadius, inner);
                ratio = Math.min(ratio, outer > 0.0D ? inner / outer : 0.0D);
                spriteRadius = Math.max(spriteRadius, asteroid.getDiameter() * 0.5D);
            }
            this.minOrbitRadius = entries.isEmpty() ? 0.0D : minRadius;
            this.maxOrbitRadius = entries.isEmpty() ? 0.0D : this.radii[this.radii.length - 1];
            this.minimumAxisRatio = ratio;
            this.maxSpriteRadius = spriteRadius;
        }

        private static double outerRadius(final AsteroidConfig asteroid) {
            return Math.max(0.0D, asteroid.getOrbitMajorRadius() + asteroid.getOrbitFocusOffset());
        }

        public AsteroidConfig getCentralBody() {
            return this.centralBody;
        }

        public AsteroidConfig getAsteroid(final int index) {
            return this.asteroids[index];
        }

        public double getMinOrbitRadius() {
            return this.minOrbitRadius;
        }

        public double getMaxOrbitRadius() {
            return this.maxOrbitRadius;
        }

        public double getMaxSpriteRadius() {
            return this.maxSpriteRadius;
        }

        /** Conservative upper bound using periapsis/apoapsis for focus-centered ellipses. */
        public double outerRadiusForDistance(final double distance) {
            return this.minimumAxisRatio > 0.0D ? distance / this.minimumAxisRatio : Double.POSITIVE_INFINITY;
        }

        public int lowerBound(final double radius) {
            int low = 0;
            int high = this.radii.length;
            while (low < high) {
                final int middle = (low + high) >>> 1;
                if (this.radii[middle] < radius) {
                    low = middle + 1;
                } else {
                    high = middle;
                }
            }
            return low;
        }

        public int upperBound(final double radius) {
            int low = 0;
            int high = this.radii.length;
            while (low < high) {
                final int middle = (low + high) >>> 1;
                if (this.radii[middle] <= radius) {
                    low = middle + 1;
                } else {
                    high = middle;
                }
            }
            return low;
        }
    }
}
