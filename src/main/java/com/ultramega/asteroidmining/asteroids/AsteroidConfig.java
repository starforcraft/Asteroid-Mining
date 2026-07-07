package com.ultramega.asteroidmining.asteroids;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.events.AsteroidReloadListener;

import java.awt.geom.Point2D;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class AsteroidConfig {
    public static final StreamCodec<RegistryFriendlyByteBuf, AsteroidConfig> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, config -> config.id,
        ByteBufCodecs.stringUtf8(512), config -> config.name,
        Identifier.STREAM_CODEC, config -> config.texture,
        ByteBufCodecs.VAR_INT, config -> config.diameter,
        AsteroidResource.LIST_STREAM_CODEC, config -> config.composition,
        OrbitData.STREAM_CODEC, config -> config.orbitData,
        AsteroidConfig::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, List<AsteroidConfig>> LIST_STREAM_CODEC = AsteroidConfig.STREAM_CODEC.apply(
        ByteBufCodecs.list(512)
    );

    public static final Codec<AsteroidConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.fieldOf("id").forGetter(AsteroidConfig::getId),
        Codec.STRING.fieldOf("name").forGetter(AsteroidConfig::getName),
        Identifier.CODEC.fieldOf("texture").forGetter(AsteroidConfig::getTexture),
        Codec.INT.optionalFieldOf("diameter", 1).forGetter(AsteroidConfig::getDiameter),
        AsteroidResource.LIST_CODEC.optionalFieldOf("composition", List.of()).forGetter(config -> config.composition),
        OrbitData.CODEC.optionalFieldOf("orbit", OrbitData.DEFAULT).forGetter(config -> config.orbitData)
        ).apply(instance, AsteroidConfig::new));

    public static final Codec<List<AsteroidConfig>> LIST_CODEC = CODEC.listOf();

    private final Identifier id;
    private String name;
    private final Identifier texture;
    private final int diameter;
    private List<AsteroidResource> composition = new ArrayList<>();

    @Nullable
    private AsteroidConfig centralBody;
    private boolean centralBodyResolved;
    private OrbitData orbitData;
    private float currentAngleDegrees;
    private float rotation;

    public AsteroidConfig(final String name, final Identifier texture, final int diameter) {
        this(null, name, texture, diameter, List.of(), OrbitData.DEFAULT);
    }

    // TODO: what if a config has the exact same id as an already existing asteroid? Will it replace it?
    //  And how do you remove provided asteroids by this mod?
    public AsteroidConfig(@Nullable final Identifier id,
                          final String name,
                          final Identifier texture,
                          final int diameter,
                          @Nullable final List<AsteroidResource> composition,
                          @Nullable final OrbitData orbitData) {
        this.name = name;
        this.id = id == null ? AsteroidMining.makeId(this.getFileName1()) : id;
        this.texture = texture;
        this.diameter = Math.max(1, diameter);
        this.composition = new ArrayList<>(composition == null ? List.of() : composition);

        final OrbitData safeOrbitData = orbitData == null ? OrbitData.DEFAULT : orbitData;

        final float semiMinorAxis = safeOrbitData.semiMinorAxis() == 0.0F
            ? safeOrbitData.semiMajorAxis()
            : safeOrbitData.semiMinorAxis();

        this.orbitData = safeOrbitData.withSemiMinorAxis(semiMinorAxis);
        this.currentAngleDegrees = this.orbitData.startingAngleDegrees();

        this.setInitialRotation();
    }

    public AsteroidConfig item(final Item item, final long amount) {
        this.composition.add(new AsteroidResource.ItemEntry(item, amount));
        return this;
    }

    public AsteroidConfig fluid(final Fluid fluid, final long amount) {
        this.composition.add(new AsteroidResource.FluidEntry(fluid, amount));
        return this;
    }

    public AsteroidConfig orbit(final String centralBodyName,
                                final float semiMajorAxis,
                                final float eccentricity,
                                final float orbitalSpeed,
                                final boolean isClockwise,
                                final float startingAngleDegrees,
                                final boolean isOrbitVisible) {
        this.orbitData = new OrbitData(
            centralBodyName,
            semiMajorAxis,
            (float) (semiMajorAxis * Math.sqrt(1 - Math.pow(eccentricity, 2))),
            orbitalSpeed,
            isClockwise,
            startingAngleDegrees,
            isOrbitVisible,
            false
        );
        this.currentAngleDegrees = startingAngleDegrees;
        this.centralBody = null;
        this.centralBodyResolved = false;
        return this;
    }

    public AsteroidConfig rotation(final boolean rotateAroundItself) {
        this.orbitData = this.orbitData.withRotateAroundItself(rotateAroundItself);
        return this;
    }

    public Identifier getId() {
        return this.id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String getFileName1() {
        return toSafePath(this.name);
    }

    public String getFileName2() {
        return toSafePath(this.id.getPath());
    }

    public Identifier getTexture() {
        return this.texture;
    }

    public int getDiameter() {
        return this.diameter;
    }

    public double getRadius() {
        // 1.5 because else the moons clip into the planets when semi-major axis is small
        // (Don't know if this is a good idea tbh)
        return this.diameter / 1.5;
    }

    public List<AsteroidResource> getComposition() {
        return List.copyOf(this.composition);
    }

    public float getSemiMajorAxis() {
        return this.orbitData.semiMajorAxis();
    }

    public float getSemiMinorAxis() {
        return this.orbitData.semiMinorAxis();
    }

    public float getOrbitalSpeed() {
        return this.orbitData.orbitalSpeed();
    }

    public boolean isClockwise() {
        return this.orbitData.clockwise();
    }

    public void increaseOrbitalAngle() {
        if (this.orbitData.semiMajorAxis() == 0.0F) {
            return;
        }

        final float angularSpeed = this.orbitData.orbitalSpeed() / (float) (2 * Math.PI * this.orbitData.semiMajorAxis());
        final float angleIncrease = angularSpeed * 360 / 20;
        this.currentAngleDegrees += this.orbitData.clockwise() ? angleIncrease : -angleIncrease;
        this.currentAngleDegrees %= 360.0F;
        if (this.currentAngleDegrees < 0.0F) {
            this.currentAngleDegrees += 360.0F;
        }
    }

    public float getCurrentAngleDegrees() {
        return this.currentAngleDegrees;
    }

    public boolean isOrbitVisible() {
        return this.orbitData.orbitVisible();
    }

    public boolean isRotateAroundItself() {
        return this.orbitData.rotateAroundItself();
    }

    public void setInitialRotation() {
        this.rotation = (float) (Math.random() * 360);
    }

    public void increaseRotation() {
        this.rotation += this.getOrbitalSpeed() / 8;
        if (this.rotation >= 360) {
            this.rotation = 0;
        }
    }

    public float getRotation() {
        return this.rotation;
    }

    public void setResolvedCentralBody(@Nullable final AsteroidConfig centralBody) {
        this.centralBody = centralBody;
        this.centralBodyResolved = true;
    }

    @Nullable
    public AsteroidConfig getCentralBody() {
        if (!this.centralBodyResolved && !this.orbitData.centralBodyName().isBlank()) {
            resolveCentralBodies(AsteroidReloadListener.INSTANCE.getData());
        }
        return this.centralBody;
    }

    public String getCentralBodyName() {
        return this.orbitData.centralBodyName();
    }

    public Point2D.Double getCenter() {
        final Point2D.Double target = new Point2D.Double();
        this.writeCenter(target);
        return target;
    }

    public void writeCenter(final Point2D.Double target) {
        final AsteroidConfig centralBody = this.getCentralBody();
        if (centralBody == null) {
            target.x = 0.0D;
            target.y = 0.0D;
            return;
        }
        centralBody.writePosition(target);
    }

    public Point2D.Double getPosition() {
        final Point2D.Double target = new Point2D.Double();
        this.writePosition(target);
        return target;
    }

    public void writePosition(final Point2D.Double target) {
        final AsteroidConfig centralBody = this.getCentralBody();
        final double centerX;
        final double centerY;
        final double centralBodyRadius;

        if (centralBody == null) {
            centerX = 0.0D;
            centerY = 0.0D;
            centralBodyRadius = 0.0D;
        } else {
            centralBody.writePosition(target);
            centerX = target.x;
            centerY = target.y;
            centralBodyRadius = centralBody.getRadius();
        }

        final double angleRadians = Math.toRadians(this.currentAngleDegrees);
        target.x = centerX + (this.orbitData.semiMajorAxis() + centralBodyRadius) * Math.cos(angleRadians);
        target.y = centerY + (this.orbitData.semiMinorAxis() + centralBodyRadius) * Math.sin(angleRadians);
    }

    public double getPositionX() {
        final Point2D.Double position = this.getPosition();
        return position.x;
    }

    public double getPositionY() {
        final Point2D.Double position = this.getPosition();
        return position.y;
    }

    public JsonElement toJson() {
        final JsonElement element = CODEC.encodeStart(JsonOps.INSTANCE, this).getOrThrow();

        // Sort JSON
        final JsonObject original = element.getAsJsonObject();
        final JsonObject orderedJson = new JsonObject();
        final String[] fieldOrder = {
            "id",
            "name",
            "texture",
            "diameter",
            "composition",
            "orbit"
        };

        for (final String field : fieldOrder) {
            if (original.has(field)) {
                orderedJson.add(field, original.get(field));
            }
        }
        return orderedJson;
    }

    public static AsteroidConfig fromJson(final JsonElement json) {
        return AsteroidConfig.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    public static List<AsteroidConfig> fromJsonElements(final JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return List.of();
        }
        if (json.isJsonArray()) {
            return AsteroidConfig.LIST_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        }
        if (json.isJsonObject()) {
            final JsonObject object = json.getAsJsonObject();
            if (object.has("asteroids") && object.get("asteroids").isJsonArray()) {
                return AsteroidConfig.LIST_CODEC.parse(JsonOps.INSTANCE, object.get("asteroids")).getOrThrow();
            }
        }
        return List.of(AsteroidConfig.fromJson(json));
    }

    public static void resolveCentralBodies(final Map<Identifier, AsteroidConfig> asteroidData) {
        final Map<String, AsteroidConfig> byLookupKey = new HashMap<>();
        for (final AsteroidConfig asteroid : asteroidData.values()) {
            byLookupKey.put(normalizeLookupKey(asteroid.getName()), asteroid);
            byLookupKey.put(normalizeLookupKey(asteroid.getId().getPath()), asteroid);
            byLookupKey.put(normalizeLookupKey(asteroid.getId().toString()), asteroid);
        }

        for (final AsteroidConfig asteroid : asteroidData.values()) {
            if (asteroid.orbitData.centralBodyName().isBlank()) {
                asteroid.centralBody = null;
                asteroid.centralBodyResolved = true;
                continue;
            }
            asteroid.centralBody = byLookupKey.get(normalizeLookupKey(asteroid.orbitData.centralBodyName()));
            asteroid.centralBodyResolved = true;
        }
    }

    private static String toSafePath(final String value) {
        final String normalized = Normalizer.normalize(value == null ? "asteroid" : value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "");
        final String safe = normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        return safe.isBlank() ? "asteroid" : safe;
    }

    private static String normalizeLookupKey(final String value) {
        return toSafePath(value).replace('_', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
