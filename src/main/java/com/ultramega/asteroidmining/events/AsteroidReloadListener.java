package com.ultramega.asteroidmining.events;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.asteroids.AsteroidConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jspecify.annotations.Nullable;

import static com.ultramega.asteroidmining.AsteroidMining.MOD_ID;

// TODO: https://docs.neoforged.net/primer/docs/1.21.4/#simplejsonresourcereloadlistener
//  change from JsonElement to AsteroidConfig
public final class AsteroidReloadListener extends SimpleJsonResourceReloadListener<JsonElement> {
    public static final AsteroidReloadListener INSTANCE = new AsteroidReloadListener();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve(MOD_ID);

    public ICondition.IContext context;

    private volatile Map<Identifier, AsteroidConfig> asteroidData = Map.of();
    private volatile List<AsteroidConfig> asteroidList = List.of();
    private volatile RenderIndex renderIndex = RenderIndex.EMPTY;
    private volatile List<SearchEntry> searchIndex = List.of();
    private volatile Map<String, AsteroidConfig> asteroidsByExactName = Map.of();

    private final Map<Identifier, AsteroidConfig> asteroidDataConfig = new HashMap<>();

    private final Map<Identifier, AsteroidConfig> pendingNetworkAsteroidData = new LinkedHashMap<>();
    private final BitSet receivedNetworkChunks = new BitSet();
    private int activeNetworkSyncId = Integer.MIN_VALUE;
    private int expectedNetworkChunks;
    private int receivedNetworkChunkCount;

    public AsteroidReloadListener() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("asteroids"));
    }

    @Override
    protected void apply(final Map<Identifier, JsonElement> dataMap, final ResourceManager resourceManager, final ProfilerFiller profiler) {
        profiler.push("AsteroidReloadListener");

        final RegistryOps<JsonElement> registryOps = this.makeConditionalOps();
        final Map<Identifier, AsteroidConfig> data = new HashMap<>(Math.max(16, (int) (dataMap.size() / 0.75F) + 1));

        for (final Map.Entry<Identifier, JsonElement> entry : dataMap.entrySet()) {
            final Identifier id = entry.getKey();
            try {
                final JsonElement jsonValue = entry.getValue();
                if (!this.shouldLoad(jsonValue, registryOps)) {
                    continue;
                }

                final JsonElement filteredJson = this.filterConditionalComposition(jsonValue, registryOps);
                for (final AsteroidConfig asteroid : AsteroidConfig.fromJsonElements(filteredJson)) {
                    data.put(asteroid.getId(), asteroid);
                }
            } catch (Exception e) {
                AsteroidMining.LOGGER.error("Skipping loading asteroid {} as its JSON was invalid", id, e);
            }
        }

        this.setData(data);
        profiler.pop();
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

    private JsonElement filterConditionalComposition(final JsonElement source, final RegistryOps<JsonElement> registryOps) {
        final JsonElement result = source.deepCopy();

        if (result.isJsonArray()) {
            this.filterAsteroidArray(result.getAsJsonArray(), registryOps);
            return result;
        }

        if (!result.isJsonObject()) {
            return result;
        }

        final JsonObject root = result.getAsJsonObject();

        if (root.has("asteroids") && root.get("asteroids").isJsonArray()) {
            this.filterAsteroidArray(root.getAsJsonArray("asteroids"), registryOps);
        } else {
            this.filterAsteroidComposition(root, registryOps);
        }

        return result;
    }

    private void filterAsteroidArray(final JsonArray asteroids, final RegistryOps<JsonElement> registryOps) {
        for (final JsonElement element : asteroids) {
            if (element.isJsonObject()) {
                this.filterAsteroidComposition(element.getAsJsonObject(), registryOps);
            }
        }
    }

    private void filterAsteroidComposition(final JsonObject asteroid, final RegistryOps<JsonElement> registryOps) {
        if (!asteroid.has("composition") || !asteroid.get("composition").isJsonArray()) {
            return;
        }

        final JsonArray original = asteroid.getAsJsonArray("composition");
        final JsonArray filtered = new JsonArray();

        for (final JsonElement element : original) {
            if (!this.shouldLoad(element, registryOps)) {
                continue;
            }

            final JsonElement cleanElement = element.deepCopy();
            if (cleanElement.isJsonObject()) {
                cleanElement.getAsJsonObject()
                    .remove("neoforge:conditions");
            }

            filtered.add(cleanElement);
        }

        asteroid.add("composition", filtered);
    }

    public void setData(final Map<Identifier, AsteroidConfig> data) {
        final Map<Identifier, AsteroidConfig> mutableData = new LinkedHashMap<>(Math.max(16, (int) ((data.size() + this.asteroidDataConfig.size()) / 0.75F) + 1));
        mutableData.putAll(data);
        // Config has priority over datapack.
        mutableData.putAll(this.asteroidDataConfig);
        this.publishData(mutableData);
    }

    private void publishData(final Map<Identifier, AsteroidConfig> mutableData) {
        AsteroidConfig.resolveCentralBodies(mutableData);

        final Map<Identifier, AsteroidConfig> dataSnapshot = Collections.unmodifiableMap(new LinkedHashMap<>(mutableData));
        final List<AsteroidConfig> listSnapshot = List.copyOf(dataSnapshot.values());
        final RenderIndex indexSnapshot = RenderIndex.create(listSnapshot);
        final List<SearchEntry> searchIndexSnapshot = new ArrayList<>(listSnapshot.size());
        final Map<String, AsteroidConfig> exactNameSnapshot = new HashMap<>(Math.max(16, (int) (listSnapshot.size() / 0.75F) + 1));
        for (final AsteroidConfig asteroid : listSnapshot) {
            searchIndexSnapshot.add(new SearchEntry(asteroid.getName(), asteroid.getName().toLowerCase(Locale.ROOT)));
            exactNameSnapshot.putIfAbsent(asteroid.getName(), asteroid);
        }

        this.asteroidData = dataSnapshot;
        this.asteroidList = listSnapshot;
        this.renderIndex = indexSnapshot;
        this.searchIndex = List.copyOf(searchIndexSnapshot);
        this.asteroidsByExactName = Collections.unmodifiableMap(exactNameSnapshot);

        AsteroidMining.LOGGER.info("Loaded {} asteroid configs in {} orbit groups", listSnapshot.size(), indexSnapshot.getOrbitGroups().size());
    }

    public Map<Identifier, AsteroidConfig> getData() {
        return this.asteroidData;
    }

    public List<AsteroidConfig> getAsteroids() {
        return this.asteroidList;
    }

    public RenderIndex getRenderIndex() {
        return this.renderIndex;
    }

    public AsteroidConfig findAsteroidByName(final String name) {
        return this.asteroidsByExactName.get(name);
    }

    public List<String> findAsteroidNames(@Nullable final String query, final int maxResults) {
        if (query == null || query.isBlank() || maxResults <= 0) {
            return List.of();
        }

        final String normalizedQuery = query.toLowerCase(Locale.ROOT);
        final List<String> results = new ArrayList<>(Math.min(10, maxResults));
        for (final SearchEntry entry : this.searchIndex) {
            if (entry.normalizedName.contains(normalizedQuery)) {
                results.add(entry.name);
                if (results.size() >= maxResults) {
                    break;
                }
            }
        }
        return results;
    }

    public synchronized void acceptAsteroidDataChunk(final int syncId,
                                                     final int chunkIndex,
                                                     final int chunkCount,
                                                     final List<AsteroidConfig> asteroids) {
        if (chunkCount <= 0 || chunkIndex >= chunkCount) {
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
            final Map<Identifier, AsteroidConfig> syncedData = new LinkedHashMap<>(this.pendingNetworkAsteroidData);
            this.pendingNetworkAsteroidData.clear();
            this.receivedNetworkChunks.clear();
            this.expectedNetworkChunks = 0;
            this.receivedNetworkChunkCount = 0;
            this.setData(syncedData);
        }
    }

//    public void updateData(final AsteroidConfig data) {
//        this.asteroidDataConfig.put(data.getId(), data);
//        final Map<Identifier, AsteroidConfig> mutableData = new LinkedHashMap<>(this.asteroidData);
//        mutableData.put(data.getId(), data);
//        this.publishData(mutableData);
//        AsteroidReloadListener.saveAsteroidInConfig(data.toJson(), data.getFileName2());
//    }

//    public static void saveAsteroidInConfig(final JsonElement json, final String fileName) {
//        final Path path = CONFIG_DIR.resolve(fileName + ".json");
//        try {
//            Files.createDirectories(path.getParent());
//            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//            try (JsonWriter jsonwriter = new JsonWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
//                jsonwriter.setSerializeNulls(false);
//                jsonwriter.setIndent(" ");
//                GsonHelper.writeValue(jsonwriter, json, null);
//            }
//            Files.write(path, outputStream.toByteArray());
//        } catch (IOException e) {
//            AsteroidMining.LOGGER.error("Failed to save asteroid config to {}", path, e);
//        }
//    }
//
//    public static void loadAsteroidsFromConfig() {
//        final List<AsteroidConfig> asteroids = new ArrayList<>();
//        try {
//            Files.createDirectories(CONFIG_DIR);
//            try (DirectoryStream<Path> stream = Files.newDirectoryStream(CONFIG_DIR, "*.json")) {
//                for (final Path path : stream) {
//                    final JsonElement json = GSON.fromJson(Files.newBufferedReader(path), JsonElement.class);
//                    asteroids.addAll(AsteroidConfig.fromJsonElements(json));
//                }
//            } catch (Exception e) {
//                AsteroidMining.LOGGER.error("Failed to parse asteroid JSON", e);
//            }
//        } catch (IOException e) {
//            AsteroidMining.LOGGER.error(e.getMessage());
//        }
//
//        for (final AsteroidConfig asteroid : asteroids) {
//            AsteroidReloadListener.INSTANCE.asteroidDataConfig.put(asteroid.getId(), asteroid);
//        }
//        AsteroidConfig.resolveCentralBodies(AsteroidReloadListener.INSTANCE.asteroidDataConfig);
//    }

    private record SearchEntry(String name, String normalizedName) {
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
            final Map<AsteroidConfig, List<OrbitEntry>> grouped = new HashMap<>();

            for (final AsteroidConfig asteroid : asteroids) {
                final AsteroidConfig centralBody = asteroid.getCentralBody();
                if (centralBody == null || asteroid.getSemiMajorAxis() <= 0.0F) {
                    roots.add(asteroid);
                    continue;
                }

                final double orbitRadius = Math.max(0.0D, asteroid.getSemiMajorAxis() + centralBody.getRadius());
                grouped.computeIfAbsent(centralBody, ignored -> new ArrayList<>())
                        .add(new OrbitEntry(asteroid, orbitRadius, stableHash(asteroid.getId())));
            }

            roots.sort((left, right) -> {
                final int diameterCompare = Integer.compare(right.getDiameter(), left.getDiameter());
                return diameterCompare != 0 ? diameterCompare : left.getName().compareToIgnoreCase(right.getName());
            });

            final List<OrbitGroup> groups = new ArrayList<>(grouped.size());
            for (final Map.Entry<AsteroidConfig, List<OrbitEntry>> entry : grouped.entrySet()) {
                final List<OrbitEntry> entries = entry.getValue();
                entries.sort((left, right) -> {
                    final int radiusCompare = Double.compare(left.getOrbitRadius(), right.getOrbitRadius());
                    return radiusCompare != 0 ? radiusCompare : Long.compareUnsigned(left.getStableHash(), right.getStableHash());
                });
                groups.add(new OrbitGroup(entry.getKey(), List.copyOf(entries)));
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
        private final List<OrbitEntry> entries;
        private final double minOrbitRadius;
        private final double maxOrbitRadius;

        private OrbitGroup(final AsteroidConfig centralBody, final List<OrbitEntry> entries) {
            this.centralBody = centralBody;
            this.entries = entries;
            this.minOrbitRadius = entries.isEmpty() ? 0.0D : entries.getFirst().getOrbitRadius();
            this.maxOrbitRadius = entries.isEmpty() ? 0.0D : entries.getLast().getOrbitRadius();
        }

        public AsteroidConfig getCentralBody() {
            return this.centralBody;
        }

        public List<OrbitEntry> getEntries() {
            return this.entries;
        }

        public double getMinOrbitRadius() {
            return this.minOrbitRadius;
        }

        public double getMaxOrbitRadius() {
            return this.maxOrbitRadius;
        }

        public int lowerBound(final double radius) {
            int low = 0;
            int high = this.entries.size();
            while (low < high) {
                final int middle = (low + high) >>> 1;
                if (this.entries.get(middle).getOrbitRadius() < radius) {
                    low = middle + 1;
                } else {
                    high = middle;
                }
            }
            return low;
        }

        public int upperBound(final double radius) {
            int low = 0;
            int high = this.entries.size();
            while (low < high) {
                final int middle = (low + high) >>> 1;
                if (this.entries.get(middle).getOrbitRadius() <= radius) {
                    low = middle + 1;
                } else {
                    high = middle;
                }
            }
            return low;
        }
    }

    public static final class OrbitEntry {
        private final AsteroidConfig asteroid;
        private final double orbitRadius;
        private final long stableHash;

        private OrbitEntry(final AsteroidConfig asteroid, final double orbitRadius, final long stableHash) {
            this.asteroid = asteroid;
            this.orbitRadius = orbitRadius;
            this.stableHash = stableHash;
        }

        public AsteroidConfig getAsteroid() {
            return this.asteroid;
        }

        public double getOrbitRadius() {
            return this.orbitRadius;
        }

        public long getStableHash() {
            return this.stableHash;
        }
    }
}
