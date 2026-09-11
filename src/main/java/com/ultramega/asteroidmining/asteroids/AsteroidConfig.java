package com.ultramega.asteroidmining.asteroids;

import com.ultramega.asteroidmining.AsteroidMining;

import java.awt.geom.Point2D;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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
    private static final Pattern MARKS = Pattern.compile("\\p{M}");
    private static final Pattern UNSAFE_PATH = Pattern.compile("[^a-z0-9._-]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
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
    private List<AsteroidResource> composition;

    @Nullable
    private AsteroidConfig centralBody;
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
        this.composition = composition == null ? List.of() : List.copyOf(composition);

        final OrbitData safeOrbitData = orbitData == null ? OrbitData.DEFAULT : orbitData;

        final float semiMinorAxis = safeOrbitData.semiMinorAxis() == 0.0F
            ? safeOrbitData.semiMajorAxis()
            : safeOrbitData.semiMinorAxis();

        this.orbitData = safeOrbitData.withSemiMinorAxis(semiMinorAxis);
        this.currentAngleDegrees = this.orbitData.startingAngleDegrees();

        this.setInitialRotation();
    }

    public AsteroidConfig item(final Item item, final long amount) {
        this.addResource(new AsteroidResource.ItemEntry(item, amount));
        return this;
    }

    public AsteroidConfig fluid(final Fluid fluid, final long amount) {
        this.addResource(new AsteroidResource.FluidEntry(fluid, amount));
        return this;
    }

    private void addResource(final AsteroidResource resource) {
        // Builders are used by data generation; runtime reads share the immutable list.
        final List<AsteroidResource> updated = new ArrayList<>(this.composition);
        updated.add(resource);
        this.composition = List.copyOf(updated);
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
        return this.composition;
    }

    public float getSemiMajorAxis() {
        return this.orbitData.semiMajorAxis();
    }

    public float getSemiMinorAxis() {
        return this.orbitData.semiMinorAxis();
    }

    /** Orbit axes are center-to-center distances, independent of the Sun's sprite size.
     * Satellite orbits retain clearance for the deliberately oversized planet sprites.
     */
    public double getOrbitMajorRadius() {
        final double major = Math.max(this.getSemiMajorAxis(), this.getSemiMinorAxis());
        final AsteroidConfig center = this.getCentralBody();
        if (major <= 0.0D || center == null || center.getCentralBody() == null) {
            return major;
        }
        return major + center.getRadius() / Math.max(0.001D, 1.0D - this.getOrbitEccentricity());
    }

    public double getOrbitMinorRadius() {
        final double eccentricity = this.getOrbitEccentricity();
        return this.getOrbitMajorRadius() * Math.sqrt(1.0D - eccentricity * eccentricity);
    }

    private double getOrbitEccentricity() {
        final double major = Math.max(this.getSemiMajorAxis(), this.getSemiMinorAxis());
        final double minor = Math.max(0.0D, Math.min(this.getSemiMajorAxis(), this.getSemiMinorAxis()));
        return major <= 0.0D ? 0.0D : Math.sqrt(Math.max(0.0D, 1.0D - minor * minor / (major * major)));
    }

    public double getOrbitFocusOffset() {
        return this.getOrbitMajorRadius() * this.getOrbitEccentricity();
    }

    /** Starting angles are mean anomalies in this schematic, not dated ephemerides. */
    public double getEccentricAnomaly(final double meanAngleDegrees) {
        final double mean = Math.toRadians(((meanAngleDegrees % 360.0D) + 360.0D) % 360.0D);
        final double eccentricity = this.getOrbitEccentricity();
        double low = 0.0D;
        double high = 2.0D * Math.PI;
        double anomaly = mean;
        // Safeguarded Newton iteration also converges for near-parabolic catalog entries.
        for (int i = 0; i < 32; i++) {
            final double error = anomaly - eccentricity * Math.sin(anomaly) - mean;
            if (Math.abs(error) < 1.0E-10D) {
                break;
            }
            if (error > 0.0D) {
                high = anomaly;
            } else {
                low = anomaly;
            }
            final double next = anomaly - error / (1.0D - eccentricity * Math.cos(anomaly));
            anomaly = Double.isFinite(next) && next > low && next < high ? next : (low + high) * 0.5D;
        }
        return anomaly;
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
    }

    @Nullable
    public AsteroidConfig getCentralBody() {
        // Resolved against the owning catalog when it is published. A global fallback
        // could link server asteroids to a client's unrelated or cleared snapshot.
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

        if (centralBody == null) {
            centerX = 0.0D;
            centerY = 0.0D;
        } else {
            centralBody.writePosition(target);
            centerX = target.x;
            centerY = target.y;
        }

        final double angleRadians = this.getEccentricAnomaly(this.currentAngleDegrees);
        target.x = centerX + this.getOrbitMajorRadius() * Math.cos(angleRadians) - this.getOrbitFocusOffset();
        target.y = centerY + this.getOrbitMinorRadius() * Math.sin(angleRadians);
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
        if (json.isJsonNull()) {
            return List.of();
        }

        final JsonArray elements;
        if (json.isJsonArray()) {
            elements = json.getAsJsonArray();
        } else if (json.isJsonObject()
            && json.getAsJsonObject().has("asteroids")
            && json.getAsJsonObject().get("asteroids").isJsonArray()) {
            elements = json.getAsJsonObject().getAsJsonArray("asteroids");
        } else {
            return List.of(fromJson(json));
        }

        final List<AsteroidConfig> result = new ArrayList<>(elements.size());

        for (int index = 0; index < elements.size(); index++) {
            final JsonElement element = elements.get(index);
            final DataResult<AsteroidConfig> decoded = AsteroidConfig.CODEC.parse(JsonOps.INSTANCE, element);

            final int finalIndex = index;
            decoded.resultOrPartial(message -> AsteroidMining.LOGGER.error(
                "Invalid asteroid at chunk index {}: {}", finalIndex, message)
            ).ifPresent(result::add);
        }

        return result;
    }

    public static void resolveCentralBodies(final Map<Identifier, AsteroidConfig> asteroidData) {
        // Usually only a few names (Sun, Earth, etc.) are referenced. Do not allocate a
        // three-key lookup map for every asteroid in the catalog.
        final Map<String, String> normalizedNames = new HashMap<>();
        final Map<String, AsteroidConfig> requested = new HashMap<>();
        for (final AsteroidConfig asteroid : asteroidData.values()) {
            final String name = asteroid.getCentralBodyName();
            if (!name.isBlank()) {
                final String key = normalizedNames.computeIfAbsent(name, AsteroidConfig::normalizeLookupKey);
                requested.put(key, null);
            }
        }
        if (!requested.isEmpty()) {
            for (final AsteroidConfig asteroid : asteroidData.values()) {
                matchCentralBody(requested, asteroid.getName(), asteroid);
                matchCentralBody(requested, asteroid.getId().getPath(), asteroid);
                matchCentralBody(requested, asteroid.getId().toString(), asteroid);
            }
        }
        for (final AsteroidConfig asteroid : asteroidData.values()) {
            asteroid.centralBody = requested.get(normalizedNames.get(asteroid.getCentralBodyName()));
        }
        // Only central bodies can participate in a cycle; no per-asteroid visited map is needed.
        final Set<AsteroidConfig> checked = new HashSet<>();
        for (final AsteroidConfig body : requested.values()) {
            final Set<AsteroidConfig> path = new HashSet<>();
            AsteroidConfig current = body;
            while (current != null && !checked.contains(current)) {
                if (!path.add(current)) {
                    AsteroidMining.LOGGER.warn("Ignoring cyclic orbit for {}", current.getId());
                    current.centralBody = null;
                    break;
                }
                current = current.centralBody;
            }
            checked.addAll(path);
        }
    }

    private static void matchCentralBody(final Map<String, AsteroidConfig> requested, final String name,
                                         final AsteroidConfig asteroid) {
        final String key = normalizeLookupKey(name);
        if (requested.containsKey(key)) {
            requested.put(key, asteroid);
        }
    }

    private static String toSafePath(final String value) {
        final String normalized = MARKS.matcher(Normalizer.normalize(value == null ? "asteroid" : value, Normalizer.Form.NFKD)).replaceAll("");
        final String safe = UNSAFE_PATH.matcher(normalized.toLowerCase(Locale.ROOT)).replaceAll("_");
        return safe.isBlank() ? "asteroid" : safe;
    }

    private static String normalizeLookupKey(final String value) {
        return WHITESPACE.matcher(toSafePath(value).replace('_', ' ')).replaceAll(" ").trim();
    }
}
