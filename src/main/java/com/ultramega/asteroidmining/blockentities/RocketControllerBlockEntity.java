package com.ultramega.asteroidmining.blockentities;

import com.ultramega.asteroidmining.asteroids.AsteroidConfig;
import com.ultramega.asteroidmining.blocks.AbstractModuleBlock;
import com.ultramega.asteroidmining.blocks.RocketEngineBlock;
import com.ultramega.asteroidmining.container.RocketControllerContainerMenu;
import com.ultramega.asteroidmining.entities.BlockStructureEntity;
import com.ultramega.asteroidmining.events.AsteroidReloadListener;
import com.ultramega.asteroidmining.launch.RocketLaunchManager;
import com.ultramega.asteroidmining.network.s2c.HidePreviewBlocksPayload;
import com.ultramega.asteroidmining.registry.ModBlockEntityTypes;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.registry.ModDataComponentTypes;
import com.ultramega.asteroidmining.registry.ModParticles;
import com.ultramega.asteroidmining.registry.ModSounds;
import com.ultramega.asteroidmining.storage.ConfigurationSavedData;
import com.ultramega.asteroidmining.storage.NetworkConfiguration;
import com.ultramega.asteroidmining.storage.RocketProperties;
import com.ultramega.asteroidmining.utils.CommonUtils;
import com.ultramega.asteroidmining.utils.PreserveData;
import com.ultramega.asteroidmining.utils.handlers.RocketControllerItemStacksResourceHandler;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

import static com.ultramega.asteroidmining.utils.CommonUtils.getMinCorner;
import static com.ultramega.asteroidmining.utils.CommonUtils.toLocalPositions;

// TODO: breaking the rocket controller on launch breaks everything
public class RocketControllerBlockEntity extends AbstractModuleBlockEntity implements MenuProvider, Nameable, PreserveData {
    private static final int COUNTDOWN_COMMENTARY_SECONDS = 10;
    private static final int COUNTDOWN_TICKS_PER_SECOND = 20;
    private static final int SMOKE_START_TICKS = 2 * COUNTDOWN_TICKS_PER_SECOND;
    private static final int LAUNCH_OVERLAY_RANGE = 100;

    private static final float CHOPSTICK_OPEN_DEGREES = 40F;
    private static final double DESCENT_OPEN_HEIGHT_ABOVE_CHOPSTICKS = 50.0D;
    private static final double DESCENT_CLOSE_HEIGHT_ABOVE_CHOPSTICKS = 8.0D;

    public final RocketControllerItemStacksResourceHandler inventoryHandler = new RocketControllerItemStacksResourceHandler(3, this);

    private final Set<BlockPos> connectedModules = new LinkedHashSet<>();

    private int selectedConfigurationIndex = -1;

    private int launchCooldown;
    private int nextLaunchCooldown = 20 * 10; // default: 10 seconds
    private int launchCooldownTick;
    private boolean launchCooldownOverlay = true;
    private boolean launchCooldownCommentator = true;
    private boolean playedTMinusSound = true;
    private int lastCommentatedSecond = Integer.MIN_VALUE;
    private boolean managedLaunchStarted = false;

    private boolean wasDescending = false;
    private boolean descentChopsticksOpened = false;
    private boolean descentChopsticksClosed = false;

    private boolean launchingRocket;
    private int launchingRocketTick;
    @Nullable
    private Identifier destinationAsteroid;

    @Nullable
    private BlockStructureEntity launchedRocket;
    @Nullable
    private UUID launchedRocketId;
    @Nullable
    private BlockStructureEntity chopstick1;
    @Nullable
    private UUID chopstick1Id;
    @Nullable
    private BlockStructureEntity chopstick2;
    @Nullable
    private UUID chopstick2Id;

    public RocketControllerBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntityTypes.ROCKET_CONTROLLER.get(), pos, blockState);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final RocketControllerBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // TODO: starting a rocket from the same launch pad is possible as of now

        blockEntity.resolveControllerOwnedEntities(serverLevel);

        if (!blockEntity.launchingRocket) {
            return;
        }

        final RocketLaunchManager launchManager = RocketLaunchManager.get(serverLevel);
        if (blockEntity.managedLaunchStarted) {
            final RocketLaunchManager.RocketLaunchSnapshot snapshot = launchManager.getSnapshotForController(pos);
            if (snapshot == null) {
                blockEntity.finishManagedLaunch();
                return;
            }

            blockEntity.tickChopsticks(snapshot);
            blockEntity.launchingRocketTick++;
            blockEntity.setChanged();
            return;
        }

        if (blockEntity.getSelectedConfigurationIndex() == -1) {
            return;
        }

        final ItemResource stack = blockEntity.inventoryHandler.getResource(blockEntity.getSelectedConfigurationIndex());
        if (stack.isEmpty() || !stack.has(ModDataComponentTypes.CONFIGURATION_PATH_DATA)) {
            blockEntity.cancelLaunch();
            return;
        }

        final UUID configurationId = stack.get(ModDataComponentTypes.CONFIGURATION_PATH_DATA);
        if (configurationId == null) {
            blockEntity.cancelLaunch();
            return;
        }

        final NetworkConfiguration configuration = ConfigurationSavedData.getConfigurationData(serverLevel).get(configurationId);
        if (configuration == null || blockEntity.destinationAsteroid == null) {
            blockEntity.cancelLaunch();
            return;
        }

        final CommonUtils.SpacePortAnalysis spacePortAnalysis = CommonUtils.analyzeSpacePort(level, configuration);
        if (!spacePortAnalysis.valid() && blockEntity.chopstick1 == null) {
            return;
        }

        final int ticksRemaining = Math.max(0, blockEntity.launchCooldown - blockEntity.launchCooldownTick);
        if (ticksRemaining <= SMOKE_START_TICKS) {
            blockEntity.ensureLaunchEntitiesBuilt(spacePortAnalysis);
            blockEntity.spawnCountdownSmoke(serverLevel);
        }

        if (blockEntity.launchCooldownTick < blockEntity.launchCooldown) {
            blockEntity.playCountdownCommentary(serverLevel, pos, ticksRemaining);
            blockEntity.sendCountdownOverlay(serverLevel, ticksRemaining);
            blockEntity.launchCooldownTick++;
            blockEntity.setChanged();
            return;
        }

        blockEntity.playCountdownCommentary(serverLevel, pos, 0);
        blockEntity.ensureLaunchEntitiesBuilt(spacePortAnalysis);

        if (blockEntity.launchedRocket != null) {
            final Direction facing = configuration.launchPadConfiguration().facing();
            launchManager.startLaunch(pos, configurationId, blockEntity.destinationAsteroid, blockEntity.launchedRocket, facing.getOpposite());

            // Launch manager now owns and manages the rocket entity
            blockEntity.launchedRocket = null;
            blockEntity.launchedRocketId = null;
            blockEntity.managedLaunchStarted = true;
            blockEntity.launchingRocketTick = 0;
            blockEntity.setChanged();
        }
    }

    private static int getCommentarySecondForFirstTick(final int launchCooldownTick, final int ticksRemaining) {
        if (ticksRemaining < 0) {
            return -1;
        }

        final int second = Math.min(COUNTDOWN_COMMENTARY_SECONDS, (ticksRemaining + COUNTDOWN_TICKS_PER_SECOND - 1) / COUNTDOWN_TICKS_PER_SECOND);
        if (second < 0) {
            return -1;
        }

        final boolean firstTickOfCountdown = launchCooldownTick == 0;
        final boolean firstTickOfDisplayedSecond = ticksRemaining % COUNTDOWN_TICKS_PER_SECOND == 0;

        return firstTickOfCountdown || firstTickOfDisplayedSecond ? second : -1;
    }

    private void playCountdownCommentary(final ServerLevel serverLevel, final BlockPos pos, final int ticksRemaining) {
        if (!this.launchCooldownCommentator || this.launchCooldown <= 0) {
            return;
        }

        final int second = getCommentarySecondForFirstTick(this.launchCooldownTick, ticksRemaining);
        if (second < 0 || second == this.lastCommentatedSecond) {
            return;
        }

        if (!this.playedTMinusSound) {
            this.playedTMinusSound = true;
            serverLevel.playSound(null, pos, ModSounds.LAUNCH_T_MINUS.value(), SoundSource.BLOCKS, 2.0F, 0.95F);
        }

        final SoundEvent sound = switch (second) {
            case 10 -> ModSounds.LAUNCH_10.value();
            case 9 -> ModSounds.LAUNCH_9.value();
            case 8 -> ModSounds.LAUNCH_8.value();
            case 7 -> ModSounds.LAUNCH_7.value();
            case 6 -> ModSounds.LAUNCH_6.value();
            case 5 -> ModSounds.LAUNCH_5.value();
            case 4 -> ModSounds.LAUNCH_4.value();
            case 3 -> ModSounds.LAUNCH_3.value();
            case 2 -> ModSounds.LAUNCH_2.value();
            case 1 -> ModSounds.LAUNCH_1.value();
            default -> null;
        };

        if (sound != null) {
            serverLevel.playSound(null, pos, sound, SoundSource.BLOCKS, 2.0F, 0.95F);
        }

        this.lastCommentatedSecond = second;
    }

    private void resolveControllerOwnedEntities(final ServerLevel serverLevel) {
        if (this.launchedRocketId != null && this.launchedRocket == null && serverLevel.getEntity(this.launchedRocketId) instanceof BlockStructureEntity entity) {
            this.launchedRocket = entity;
        }

        if (this.chopstick1Id != null && this.chopstick1 == null && serverLevel.getEntity(this.chopstick1Id) instanceof BlockStructureEntity entity) {
            this.chopstick1 = entity;
        }

        if (this.chopstick2Id != null && this.chopstick2 == null && serverLevel.getEntity(this.chopstick2Id) instanceof BlockStructureEntity entity) {
            this.chopstick2 = entity;
        }
    }

    private void ensureLaunchEntitiesBuilt(final CommonUtils.SpacePortAnalysis spacePortAnalysis) {
        if (this.launchedRocket == null) {
            this.buildEntitiesFromBlocks(spacePortAnalysis);
            this.openChopsticks();
        }
    }

    private void sendCountdownOverlay(final ServerLevel serverLevel, final int ticksRemaining) {
        if (!this.launchCooldownOverlay) {
            return;
        }

        final int secondsRemaining = Math.clamp((ticksRemaining + 19) / 20, 0, COUNTDOWN_COMMENTARY_SECONDS);
        final List<Player> players = serverLevel.getNearbyPlayers(TargetingConditions.forNonCombat(), null, new AABB(this.getBlockPos()).inflate(LAUNCH_OVERLAY_RANGE));
        for (final Player player : players) {
            player.sendOverlayMessage(Component.literal("T-" + secondsRemaining));
        }
    }

    private void spawnCountdownSmoke(final ServerLevel serverLevel) {
        if (this.launchedRocket == null) {
            return;
        }

        final Vec3 rocketPos = this.launchedRocket.position();
        final RandomSource random = serverLevel.getRandom();
        final double offsetX = CommonUtils.randomOffset(random, 1F);
        final double offsetY = random.nextDouble() * 0.01D;
        final double offsetZ = CommonUtils.randomOffset(random, 1F);

        serverLevel.sendParticles(ModParticles.BIG_SMOKE_PARTICLE.get(), true, true, rocketPos.x, rocketPos.y - 2.0D, rocketPos.z, 10, offsetX, offsetY, offsetZ, 0.0D);
    }

    private void tickChopsticks(final RocketLaunchManager.RocketLaunchSnapshot snapshot) {
        if (this.chopstick1 == null || this.chopstick2 == null) {
            return;
        }

        if (snapshot.isAscending()) {
            this.wasDescending = false;

            if (snapshot.phaseTick() == 80) {
                this.openChopsticks();
            } else if (snapshot.phaseTick() == 120) {
                this.closeChopsticks();

                PacketDistributor.sendToAllPlayers(new HidePreviewBlocksPayload(this.getBlockPos(), false));
            }

            return;
        } else if (snapshot.isDescending()) {
            if (!this.wasDescending) {
                this.wasDescending = true;
                this.descentChopsticksOpened = false;
                this.descentChopsticksClosed = false;
            }

            final double heightAboveChopsticks = snapshot.getRocketHeightAboveLanding();
            if (!this.descentChopsticksClosed && heightAboveChopsticks <= DESCENT_CLOSE_HEIGHT_ABOVE_CHOPSTICKS) {
                this.closeChopsticks();
                this.descentChopsticksClosed = true;
                return;
            }

            if (!this.descentChopsticksOpened && heightAboveChopsticks <= DESCENT_OPEN_HEIGHT_ABOVE_CHOPSTICKS) {
                this.openChopsticks();
                this.descentChopsticksOpened = true;
            }

            return;
        }

        this.wasDescending = false;
    }

    private void openChopsticks() {
        if (this.chopstick1 == null || this.chopstick2 == null) {
            return;
        }
        this.chopstick1.setTargetYRot(CHOPSTICK_OPEN_DEGREES);
        this.chopstick2.setTargetYRot(-CHOPSTICK_OPEN_DEGREES);
    }

    private void closeChopsticks() {
        if (this.chopstick1 == null || this.chopstick2 == null) {
            return;
        }
        this.chopstick1.setTargetYRot(0F);
        this.chopstick2.setTargetYRot(0F);
    }

    public void onManagedRocketLanded() {
        this.finishManagedLaunch();
    }

    private void cancelLaunch() {
        if (this.launchedRocket != null) {
            this.launchedRocket.remove(Entity.RemovalReason.DISCARDED);
            this.launchedRocket = null;
            this.launchedRocketId = null;
        }

        this.finishManagedLaunch();
    }

    private void finishManagedLaunch() {
        if (this.chopstick1 != null) {
            this.chopstick1.remove(Entity.RemovalReason.DISCARDED);
            this.chopstick1 = null;
            this.chopstick1Id = null;
        }

        if (this.chopstick2 != null) {
            this.chopstick2.remove(Entity.RemovalReason.DISCARDED);
            this.chopstick2 = null;
            this.chopstick2Id = null;
        }

        this.launchCooldownTick = 0;
        this.launchingRocketTick = 0;
        this.launchingRocket = false;
        this.managedLaunchStarted = false;
        this.playedTMinusSound = true;
        this.lastCommentatedSecond = Integer.MIN_VALUE;
        this.setChanged();
    }

    public void buildEntitiesFromBlocks(final CommonUtils.SpacePortAnalysis spacePortAnalysis) {
        if (this.level == null || !spacePortAnalysis.valid() || spacePortAnalysis.rocketPositions().isEmpty()) {
            return;
        }

        final List<BlockPos> rocketPos = spacePortAnalysis.rocketPositions();
        final BlockPos rocketOrigin = getMinCorner(rocketPos);
        final List<BlockPos> rocketLocalPos = toLocalPositions(rocketPos, rocketOrigin);

        final BlockStructureEntity rocketEntity = new BlockStructureEntity(this.level, rocketPos, rocketLocalPos, true);
        rocketEntity.setPos(rocketOrigin.getX() + 0.5, rocketOrigin.getY(), rocketOrigin.getZ() + 0.5);

        this.level.addFreshEntity(rocketEntity);
        this.launchedRocketId = rocketEntity.getUUID();
        this.launchedRocket = rocketEntity;

        final List<BlockPos> chopstick1Pos = spacePortAnalysis.chopstick1Positions();
        final List<BlockPos> chopstick2Pos = spacePortAnalysis.chopstick2Positions();

        PacketDistributor.sendToAllPlayers(new HidePreviewBlocksPayload(this.getBlockPos(), true));

        final BlockPos chopstick1Origin = getMinCorner(chopstick1Pos);
        final BlockPos chopstick2Origin = getMinCorner(chopstick2Pos);

        final List<BlockPos> chopstick1LocalPos = toLocalPositions(chopstick1Pos, chopstick1Origin);
        final List<BlockPos> chopstick2LocalPos = toLocalPositions(chopstick2Pos, chopstick2Origin);

        final BlockPos pivotWorldPos = spacePortAnalysis.chopstickPivotWorldPos();

        final BlockStructureEntity chopstick1Entity = new BlockStructureEntity(this.level, chopstick1Pos, chopstick1LocalPos, false);
        chopstick1Entity.setPos(chopstick1Origin.getX() + 0.5, chopstick1Origin.getY(), chopstick1Origin.getZ() + 0.5);
        chopstick1Entity.setPivotPoint(pivotWorldPos.subtract(chopstick1Origin));

        this.level.addFreshEntity(chopstick1Entity);
        this.chopstick1Id = chopstick1Entity.getUUID();
        this.chopstick1 = chopstick1Entity;

        final BlockStructureEntity chopstick2Entity = new BlockStructureEntity(this.level, chopstick2Pos, chopstick2LocalPos, false);
        chopstick2Entity.setPos(chopstick2Origin.getX() + 0.5, chopstick2Origin.getY(), chopstick2Origin.getZ() + 0.5);
        chopstick2Entity.setPivotPoint(pivotWorldPos.subtract(chopstick2Origin));

        this.level.addFreshEntity(chopstick2Entity);
        this.chopstick2Id = chopstick2Entity.getUUID();
        this.chopstick2 = chopstick2Entity;
    }

    public void updateRocketStats() {
        // TODO: refactor
        if (this.launchingRocket || !(this.level instanceof ServerLevel serverLevel)) {
            return;
        }

        final UUID uuid = this.getSelectedConfigurationUUID();
        if (uuid != null) {
            final ConfigurationSavedData data = ConfigurationSavedData.getConfigurationData(serverLevel);
            final NetworkConfiguration configuration = data.get(uuid);
            if (configuration != null) {
                data.set(uuid, new NetworkConfiguration(configuration.launchPadConfiguration(),
                    Optional.of(this.calculateRocketStats(configuration)), configuration.moduleProperties()));
            }
        }
    }

    private RocketProperties calculateRocketStats(final NetworkConfiguration configuration) {
        int weight = 0;
        int thrustForce = 0;
        final int fuelUsage = 0;

        if (this.level != null) {
            final CommonUtils.SpacePortAnalysis spacePortAnalysis = CommonUtils.analyzeSpacePort(this.level, configuration);
            for (final BlockPos rocketPos : spacePortAnalysis.rocketPositions()) {
                final BlockState state = this.level.getBlockState(rocketPos);
                if (state.getBlock() instanceof RocketEngineBlock rocketEngineBlock) {
                    thrustForce += rocketEngineBlock.getType().getThrustForce();
                    weight += rocketEngineBlock.getType().getWeight();
                } else if (!state.isAir()) {
                    weight += 1;
                }
            }
        }

        return new RocketProperties(weight, thrustForce, fuelUsage);
    }

    public void updateConnectedModules() {
        this.connectedModules.clear();
        this.connectedModules.add(this.getBlockPos());

        final Set<BlockPos> checked = new LinkedHashSet<>();
        final Queue<BlockPos> toCheck = new LinkedList<>();
        toCheck.add(this.getBlockPos());

        while (!toCheck.isEmpty()) {
            final BlockPos checkPos = toCheck.poll();
            checked.add(checkPos);

            for (final Direction direction : Direction.values()) {
                final BlockPos adjacentPos = checkPos.relative(direction);
                if (checked.contains(adjacentPos)) {
                    continue;
                }

                final BlockState adjacentState = this.level.getBlockState(adjacentPos);
                if (adjacentState.getBlock() instanceof AbstractModuleBlock) {
                    if (this.level.getBlockEntity(adjacentPos) instanceof AbstractModuleBlockEntity moduleBlockEntity) {
                        moduleBlockEntity.setControllerPos(this.getBlockPos());
                        moduleBlockEntity.setChanged();
                    }
                    //TODO: active = false missing
                    this.level.setBlock(adjacentPos, adjacentState.setValue(AbstractModuleBlock.ACTIVE, true), Block.UPDATE_ALL);

                    this.connectedModules.add(adjacentPos);
                    toCheck.add(adjacentPos);
                }
            }
        }

        this.setChanged();
    }

    public Set<BlockPos> getConnectedModules() {
        return this.connectedModules;
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);

        this.selectedConfigurationIndex = input.getIntOr("selectedConfigurationIndex", -1);

        this.inventoryHandler.deserialize(input.childOrEmpty("inventory"));

        this.connectedModules.clear();
        this.connectedModules.addAll(input.read("connectedModules", CommonUtils.BLOCK_POS_LIST).orElse(List.of()));

        this.launchCooldown = input.getIntOr("launchCooldown", 0);
        this.nextLaunchCooldown = input.getIntOr("nextLaunchCooldown", 20 * 10);
        this.launchCooldownTick = input.getIntOr("launchCooldownTick", 0);
        this.launchCooldownOverlay = input.getBooleanOr("launchCooldownOverlay", true);
        this.launchCooldownCommentator = input.getBooleanOr("launchCooldownCommentator", true);
        this.playedTMinusSound = input.getBooleanOr("playedTMinusSound", true);
        this.lastCommentatedSecond = input.getIntOr("lastCommentatedSecond", Integer.MIN_VALUE);
        this.managedLaunchStarted = input.getBooleanOr("managedLaunchStarted", false);

        this.wasDescending = input.getBooleanOr("wasDescending", false);
        this.descentChopsticksOpened = input.getBooleanOr("descentChopsticksOpened", false);
        this.descentChopsticksClosed = input.getBooleanOr("descentChopsticksClosed", false);

        this.launchingRocket = input.getBooleanOr("launchingRocket", false);
        this.launchingRocketTick = input.getIntOr("launchingRocketTick", 0);
        this.destinationAsteroid = input.read("destinationAsteroid", Identifier.CODEC).orElse(null);

        this.launchedRocketId = input.read("launchedRocketId", UUIDUtil.CODEC).orElse(null);
        this.chopstick1Id = input.read("chopstick1Id", UUIDUtil.CODEC).orElse(null);
        this.chopstick2Id = input.read("chopstick2Id", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);

        output.putInt("selectedConfigurationIndex", this.selectedConfigurationIndex);

        this.inventoryHandler.serialize(output.child("inventory"));

        output.store("connectedModules", CommonUtils.BLOCK_POS_LIST, this.connectedModules.stream().toList());

        output.putInt("launchCooldown", this.launchCooldown);
        output.putInt("nextLaunchCooldown", this.nextLaunchCooldown);
        output.putInt("launchCooldownTick", this.launchCooldownTick);
        output.putBoolean("launchCooldownOverlay", this.launchCooldownOverlay);
        output.putBoolean("launchCooldownCommentator", this.launchCooldownCommentator);
        output.putBoolean("playedTMinusSound", this.playedTMinusSound);
        output.putInt("lastCommentatedSecond", this.lastCommentatedSecond);
        output.putBoolean("managedLaunchStarted", this.managedLaunchStarted);

        output.putBoolean("wasDescending", this.wasDescending);
        output.putBoolean("descentChopsticksOpened", this.descentChopsticksOpened);
        output.putBoolean("descentChopsticksClosed", this.descentChopsticksClosed);

        output.putBoolean("launchingRocket", this.launchingRocket);
        output.putInt("launchingRocketTick", this.launchingRocketTick);
        if (this.destinationAsteroid != null) {
            output.store("destinationAsteroid", Identifier.CODEC, this.destinationAsteroid);
        }

        if (this.launchedRocketId != null) {
            output.store("launchedRocketId", UUIDUtil.CODEC, this.launchedRocketId);
        }
        if (this.chopstick1Id != null) {
            output.store("chopstick1Id", UUIDUtil.CODEC, this.chopstick1Id);
        }
        if (this.chopstick2Id != null) {
            output.store("chopstick2Id", UUIDUtil.CODEC, this.chopstick2Id);
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();

        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Override
    public Component getName() {
        return Component.translatable(ModBlocks.ROCKET_CONTROLLER.get().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        if (this.level == null) {
            return null;
        }
        final var menu = new RocketControllerContainerMenu(containerId, inventory, this, ContainerLevelAccess.create(this.level, this.getBlockPos()));
        menu.setOverwriteStillValid(this.overwriteStillValid);
        return menu;
    }

    public void setLaunchingRocket(final boolean launchingRocket) {
        final var config = this.getSelectedServerNetworkConfiguration();
        if (config == null) {
            return;
        }

        final var optionalSelectedAsteroid = config.moduleProperties().selectedAsteroid();
        if (optionalSelectedAsteroid.isEmpty()) {
            return;
        }

        final var selectedAsteroid = optionalSelectedAsteroid.get();
        if (this.getAsteroidConfig(selectedAsteroid).isEmpty()) {
            return;
        }

        this.launchingRocket = launchingRocket;
        this.launchCooldown = Math.max(0, this.nextLaunchCooldown);
        this.launchCooldownTick = 0;
        this.launchingRocketTick = 0;
        this.managedLaunchStarted = false;
        this.playedTMinusSound = false;
        this.lastCommentatedSecond = Integer.MIN_VALUE;
        this.destinationAsteroid = selectedAsteroid; //TODO: remove destinationAsteroid?
        this.setChanged();
    }

    public void setSelectedConfigurationIndex(final int selectedConfigurationIndex) {
        this.selectedConfigurationIndex = selectedConfigurationIndex;
        this.updateRocketStats();
    }

    public int getSelectedConfigurationIndex() {
        return this.selectedConfigurationIndex;
    }

    public void setNextLaunchCooldown(final int nextLaunchCooldown) {
        this.nextLaunchCooldown = nextLaunchCooldown;
    }

    public int getNextLaunchCooldown() {
        return this.nextLaunchCooldown;
    }

    public void setLaunchCooldownOverlay(final boolean launchCooldownOverlay) {
        this.launchCooldownOverlay = launchCooldownOverlay;
    }

    public boolean isLaunchCooldownOverlay() {
        return this.launchCooldownOverlay;
    }

    public void setLaunchCooldownCommentator(final boolean launchCooldownCommentator) {
        this.launchCooldownCommentator = launchCooldownCommentator;
    }

    public boolean isLaunchCooldownCommentator() {
        return this.launchCooldownCommentator;
    }

    public void setPlayedTMinusSound(final boolean playedTMinusSound) {
        this.playedTMinusSound = playedTMinusSound;
    }

    public Optional<AsteroidConfig> getAsteroidConfig(final Identifier asteroidId) {
        return AsteroidReloadListener.INSTANCE.getData().values()
            .stream()
            .filter(config -> config.getId().equals(asteroidId))
            .findFirst();
    }
}
