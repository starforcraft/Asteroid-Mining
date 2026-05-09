package com.ultramega.asteroidmining.registry;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.blocks.AirAbsorberBlock;
import com.ultramega.asteroidmining.blocks.BiogasPlantBlock;
import com.ultramega.asteroidmining.blocks.BoundingBoxBlock;
import com.ultramega.asteroidmining.blocks.DistillationColumnBlock;
import com.ultramega.asteroidmining.blocks.ElectrolysisPlantBlock;
import com.ultramega.asteroidmining.blocks.HeatExchangerBlock;
import com.ultramega.asteroidmining.blocks.LaunchPadBuilderBlock;
import com.ultramega.asteroidmining.blocks.ObservatoryBlock;
import com.ultramega.asteroidmining.blocks.RocketControllerBlock;
import com.ultramega.asteroidmining.blocks.RocketDrillBlock;
import com.ultramega.asteroidmining.blocks.RocketEngineBlock;
import com.ultramega.asteroidmining.blocks.RocketStorageViewerBlock;
import com.ultramega.asteroidmining.blocks.StorageTankBlock;

import java.util.function.Supplier;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AsteroidMining.MOD_ID);

    public static final DeferredBlock<Block> TELESCOPE = BLOCKS.registerBlock("telescope", ObservatoryBlock::new,
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(4F, 5F));
    public static final DeferredBlock<Block> SMALL_OBSERVATORY = BLOCKS.registerBlock("small_observatory", ObservatoryBlock::new,
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(4F, 5F));

    public static final DeferredBlock<Block> DISTILLATION_COLUMN = BLOCKS.registerBlock("distillation_column", DistillationColumnBlock::new,
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(9F, 50F));
    public static final DeferredBlock<Block> AIR_ABSORBER = BLOCKS.registerBlock("air_absorber", AirAbsorberBlock::new,
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(9F, 50F));
    public static final DeferredBlock<Block> HEAT_EXCHANGER = BLOCKS.registerBlock("heat_exchanger", HeatExchangerBlock::new,
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(9F, 50F));
    public static final DeferredBlock<Block> ELECTROLYSIS_PLANT = BLOCKS.registerBlock("electrolysis_plant", ElectrolysisPlantBlock::new,
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(9F, 50F));
    public static final DeferredBlock<Block> BIOGAS_PLANT = BLOCKS.registerBlock("biogas_plant", BiogasPlantBlock::new,
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(9F, 50F));
    public static final DeferredBlock<Block> TRANSFORMER = BLOCKS.registerBlock("transformer", Block::new,
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(9F, 50F));

    public static final DeferredBlock<Block> ROCKET_STORAGE_VIEWER = BLOCKS.registerBlock("rocket_storage_viewer", RocketStorageViewerBlock::new,
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(2.5F, 5F));
    public static final DeferredBlock<Block> ROCKET_CONTROLLER = BLOCKS.registerBlock("rocket_controller", RocketControllerBlock::new,
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(2.5F, 5F));
    public static final DeferredBlock<Block> LAUNCH_PAD_BUILDER = BLOCKS.registerBlock("launch_pad_builder", LaunchPadBuilderBlock::new,
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(2.5F, 5F));

    //TODO: remove "ROCKET_ENGINE"
    public static final DeferredBlock<RocketEngineBlock> ROCKET_ENGINE = BLOCKS.registerBlock("rocket_engine", (props) -> new RocketEngineBlock(RocketEngineBlock.Type.F1, props),
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(4F, 5F));
    public static final DeferredBlock<RocketEngineBlock> RS25_ENGINE = BLOCKS.registerBlock("rs25_engine", (props) -> new RocketEngineBlock(RocketEngineBlock.Type.RS25, props),
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(4F, 5F));

    public static final DeferredBlock<Block> METAL_SCAFFOLDING = BLOCKS.registerSimpleBlock("metal_scaffolding",
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(3F, 20F));
    public static final DeferredBlock<Block> ROCKET_BASE = BLOCKS.registerSimpleBlock("rocket_base",
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(2.5F, 30F));

    public static final DeferredBlock<StorageTankBlock> ITEM_STORAGE_TIER_1 = BLOCKS.registerBlock("item_storage_tier_1", (props) ->
            new StorageTankBlock(StorageTankBlock.Type.ITEMS, StorageTankBlock.Capacity.TIER_1, props),
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(4F, 5F));
    public static final DeferredBlock<StorageTankBlock> ITEM_STORAGE_TIER_2 = BLOCKS.registerBlock("item_storage_tier_2", (props) ->
            new StorageTankBlock(StorageTankBlock.Type.ITEMS, StorageTankBlock.Capacity.TIER_2, props),
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(4F, 5F));
    public static final DeferredBlock<StorageTankBlock> ITEM_STORAGE_TIER_3 = BLOCKS.registerBlock("item_storage_tier_3", (props) ->
            new StorageTankBlock(StorageTankBlock.Type.ITEMS, StorageTankBlock.Capacity.TIER_3, props),
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(4F, 5F));
    public static final DeferredBlock<StorageTankBlock> ITEM_STORAGE_TIER_4 = BLOCKS.registerBlock("item_storage_tier_4", (props) ->
            new StorageTankBlock(StorageTankBlock.Type.ITEMS, StorageTankBlock.Capacity.TIER_4, props),
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(4F, 5F));
    public static final DeferredBlock<StorageTankBlock> FLUID_TANK_TIER_1 = BLOCKS.registerBlock("fluid_tank_tier_1", (props) ->
            new StorageTankBlock(StorageTankBlock.Type.FLUIDS, StorageTankBlock.Capacity.TIER_1, props),
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(4F, 5F));
    public static final DeferredBlock<StorageTankBlock> FLUID_TANK_TIER_2 = BLOCKS.registerBlock("fluid_tank_tier_2", (props) ->
            new StorageTankBlock(StorageTankBlock.Type.FLUIDS, StorageTankBlock.Capacity.TIER_2, props),
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(4F, 5F));
    public static final DeferredBlock<StorageTankBlock> FLUID_TANK_TIER_3 = BLOCKS.registerBlock("fluid_tank_tier_3", (props) ->
            new StorageTankBlock(StorageTankBlock.Type.FLUIDS, StorageTankBlock.Capacity.TIER_3, props),
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(4F, 5F));
    public static final DeferredBlock<StorageTankBlock> FLUID_TANK_TIER_4 = BLOCKS.registerBlock("fluid_tank_tier_4", (props) ->
            new StorageTankBlock(StorageTankBlock.Type.FLUIDS, StorageTankBlock.Capacity.TIER_4, props),
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(4F, 5F));

    public static final DeferredBlock<Block> IRON_ROCKET_DRILL = BLOCKS.registerBlock("iron_rocket_drill", (props) ->
            new RocketDrillBlock(props, RocketDrillBlock.DrillType.IRON),
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(4F, 5F));
    public static final DeferredBlock<Block> DIAMOND_ROCKET_DRILL = BLOCKS.registerBlock("diamond_rocket_drill", (props) ->
            new RocketDrillBlock(props, RocketDrillBlock.DrillType.DIAMOND),
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(5F, 6F));
    public static final DeferredBlock<Block> EMERALD_ROCKET_DRILL = BLOCKS.registerBlock("emerald_rocket_drill", (props) ->
            new RocketDrillBlock(props, RocketDrillBlock.DrillType.EMERALD),
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(5F, 6F));
    public static final DeferredBlock<Block> NETHERITE_ROCKET_DRILL = BLOCKS.registerBlock("netherite_rocket_drill", (props) ->
            new RocketDrillBlock(props, RocketDrillBlock.DrillType.NETHERITE),
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion().strength(50F, 1200F));

    public static final DeferredBlock<Block> BOUNDING_BOX = BLOCKS.registerBlock("bounding_box", BoundingBoxBlock::new,
        () -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().noOcclusion());

    public static final DeferredBlock<LiquidBlock> PETROLEUM = registerFluid("petroleum", ModFluids.PETROLEUM_SOURCE, MapColor.COLOR_BROWN);
    public static final DeferredBlock<LiquidBlock> KEROSENE = registerFluid("kerosene", ModFluids.KEROSENE_SOURCE, MapColor.COLOR_YELLOW);

    private ModBlocks() {
    }

    private static DeferredBlock<LiquidBlock> registerFluid(final String name, final Supplier<? extends FlowingFluid> fluid, final MapColor mapColor) {
        return BLOCKS.registerBlock(name,
            props -> new LiquidBlock(fluid.get(), props),
            () -> BlockBehaviour.Properties.of()
                .mapColor(mapColor)
                .replaceable()
                .noCollision()
                .strength(100.0F)
                .pushReaction(PushReaction.DESTROY)
                .noLootTable()
                .liquid()
                .sound(SoundType.EMPTY)
        );
    }

    /**
     * Copied and modified from {@link ClientLevel#addBreakingBlockEffect(BlockPos, Direction, HitResult)}
     */
    public static IClientBlockExtensions crack() {
        return new IClientBlockExtensions() {
            @Override
            public boolean addHitEffects(final BlockState state, final Level level, final HitResult target, final ParticleEngine manager) {
                if (target.getType() == HitResult.Type.BLOCK && target instanceof BlockHitResult blockTarget) {
                    final BlockPos pos = blockTarget.getBlockPos();
                    final BlockPos mainPos = BoundingBoxBlock.getMainBlockPos(level, pos);
                    if (mainPos != null) {
                        final BlockState mainState = level.getBlockState(mainPos);
                        if (!mainState.isAir()) {
                            final AABB shape = state.getShape(level, pos).bounds();
                            double xp = pos.getX() + level.getRandom().nextDouble() * (shape.maxX - shape.minX - 0.2F) + 0.1F + shape.minX;
                            double yp = pos.getY() + level.getRandom().nextDouble() * (shape.maxY - shape.minY - 0.2F) + 0.1F + shape.minY;
                            double zp = pos.getZ() + level.getRandom().nextDouble() * (shape.maxZ - shape.minZ - 0.2F) + 0.1F + shape.minZ;
                            final Direction side = blockTarget.getDirection();
                            switch (side) {
                                case DOWN -> yp = pos.getY() + shape.minY - 0.1F;
                                case UP -> yp = pos.getY() + shape.maxY + 0.1F;
                                case NORTH -> zp = pos.getZ() + shape.minZ - 0.1F;
                                case SOUTH -> zp = pos.getZ() + shape.maxZ + 0.1F;
                                case WEST -> xp = pos.getX() + shape.minX - 0.1F;
                                case EAST -> xp = pos.getX() + shape.maxX + 0.1F;
                            }
                            manager.add(new TerrainParticle((ClientLevel) level, xp, yp, zp, 0, 0, 0, mainState)
                                .updateSprite(mainState, mainPos).setPower(0.2F).scale(0.6F));
                            return true;
                        }
                    }
                }
                return false;
            }
        };
    }
}
