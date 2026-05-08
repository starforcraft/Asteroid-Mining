package com.ultramega.asteroidmining.utils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.TickPriority;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.model.data.ModelData;
import org.jspecify.annotations.Nullable;

public final class FakeForwardingServerLevel implements ServerLevelAccessor {
    private final LevelAccessor delegate;

    public FakeForwardingServerLevel(final LevelAccessor delegate) {
        this.delegate = delegate;
    }

    @Override
    public ServerLevel getLevel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EnvironmentAttributeReader environmentAttributes() {
        return this.delegate.environmentAttributes();
    }

    @Override
    public long nextSubTickCount() {
        return this.delegate.nextSubTickCount();
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return this.delegate.getBlockTicks();
    }

    @Override
    public void scheduleTick(final BlockPos pos, final Block block, final int delay, final TickPriority priority) {
        this.delegate.scheduleTick(pos, block, delay, priority);
    }

    @Override
    public void scheduleTick(final BlockPos pos, final Block block, final int delay) {
        this.delegate.scheduleTick(pos, block, delay);
    }

    @Override
    public void scheduleTick(final BlockPos pos, final Fluid fluid, final int delay, final TickPriority priority) {
        this.delegate.scheduleTick(pos, fluid, delay, priority);
    }

    @Override
    public void scheduleTick(final BlockPos pos, final Fluid fluid, final int delay) {
        this.delegate.scheduleTick(pos, fluid, delay);
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return this.delegate.getFluidTicks();
    }

    @Override
    public LevelData getLevelData() {
        return this.delegate.getLevelData();
    }

    @Override
    public DifficultyInstance getCurrentDifficultyAt(final BlockPos pos) {
        return new DifficultyInstance(this.delegate.getDifficulty(), 0L, 0L, 0);
    }

    @Override
    @Nullable
    public MinecraftServer getServer() {
        return this.delegate.getServer();
    }

    @Override
    public Difficulty getDifficulty() {
        return this.delegate.getDifficulty();
    }

    @Override
    public ChunkSource getChunkSource() {
        return this.delegate.getChunkSource();
    }

    @Override
    public boolean hasChunk(final int chunkX, final int chunkZ) {
        return this.delegate.hasChunk(chunkX, chunkZ);
    }

    @Override
    public RandomSource getRandom() {
        return this.delegate.getRandom();
    }

    @Override
    public void playSound(@Nullable final Entity except, final BlockPos pos, final SoundEvent soundEvent, final SoundSource source) {
        this.delegate.playSound(except, pos, soundEvent, source);
    }

    @Override
    public void playSound(@Nullable final Entity entity, final BlockPos blockPos, final SoundEvent soundEvent, final SoundSource soundSource, final float v,
                          final float v1) {
        this.delegate.playSound(entity, blockPos, soundEvent, soundSource, v, v1);
    }

    @Override
    public void addParticle(final ParticleOptions particleData, final double x, final double y, final double z, final double speedX, final double speedY, final double speedZ) {
        this.delegate.addParticle(particleData, x, y, z, speedX, speedY, speedZ);
    }

    @Override
    public void levelEvent(@Nullable final Entity entity, final int type, final BlockPos pos, final int data) {
        this.delegate.levelEvent(entity, type, pos, data);
    }

    @Override
    public void levelEvent(final int type, final BlockPos pos, final int data) {
        this.delegate.levelEvent(type, pos, data);
    }

    @Override
    public void gameEvent(final ResourceKey<GameEvent> gameEvent, final BlockPos pos, final GameEvent.Context context) {
        this.delegate.gameEvent(gameEvent, pos, context);
    }

    @Override
    public void gameEvent(final Holder<GameEvent> event, final Vec3 position, final GameEvent.Context context) {
        this.delegate.gameEvent(event, position, context);
    }

    @Override
    public void gameEvent(@Nullable final Entity entity, final Holder<GameEvent> event, final Vec3 position) {
        this.delegate.gameEvent(entity, event, position);
    }

    @Override
    public void gameEvent(@Nullable final Entity entity, final Holder<GameEvent> event, final BlockPos pos) {
        this.delegate.gameEvent(entity, event, pos);
    }

    @Override
    public void gameEvent(final Holder<GameEvent> event, final BlockPos pos, final GameEvent.Context context) {
        this.delegate.gameEvent(event, pos, context);
    }

    @Override
    public List<VoxelShape> getEntityCollisions(@Nullable final Entity entity, final AABB collisionBox) {
        return this.delegate.getEntityCollisions(entity, collisionBox);
    }

    @Override
    public BlockPos getHeightmapPos(final Heightmap.Types heightmapType, final BlockPos pos) {
        return this.delegate.getHeightmapPos(heightmapType, pos);
    }

    @Override
    public <T extends Entity> List<T> getEntities(final EntityTypeTest<Entity, T> entityTypeTest, final AABB bounds, final Predicate<? super T> predicate) {
        return this.delegate.getEntities(entityTypeTest, bounds, predicate);
    }

    @Override
    public List<Entity> getEntities(@Nullable final Entity entity, final AABB area, final Predicate<? super Entity> predicate) {
        return this.delegate.getEntities(entity, area, predicate);
    }

    @Override
    public List<Entity> getEntities(@Nullable final Entity entity, final AABB area) {
        return this.delegate.getEntities(entity, area);
    }

    @Override
    public <T extends Entity> List<T> getEntitiesOfClass(final Class<T> clazz, final AABB area, final Predicate<? super T> filter) {
        return this.delegate.getEntitiesOfClass(clazz, area, filter);
    }

    @Override
    public <T extends Entity> List<T> getEntitiesOfClass(final Class<T> entityClass, final AABB area) {
        return this.delegate.getEntitiesOfClass(entityClass, area);
    }

    @Override
    public List<? extends Player> players() {
        return this.delegate.players();
    }

    @Override
    @Nullable
    public Player getNearestPlayer(final double x, final double y, final double z, final double distance, @Nullable final Predicate<Entity> predicate) {
        return this.delegate.getNearestPlayer(x, y, z, distance, predicate);
    }

    @Override
    @Nullable
    public Player getNearestPlayer(final Entity entity, final double distance) {
        return this.delegate.getNearestPlayer(entity, distance);
    }

    @Override
    @Nullable
    public Player getNearestPlayer(final double x, final double y, final double z, final double distance, final boolean creativePlayers) {
        return this.delegate.getNearestPlayer(x, y, z, distance, creativePlayers);
    }

    @Override
    public boolean hasNearbyAlivePlayer(final double x, final double y, final double z, final double distance) {
        return this.delegate.hasNearbyAlivePlayer(x, y, z, distance);
    }

    @Override
    @Nullable
    public Player getPlayerByUUID(final UUID uniqueId) {
        return this.delegate.getPlayerByUUID(uniqueId);
    }

    @Override
    public int getHeight(final Heightmap.Types heightmapType, final int x, final int z) {
        return this.delegate.getHeight(heightmapType, x, z);
    }

    @Override
    public int getHeight() {
        return this.delegate.getHeight();
    }

    @Override
    public int getSkyDarken() {
        return this.delegate.getSkyDarken();
    }

    @Override
    public BiomeManager getBiomeManager() {
        return this.delegate.getBiomeManager();
    }

    @Override
    public Holder<Biome> getBiome(final BlockPos pos) {
        return this.delegate.getBiome(pos);
    }

    @Override
    public Stream<BlockState> getBlockStatesIfLoaded(final AABB aabb) {
        return this.delegate.getBlockStatesIfLoaded(aabb);
    }

    @Override
    public Holder<Biome> getNoiseBiome(final int i, final int j, final int k) {
        return this.delegate.getNoiseBiome(i, j, k);
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(final int x, final int y, final int z) {
        return this.delegate.getUncachedNoiseBiome(x, y, z);
    }

    @Override
    public boolean isClientSide() {
        return this.delegate.isClientSide();
    }

    @Override
    @Deprecated
    public int getSeaLevel() {
        return this.delegate.getSeaLevel();
    }

    @Override
    public DimensionType dimensionType() {
        return this.delegate.dimensionType();
    }

    @Override
    public boolean isEmptyBlock(final BlockPos pos) {
        return this.delegate.isEmptyBlock(pos);
    }

    @Override
    public boolean canSeeSkyFromBelowWater(final BlockPos pos) {
        return this.delegate.canSeeSkyFromBelowWater(pos);
    }

    @Override
    public float getPathfindingCostFromLightLevels(final BlockPos blockPos) {
        return this.delegate.getPathfindingCostFromLightLevels(blockPos);
    }

    @Override
    @Deprecated
    public float getLightLevelDependentMagicValue(final BlockPos blockPos) {
        return this.delegate.getLightLevelDependentMagicValue(blockPos);
    }

    @Override
    public int getDirectSignal(final BlockPos pos, final Direction direction) {
        return this.delegate.getDirectSignal(pos, direction);
    }

    @Override
    @Nullable
    public ChunkAccess getChunk(final int x, final int z, final ChunkStatus requiredStatus, final boolean nonnull) {
        return this.delegate.getChunk(x, z, requiredStatus, nonnull);
    }

    @Override
    public ChunkAccess getChunk(final BlockPos pos) {
        return this.delegate.getChunk(pos);
    }

    @Override
    public ChunkAccess getChunk(final int chunkX, final int chunkZ) {
        return this.delegate.getChunk(chunkX, chunkZ);
    }

    @Override
    public ChunkAccess getChunk(final int chunkX, final int chunkZ, final ChunkStatus requiredStatus) {
        return this.delegate.getChunk(chunkX, chunkZ, requiredStatus);
    }

    @Override
    @Nullable
    public BlockGetter getChunkForCollisions(final int chunkX, final int chunkZ) {
        return this.delegate.getChunkForCollisions(chunkX, chunkZ);
    }

    @Override
    public boolean isWaterAt(final BlockPos pos) {
        return this.delegate.isWaterAt(pos);
    }

    @Override
    public boolean containsAnyLiquid(final AABB bb) {
        return this.delegate.containsAnyLiquid(bb);
    }

    @Override
    public int getMaxLocalRawBrightness(final BlockPos pos) {
        return this.delegate.getMaxLocalRawBrightness(pos);
    }

    @Override
    public int getMaxLocalRawBrightness(final BlockPos pos, final int amount) {
        return this.delegate.getMaxLocalRawBrightness(pos, amount);
    }

    @Override
    @Deprecated
    public boolean hasChunkAt(final int x, final int z) {
        return this.delegate.hasChunkAt(x, z);
    }

    @Override
    @Deprecated
    public boolean hasChunkAt(final BlockPos pos) {
        return this.delegate.hasChunkAt(pos);
    }

    @Override
    @Deprecated
    public boolean hasChunksAt(final BlockPos from, final BlockPos to) {
        return this.delegate.hasChunksAt(from, to);
    }

    @Override
    @Deprecated
    public boolean hasChunksAt(final int fromX, final int fromY, final int fromZ, final int toX, final int toY, final int toZ) {
        return this.delegate.hasChunksAt(fromX, fromY, fromZ, toX, toY, toZ);
    }

    @Override
    @Deprecated
    public boolean hasChunksAt(final int fromX, final int fromZ, final int toX, final int toZ) {
        return this.delegate.hasChunksAt(fromX, fromZ, toX, toZ);
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.delegate.registryAccess();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return this.delegate.enabledFeatures();
    }

    @Override
    public <T> HolderLookup<T> holderLookup(final ResourceKey<? extends Registry<? extends T>> resourceKey) {
        return this.delegate.holderLookup(resourceKey);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.delegate.getLightEngine();
    }

    @Override
    public int getBrightness(final LightLayer lightType, final BlockPos blockPos) {
        return this.delegate.getBrightness(lightType, blockPos);
    }

    @Override
    public int getRawBrightness(final BlockPos blockPos, final int amount) {
        return this.delegate.getRawBrightness(blockPos, amount);
    }

    @Override
    public boolean canSeeSky(final BlockPos blockPos) {
        return this.delegate.canSeeSky(blockPos);
    }

    @Override
    @Nullable
    public BlockEntity getBlockEntity(final BlockPos pos) {
        return this.delegate.getBlockEntity(pos);
    }

    @Override
    public <T extends BlockEntity> Optional<T> getBlockEntity(final BlockPos pos, final BlockEntityType<T> type) {
        return this.delegate.getBlockEntity(pos, type);
    }

    @Override
    public BlockState getBlockState(final BlockPos pos) {
        return this.delegate.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(final BlockPos pos) {
        return this.delegate.getFluidState(pos);
    }

    @Override
    public int getLightEmission(final BlockPos pos) {
        return this.delegate.getLightEmission(pos);
    }

    @Override
    public Stream<BlockState> getBlockStates(final AABB area) {
        return this.delegate.getBlockStates(area);
    }

    @Override
    public BlockHitResult isBlockInLine(final ClipBlockStateContext context) {
        return this.delegate.isBlockInLine(context);
    }

    @Override
    public BlockHitResult clip(final ClipContext context) {
        return this.delegate.clip(context);
    }

    @Override
    @Nullable
    public BlockHitResult clipWithInteractionOverride(final Vec3 startVec, final Vec3 endVec, final BlockPos pos, final VoxelShape shape,
                                                      final BlockState state) {
        return this.delegate.clipWithInteractionOverride(startVec, endVec, pos, shape, state);
    }

    @Override
    public double getBlockFloorHeight(final VoxelShape shape, final Supplier<VoxelShape> belowShapeSupplier) {
        return this.delegate.getBlockFloorHeight(shape, belowShapeSupplier);
    }

    @Override
    public double getBlockFloorHeight(final BlockPos pos) {
        return this.delegate.getBlockFloorHeight(pos);
    }

    public static <T, C> T traverseBlocks(final Vec3 from, final Vec3 to, final C context, final BiFunction<C, BlockPos, T> tester,
                                          final Function<C, T> onFail) {
        return BlockGetter.traverseBlocks(from, to, context, tester, onFail);
    }

    @Override
    public int getSectionsCount() {
        return this.delegate.getSectionsCount();
    }

    @Override
    public boolean isOutsideBuildHeight(final BlockPos pos) {
        return this.delegate.isOutsideBuildHeight(pos);
    }

    @Override
    public boolean isOutsideBuildHeight(final int y) {
        return this.delegate.isOutsideBuildHeight(y);
    }

    @Override
    public int getSectionIndex(final int y) {
        return this.delegate.getSectionIndex(y);
    }

    @Override
    public int getSectionIndexFromSectionY(final int sectionIndex) {
        return this.delegate.getSectionIndexFromSectionY(sectionIndex);
    }

    @Override
    public int getSectionYFromSectionIndex(final int sectionIndex) {
        return this.delegate.getSectionYFromSectionIndex(sectionIndex);
    }

    public static LevelHeightAccessor create(final int minBuildHeight, final int height) {
        return LevelHeightAccessor.create(minBuildHeight, height);
    }

    @Override
    public WorldBorder getWorldBorder() {
        return this.delegate.getWorldBorder();
    }

    @Override
    public boolean isUnobstructed(final BlockState state, final BlockPos pos, final CollisionContext context) {
        return this.delegate.isUnobstructed(state, pos, context);
    }

    @Override
    public boolean isUnobstructed(@Nullable final Entity entity, final VoxelShape shape) {
        return this.delegate.isUnobstructed(entity, shape);
    }

    @Override
    public boolean isUnobstructed(final Entity entity) {
        return this.delegate.isUnobstructed(entity);
    }

    @Override
    public boolean noCollision(final AABB collisionBox) {
        return this.delegate.noCollision(collisionBox);
    }

    @Override
    public boolean noCollision(final Entity entity) {
        return this.delegate.noCollision(entity);
    }

    @Override
    public boolean noCollision(@Nullable final Entity entity, final AABB collisionBox) {
        return this.delegate.noCollision(entity, collisionBox);
    }

    @Override
    public Iterable<VoxelShape> getCollisions(@Nullable final Entity entity, final AABB collisionBox) {
        return this.delegate.getCollisions(entity, collisionBox);
    }

    @Override
    public Iterable<VoxelShape> getBlockCollisions(@Nullable final Entity entity, final AABB collisionBox) {
        return this.delegate.getBlockCollisions(entity, collisionBox);
    }

    @Override
    public boolean collidesWithSuffocatingBlock(@Nullable final Entity entity, final AABB box) {
        return this.delegate.collidesWithSuffocatingBlock(entity, box);
    }

    @Override
    public Optional<Vec3> findFreePosition(@Nullable final Entity entity, final VoxelShape shape, final Vec3 pos, final double x, final double y,
                                           final double z) {
        return this.delegate.findFreePosition(entity, shape, pos, x, y, z);
    }

    @Override
    public boolean isStateAtPosition(final BlockPos pos, final Predicate<BlockState> state) {
        return this.delegate.isStateAtPosition(pos, state);
    }

    @Override
    public boolean isFluidAtPosition(final BlockPos pos, final Predicate<FluidState> predicate) {
        return this.delegate.isFluidAtPosition(pos, predicate);
    }

    @Override
    public boolean setBlock(final BlockPos pos, final BlockState state, final int flags, final int recursionLeft) {
        return this.delegate.setBlock(pos, state, flags, recursionLeft);
    }

    @Override
    public boolean setBlock(final BlockPos pos, final BlockState newState, final int flags) {
        return this.delegate.setBlock(pos, newState, flags);
    }

    @Override
    public boolean removeBlock(final BlockPos pos, final boolean isMoving) {
        return this.delegate.removeBlock(pos, isMoving);
    }

    @Override
    public boolean destroyBlock(final BlockPos pos, final boolean dropBlock) {
        return this.delegate.destroyBlock(pos, dropBlock);
    }

    @Override
    public boolean destroyBlock(final BlockPos pos, final boolean dropBlock, @Nullable final Entity entity) {
        return this.delegate.destroyBlock(pos, dropBlock, entity);
    }

    @Override
    public boolean destroyBlock(final BlockPos pos, final boolean dropBlock, @Nullable final Entity entity, final int recursionLeft) {
        return this.delegate.destroyBlock(pos, dropBlock, entity, recursionLeft);
    }

    @Override
    public boolean addFreshEntity(final Entity entity) {
        return this.delegate.addFreshEntity(entity);
    }

    @Override
    public boolean isAreaLoaded(final BlockPos center, final int range) {
        return this.delegate.isAreaLoaded(center, range);
    }

    @Override
    public @Nullable AuxiliaryLightManager getAuxLightManager(final ChunkPos pos) {
        return this.delegate.getAuxLightManager(pos);
    }

    @Override
    public ModelData getModelData(final BlockPos pos) {
        return this.delegate.getModelData(pos);
    }

    @Override
    public boolean noBlockCollision(@Nullable final Entity entity, final AABB aabb) {
        return this.delegate.noBlockCollision(entity, aabb);
    }

    @Override
    public Optional<BlockPos> findSupportingBlock(final Entity entity, final AABB aabb) {
        return this.delegate.findSupportingBlock(entity, aabb);
    }

    @Override
    public int getDirectSignalTo(final BlockPos pos) {
        return this.delegate.getDirectSignalTo(pos);
    }

    @Override
    public int getControlInputSignal(final BlockPos pos, final Direction direction, final boolean onlyDiodes) {
        return this.delegate.getControlInputSignal(pos, direction, onlyDiodes);
    }

    @Override
    public boolean hasSignal(final BlockPos pos, final Direction direction) {
        return this.delegate.hasSignal(pos, direction);
    }

    @Override
    public int getSignal(final BlockPos pos, final Direction direction) {
        return this.delegate.getSignal(pos, direction);
    }

    @Override
    public boolean hasNeighborSignal(final BlockPos pos) {
        return this.delegate.hasNeighborSignal(pos);
    }

    @Override
    public int getBestNeighborSignal(final BlockPos pos) {
        return this.delegate.getBestNeighborSignal(pos);
    }
}
