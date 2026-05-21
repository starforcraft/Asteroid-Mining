package com.ultramega.asteroidmining.launch;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.asteroids.AsteroidConfig;
import com.ultramega.asteroidmining.blockentities.RocketControllerBlockEntity;
import com.ultramega.asteroidmining.entities.BlockStructureEntity;
import com.ultramega.asteroidmining.events.AsteroidReloadListener;
import com.ultramega.asteroidmining.registry.ModParticles;
import com.ultramega.asteroidmining.storage.ConfigurationSavedData;
import com.ultramega.asteroidmining.storage.NetworkConfiguration;
import com.ultramega.asteroidmining.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public class RocketLaunchManager extends SavedData {
    private static final double ORBIT_Y = 500.0D;
    private static final int ORBIT_WAIT_TICKS = 20 /* * 60 * 3*/; //TODO: calculate and set this value accordingly

    private static final double REENTRY_ALIGNMENT_START_ABOVE_LANDING = 96.0D;
    private static final double REENTRY_OFFSET_BLOCKS = 16.0D;
    private static final double REENTRY_FORCE_ALIGNED_ABOVE_LANDING = 8.0D;

    private static final float LANDING_MAX_PITCH_DEGREES = 8.0F;
    private static final double LANDING_PITCH_START_ABOVE_LANDING = 60.0D;
    private static final double LANDING_FORCE_UPRIGHT_ABOVE_LANDING = 6.0D;

    private static final Codec<RocketFlight> ROCKET_FLIGHT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.CODEC.fieldOf("id").forGetter(flight -> flight.id),
        BlockPos.CODEC.fieldOf("controllerPos").forGetter(flight -> flight.controllerPos),
        UUIDUtil.CODEC.fieldOf("configurationId").forGetter(flight -> flight.configurationId),
        Identifier.CODEC.fieldOf("destinationAsteroid").forGetter(flight -> flight.destinationAsteroid),
        CommonUtils.STRUCTURE_BLOCK_INFO_LIST_CODEC.fieldOf("structureBlockInfos").forGetter(flight -> flight.structureBlockInfos),
        Codec.DOUBLE.fieldOf("landingX").forGetter(flight -> flight.landingX),
        Codec.DOUBLE.fieldOf("landingY").forGetter(flight -> flight.landingY),
        Codec.DOUBLE.fieldOf("landingZ").forGetter(flight -> flight.landingZ),
        Codec.DOUBLE.fieldOf("rocketX").forGetter(flight -> flight.rocketX),
        Codec.DOUBLE.fieldOf("rocketY").forGetter(flight -> flight.rocketY),
        Codec.DOUBLE.fieldOf("rocketZ").forGetter(flight -> flight.rocketZ),
        Codec.DOUBLE.fieldOf("reentryOffsetX").forGetter(flight -> flight.reentryOffsetX),
        Codec.DOUBLE.fieldOf("reentryOffsetZ").forGetter(flight -> flight.reentryOffsetZ),
        RocketPhase.CODEC.fieldOf("phase").forGetter(flight -> flight.phase),
        Codec.INT.fieldOf("phaseTick").forGetter(flight -> flight.phaseTick),
        UUIDUtil.CODEC.optionalFieldOf("rocketEntityId").forGetter(flight -> Optional.ofNullable(flight.rocketEntityId))
    ).apply(instance, RocketFlight::new));

    public static final SavedDataType<RocketLaunchManager> TYPE = new SavedDataType<>(
        AsteroidMining.makeId("rocket_launch_manager"),
        RocketLaunchManager::new,
        RecordCodecBuilder.create(instance -> instance.group(
            ROCKET_FLIGHT_CODEC.listOf().fieldOf("launches").forGetter(data -> List.copyOf(data.launches.values()))
        ).apply(instance, RocketLaunchManager::new)));

    private final Map<UUID, RocketFlight> launches = new HashMap<>();

    public RocketLaunchManager() {
    }

    public RocketLaunchManager(final List<RocketFlight> launches) {
        for (final RocketFlight launch : launches) {
            this.launches.put(launch.id, launch);
        }
    }

    public static RocketLaunchManager get(final ServerLevel level) {
        final ServerLevel overworld = requireNonNull(level.getServer().getLevel(Level.OVERWORLD));
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public static void onServerTick(final ServerTickEvent.Post event) { //TODO: this is shit (?)
        final ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);
        if (overworld != null) {
            RocketLaunchManager.get(overworld).tick(overworld);
        }
    }

    public UUID startLaunch(final BlockPos controllerPos,
                            final UUID configurationId,
                            final Identifier destinationAsteroid,
                            final BlockStructureEntity rocket,
                            final Direction launchPadFacing) {
        final RocketFlight existing = this.getLaunchForController(controllerPos);
        if (existing != null) {
            return existing.id;
        }

        final UUID launchId = UUID.randomUUID();
        final RocketFlight launch = new RocketFlight(
            launchId,
            controllerPos.immutable(),
            configurationId,
            destinationAsteroid,
            new ArrayList<>(rocket.getStructureBlockInfos()),
            rocket.getX(),
            rocket.getY(),
            rocket.getZ(),
            rocket.getX(),
            rocket.getY(),
            rocket.getZ(),
            launchPadFacing.getStepX() * REENTRY_OFFSET_BLOCKS,
            launchPadFacing.getStepZ() * REENTRY_OFFSET_BLOCKS,
            RocketPhase.PHASE_ASCENT,
            0,
            Optional.of(rocket.getUUID()));

        rocket.setDeltaMovement(Vec3.ZERO);
        rocket.setRocketEnginesActiveness(true);

        this.launches.put(launchId, launch);
        this.setDirty();
        return launchId;
    }

    @Nullable
    public RocketLaunchSnapshot getSnapshotForController(final BlockPos controllerPos) {
        final RocketFlight launch = this.getLaunchForController(controllerPos);
        if (launch == null) {
            return null;
        }

        return new RocketLaunchSnapshot(launch.id, launch.phase, launch.phaseTick, launch.rocketY, launch.landingY, Optional.ofNullable(launch.rocketEntityId));
    }

    public boolean hasLaunchForController(final BlockPos controllerPos) { //TODO: what are we going to do if the controller was removed?
        return this.getLaunchForController(controllerPos) != null;
    }

    private void tick(final ServerLevel level) {
        boolean changed = false;
        final Iterator<RocketFlight> iterator = this.launches.values().iterator();

        while (iterator.hasNext()) {
            final RocketFlight launch = iterator.next();

            if (this.tickLaunch(level, launch)) {
                iterator.remove();
                changed = true;
            } else if (launch.phaseTick % 20 == 0) {
                changed = true;
            }
        }

        if (changed) {
            this.setDirty();
        }
    }

    private boolean tickLaunch(final ServerLevel level, final RocketFlight launch) {
        return switch (launch.phase) {
            case PHASE_ASCENT -> {
                this.tickAscent(level, launch);
                yield false;
            }
            case PHASE_ORBIT_WAIT -> {
                this.tickOrbitWait(level, launch);
                yield false;
            }
            case PHASE_DESCENT -> this.tickDescent(level, launch);
        };
    }

    private void tickAscent(final ServerLevel level, final RocketFlight launch) {
        final BlockStructureEntity rocket = this.getOrRespawnRocket(level, launch, launch.rocketX, launch.rocketY, launch.rocketZ);

        final double accelerationProgress = Mth.clamp(launch.phaseTick / 240.0D, 0.0D, 1.0D);
        final double velocityY = 0.02D + smoothStep(accelerationProgress) * 1.18D;

        rocket.setRocketEnginesActiveness(true);
        rocket.setDeltaMovement(0.0D, velocityY, 0.0D);
        this.spawnRocketSmoke(level, rocket, 6, 1.0D);
        this.rememberRocketPosition(launch, rocket);

        if (rocket.getY() >= ORBIT_Y) {
            launch.rocketX = rocket.getX();
            launch.rocketY = ORBIT_Y;
            launch.rocketZ = rocket.getZ();
            launch.rocketEntityId = null;
            launch.phase = RocketPhase.PHASE_ORBIT_WAIT;
            launch.phaseTick = 0;

            rocket.setDeltaMovement(Vec3.ZERO);
            rocket.setRocketEnginesActiveness(false);
            rocket.discardWithoutFreeingBlocks();
            this.setDirty();
            return;
        }

        launch.phaseTick++;
    }

    private void tickOrbitWait(final ServerLevel level, final RocketFlight launch) {
        if (++launch.phaseTick < ORBIT_WAIT_TICKS) {
            return;
        }

        final BlockStructureEntity rocket = this.spawnRocket(level, launch, launch.landingX + launch.reentryOffsetX, ORBIT_Y, launch.landingZ + launch.reentryOffsetZ);
        rocket.setDeltaMovement(Vec3.ZERO);
        rocket.setRocketEnginesActiveness(true);

        launch.phase = RocketPhase.PHASE_DESCENT;
        launch.phaseTick = 0;
        this.rememberRocketPosition(launch, rocket);
        this.setDirty();
    }

    private boolean tickDescent(final ServerLevel level, final RocketFlight launch) {
        final BlockStructureEntity rocket = this.getOrRespawnRocket(level, launch, launch.rocketX, launch.rocketY, launch.rocketZ);

        final double heightAboveLanding = rocket.getY() - launch.landingY;
        final double pitchHeightProgress = Mth.clamp(
            (heightAboveLanding - LANDING_FORCE_UPRIGHT_ABOVE_LANDING) / (LANDING_PITCH_START_ABOVE_LANDING - LANDING_FORCE_UPRIGHT_ABOVE_LANDING),
            0.0D,
            1.0D);
        final double easedPitchProgress = 1.0D - Math.pow(1.0D - pitchHeightProgress, 4.0D);
        final float targetPitch = (float) (LANDING_MAX_PITCH_DEGREES * easedPitchProgress);
        rocket.setTargetXRot(targetPitch); //TODO: give it a pitch only once close to landing y

        final double fullDrop = Math.max(1.0D, ORBIT_Y - launch.landingY);
        final double remainingDrop = Mth.clamp(heightAboveLanding / fullDrop, 0.0D, 1.0D);
        final double landingProgress = 1.0D - remainingDrop;
        final double easedLandingProgress = smoothStep(landingProgress);

        final double alignmentHeightProgress = Mth.clamp(
            (heightAboveLanding - REENTRY_FORCE_ALIGNED_ABOVE_LANDING)
                / (REENTRY_ALIGNMENT_START_ABOVE_LANDING - REENTRY_FORCE_ALIGNED_ABOVE_LANDING),
            0.0D,
            1.0D
        );
        final double rawAlignmentProgress = 1.0D - alignmentHeightProgress;
        final double alignmentProgress = Math.pow(rawAlignmentProgress, 2.0D);
        final double remainingOffsetMultiplier = 1.0D - alignmentProgress;

        final double desiredX = launch.landingX + launch.reentryOffsetX * remainingOffsetMultiplier;
        final double desiredZ = launch.landingZ + launch.reentryOffsetZ * remainingOffsetMultiplier;
        final double velocityX = (desiredX - rocket.getX()) * 0.45D;
        final double velocityZ = (desiredZ - rocket.getZ()) * 0.45D;
        final double velocityY = -(0.09D + (1.0D - easedLandingProgress) * 2.52D);

        rocket.setRocketEnginesActiveness(true);
        //TODO: I don't like this descent smoke right now (completely the vision of the rocket)
        //this.spawnRocketSmoke(level, rocket, 1, 1.25D);

        if (rocket.getY() + velocityY <= launch.landingY) {
            rocket.setTargetXRot(0F);
            rocket.setPos(launch.landingX, launch.landingY, launch.landingZ);
            rocket.setDeltaMovement(Vec3.ZERO);
            rocket.setRocketEnginesActiveness(false);

            rocket.remove(Entity.RemovalReason.DISCARDED);
            this.finishLaunch(level, launch);
            return true;
        }

        rocket.setDeltaMovement(velocityX, velocityY, velocityZ);
        this.rememberRocketPosition(launch, rocket);
        launch.phaseTick++;
        return false;
    }

    @Nullable
    private BlockStructureEntity getRocket(final ServerLevel level, final RocketFlight launch) {
        if (launch.rocketEntityId == null) {
            return null;
        }

        final Entity entity = level.getEntity(launch.rocketEntityId);
        return entity instanceof BlockStructureEntity blockStructureEntity ? blockStructureEntity : null;
    }

    private BlockStructureEntity getOrRespawnRocket(final ServerLevel level,
                                                    final RocketFlight launch,
                                                    final double x,
                                                    final double y,
                                                    final double z) {
        final BlockStructureEntity existing = this.getRocket(level, launch);
        if (existing != null) {
            return existing;
        }
        return this.spawnRocket(level, launch, x, y, z);
    }

    private BlockStructureEntity spawnRocket(final ServerLevel level,
                                             final RocketFlight launch,
                                             final double x,
                                             final double y,
                                             final double z) {
        final BlockStructureEntity rocket = new BlockStructureEntity(level, launch.structureBlockInfos, true, x, y, z);

        rocket.setDeltaMovement(Vec3.ZERO);
        rocket.setRocketEnginesActiveness(true);
        level.addFreshEntity(rocket);

        launch.rocketEntityId = rocket.getUUID();
        this.rememberRocketPosition(launch, rocket);
        this.setDirty();
        return rocket;
    }

    private void rememberRocketPosition(final RocketFlight launch, final BlockStructureEntity rocket) {
        launch.rocketX = rocket.getX();
        launch.rocketY = rocket.getY();
        launch.rocketZ = rocket.getZ();
    }

    private void spawnRocketSmoke(final ServerLevel level,
                                  final BlockStructureEntity rocket,
                                  final int count,
                                  final double radius) {
        final RandomSource random = level.getRandom();
        final Vec3 base = rocket.position().add(0.0D, -0.25D, 0.0D);

        // TODO: spawn under each rocket engine instead
        for (int i = 0; i < count; i++) {
            final double offsetX = CommonUtils.randomOffset(random, (float) radius);
            final double offsetY = random.nextDouble() * 0.01D;
            final double offsetZ = CommonUtils.randomOffset(random, (float) radius);

            level.sendParticles(ModParticles.BIG_SMOKE_PARTICLE.get(), true, true, base.x(), base.y() - 4.0D, base.z(), 2, offsetX, offsetY, offsetZ, 0.0D);
        }
    }

    private void finishLaunch(final ServerLevel level, final RocketFlight launch) {
        final ConfigurationSavedData configurationData = ConfigurationSavedData.getConfigurationData(level);
        final NetworkConfiguration configuration = configurationData.get(launch.configurationId);

        if (configuration != null) {
            final Optional<AsteroidConfig> asteroid = AsteroidReloadListener.INSTANCE.getData().values()
                .stream()
                .filter(config -> config.getId().equals(launch.destinationAsteroid))
                .findFirst();

            asteroid.ifPresent(config -> {
                configuration.moduleProperties().addResources(config.getComposition());
                configurationData.set(launch.configurationId, configuration);
            });
        }

        if (level.getBlockEntity(launch.controllerPos) instanceof RocketControllerBlockEntity controller) {
            controller.onManagedRocketLanded();
        }
    }

    @Nullable
    private RocketFlight getLaunchForController(final BlockPos controllerPos) {
        for (final RocketFlight launch : this.launches.values()) {
            if (launch.controllerPos.equals(controllerPos)) {
                return launch;
            }
        }

        return null;
    }

    private static double smoothStep(final double value) {
        final double t = Mth.clamp(value, 0.0D, 1.0D);
        return t * t * (3.0D - 2.0D * t);
    }

    public record RocketLaunchSnapshot(UUID id, RocketPhase phase, int phaseTick, double rocketY, double landingY, Optional<UUID> rocketEntityId) {
        public boolean isAscending() {
            return this.phase == RocketPhase.PHASE_ASCENT;
        }

        public boolean isWaitingInOrbit() {
            return this.phase == RocketPhase.PHASE_ORBIT_WAIT;
        }

        public boolean isDescending() {
            return this.phase == RocketPhase.PHASE_DESCENT;
        }

        public double getRocketHeightAboveLanding() {
            return this.rocketY - this.landingY;
        }
    }

    public static final class RocketFlight {
        private final UUID id;
        private final BlockPos controllerPos;
        private final UUID configurationId;
        private final Identifier destinationAsteroid;
        private final List<StructureTemplate.StructureBlockInfo> structureBlockInfos;
        private final double landingX;
        private final double landingY;
        private final double landingZ;
        private double rocketX;
        private double rocketY;
        private double rocketZ;
        private final double reentryOffsetX;
        private final double reentryOffsetZ;
        private RocketPhase phase;
        private int phaseTick;
        @Nullable
        private UUID rocketEntityId;

        public RocketFlight(final UUID id,
                            final BlockPos controllerPos,
                            final UUID configurationId,
                            final Identifier destinationAsteroid,
                            final List<StructureTemplate.StructureBlockInfo> structureBlockInfos,
                            final double landingX,
                            final double landingY,
                            final double landingZ,
                            final double rocketX,
                            final double rocketY,
                            final double rocketZ,
                            final double reentryOffsetX,
                            final double reentryOffsetZ,
                            final RocketPhase phase,
                            final int phaseTick,
                            final Optional<UUID> rocketEntityId) {
            this.id = id;
            this.controllerPos = controllerPos;
            this.configurationId = configurationId;
            this.destinationAsteroid = destinationAsteroid;
            this.structureBlockInfos = List.copyOf(structureBlockInfos);
            this.landingX = landingX;
            this.landingY = landingY;
            this.landingZ = landingZ;
            this.rocketX = rocketX;
            this.rocketY = rocketY;
            this.rocketZ = rocketZ;
            this.reentryOffsetX = reentryOffsetX;
            this.reentryOffsetZ = reentryOffsetZ;
            this.phase = phase;
            this.phaseTick = phaseTick;
            this.rocketEntityId = rocketEntityId.orElse(null);
        }
    }

    public enum RocketPhase implements StringRepresentable {
        PHASE_ASCENT("ascent"),
        PHASE_ORBIT_WAIT("orbit_wait"),
        PHASE_DESCENT("descent");

        public static final Codec<RocketPhase> CODEC = StringRepresentable.fromEnum(RocketPhase::values);

        private final String serializedName;

        RocketPhase(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return this.serializedName;
        }
    }
}
