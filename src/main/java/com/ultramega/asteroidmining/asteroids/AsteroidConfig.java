package com.ultramega.asteroidmining.asteroids;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.events.AsteroidReloadListener;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

public final class AsteroidConfig {
    public static final Codec<AsteroidConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.fieldOf("id").forGetter(c -> c.id),
        Codec.STRING.fieldOf("name").forGetter(c -> c.name),
        Identifier.CODEC.fieldOf("texture").forGetter(c -> c.texture),
        Codec.INT.fieldOf("diameter").forGetter(c -> c.diameter),
        AsteroidResource.LIST_CODEC.fieldOf("composition").forGetter(c -> c.composition),
        Codec.STRING.fieldOf("centralBodyName").forGetter(c -> c.centralBodyName),
        Codec.FLOAT.fieldOf("semiMajorAxis").forGetter(c -> c.semiMajorAxis),
        Codec.FLOAT.fieldOf("semiMinorAxis").forGetter(c -> c.semiMinorAxis),
        Codec.FLOAT.fieldOf("orbitalSpeed").forGetter(c -> c.orbitalSpeed),
        Codec.BOOL.fieldOf("isClockwise").forGetter(c -> c.isClockwise),
        Codec.FLOAT.fieldOf("startingAngleDegrees").forGetter(c -> c.startingAngleDegrees),
        Codec.BOOL.fieldOf("isOrbitVisible").forGetter(c -> c.isOrbitVisible),
        Codec.BOOL.optionalFieldOf("shouldRotate", false).forGetter(c -> c.shouldRotate)
    ).apply(instance, AsteroidConfig::new));

    private final Identifier id;
    private String name;
    private final Identifier texture;
    private final int diameter;
    private List<AsteroidResource> composition = new ArrayList<>();

    @Nullable
    private AsteroidConfig centralBody;
    private String centralBodyName = "";
    private float semiMajorAxis;
    private float semiMinorAxis;
    private float orbitalSpeed;
    private boolean isClockwise;
    private float startingAngleDegrees;
    private float currentAngleDegrees;
    private boolean isOrbitVisible;

    private boolean shouldRotate; //TODO: rename these to something like shouldRotateAroundItself
    private float rotation;

    public AsteroidConfig(final String name, final Identifier texture, final int diameter) {
        this.name = name;
        this.id = AsteroidMining.makeId(this.getFileName1());
        this.texture = texture;
        this.diameter = diameter;

        this.setInitialRotation();
    }

    public AsteroidConfig(@Nullable final Identifier id,
                          final String name,
                          final Identifier texture,
                          final int diameter,
                          final List<AsteroidResource> composition,
                          final String centralBodyName,
                          final float semiMajorAxis,
                          final float semiMinorAxis,
                          final float orbitalSpeed,
                          final boolean isClockwise,
                          final float startingAngleDegrees,
                          final boolean isOrbitVisible,
                          final boolean shouldRotate) { //TODO: add priority value (what did I mean by this?)
        this.name = name;
        this.id = id == null ? AsteroidMining.makeId(this.getFileName1()) : id;
        this.texture = texture;
        this.diameter = diameter;
        this.composition = composition;
        this.centralBodyName = centralBodyName;
        this.semiMajorAxis = semiMajorAxis;
        this.semiMinorAxis = semiMinorAxis;
        this.orbitalSpeed = orbitalSpeed;
        this.isClockwise = isClockwise;
        this.startingAngleDegrees = startingAngleDegrees;
        this.currentAngleDegrees = startingAngleDegrees;
        this.isOrbitVisible = isOrbitVisible;
        this.shouldRotate = shouldRotate;

        this.setInitialRotation();
    }

    public AsteroidConfig item(final Item item, final long amount) { //TODO: this currently crashes on datagen
        this.composition.add(new AsteroidResource.ItemEntry(ItemResource.of(item), amount));
        return this;
    }

    public AsteroidConfig fluid(final Fluid fluid, final long amount) {
        this.composition.add(new AsteroidResource.FluidEntry(FluidResource.of(fluid), amount));
        return this;
    }

    public AsteroidConfig orbit(final String centralBodyName,
                                final float semiMajorAxis,
                                final float eccentricity,
                                final float orbitalSpeed,
                                final boolean isClockwise,
                                final float startingAngleDegrees,
                                final boolean isOrbitVisible) {
        this.centralBodyName = centralBodyName;
        this.semiMajorAxis = semiMajorAxis;
        this.semiMinorAxis = (float) (semiMajorAxis * Math.sqrt(1 - Math.pow(eccentricity, 2)));
        this.orbitalSpeed = orbitalSpeed;
        this.isClockwise = isClockwise;
        this.startingAngleDegrees = startingAngleDegrees;
        this.currentAngleDegrees = startingAngleDegrees;
        this.isOrbitVisible = isOrbitVisible;
        return this;
    }

    public AsteroidConfig rotation(final boolean shouldRotate) {
        this.shouldRotate = shouldRotate;
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
        return this.name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    }

    public String getFileName2() {
        return this.id.getPath().split(":")[0];
    }

    public Identifier getTexture() {
        return this.texture;
    }

    public int getDiameter() {
        return this.diameter;
    }

    public double getRadius() {
        // 1.5 because else the moons clip into the planets when semi-mayor axis is small (Don't know if this is a good idea tbh)
        return this.diameter / 1.5;
    }

    public List<AsteroidResource> getComposition() {
        return List.copyOf(this.composition);
    }

    public float getSemiMajorAxis() {
        return this.semiMajorAxis;
    }

    public float getSemiMinorAxis() {
        return this.semiMinorAxis;
    }

    public float getOrbitalSpeed() {
        return this.orbitalSpeed;
    }

    public boolean isClockwise() {
        return this.isClockwise;
    }

    public void increaseOrbitalAngle() {
        if (this.semiMajorAxis == 0.0) {
            return;
        }

        final float angularSpeed = this.orbitalSpeed / (float) (2 * Math.PI * this.semiMajorAxis);
        final float angleIncrease = angularSpeed * 360 / 20;
        if (this.isClockwise) {
            this.currentAngleDegrees += angleIncrease;
            if (this.currentAngleDegrees >= 360) {
                this.currentAngleDegrees = 0;
            }
        } else {
            this.currentAngleDegrees -= angleIncrease;
            if (this.currentAngleDegrees <= 0) {
                this.currentAngleDegrees = 360;
            }
        }
    }

    public float getCurrentAngleDegrees() {
        return this.currentAngleDegrees;
    }

    public boolean isOrbitVisible() {
        return this.isOrbitVisible;
    }

    public boolean isShouldRotate() {
        return this.shouldRotate;
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

    @Nullable
    public AsteroidConfig getCentralBody() {
        if (this.centralBody == null && !this.centralBodyName.isBlank()) {
            this.centralBody = AsteroidReloadListener.INSTANCE.getData().values().stream()
                .filter((asteroidConfig) -> asteroidConfig.getName().equals(this.centralBodyName))
                .findFirst()
                .orElse(null);
        }

        return this.centralBody;
    }

    public String getCentralBodyName() {
        return this.centralBodyName;
    }

    public Point2D.Double getCenter() {
        final AsteroidConfig centralBody = this.getCentralBody();
        if (centralBody == null) {
            return new Point2D.Double(0.0, 0.0);
        }
        return centralBody.getPosition();
    }

    public Point2D.Double getPosition() {
        final AsteroidConfig centralBody = this.getCentralBody();
        final double centralBodyRadius = (centralBody != null) ? centralBody.getRadius() : 0.0;

        final double angleRadians = Math.toRadians(this.getCurrentAngleDegrees());

        final double offsetX = (this.getSemiMajorAxis() + centralBodyRadius) * Math.cos(angleRadians);
        final double offsetY = (this.getSemiMinorAxis() + centralBodyRadius) * Math.sin(angleRadians);

        final Point2D.Double center = this.getCenter();

        return new Point2D.Double(center.x + offsetX, center.y + offsetY);
    }

    public JsonElement toJson() {
        final JsonElement element = CODEC.encodeStart(JsonOps.INSTANCE, this).getOrThrow();

        // Sort JSON
        final JsonObject original = element.getAsJsonObject();
        final JsonObject orderedJson = new JsonObject();

        final String[] fieldOrder = {
            "id", "name", "texture", "diameter", "compositionItems", "compositionFluids",
            "centralBodyName", "semiMajorAxis", "semiMinorAxis", "orbitalSpeed", "isClockwise", "startingAngleDegrees", "isOrbitVisible", "shouldRotate"
        };

        for (final String field : fieldOrder) {
            if (original.has(field)) { //TODO: remove default values out of json
                orderedJson.add(field, original.get(field));
            }
        }

        return orderedJson;
    }

    public static AsteroidConfig fromJson(final JsonElement json) {
        return AsteroidConfig.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }
}
