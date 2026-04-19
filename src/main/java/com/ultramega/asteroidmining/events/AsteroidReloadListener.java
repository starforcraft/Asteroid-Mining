package com.ultramega.asteroidmining.events;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.utils.AsteroidConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.conditions.ICondition;

import static com.ultramega.asteroidmining.AsteroidMining.MOD_ID;

// TODO: https://docs.neoforged.net/primer/docs/1.21.4/#simplejsonresourcereloadlistener change from JsonElement to AsteroidConfig
public class AsteroidReloadListener extends SimpleJsonResourceReloadListener<JsonElement> {
    public static final AsteroidReloadListener INSTANCE = new AsteroidReloadListener();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve(MOD_ID);

    public ICondition.IContext context;

    private Map<Identifier, AsteroidConfig> asteroidData = new LinkedHashMap<>();
    private final Map<Identifier, AsteroidConfig> asteroidDataConfig = new HashMap<>();

    public AsteroidReloadListener() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("asteroids"));
    }

    @Override
    protected void apply(final Map<Identifier, JsonElement> dataMap, final ResourceManager resourceManager, final ProfilerFiller profiler) {
        profiler.push("AsteroidReloadListener");

        final RegistryOps<JsonElement> registryOps = this.makeConditionalOps();

        final Map<Identifier, AsteroidConfig> data = new HashMap<>();
        for (final Map.Entry<Identifier, JsonElement> entry : dataMap.entrySet()) {
            final Identifier id = entry.getKey();
            try {
                boolean enabled = true;
                final JsonObject jsonValue = entry.getValue().getAsJsonObject();
                if (jsonValue.has("neoforge:conditions")) {
                    final var conditions = ICondition.LIST_CODEC.decode(registryOps, jsonValue.getAsJsonArray("neoforge:conditions"));
                    if (conditions.result().isPresent()) {
                        for (final ICondition condition: conditions.result().get().getFirst()) {
                            if (!condition.test(this.context)) {
                                enabled = false;
                            }
                        }
                    }
                }

                if (enabled) {
                    final Identifier simpleId = id.getPath().contains("/") ? Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath().substring(id.getPath().lastIndexOf("/") + 1)) : id;
                    final AsteroidConfig asteroid = AsteroidConfig.fromJson(jsonValue);

                    // Overwrite existing asteroid data or add new asteroid data
                    data.put(simpleId, asteroid);

                    AsteroidMining.LOGGER.debug("Adding to asteroid data {}", simpleId);
                }
            } catch (Exception e) {
                AsteroidMining.LOGGER.debug("Skipping loading asteroid {} as its conditions were invalid", id);
                throw e;
            }
        }

        this.setData(data);

        profiler.popPush("AsteroidReloadListener");
    }

    private Map<Identifier, AsteroidConfig> sortByName(final Map<Identifier, AsteroidConfig> unsortedMap) {
        final List<Map.Entry<Identifier, AsteroidConfig>> entries = new ArrayList<>(unsortedMap.entrySet());

        entries.sort(Comparator.comparing(entry -> entry.getValue().getName()));

        final Map<Identifier, AsteroidConfig> sortedMap = new LinkedHashMap<>();
        for (final Map.Entry<Identifier, AsteroidConfig> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public void setData(final Map<Identifier, AsteroidConfig> data) {
        // Config has priority over datapack
        final Map<Identifier, AsteroidConfig> mutableData = new HashMap<>(data);
        mutableData.putAll(asteroidDataConfig);
        asteroidData = sortByName(mutableData);
    }

    public Map<Identifier, AsteroidConfig> getData() {
        return asteroidData;
    }

    public AsteroidConfig getData(final Identifier id) {
        return asteroidData.get(id);
    }

    public void updateData(final AsteroidConfig data) {
        asteroidData.put(data.getId(), data);

        AsteroidReloadListener.saveAsteroidInConfig(data.toJson(), data.getFileName2());
    }

    public static void saveAsteroidInConfig(final JsonElement json, final String fileName) {
        final Path path = CONFIG_DIR.resolve(fileName + ".json");
        try {
            Files.createDirectories(path.getParent());

            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (JsonWriter jsonwriter = new JsonWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
                jsonwriter.setSerializeNulls(false);
                jsonwriter.setIndent("  ");
                GsonHelper.writeValue(jsonwriter, json, null);
            }

            Files.write(path, outputStream.toByteArray());
        } catch (IOException e) {
            AsteroidMining.LOGGER.error("Failed to save asteroid config to {}", path, e);
        }
    }

    public static void loadAsteroidsFromConfig() {
        final List<AsteroidConfig> asteroids = new ArrayList<>();

        try {
            Files.createDirectories(CONFIG_DIR);
            final DirectoryStream<Path> stream = Files.newDirectoryStream(CONFIG_DIR, "*.json");

            for (final Path path : stream) {
                try {
                    final JsonElement json = GSON.fromJson(Files.newBufferedReader(path), JsonElement.class);
                    asteroids.add(AsteroidConfig.fromJson(json));
                } catch (Exception e) {
                    AsteroidMining.LOGGER.error("Failed to parse asteroid JSON: {}", path);
                }
            }
        } catch (IOException e) {
            AsteroidMining.LOGGER.error(e.getMessage());
        }

        for (final AsteroidConfig asteroid : asteroids) {
            AsteroidReloadListener.INSTANCE.asteroidDataConfig.put(asteroid.getId(), asteroid);
        }
    }
}
