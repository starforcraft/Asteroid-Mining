package com.ultramega.asteroidmining.datagen;

import com.ultramega.asteroidmining.asteroids.AsteroidConfig;
import com.ultramega.asteroidmining.asteroids.AsteroidTextures;
import com.ultramega.asteroidmining.events.AsteroidReloadListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Util;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import static com.ultramega.asteroidmining.utils.Utils.BUCKET_AMOUNT;

public class AsteroidProvider implements DataProvider {
    private final PackOutput output;

    public AsteroidProvider(final PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cachedOutput) {
        final PackOutput.PathProvider path = this.output.createPathProvider(PackOutput.Target.DATA_PACK, "asteroids");

        final List<CompletableFuture<?>> output = new ArrayList<>();

        final Map<Identifier, Supplier<JsonElement>> asteroids = Maps.newHashMap();

        final Map<Identifier, AsteroidConfig> asteroidData = new HashMap<>();
        this.getAsteroidConfigs().forEach(config -> {
            final Identifier id = config.getId();
            asteroids.put(id, config::toJson);
            asteroidData.put(id, AsteroidConfig.fromJson(asteroids.get(id).get().getAsJsonObject()));
        });

        AsteroidReloadListener.INSTANCE.setData(asteroidData);

        asteroids.forEach((loc, supplier) -> output.add(saveStable(cachedOutput, supplier.get(), path.json(loc))));
        return CompletableFuture.allOf(output.toArray(CompletableFuture[]::new));
    }

    static CompletableFuture<?> saveStable(final CachedOutput output, final JsonElement json, final Path path) {
        return CompletableFuture.runAsync(() -> {
            try {
                final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                final HashingOutputStream hashingOutputStream = new HashingOutputStream(Hashing.sha256(), outputStream);

                try (JsonWriter jsonwriter = new JsonWriter(new OutputStreamWriter(hashingOutputStream, StandardCharsets.UTF_8))) {
                    jsonwriter.setSerializeNulls(false);
                    jsonwriter.setIndent("  ");
                    GsonHelper.writeValue(jsonwriter, json, null);
                }

                output.writeIfNeeded(path, outputStream.toByteArray(), hashingOutputStream.hash());
            } catch (IOException ioexception) {
                LOGGER.error("Failed to save file to {}", path, ioexception);
            }

        }, Util.backgroundExecutor());
    }

    protected List<AsteroidConfig> getAsteroidConfigs() {
        final List<AsteroidConfig> asteroids = new ArrayList<>();

        // Diameter: 1 -> ~2500 km (radius)
        // Semi-Major Axis: 1 -> ~0.01 AE (shape of the ellipse)
        // Speed: 0.1 -> ~100 km/s <=> 1 km/s -> 0.01 (average orbital trustForce)

        // <<< PLANETS >>>

        // The sun is actually way bigger but that would cause many problems so we will just fake it
        asteroids.add(new AsteroidConfig("Sun", AsteroidTextures.SUN, 28));

        // TODO: should I give all orbits a different color?
        asteroids.add(new AsteroidConfig("Mercury", AsteroidTextures.MERCURY, 4)
            .orbit("Sun", 38.7f, 0.206f, 0.1f, true, 90f, true));

        asteroids.add(new AsteroidConfig("Venus", AsteroidTextures.VENUS, 6)
            .orbit("Sun", 72.3f, 0.01f, 0.35f, true, 45f, true));

        asteroids.add(new AsteroidConfig("Earth", AsteroidTextures.EARTH, 6)
            .orbit("Sun", 100f, 0.02f, 0.298f, true, 0f, true));

        asteroids.add(new AsteroidConfig("Mars", AsteroidTextures.MARS, 4)
            .orbit("Sun", 152.3f, 0.1f, 0.241f, true, 20f, true));

        asteroids.add(new AsteroidConfig("Jupiter", AsteroidTextures.JUPITER, 28)
            .orbit("Sun", 520.3f, 0.05f, 0.131f, true, 60f, true));

        asteroids.add(new AsteroidConfig("Saturn", AsteroidTextures.SATURN, 24)
            .orbit("Sun", 958.2f, 0.05f, 0.097f, true, 10f, true));

        asteroids.add(new AsteroidConfig("Uranus", AsteroidTextures.URANUS, 10)
            .orbit("Sun", 1919.3f, 0.05f, 0.068f, true, 110f, true));

        asteroids.add(new AsteroidConfig("Neptune", AsteroidTextures.NEPTUNE, 10)
            .orbit("Sun", 3007f, 0.01f, 0.054f, true, 140f, true));

        // <<< MOONS >>>
        asteroids.add(new AsteroidConfig("Luna", AsteroidTextures.GENERIC_MOON_1, 1) //TODO: make a texture
            .orbit("Earth", 1.3f, 0.05f, 0.01f, true, 0f, false));

        asteroids.add(new AsteroidConfig("Phobos", AsteroidTextures.GENERIC_MOON_1, 1) //TODO: make a texture
            .orbit("Mars", 1.2f, 0.02f, 0.021f, true, 15f, false));
        asteroids.add(new AsteroidConfig("Deimos", AsteroidTextures.GENERIC_MOON_2, 1) //TODO: make a texture
            .orbit("Mars", 2.3f, 0f, 0.014f, true, 120f, false));

        asteroids.add(new AsteroidConfig("Io", AsteroidTextures.IO, 2)
            .orbit("Jupiter", 0.6f, 0f, 0.173f, true, 0f, false));
        asteroids.add(new AsteroidConfig("Europa", AsteroidTextures.EUROPA, 2)
            .orbit("Jupiter", 1.0f, 0f, 0.137f, true, 20f, false));
        asteroids.add(new AsteroidConfig("Ganymede", AsteroidTextures.GANYMEDE, 2)
            .orbit("Jupiter", 1.8f, 0f, 0.109f, true, 80f, false));
        asteroids.add(new AsteroidConfig("Callisto", AsteroidTextures.CALLISTO, 2)
            .orbit("Jupiter", 2.5f, 0f, 0.082f, true, 140f, false));
        asteroids.add(new AsteroidConfig("Amalthea", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 0.4f, 0f, 0.027f, true, 150f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Himalia", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 6.8f, 0.16f, 0.033f, true, 15f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Elara", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 6.9f, 0.21f, 0.033f, true, 65f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Pasiphae", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 11.2f, 0.41f, 0.04f, false, 40f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Sinope", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 11.8f, 0.25f, 0.023f, false, 140f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Lysithea", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 6.9f, 0.12f, 0.033f, true, 60f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Carme", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 10.9f, 0.26f, 0.023f, false, 57f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Ananke", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 9.5f, 0.24f, 0.024f, false, 95f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Leda", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 6.5f, 0.16f, 0.05f, true, 25f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Thebe", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 0.5f, 0.01f, 0.0239f, true, 115f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Adrastea", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 0.2f, 0f, 0.0315f, true, 135f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Metis", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 0.2f, 0f, 0.0315f, true, 92f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Callirrhoe", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 11.6f, 0.3f, 0.021f, false, 75f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Themisto", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 5.0f, 0.34f, 0.024f, true, 160f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Megaclite", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 11.4f, 0.42f, 0.015f, false, 165f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Taygete", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 10.7f, 0.25f, 0.031f, false, 145f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Chaldene", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 10.4f, 0.27f, 0.022f, false, 140f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Harpalyke", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 9.2f, 0.23f, 0.023f, false, 50f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Kalyke", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 11.1f, 0.26f, 0.011f, false, 155f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Iocaste", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 9.5f, 0.23f, 0.01f, false, 85f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Erinome", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 10.5f, 0.28f, 0.009f, false, 90f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Isonoe", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 10.5f, 0.25f, 0.012f, false, 45f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Praxidike", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 9.3f, 0.25f, 0.032f, false, 260f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Autonoe", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 11.6f, 0.33f, 0.023f, false, 55f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Thyone", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 9.4f, 0.23f, 0.015f, false, 220f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Hermippe", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 9.5f, 0.22f, 0.016f, false, 310f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Aitne", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 10.6f, 0.28f, 0.019f, false, 230f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Eurydome", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 10.3f, 0.3f, 0.017f, false, 275f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Euanthe", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 9.2f, 0.24f, 0.017f, false, 185f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Euporie", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 8.8f, 0.15f, 0.018f, false, 99f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Orthosie", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 9.2f, 0.3f, 0.019f, false, 105f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Sponde", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 11.3f, 0.32f, 0.019f, false, 210f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Kale", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 10.5f, 0.26f, 0.024f, false, 140f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Pasithee", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 10.2f, 0.27f, 0.014f, false, 150f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Hegemone", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 11.1f, 0.36f, 0.018f, false, 117f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Mneme", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 9.1f, 0.25f, 0.017f, false, 100f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Aoede", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 11.5f, 0.44f, 0.016f, false, 47f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Thelxinoe", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 9.4f, 0.23f, 0.015f, false, 228f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Arche", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 10.7f, 0.26f, 0.02f, false, 69f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Kallichore", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 10.5f, 0.25f, 0.022f, false, 342f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Helike", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 9.3f, 0.15f, 0.017f, false, 35f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Carpo", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 8.2f, 0.42f, 0.019f, true, 325f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Eukelade", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 10.6f, 0.28f, 0.014f, false, 84f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Cyllene", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 11.4f, 0.42f, 0.015f, false, 96f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Kore", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 12.0f, 0.33f, 0.024f, false, 274f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Herse", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 10.9f, 0.26f, 0.01f, false, 58f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2010 J 1", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 11.0f, 0.25f, 0.018f, false, 326f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2010 J 2", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 9.1f, 0.25f, 0.018f, false, 95f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Dia", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 7.1f, 0.23f, 0.010f, true, 102f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2016 J 1", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 9.1f, 0.23f, 0.011f, false, 145f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2003 J 18", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 9.0f, 0.1f, 0.01f, false, 123f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2011 J 2", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 10.3f, 0.36f, 0.011f, false, 214f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Eirene", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 10.5f, 0.26f, 0.017f, false, 116f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Philophrosyne", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 10.0f, 0.23f, 0.03f, false, 156f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2017 J 1", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 11.5f, 0.33f, 0.03f, false, 120f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Eupheme", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 9.1f, 0.24f, 0.03f, false, 221f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2003 J 19", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 10.9f, 0.27f, 0.031f, false, 244f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Valetudo", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 8.6f, 0.22f, 0.033f, true, 320f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2017 J 2", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 10.5f, 0.27f, 0.035f, false, 302f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2017 J 3", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 9.3f, 0.23f, 0.03f, false, 305f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Pandia", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 6.8f, 0.18f, 0.02f, true, 260f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2017 J 5", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 11.0f, 0.26f, 0.01f, false, 256f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2017 J 6", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 11.0f, 0.34f, 0.02f, false, 240f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2017 J 7", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 9.4f, 0.23f, 0.01f, false, 48f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2017 J 8", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 10.2f, 0.26f, 0.015f, false, 255f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2017 J 9", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 9.7f, 0.2f, 0.015f, false, 280f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("Ersa", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 6.6f, 0.12f, 0.015f, true, 354f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2011 J 1", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 10.7f, 0.27f, 0.026f, false, 340f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2003 J 2", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 9.5f, 0.23f, 0.016f, false, 125f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2003 J 4", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 10.4f, 0.33f, 0.018f, false, 206f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2003 J 9", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 11.0f, 0.26f, 0.019f, false, 286f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2003 J 10", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 11.3f, 0.26f, 0.012f, false, 275f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2003 J 12", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 9.4f, 0.24f, 0.021f, false, 290f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2003 J 16", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 9.2f, 0.24f, 0.029f, false, 295f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2003 J 23", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 11.7f, 0.31f, 0.041f, false, 254f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2003 J 24", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 10.3f, 0.26f, 0.017f, false, 211f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2011 J 3", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 6.9f, 0.19f, 0.015f, true, 104f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2016 J 3", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 10.0f, 0.25f, 0.011f, false, 144f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2016 J 4", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 10.7f, 0.29f, 0.019f, false, 341f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2017 J 10", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 9.7f, 0.26f, 0.019f, false, 314f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2017 J 11", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 10.0f, 0.17f, 0.035f, false, 328f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2018 J 2", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 6.7f, 0.15f, 0.035f, true, 347f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2018 J 3", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 11.2f, 0.27f, 0.025f, false, 357f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2018 J 4", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 8.0f, 0.18f, 0.025f, true, 334f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2021 J 1", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 9.4f, 0.23f, 0.016f, false, 325f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2021 J 2", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 9.3f, 0.24f, 0.012f, false, 249f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2021 J 3", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 9.1f, 0.24f, 0.018f, false, 291f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2021 J 4", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 10.5f, 0.27f, 0.013f, false, 118f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2021 J 5", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 11.2f, 0.27f, 0.026f, false, 196f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2021 J 6", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 10.3f, 0.27f, 0.027f, false, 71f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2022 J 1", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 9.8f, 0.19f, 0.028f, false, 187f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2022 J 2", AsteroidTextures.GENERIC_MOON_2, 1)
            .orbit("Jupiter", 9.9f, 0.18f, 0.029f, false, 98f, false)
            .rotation(true));
        asteroids.add(new AsteroidConfig("S/2022 J 3", AsteroidTextures.GENERIC_MOON_1, 1)
            .orbit("Jupiter", 9.4f, 0.27f, 0.03f, false, 289f, false)
            .rotation(true));

        // <<< ASTEROIDS >>>

        //TODO!!

        asteroids.add(new AsteroidConfig("1989 ML", AsteroidTextures.ASTEROID_1, 1)
            .item(Items.IRON_ORE, 640_000L)
            .fluid(Fluids.WATER, 320L * BUCKET_AMOUNT)
            .fluid(Fluids.LAVA, 16L * BUCKET_AMOUNT)
            .orbit("Mercury", 10f, 0.5f, 2.3f, true, 0f, false)
            .rotation(true));

        asteroids.add(new AsteroidConfig("Ceres", AsteroidTextures.ASTEROID_2, 4)
            .item(Items.IRON_ORE, 32000L)
            .item(Items.GOLD_ORE, 32000L)
            .orbit("Sun", 100f, 0.5f, 2.3f, false, 5f, false)
            .rotation(true));

        /*asteroids.add(new AsteroidConfig("1989 ML", AsteroidTextures.ASTEROID_2, 1)
            .item(Items.IRON_ORE, 640_000)
            .orbit(120, 0.14f, 0.005f, 2.3f, false)
            .rotation(true));*/

        /*asteroids.add(new AsteroidConfig("Ceres", AsteroidTextures.ASTEROID_1, 4)
            .item(Items.IRON_ORE, 32000)
            .item(Items.GOLD_ORE, 32000)
            .orbit(130, 0.9f, 0.2f, 0f, false)
            .rotation(true));

        asteroids.add(new AsteroidConfig("Anteros", AsteroidTextures.ASTEROID_1, 5)
            .item(Items.IRON_ORE, 16000)
            .item(Items.DIAMOND_ORE, 3200)
            .item(Items.REDSTONE_ORE, 3200)
            .item(Items.EMERALD_ORE, 3200)
            .item(Items.GOLD_ORE, 3200)
            .item(Items.COAL_ORE, 3200)
            .orbit(145, 0.9f, 0.215f, 2.2f, false)
            .rotation(true));

        asteroids.add(new AsteroidConfig("2001 CC21", AsteroidTextures.ASTEROID_2, 6)
            .item(Items.IRON_ORE, 16000)
            .item(Items.EMERALD_ORE, 3200)
            .item(Items.DIAMOND_ORE, 3200)
            .item(Items.REDSTONE_ORE, 3200)
            .orbit(150, 0.9f, 0.22f, 0.12f, false)
            .rotation(true));

        asteroids.add(new AsteroidConfig("1992 TC", AsteroidTextures.ASTEROID_1, 4)
            .item(Items.IRON_ORE, 320000)
            .orbit(160, 0.9f, 0.3f, 6f, false)
            .rotation(true));

        asteroids.add(new AsteroidConfig("1998 UT18", AsteroidTextures.ASTEROID_2, 4)
            .item(Items.IRON_ORE, 32000)
            .orbit(170, 0.9f, 0.3f, 5.8f, false)
            .rotation(true));

        asteroids.add(new AsteroidConfig("2002 CS11", AsteroidTextures.ASTEROID_2, 4)
            .item(Items.IRON_ORE, 32000)
            .orbit(180, 0.9f, 0.4f, 4.5f, false)
            .rotation(true));

        asteroids.add(new AsteroidConfig("2001 HW15", AsteroidTextures.ASTEROID_1, 4)
            .item(Items.IRON_ORE, 32000)
            .orbit(190, 0.9f, 0.4f, 3.2f, false)
            .rotation(true));

        asteroids.add(new AsteroidConfig("Viola", AsteroidTextures.ASTEROID_1, 5)
            .item(Items.IRON_ORE, 48000)
            .orbit(200, 0.9f, 0.12f, 6.3f, false)
            .rotation(true));

        asteroids.add(new AsteroidConfig("Flagsymphony", AsteroidTextures.ASTEROID_2, 5)
            .item(Items.IRON_ORE, 72000)
            .orbit(210, 0.9f, 0.14f, 6.8f, false)
            .rotation(true));

        asteroids.add(new AsteroidConfig("Endymion", AsteroidTextures.ASTEROID_2, 5)
            .item(Items.IRON_ORE, 72000)
            .orbit(220, 0.9f, 0.141f, 2.1f, false)
            .rotation(true));

        asteroids.add(new AsteroidConfig("Backlunda", AsteroidTextures.ASTEROID_1, 5)
            .item(Items.IRON_ORE, 64000)
            .orbit(230, 0.9f, 0.13f, 7.9f, false)
            .rotation(true));*/

        return asteroids;
    }

    @Override
    public String getName() {
        return "Asteroid Mining Data Provider";
    }
}
