package com.ultramega.asteroidmining.registry;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.blockentities.AirAbsorberBlockEntity;
import com.ultramega.asteroidmining.blockentities.BiogasPlantBlockEntity;
import com.ultramega.asteroidmining.blockentities.BoundingBoxBlockEntity;
import com.ultramega.asteroidmining.blockentities.DistillationColumnBlockEntity;
import com.ultramega.asteroidmining.blockentities.ElectrolysisPlantBlockEntity;
import com.ultramega.asteroidmining.blockentities.HeatExchangerBlockEntity;
import com.ultramega.asteroidmining.blockentities.LaunchPadBuilderBlockEntity;
import com.ultramega.asteroidmining.blockentities.ObservatoryBlockEntity;
import com.ultramega.asteroidmining.blockentities.RocketControllerBlockEntity;
import com.ultramega.asteroidmining.blockentities.RocketEngineBlockEntity;
import com.ultramega.asteroidmining.blockentities.RocketStorageViewerBlockEntity;
import com.ultramega.asteroidmining.blocks.RocketEngineBlock;

import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, AsteroidMining.MOD_ID);

    public static final Supplier<BlockEntityType<DistillationColumnBlockEntity>> DISTILLATION_COLUMN =
        BLOCK_ENTITY_TYPES.register("distillation_column", () -> new BlockEntityType<>(
            DistillationColumnBlockEntity::new, ModBlocks.DISTILLATION_COLUMN.get()));
    public static final Supplier<BlockEntityType<AirAbsorberBlockEntity>> AIR_ABSORBER =
        BLOCK_ENTITY_TYPES.register("air_absorber", () -> new BlockEntityType<>(
            AirAbsorberBlockEntity::new, ModBlocks.AIR_ABSORBER.get()));
    public static final Supplier<BlockEntityType<HeatExchangerBlockEntity>> HEAT_EXCHANGER =
        BLOCK_ENTITY_TYPES.register("heat_exchanger", () -> new BlockEntityType<>(
            HeatExchangerBlockEntity::new, ModBlocks.HEAT_EXCHANGER.get()));
    public static final Supplier<BlockEntityType<ElectrolysisPlantBlockEntity>> ELECTROLYSIS_PLANT =
        BLOCK_ENTITY_TYPES.register("electrolysis_plant", () -> new BlockEntityType<>(
            ElectrolysisPlantBlockEntity::new, ModBlocks.ELECTROLYSIS_PLANT.get()));
    public static final Supplier<BlockEntityType<BiogasPlantBlockEntity>> BIOGAS_PLANT =
        BLOCK_ENTITY_TYPES.register("biogas_plant", () -> new BlockEntityType<>(
            BiogasPlantBlockEntity::new, ModBlocks.BIOGAS_PLANT.get()));

    public static final Supplier<BlockEntityType<RocketStorageViewerBlockEntity>> ROCKET_STORAGE_VIEWER =
        BLOCK_ENTITY_TYPES.register("rocket_storage_viewer", () -> new BlockEntityType<>(
            RocketStorageViewerBlockEntity::new, ModBlocks.ROCKET_STORAGE_VIEWER.get()));
    public static final Supplier<BlockEntityType<RocketControllerBlockEntity>> ROCKET_CONTROLLER =
        BLOCK_ENTITY_TYPES.register("rocket_controller", () -> new BlockEntityType<>(
            RocketControllerBlockEntity::new, ModBlocks.ROCKET_CONTROLLER.get()));
    public static final Supplier<BlockEntityType<LaunchPadBuilderBlockEntity>> LAUNCH_PAD_BUILDER =
        BLOCK_ENTITY_TYPES.register("launch_pad_builder", () -> new BlockEntityType<>(
            LaunchPadBuilderBlockEntity::new, ModBlocks.LAUNCH_PAD_BUILDER.get()));
    public static final Supplier<BlockEntityType<ObservatoryBlockEntity>> SMALL_OBSERVATORY =
        BLOCK_ENTITY_TYPES.register("small_observatory", () -> new BlockEntityType<>(
            ObservatoryBlockEntity::new, ModBlocks.TELESCOPE.get(), ModBlocks.SMALL_OBSERVATORY.get()));

    //TODO: remove "ROCKET_ENGINE"
    public static final Supplier<BlockEntityType<RocketEngineBlockEntity>> ROCKET_ENGINE =
        BLOCK_ENTITY_TYPES.register("rocket_engine", () -> new BlockEntityType<>(
            (pos, state) -> new RocketEngineBlockEntity(RocketEngineBlock.Type.F1, pos, state), ModBlocks.ROCKET_ENGINE.get()));
    public static final Supplier<BlockEntityType<RocketEngineBlockEntity>> RS25_ENGINE =
        BLOCK_ENTITY_TYPES.register("rs25_engine", () -> new BlockEntityType<>(
            (pos, state) -> new RocketEngineBlockEntity(RocketEngineBlock.Type.RS25, pos, state), ModBlocks.RS25_ENGINE.get()));

    public static final Supplier<BlockEntityType<BoundingBoxBlockEntity>> BOUNDING_BOX =
        BLOCK_ENTITY_TYPES.register("bounding_box", () -> new BlockEntityType<>(
            BoundingBoxBlockEntity::new, ModBlocks.BOUNDING_BOX.get()));

    private ModBlockEntityTypes() {
    }
}
