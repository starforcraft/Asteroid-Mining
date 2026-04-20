package com.ultramega.asteroidmining.blockentities;

import com.ultramega.asteroidmining.blocks.AbstractModuleBlock;
import com.ultramega.asteroidmining.blocks.RocketEngineBlock;
import com.ultramega.asteroidmining.container.RocketControllerContainerMenu;
import com.ultramega.asteroidmining.entities.BlockStructureEntity;
import com.ultramega.asteroidmining.events.AsteroidReloadListener;
import com.ultramega.asteroidmining.events.ClientEvents;
import com.ultramega.asteroidmining.network.s2c.HidePreviewBlocksMessage;
import com.ultramega.asteroidmining.network.s2c.SendLaunchPreviewDataMessage;
import com.ultramega.asteroidmining.registry.ModBlockEntityTypes;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.registry.ModDataComponentTypes;
import com.ultramega.asteroidmining.registry.ModParticles;
import com.ultramega.asteroidmining.registry.ModSounds;
import com.ultramega.asteroidmining.storage.ConfigurationSavedData;
import com.ultramega.asteroidmining.storage.NetworkConfiguration;
import com.ultramega.asteroidmining.storage.RocketProperties;
import com.ultramega.asteroidmining.utils.AsteroidConfig;
import com.ultramega.asteroidmining.utils.ItemFluidStack;
import com.ultramega.asteroidmining.utils.PreviewInfo;
import com.ultramega.asteroidmining.utils.Utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;

import static com.ultramega.asteroidmining.utils.Utils.rotateOffset;

public class RocketControllerBlockEntity extends AbstractModuleBlockEntity implements MenuProvider, Nameable {
    public final RocketControllerItemStacksResourceHandler inventoryHandler = new RocketControllerItemStacksResourceHandler(3);

    private final Set<BlockPos> connectedModules = new LinkedHashSet<>();

    private int selectedConfigurationIndex = -1;

    private int launchCooldown;
    public int nextLaunchCooldown = 20 * 10; // default: 10 seconds
    private int launchCooldownTick;
    public boolean launchCooldownOverlay = true;
    public boolean launchCooldownCommentator = true;
    public boolean playedTMinusSound = true;

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

    public static void clientTick(final Level level, final BlockPos pos, final BlockState state, final RocketControllerBlockEntity blockEntity) {
        /*if (blockEntity.launchedRocket != null) {
            if (blockEntity.launchingRocketTick == 720) {
                blockEntity.launchedRocket.setRocketEnginesActiveness(false);
            } else if (blockEntity.launchingRocketTick == 760) {
                blockEntity.launchedRocket.setRocketEnginesActiveness(true);
            }
        }*/

        if (!blockEntity.launchCooldownCommentator) {
            return;
        }

        final SoundManager soundManager = Minecraft.getInstance().getSoundManager();

        if (blockEntity.launchCooldown == 0) {
            return;
        }

        final int secondsTillLaunch = (blockEntity.launchCooldown - blockEntity.launchCooldownTick + 15);
        if (secondsTillLaunch == -1 + 15 || secondsTillLaunch == blockEntity.launchCooldown + 15) {
            return;
        }

        if (secondsTillLaunch <= 10 * 20 + 15 && !blockEntity.playedTMinusSound) {
            blockEntity.playedTMinusSound = true;
            final SimpleSoundInstance instance2 = new SimpleSoundInstance(ModSounds.LAUNCH_T_MINUS.value(),
                SoundSource.BLOCKS, 2.0f, 0.95f,
                level.getRandom(), pos.getX(), pos.getY(), pos.getZ());
            soundManager.play(instance2);
        }

        // TODO: Switch to own AbstractTickableSoundInstance?
        SoundEvent sound = null;
        if (secondsTillLaunch == 10 * 20) {
            sound = ModSounds.LAUNCH_10.value();
        } else if (secondsTillLaunch == 9 * 20) {
            sound = ModSounds.LAUNCH_9.value();
        } else if (secondsTillLaunch == 8 * 20) {
            sound = ModSounds.LAUNCH_8.value();
        } else if (secondsTillLaunch == 7 * 20) {
            sound = ModSounds.LAUNCH_7.value();
        } else if (secondsTillLaunch == 6 * 20) {
            sound = ModSounds.LAUNCH_6.value();
        } else if (secondsTillLaunch == 5 * 20) {
            sound = ModSounds.LAUNCH_5.value();
        } else if (secondsTillLaunch == 4 * 20) {
            sound = ModSounds.LAUNCH_4.value();
        } else if (secondsTillLaunch == 3 * 20) {
            sound = ModSounds.LAUNCH_3.value();
        } else if (secondsTillLaunch == 2 * 20) {
            sound = ModSounds.LAUNCH_2.value();
        } else if (secondsTillLaunch == 20) {
            sound = ModSounds.LAUNCH_1.value();
        }

        if (sound != null) {
            final SimpleSoundInstance instance = new SimpleSoundInstance(sound,
                SoundSource.BLOCKS, 2.0f, 0.95f,
                level.getRandom(), pos.getX(), pos.getY(), pos.getZ());

            soundManager.play(instance);
        }
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final RocketControllerBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Load entities
        if (blockEntity.launchedRocketId != null
            && blockEntity.launchedRocket == null
            && serverLevel.getEntity(blockEntity.launchedRocketId) instanceof BlockStructureEntity entity) {
            blockEntity.launchedRocket = entity;
        }
        if (blockEntity.chopstick1Id != null
            && blockEntity.chopstick1 == null
            && serverLevel.getEntity(blockEntity.chopstick1Id) instanceof BlockStructureEntity entity) {
            blockEntity.chopstick1 = entity;
        }
        if (blockEntity.chopstick2Id != null
            && blockEntity.chopstick2 == null
            && serverLevel.getEntity(blockEntity.chopstick2Id) instanceof BlockStructureEntity entity) {
            blockEntity.chopstick2 = entity;
        }

        // Rocket launch logic
        if (blockEntity.getSelectedConfigurationIndex() == -1) {
            return;
        }

        final ItemResource stack = blockEntity.inventoryHandler.getResource(blockEntity.getSelectedConfigurationIndex());
        if (!blockEntity.launchingRocket || stack.isEmpty() || !stack.has(ModDataComponentTypes.CONFIGURATION_PATH_DATA)) {
            blockEntity.launchingRocketTick = 0; //TODO: this doesn't make sense right now
            return;
        }

        final UUID uuid = stack.get(ModDataComponentTypes.CONFIGURATION_PATH_DATA);
        if (uuid == null) {
            return;
        }

        final NetworkConfiguration configuration = ConfigurationSavedData.getConfigurationData(serverLevel).get(uuid);
        if (configuration == null) {
            return;
        }

        if (!Utils.isSpacePortValid(level, configuration.launchPadConfiguration()) && blockEntity.chopstick1 == null) { //TODO: is this dumb?
            // TODO: what if someone started the launch but then broke the launch pad?
            // PacketDistributor.sendToAllPlayers(new HidePreviewBlocksMessage(pos, false));
            return;
        }

        // Cooldown
        if (blockEntity.launchCooldownTick++ <= blockEntity.launchCooldown) {
            if (blockEntity.launchCooldownOverlay) {
                for (final Player player : serverLevel.getNearbyPlayers(TargetingConditions.forNonCombat(), null, new AABB(pos).inflate(100))) {
                    player.sendOverlayMessage(Component.literal("T-" + (blockEntity.launchCooldown - blockEntity.launchCooldownTick + 20) / 20));
                }
            }
            blockEntity.setChanged();
            return;
        }

        final BlockPos mainPos = configuration.launchPadConfiguration().mainPos();
        final int width = configuration.launchPadConfiguration().width();
        final int height = configuration.launchPadConfiguration().height();
        final Direction facing = configuration.launchPadConfiguration().facing();

        if (blockEntity.launchingRocketTick == 0) {
            blockEntity.buildEntitiesFromBlocks(mainPos, width, height, facing);
        }

        // Open chopsticks
        if (blockEntity.chopstick1 != null && blockEntity.chopstick2 != null) {
            if (blockEntity.launchingRocketTick == 0 || blockEntity.launchingRocketTick == 690) {
                blockEntity.chopstick1.setRotateTowards(0.8F);
                blockEntity.chopstick2.setRotateTowards(-0.8F);
            }

            // Close chopsticks once opened
            if (blockEntity.launchingRocketTick == 80 || blockEntity.launchingRocketTick == 750) {
                blockEntity.chopstick1.setRotateTowards(-0.8F);
                blockEntity.chopstick2.setRotateTowards(0.8F);
            }

            // Stop chopsticks rotating
            if (blockEntity.launchingRocketTick == 40 || blockEntity.launchingRocketTick == 80 + 40
                || blockEntity.launchingRocketTick == 690 + 40 || blockEntity.launchingRocketTick == 750 + 40) {
                blockEntity.chopstick1.setRotateTowards(0F);
                blockEntity.chopstick2.setRotateTowards(0F);

                if (blockEntity.launchingRocketTick != 40 && blockEntity.launchingRocketTick != 690 + 40) {
                    PacketDistributor.sendToAllPlayers(new HidePreviewBlocksMessage(pos, false));
                }
            }
        }

        // TODO: actually calculate the speed by the weight and thrust force?
        if (blockEntity.launchedRocket != null) {
            double velocityY = 0;

            final int tick = blockEntity.launchingRocketTick;

            // Ascend rocket
            if (tick >= 1 && tick <= 20) {
                velocityY = 0.04;    // 20 ticks
            } else if (tick <= 40) {
                velocityY = 0.08;    // next 20 ticks
            } else if (tick <= 50) {
                velocityY = 0.10;    // next 10 ticks
            } else if (tick <= 60) {
                velocityY = 0.20;    // next 10 ticks
            } else if (tick <= 70) {
                velocityY = 0.30;    // next 10 ticks
            } else if (tick <= 80) {
                velocityY = 0.40;    // next 10 ticks
            } else if (tick <= 400) {
                velocityY = 0.50;    // next 320 ticks
            }
            // Descend rocket (mirror of above)
            else if (tick <= 720) { // 400 + 320
                //blockEntity.launchedRocket.setRocketEnginesActiveness(false);
                velocityY = -0.50;   // 320 ticks descending at 0.5
            } else if (tick <= 730) { // +10
                velocityY = -0.40;   // 10 ticks at 0.4
            } else if (tick <= 740) { // +10
                velocityY = -0.30;   // 10 ticks at 0.3
            } else if (tick <= 750) { // +10
                velocityY = -0.20;   // 10 ticks at 0.2
            } else if (tick <= 760) { // +10
                //blockEntity.launchedRocket.setRocketEnginesActiveness(true);
                velocityY = -0.10;   // 10 ticks at 0.1
            } else if (tick <= 780) { // +20
                velocityY = -0.08;   // 20 ticks at 0.08
            } else if (tick <= 800) { // +20
                velocityY = -0.04;   // final 20 ticks at 0.04
            }

            blockEntity.launchedRocket.setDeltaMovement(0, velocityY, 0);
        }

        // Smoke
        if (blockEntity.launchedRocket != null) {
            final Vec3 rocketPos = blockEntity.launchedRocket.position();
            final RandomSource random = blockEntity.launchedRocket.getRandom();

            if (blockEntity.launchingRocketTick >= 1 && blockEntity.launchingRocketTick <= 10
                || (blockEntity.launchingRocketTick >= 780 && blockEntity.launchingRocketTick <= 810)) {
                final double offsetX = Utils.randomOffset(random, 1F);
                final double offsetY = random.nextDouble() * 0.01;
                final double offsetZ = Utils.randomOffset(random, 1F);

                serverLevel.sendParticles(
                    ModParticles.BIG_SMOKE_PARTICLE.get(),
                    rocketPos.x, rocketPos.y - 2, rocketPos.z,
                    10,
                    offsetX, offsetY, offsetZ,
                    0.0
                );
            }
        }

        //TODO: remove later
        if (blockEntity.launchingRocketTick > 810) {
            blockEntity.landedRocket(uuid, configuration);
        }

        // TODO
        // Rocket has reached orbit so remove it and wait for the mining duration to tick off
        if (blockEntity.launchedRocket != null && blockEntity.launchedRocket.getOnPos().getY() > 300) {

        }

        blockEntity.launchingRocketTick++;
    }

    public void buildEntitiesFromBlocks(final BlockPos mainPos, final int width, final int height, final Direction facing) {
        if (this.level == null) {
            return;
        }

        // TODO: duplicate
        // Build Rocket
        final List<BlockPos> rocketPos = new ArrayList<>();
        int minOffset = -(width - 1) / 2;
        final int maxOffset = width / 2;
        for (int dx = minOffset + 4; dx <= maxOffset - 4; dx++) {
            for (int dz = 2; dz <= width - 7; dz++) {
                for (int dy = 0; dy < height + 1; dy++) { // + 1 because the height starts at the floor
                    final BlockPos rotatedPos = rotateOffset(mainPos.above(dy), facing.getOpposite(), dx, dz);
                    if (!this.level.getBlockState(rotatedPos).isAir()) {
                        rocketPos.add(rotatedPos);
                    }
                }
            }
        }

        final BlockStructureEntity rocketEntity = new BlockStructureEntity(this.level, rocketPos, true);
        rocketEntity.setPos(Utils.getBottomCenter(rocketPos));
        this.level.addFreshEntity(rocketEntity);
        this.launchedRocketId = rocketEntity.getUUID();
        this.launchedRocket = rocketEntity;

        // TODO: duplicate
        // Build chopsticks
        final int towerWidth = width - 8;
        minOffset = -(towerWidth - 1) / 2;

        final List<BlockPos> chopstick1Pos = new ArrayList<>();
        final List<BlockPos> chopstick2Pos = new ArrayList<>();
        final int chopstickWidth = width - 5;
        for (int dz = 0; dz < chopstickWidth; dz++) {
            final BlockPos targetPos1 = rotateOffset(mainPos.above(height - 2), facing.getOpposite(), -minOffset + 1, dz);
            final BlockPos targetPos2 = rotateOffset(mainPos.above(height - 2), facing.getOpposite(), minOffset - 1, dz);

            chopstick1Pos.add(targetPos1);
            chopstick2Pos.add(targetPos2);

            if (dz != 0) {
                final BlockPos targetPos3 = rotateOffset(mainPos.above(height - 2), facing.getOpposite(), -minOffset + 2, dz);
                final BlockPos targetPos4 = rotateOffset(mainPos.above(height - 2), facing.getOpposite(), minOffset - 2, dz);

                chopstick1Pos.add(targetPos3);
                chopstick2Pos.add(targetPos4);
            }
        }

        PacketDistributor.sendToAllPlayers(new HidePreviewBlocksMessage(this.getBlockPos(), true));

        final BlockStructureEntity chopstick1Entity = new BlockStructureEntity(this.level, chopstick1Pos, false);
        chopstick1Entity.setPos(Utils.getBottomCenter(chopstick1Pos).add(0.5, 0, 0).subtract(0, 0, 1.5)); //TODO: this is dumb (and only works facing south)
        // .add(0.5, 0, 0).subtract(0, 0, 1.5)
        chopstick1Entity.setPivotPoint(mainPos.above(height - 2).subtract(chopstick1Entity.getOnPos().above()));
        this.level.addFreshEntity(chopstick1Entity);
        this.chopstick1Id = chopstick1Entity.getUUID();
        this.chopstick1 = chopstick1Entity;

        final BlockStructureEntity chopstick2Entity = new BlockStructureEntity(this.level, chopstick2Pos, false);
        chopstick2Entity.setPos(Utils.getBottomCenter(chopstick2Pos).subtract(0.5, 0, 1.5)); //TODO: this is dumb (and only works facing south)
        //chopstick2Entity.setPivotPoint(mainPos.above(height - 2).subtract(chopstick2Entity.getOnPos().above()));
        this.level.addFreshEntity(chopstick2Entity);
        this.chopstick2Id = chopstick2Entity.getUUID();
        this.chopstick2 = chopstick2Entity;
    }

    public void landedRocket(final UUID uuid, final NetworkConfiguration configuration) {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.launchedRocket != null && this.chopstick1 != null && this.chopstick2 != null) {
            this.launchedRocket.remove(Entity.RemovalReason.KILLED);
            this.chopstick1.remove(Entity.RemovalReason.KILLED);
            this.chopstick2.remove(Entity.RemovalReason.KILLED);
        }
        this.launchCooldownTick = 0;
        this.launchingRocketTick = 0;
        this.launchingRocket = false;
        this.setChanged();

        final Optional<AsteroidConfig> asteroid = this.getAsteroidConfig(this.destinationAsteroid);
        if (asteroid.isPresent()) {
            // TODO: calculate the amount of materials mined instead of a set amount
            for (final ItemFluidStack compositionStack : asteroid.get().getCompositionStacks()) {
                configuration.moduleProperties().addItemFluidStack(compositionStack.copyWithCount(600));
            }

            ConfigurationSavedData.getConfigurationData(serverLevel).set(uuid, configuration);
        }
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
                data.set(uuid, new NetworkConfiguration(configuration.launchPadConfiguration(), Optional.of(this.calculateRocketStats(configuration)), configuration.moduleProperties()));
            }
        }
    }

    private RocketProperties calculateRocketStats(final NetworkConfiguration configuration) {
        final BlockPos mainPos = configuration.launchPadConfiguration().mainPos();
        final int width = configuration.launchPadConfiguration().width();
        final int height = configuration.launchPadConfiguration().height();
        final Direction facing = configuration.launchPadConfiguration().facing();

        int weight = 0;
        int thrustForce = 0;
        int fuelUsage = 0; //TODO: calculate this too (also split into fuel and oxidizer)

        if (this.level != null) {
            // TODO: this is duplicate again
            final int minOffset = -(width - 1) / 2;
            final int maxOffset = width / 2;
            for (int dx = minOffset + 4; dx <= maxOffset - 4; dx++) {
                for (int dz = 2; dz <= width - 7; dz++) {
                    for (int dy = 0; dy < height + 1; dy++) { // + 1 because the height starts at the floor
                        final BlockPos rotatedPos = rotateOffset(mainPos.above(dy), facing.getOpposite(), dx, dz);
                        final BlockState state = this.level.getBlockState(rotatedPos);

                        if (state.getBlock() instanceof RocketEngineBlock rocketEngineBlock) {
                            thrustForce += rocketEngineBlock.getType().getThrustForce();
                            weight += rocketEngineBlock.getType().getWeight();
                        } else if (!state.isAir()) {
                            weight += 1;
                        }
                    }
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

        this.inventoryHandler.deserialize(input);

        this.connectedModules.clear();
        this.connectedModules.addAll(input.read("connectedModules", Utils.BLOCK_POS_LIST).orElse(List.of()));

        this.launchCooldown = input.getIntOr("launchCooldown", 0);
        this.nextLaunchCooldown = input.getIntOr("nextLaunchCooldown", 20 * 10);
        this.launchCooldownTick = input.getIntOr("launchCooldownTick", 0);
        this.launchCooldownOverlay = input.getBooleanOr("launchCooldownOverlay", true);
        this.launchCooldownCommentator = input.getBooleanOr("launchCooldownCommentator", true);

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

        this.inventoryHandler.serialize(output);

        output.store("connectedModules", Utils.BLOCK_POS_LIST, this.connectedModules.stream().toList());

        output.putInt("launchCooldown", this.launchCooldown);
        output.putInt("nextLaunchCooldown", this.nextLaunchCooldown);
        output.putInt("launchCooldownTick", this.launchCooldownTick);
        output.putBoolean("launchCooldownOverlay", this.launchCooldownOverlay);
        output.putBoolean("launchCooldownCommentator", this.launchCooldownCommentator);

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
        final var menu = new RocketControllerContainerMenu(containerId, inventory, this, ContainerLevelAccess.create(this.level, this.getBlockPos()));
        menu.setOverwriteStillValid(this.overwriteStillValid);
        return menu;
    }

    public void setLaunchingRocket(final boolean launchingRocket) {
        final var config = this.getSelectedServerNetworkConfiguration();
        if (config == null) {
            return;
        }

        final var properties = config.moduleProperties();
        if (properties == null) {
            return;
        }

        final var optionalSelectedAsteroid = properties.selectedAsteroid();
        if (optionalSelectedAsteroid.isEmpty()) {
            return;
        }

        final var selectedAsteroid = optionalSelectedAsteroid.get();
        if (this.getAsteroidConfig(selectedAsteroid).isEmpty()) {
            return;
        }

        this.launchingRocket = launchingRocket;
        this.launchCooldown = this.nextLaunchCooldown;
        this.destinationAsteroid = selectedAsteroid;
    }

    public boolean isLaunchingRocket() {
        return this.launchingRocket;
    }

    public void setSelectedConfigurationIndex(final int selectedConfigurationIndex) {
        this.selectedConfigurationIndex = selectedConfigurationIndex;
        this.updateRocketStats();
    }

    public int getSelectedConfigurationIndex() {
        return this.selectedConfigurationIndex;
    }

    public Optional<AsteroidConfig> getAsteroidConfig(final Identifier asteroidId) {
        return AsteroidReloadListener.INSTANCE.getData().values()
            .stream()
            .filter(config -> config.getId().equals(asteroidId))
            .findFirst();
    }

    public class RocketControllerItemStacksResourceHandler extends ItemStacksResourceHandler {
        public RocketControllerItemStacksResourceHandler(final int size) {
            super(size);
        }

        public void triggerContentsChanged() {
            this.onContentsChanged(-1, ItemStack.EMPTY);
        }

        @Override
        public void onContentsChanged(final int index, final ItemStack previousContents) {
            RocketControllerBlockEntity.super.setChanged();

            final int selectedIndex = RocketControllerBlockEntity.this.getSelectedConfigurationIndex();
            if (selectedIndex == -1) {
                return;
            }
            final ItemResource stack = this.getResource(selectedIndex);
            if (!stack.has(ModDataComponentTypes.CONFIGURATION_PATH_DATA.get())) {
                if (RocketControllerBlockEntity.this.level != null && RocketControllerBlockEntity.this.level.isClientSide()) {
                    ClientEvents.LAUNCH_PAD_BUILDER_POS.remove(RocketControllerBlockEntity.this.getBlockPos());
                    ClientEvents.LAUNCH_PAD_PREVIEW_BLOCKS.remove(RocketControllerBlockEntity.this.getBlockPos());
                }
            } else {
                if (RocketControllerBlockEntity.this.level instanceof ServerLevel serverLevel) {
                    final UUID uuid = stack.get(ModDataComponentTypes.CONFIGURATION_PATH_DATA.get());
                    final NetworkConfiguration configuration = ConfigurationSavedData.getConfigurationData(serverLevel).get(uuid);
                    if (configuration != null) {
                        final List<PreviewInfo> previewInfos = Utils.calculateSpacePort(RocketControllerBlockEntity.this.level,
                            configuration.launchPadConfiguration(), false);
                        PacketDistributor.sendToAllPlayers(new SendLaunchPreviewDataMessage(RocketControllerBlockEntity.this.getBlockPos(), uuid, previewInfos));
                    }
                }
            }
        }

        @Override
        public void deserialize(final ValueInput input) {
            super.deserialize(input);
            this.triggerContentsChanged();
        }
    }
}
