package com.ultramega.asteroidmining.entities;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.blocks.RocketEngineBlock;
import com.ultramega.asteroidmining.registry.ModEntityDataSerializers;
import com.ultramega.asteroidmining.registry.ModEntityTypes;
import com.ultramega.asteroidmining.utils.Utils;

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
    public static final EntityDataAccessor<Float> ROTATE_TOWARDS =
        SynchedEntityData.defineId(BlockStructureEntity.class, EntityDataSerializers.FLOAT);

    public final Map<BlockPos, BlockEntity> blockEntityCache = new HashMap<>();

    public BlockStructureEntity(final Level level) {
        this(level, new ArrayList<>(), true);
    }

    public BlockStructureEntity(final Level level, final List<BlockPos> structurePos, final boolean isRocket) {
        super(ModEntityTypes.BLOCK_STRUCTURE_ENTITY.get(), level);
        this.setIsRocket(isRocket);
        this.absorbBlocks(structurePos);
        this.setRocketEnginesActiveness(true);
        this.noPhysics = true;
    }

    @Override
    public void tick() {
        this.move(MoverType.SELF, this.getDeltaMovement());

        // Tick Block Entities
        this.blockEntityCache.forEach((pos, blockEntity) -> {
            final BlockEntityTicker<BlockEntity> ticker = blockEntity.getBlockState().getTicker(this.level(), (BlockEntityType<BlockEntity>) blockEntity.getType());
            if (ticker != null) {
                final BlockPos realPos = new BlockPos(
                    pos.getX() + Math.abs(pos.getX() - this.getOnPos().getX()),
                    pos.getY() + Math.abs(pos.getY() - this.getOnPos().getY()),
                    pos.getZ() + Math.abs(pos.getZ() - this.getOnPos().getZ()));
                ticker.tick(this.level(), realPos, blockEntity.getBlockState(), blockEntity);
            }
        });

        //TODO: because of this clientside check the rotation isn't saved when rejoining the world but if I were to remove this the rotation stops being smooth
        if (this.level().isClientSide() && this.getRotateTowards() != 0.0F) {
            this.yRotO = this.getYRot();
            this.setYRot(this.getYRot() + this.getRotateTowards());
        }
    }

    @Override
    public boolean hurtServer(final ServerLevel serverLevel, final DamageSource damageSource, final float v) {
        return false;
    }

    private void absorbBlocks(final List<BlockPos> structurePos) {
        if (structurePos.isEmpty()) {
            return;
        }

        for (final BlockPos pos : structurePos) {
            final BlockState state = this.level().getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(this.level(), pos) <= 0) {
                return;
            }

            final BlockEntity blockEntity = this.level().getBlockEntity(pos);
            final CompoundTag nbt = (blockEntity != null) ? blockEntity.saveWithoutMetadata(this.registryAccess()) : new CompoundTag();

            this.addStructureBlockInfo(new StructureTemplate.StructureBlockInfo(pos, state, nbt));

            this.level().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        this.createBlockEntities();
    }

    public void freeBlocks() {
        this.setRocketEnginesActiveness(false);

        for (final StructureTemplate.StructureBlockInfo blockInfo : this.getStructureBlockInfos()) {
            final BlockPos pos = blockInfo.pos();
            this.level().setBlockAndUpdate(pos, blockInfo.state());

            if (blockInfo.nbt() != null) {
                final BlockEntity blockEntity = this.level().getBlockEntity(pos);
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
        for (final StructureTemplate.StructureBlockInfo blockInfo : this.getStructureBlockInfos()) {
            if (blockInfo.state().getBlock() instanceof EntityBlock entityBlock) {
                final BlockEntity blockEntity = entityBlock.newBlockEntity(blockInfo.pos(), blockInfo.state());
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
        builder.define(ROTATE_TOWARDS, 0F);
    }

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
        input.read("BlockList", Utils.STRUCTURE_BLOCK_INFO_LIST_CODEC).ifPresent(this::setStructureBlockInfo);
        this.setIsRocket(input.getBooleanOr("isRocket", false));
    }

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
        output.store("BlockList", Utils.STRUCTURE_BLOCK_INFO_LIST_CODEC, this.getStructureBlockInfos());
        output.putBoolean("isRocket", this.isRocket());
    }

    @Override
    public void readSpawnData(final RegistryFriendlyByteBuf buf) {
        final int size = buf.readVarInt();
        final List<StructureTemplate.StructureBlockInfo> blockInfos = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            final BlockPos pos = buf.readBlockPos();
            final CompoundTag stateTag = buf.readNbt();
            final BlockState state = NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK), stateTag);
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
        final int width = Utils.getWidth(structureBlockInfos);
        final int height = Utils.getHeight(structureBlockInfos);
        this.dimensions = EntityDimensions.scalable(width, height);
    }

    @Override
    public void remove(final RemovalReason reason) {
        this.freeBlocks();

        super.remove(reason);
    }

    @Override
    public void onRemovedFromLevel() {
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

    public void setRotateTowards(final float rotateTowards) {
        this.getEntityData().set(ROTATE_TOWARDS, rotateTowards);
    }

    public float getRotateTowards() {
        return this.getEntityData().get(ROTATE_TOWARDS);
    }
}
