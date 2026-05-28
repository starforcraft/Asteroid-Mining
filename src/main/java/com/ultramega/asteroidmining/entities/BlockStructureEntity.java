package com.ultramega.asteroidmining.entities;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.blockentities.RocketEngineBlockEntity;
import com.ultramega.asteroidmining.blockentities.RocketEngineBlockEntityClient;
import com.ultramega.asteroidmining.blocks.RocketEngineBlock;
import com.ultramega.asteroidmining.registry.ModEntityDataSerializers;
import com.ultramega.asteroidmining.registry.ModEntityTypes;
import com.ultramega.asteroidmining.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

public class BlockStructureEntity extends Entity implements IEntityWithComplexSpawn {
    public static final EntityDataAccessor<List<StructureTemplate.StructureBlockInfo>> STRUCTURE_BLOCK_INFO_DATA =
        SynchedEntityData.defineId(BlockStructureEntity.class, ModEntityDataSerializers.STRUCTURE_BLOCK_INFO_LIST_REGISTER.get());
    public static final EntityDataAccessor<Boolean> IS_ROCKET =
        SynchedEntityData.defineId(BlockStructureEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<BlockPos> PIVOT_POINT =
        SynchedEntityData.defineId(BlockStructureEntity.class, EntityDataSerializers.BLOCK_POS);
    public static final EntityDataAccessor<Float> TARGET_Y_ROT =
        SynchedEntityData.defineId(BlockStructureEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> TARGET_X_ROT =
        SynchedEntityData.defineId(BlockStructureEntity.class, EntityDataSerializers.FLOAT);

    private static final String TAG_BLOCK_LIST = "BlockList";
    private static final String TAG_IS_ROCKET = "IsRocket";
    private static final String TAG_PIVOT_POINT = "PivotPoint";
    private static final String TAG_TARGET_X_ROT = "TargetXRot";
    private static final String TAG_TARGET_Y_ROT = "TargetYRot";

    private static final float ROTATION_SPEED_DEGREES_PER_TICK = 0.4F;
    private static final float PITCH_SPEED_DEGREES_PER_TICK = 0.5F;

    public final Map<BlockPos, BlockEntity> blockEntityCache = new HashMap<>();

    private boolean freeBlocksOnRemove = true;

    public BlockStructureEntity(final Level level) {
        this(level, new ArrayList<>(), new ArrayList<>(), true);
    }

    public BlockStructureEntity(final Level level,
                                final List<BlockPos> worldPositions,
                                final List<BlockPos> localPositions,
                                final boolean isRocket) {
        super(ModEntityTypes.BLOCK_STRUCTURE_ENTITY.get(), level);
        this.setIsRocket(isRocket);
        this.absorbBlocks(worldPositions, localPositions);
        this.setRocketEnginesActiveness(true);
        this.noPhysics = true;
    }

    public BlockStructureEntity(final Level level,
                                final List<StructureTemplate.StructureBlockInfo> structureBlockInfos,
                                final boolean isRocket,
                                final double x,
                                final double y,
                                final double z) {
        super(ModEntityTypes.BLOCK_STRUCTURE_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.setIsRocket(isRocket);
        this.createBlockEntities();
        this.setStructureBlockInfo(structureBlockInfos);
        this.noPhysics = true;
    }

    @Override
    public void tick() {
        this.move(MoverType.SELF, this.getDeltaMovement());

        this.tickBlockEntities();
        this.tickTargetYRotation();
        this.tickTargetXRotation();
    }

    private void tickBlockEntities() {
        for (final StructureTemplate.StructureBlockInfo blockInfo : this.getStructureBlockInfos()) {
            final BlockEntity blockEntity = this.blockEntityCache.get(blockInfo.pos());
            if (blockEntity == null) {
                continue;
            }

            final BlockEntityTicker<BlockEntity> ticker = blockInfo.state().getTicker(this.level(), (BlockEntityType<BlockEntity>) blockEntity.getType());
            if (ticker != null) {
                ticker.tick(this.level(), this.getStructureWorldOrigin().offset(blockInfo.pos()), blockInfo.state(), blockEntity);
            }
        }
    }

    private void tickTargetYRotation() {
        final float current = this.getYRot();
        final float target = this.getTargetYRot();
        final float difference = Mth.wrapDegrees(target - current);

        if (Math.abs(difference) < 0.01F) {
            this.setYRot(target);
            this.yRotO = target;
            return;
        }

        this.yRotO = current;

        final float step = Mth.clamp(difference, -ROTATION_SPEED_DEGREES_PER_TICK, ROTATION_SPEED_DEGREES_PER_TICK);
        this.setYRot(Mth.wrapDegrees(current + step));
    }

    private void tickTargetXRotation() {
        final float current = this.getXRot();
        final float target = this.getTargetXRot();
        final float difference = target - current;

        if (Math.abs(difference) < 0.01F) {
            this.setXRot(target);
            this.xRotO = target;
            return;
        }

        this.xRotO = current;

        final float step = Mth.clamp(difference, -PITCH_SPEED_DEGREES_PER_TICK, PITCH_SPEED_DEGREES_PER_TICK);
        this.setXRot(current + step);
    }

    @Override
    public boolean hurtServer(final ServerLevel serverLevel, final DamageSource damageSource, final float v) {
        return false;
    }

    private void absorbBlocks(final List<BlockPos> worldPositions, final List<BlockPos> localPositions) {
        if (worldPositions.isEmpty() || worldPositions.size() != localPositions.size()) {
            return;
        }

        for (int i = 0; i < worldPositions.size(); i++) {
            final BlockPos worldPos = worldPositions.get(i);
            final BlockPos localPos = localPositions.get(i);

            final BlockState state = this.level().getBlockState(worldPos);
            if (state.isAir() || state.getDestroySpeed(this.level(), worldPos) <= 0) {
                return;
            }

            final BlockEntity blockEntity = this.level().getBlockEntity(worldPos);
            final CompoundTag nbt = blockEntity != null
                ? blockEntity.saveWithoutMetadata(this.registryAccess())
                : new CompoundTag();

            this.addStructureBlockInfo(new StructureTemplate.StructureBlockInfo(localPos, state, nbt));

            this.level().setBlock(worldPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        this.createBlockEntities();
    }

    public void freeBlocks() {
        this.setRocketEnginesActiveness(false);

        final BlockPos origin = this.getStructureWorldOrigin();
        for (final StructureTemplate.StructureBlockInfo blockInfo : this.getStructureBlockInfos()) {
            final BlockPos worldPos = origin.offset(blockInfo.pos());
            this.level().setBlockAndUpdate(worldPos, blockInfo.state());

            if (blockInfo.nbt() != null) {
                final BlockEntity blockEntity = this.level().getBlockEntity(worldPos);
                if (blockEntity != null) {
                    try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(this.problemPath(), AsteroidMining.LOGGER)) {
                        blockEntity.loadWithComponents(TagValueInput.create(reporter, this.level().registryAccess(), blockInfo.nbt()));
                    }
                    blockEntity.setChanged();
                }
            }
        }
    }

    private void createBlockEntities() {
        final BlockPos origin = this.getStructureWorldOrigin();
        for (final StructureTemplate.StructureBlockInfo blockInfo : this.getStructureBlockInfos()) {
            if (blockInfo.state().getBlock() instanceof EntityBlock entityBlock) {
                final BlockEntity blockEntity = entityBlock.newBlockEntity(origin.offset(blockInfo.pos()), blockInfo.state());
                if (blockEntity != null) {
                    blockEntity.setLevel(this.level());
                    this.blockEntityCache.putIfAbsent(blockInfo.pos(), blockEntity);
                }
            }
        }
    }

    private void freeBlockEntities() {
        this.blockEntityCache.forEach((pos, blockEntity) -> blockEntity.setRemoved());
        this.blockEntityCache.clear();
    }

    private BlockPos getStructureWorldOrigin() {
        return BlockPos.containing(this.getX() - 0.5, this.getY(), this.getZ() - 0.5);
    }

    public void setRocketEnginesActiveness(final boolean active) {
        if (!this.isRocket()) {
            return;
        }

        final List<StructureTemplate.StructureBlockInfo> updatedInfos = this.getStructureBlockInfos().stream()
            .map(info -> {
                BlockState state = info.state();
                if (state.getBlock() instanceof RocketEngineBlock) {
                    state = state.setValue(RocketEngineBlock.RUNNING, active);
                }
                return new StructureTemplate.StructureBlockInfo(info.pos(), state, info.nbt());
            })
            .toList();

        this.setStructureBlockInfo(updatedInfos);
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        builder.define(STRUCTURE_BLOCK_INFO_DATA, List.of());
        builder.define(IS_ROCKET, true);
        builder.define(PIVOT_POINT, BlockPos.ZERO);
        builder.define(TARGET_X_ROT, 0F);
        builder.define(TARGET_Y_ROT, 0F);
    }

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
        input.read(TAG_BLOCK_LIST, CommonUtils.STRUCTURE_BLOCK_INFO_LIST_CODEC).ifPresent(this::setStructureBlockInfo);
        this.setIsRocket(input.getBooleanOr(TAG_IS_ROCKET, false));
        input.read(TAG_PIVOT_POINT, BlockPos.CODEC).ifPresent(this::setPivotPoint);
        this.setTargetXRot(input.getFloatOr(TAG_TARGET_X_ROT, 0F));
        this.setTargetYRot(input.getFloatOr(TAG_TARGET_Y_ROT, 0F));
    }

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
        output.store(TAG_BLOCK_LIST, CommonUtils.STRUCTURE_BLOCK_INFO_LIST_CODEC, this.getStructureBlockInfos());
        output.putBoolean(TAG_IS_ROCKET, this.isRocket());
        output.store(TAG_PIVOT_POINT, BlockPos.CODEC, this.getPivotPoint());
        output.putFloat(TAG_TARGET_X_ROT, this.getTargetXRot());
        output.putFloat(TAG_TARGET_Y_ROT, this.getTargetYRot());
    }

    @Override
    public void onSyncedDataUpdated(final EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if (STRUCTURE_BLOCK_INFO_DATA.equals(key)) {
            this.updateBoundingBox();
        }
    }

    @Override
    public void readSpawnData(final RegistryFriendlyByteBuf buf) {
        final int size = buf.readVarInt();
        final List<StructureTemplate.StructureBlockInfo> blockInfos = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            final BlockPos pos = buf.readBlockPos();
            final CompoundTag stateTag = buf.readNbt();
            final BlockState state = NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK), stateTag == null ? new CompoundTag() : stateTag);
            CompoundTag nbt = buf.readNbt();
            if (nbt != null && nbt.isEmpty()) {
                nbt = null;
            }

            blockInfos.add(new StructureTemplate.StructureBlockInfo(pos, state, nbt));
        }

        this.setStructureBlockInfo(blockInfos);

        this.setIsRocket(buf.readBoolean());

        if (this.level().isClientSide()) {
            this.createBlockEntities();
        }
    }

    @Override
    public void writeSpawnData(final RegistryFriendlyByteBuf buf) {
        final List<StructureTemplate.StructureBlockInfo> blockInfos = this.getStructureBlockInfos();
        buf.writeVarInt(blockInfos.size());

        for (final StructureTemplate.StructureBlockInfo info : blockInfos) {
            buf.writeBlockPos(info.pos());
            buf.writeNbt(NbtUtils.writeBlockState(info.state()));
            buf.writeNbt(info.nbt() != null ? info.nbt() : new CompoundTag());
        }

        buf.writeBoolean(this.isRocket());
    }

    private void updateBoundingBox() {
        final List<StructureTemplate.StructureBlockInfo> structureBlockInfos = this.getStructureBlockInfos();
        final int width = CommonUtils.getWidth(structureBlockInfos);
        final int height = CommonUtils.getHeight(structureBlockInfos);
        this.dimensions = EntityDimensions.scalable(width, height);
    }

    public void discardWithoutFreeingBlocks() {
        this.freeBlocksOnRemove = false;
        this.remove(RemovalReason.DISCARDED);
    }

    @Override
    public void remove(final RemovalReason reason) {
        if (this.freeBlocksOnRemove) {
            this.freeBlocks();
        }

        super.remove(reason);
    }

    @Override
    public void onRemovedFromLevel() {
        if (this.level().isClientSide()) {
            this.blockEntityCache.values().forEach(blockEntity -> {
                if (blockEntity instanceof RocketEngineBlockEntity rocketEngine) {
                    RocketEngineBlockEntityClient.stopEffects(rocketEngine);
                }
            });
        }

        this.freeBlockEntities();

        super.onRemovedFromLevel();
    }

    @Override
    public Component getCustomName() {
        return this.getName();
    }

    @Override
    public Component getName() {
        return Component.translatable("entity.asteroidmining." + (this.isRocket() ? "rocket" : "tower_chopstick"));
    }

    public void addStructureBlockInfo(final StructureTemplate.StructureBlockInfo blockInfo) {
        final List<StructureTemplate.StructureBlockInfo> infos = new ArrayList<>(this.getStructureBlockInfos());
        infos.add(blockInfo);
        this.setStructureBlockInfo(infos);
    }

    public void setStructureBlockInfo(final List<StructureTemplate.StructureBlockInfo> blockInfos) {
        this.getEntityData().set(STRUCTURE_BLOCK_INFO_DATA, blockInfos);
        this.updateBoundingBox();
    }

    public List<StructureTemplate.StructureBlockInfo> getStructureBlockInfos() {
        return this.getEntityData().get(STRUCTURE_BLOCK_INFO_DATA);
    }

    public void setIsRocket(final boolean isRocket) {
        this.getEntityData().set(IS_ROCKET, isRocket);
    }

    public boolean isRocket() {
        return this.getEntityData().get(IS_ROCKET);
    }

    public void setPivotPoint(final BlockPos pivotPoint) {
        this.getEntityData().set(PIVOT_POINT, pivotPoint);
    }

    public BlockPos getPivotPoint() {
        return this.getEntityData().get(PIVOT_POINT);
    }

    public void setTargetXRot(final float targetXRot) {
        this.getEntityData().set(TARGET_X_ROT, Mth.clamp(targetXRot, -45F, 45F));
    }

    public float getTargetXRot() {
        return this.getEntityData().get(TARGET_X_ROT);
    }

    public void setTargetYRot(final float targetYRot) {
        this.getEntityData().set(TARGET_Y_ROT, Mth.wrapDegrees(targetYRot));
    }

    public float getTargetYRot() {
        return this.getEntityData().get(TARGET_Y_ROT);
    }

    public float getPrecisePitchRotation(final float partialTicks) {
        return Mth.lerp(partialTicks, this.xRotO, this.getXRot());
    }
}
