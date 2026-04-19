package com.ultramega.asteroidmining.registry;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.items.BaseBlockItem;
import com.ultramega.asteroidmining.items.ConfigurationCardItem;
import com.ultramega.asteroidmining.items.RocketEngineBlockItem;
import com.ultramega.asteroidmining.items.StorageTankBlockItem;

import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AsteroidMining.MOD_ID);

    public static final DeferredItem<Item> CONFIGURATION_CARD = ITEMS.registerItem("configuration_card", ConfigurationCardItem::new);

    public static final DeferredItem<Item> AIR_BUCKET = registerBucket("air_bucket", (props) ->
        new BucketItem(ModFluids.AIR.get(), props));
    public static final DeferredItem<Item> LIQUID_AIR_BUCKET = registerBucket("liquid_air_bucket", (props) ->
        new BucketItem(ModFluids.LIQUID_AIR.get(), props));
    public static final DeferredItem<Item> LIQUID_OXYGEN_BUCKET = registerBucket("liquid_oxygen_bucket", (props) ->
        new BucketItem(ModFluids.LIQUID_OXYGEN.get(), props));
    public static final DeferredItem<Item> METHANE_BUCKET = registerBucket("methane_bucket", (props) ->
        new BucketItem(ModFluids.METHANE.get(), props));
    public static final DeferredItem<Item> LIQUID_METHANE_BUCKET = registerBucket("liquid_methane_bucket", (props) ->
        new BucketItem(ModFluids.LIQUID_METHANE.get(), props));
    public static final DeferredItem<Item> HYDROGEN_BUCKET = registerBucket("hydrogen_bucket", (props) ->
        new BucketItem(ModFluids.HYDROGEN.get(), props));
    public static final DeferredItem<Item> LIQUID_HYDROGEN_BUCKET = registerBucket("liquid_hydrogen_bucket", (props) ->
        new BucketItem(ModFluids.LIQUID_HYDROGEN.get(), props));
    public static final DeferredItem<Item> ROCKET_PROPELLANT_BUCKET = registerBucket("rocket_propellant_bucket", (props) ->
        new BucketItem(ModFluids.ROCKET_PROPELLANT.get(), props));
    public static final DeferredItem<Item> PETROLEUM_BUCKET = registerBucket("petroleum_bucket", (props) ->
        new BucketItem(ModFluids.PETROLEUM_SOURCE.get(), props));
    public static final DeferredItem<Item> KEROSENE_BUCKET = registerBucket("kerosene_bucket", (props) ->
        new BucketItem(ModFluids.KEROSENE_SOURCE.get(), props));

    public static final DeferredItem<BlockItem> TELESCOPE = registerSimpleBlockItem("telescope", ModBlocks.TELESCOPE);
    public static final DeferredItem<BlockItem> SMALL_OBSERVATORY = registerSimpleBlockItem("small_observatory", ModBlocks.SMALL_OBSERVATORY);

    public static final DeferredItem<BlockItem> DISTILLATION_COLUMN = registerSimpleBlockItem("distillation_column", ModBlocks.DISTILLATION_COLUMN);
    public static final DeferredItem<BlockItem> AIR_ABSORBER = registerSimpleBlockItem("air_absorber", ModBlocks.AIR_ABSORBER);
    public static final DeferredItem<BlockItem> HEAT_EXCHANGER = registerSimpleBlockItem("heat_exchanger", ModBlocks.HEAT_EXCHANGER);
    public static final DeferredItem<BlockItem> ELECTROLYSIS_PLANT = registerSimpleBlockItem("electrolysis_plant", ModBlocks.ELECTROLYSIS_PLANT);
    public static final DeferredItem<BlockItem> BIOGAS_PLANT = registerSimpleBlockItem("biogas_plant", ModBlocks.BIOGAS_PLANT);
    public static final DeferredItem<BlockItem> TRANSFORMER = registerSimpleBlockItem("transformer", ModBlocks.TRANSFORMER);

    public static final DeferredItem<BlockItem> ROCKET_STORAGE_VIEWER = registerSimpleBlockItem("rocket_storage_viewer", ModBlocks.ROCKET_STORAGE_VIEWER);
    public static final DeferredItem<BlockItem> ROCKET_CONTROLLER = registerSimpleBlockItem("rocket_controller", ModBlocks.ROCKET_CONTROLLER);
    public static final DeferredItem<BlockItem> LAUNCH_PAD_BUILDER = registerSimpleBlockItem("launch_pad_builder", ModBlocks.LAUNCH_PAD_BUILDER);

    public static final DeferredItem<BlockItem> ROCKET_ENGINE = ITEMS.registerItem("rocket_engine", props ->
        new RocketEngineBlockItem(ModBlocks.ROCKET_ENGINE.get(), props));
    public static final DeferredItem<BlockItem> RS25_ENGINE = ITEMS.registerItem("rs25_engine", props ->
        new RocketEngineBlockItem(ModBlocks.RS25_ENGINE.get(), props));

    public static final DeferredItem<BlockItem> METAL_SCAFFOLDING = registerSimpleBlockItem("metal_scaffolding", ModBlocks.METAL_SCAFFOLDING);
    public static final DeferredItem<BlockItem> ROCKET_BASE = registerSimpleBlockItem("rocket_base", ModBlocks.ROCKET_BASE);

    public static final DeferredItem<BlockItem> ITEM_STORAGE_TIER_1 = ITEMS.registerItem("item_storage_tier_1", props ->
        new StorageTankBlockItem(ModBlocks.ITEM_STORAGE_TIER_1.get(), props));
    public static final DeferredItem<BlockItem> ITEM_STORAGE_TIER_2 = ITEMS.registerItem("item_storage_tier_2", props ->
        new StorageTankBlockItem(ModBlocks.ITEM_STORAGE_TIER_2.get(), props));
    public static final DeferredItem<BlockItem> ITEM_STORAGE_TIER_3 = ITEMS.registerItem("item_storage_tier_3", props ->
        new StorageTankBlockItem(ModBlocks.ITEM_STORAGE_TIER_3.get(), props));
    public static final DeferredItem<BlockItem> ITEM_STORAGE_TIER_4 = ITEMS.registerItem("item_storage_tier_4", props ->
        new StorageTankBlockItem(ModBlocks.ITEM_STORAGE_TIER_4.get(), props));
    public static final DeferredItem<BlockItem> FLUID_TANK_TIER_1 = ITEMS.registerItem("fluid_tank_tier_1", props ->
        new StorageTankBlockItem(ModBlocks.FLUID_TANK_TIER_1.get(), props));
    public static final DeferredItem<BlockItem> FLUID_TANK_TIER_2 = ITEMS.registerItem("fluid_tank_tier_2", props ->
        new StorageTankBlockItem(ModBlocks.FLUID_TANK_TIER_2.get(), props));
    public static final DeferredItem<BlockItem> FLUID_TANK_TIER_3 = ITEMS.registerItem("fluid_tank_tier_3", props ->
        new StorageTankBlockItem(ModBlocks.FLUID_TANK_TIER_3.get(), props));
    public static final DeferredItem<BlockItem> FLUID_TANK_TIER_4 = ITEMS.registerItem("fluid_tank_tier_4", props ->
        new StorageTankBlockItem(ModBlocks.FLUID_TANK_TIER_4.get(), props));

    public static final DeferredItem<BlockItem> IRON_ROCKET_DRILL = registerSimpleBlockItem("iron_rocket_drill", ModBlocks.IRON_ROCKET_DRILL);
    public static final DeferredItem<BlockItem> DIAMOND_ROCKET_DRILL = registerSimpleBlockItem("diamond_rocket_drill", ModBlocks.DIAMOND_ROCKET_DRILL);
    public static final DeferredItem<BlockItem> EMERALD_ROCKET_DRILL = registerSimpleBlockItem("emerald_rocket_drill", ModBlocks.EMERALD_ROCKET_DRILL);
    public static final DeferredItem<BlockItem> NETHERITE_ROCKET_DRILL = registerSimpleBlockItem("netherite_rocket_drill", ModBlocks.NETHERITE_ROCKET_DRILL);

    private ModItems() {
    }

    private static <I extends Item> DeferredItem<I> registerBucket(final String name, final Function<Item.Properties, ? extends I> func) {
        return ITEMS.register(name, key -> func.apply(new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET).setId(ResourceKey.create(Registries.ITEM, key))));
    }

    private static DeferredItem<BlockItem> registerSimpleBlockItem(final String name, final Supplier<? extends Block> block) {
        return registerSimpleBlockItem(name, block, Item.Properties::new);
    }

    private static DeferredItem<BlockItem> registerSimpleBlockItem(final String name, final Supplier<? extends Block> block, final Supplier<Item.Properties> properties) {
        return ITEMS.registerItem(name, props -> new BaseBlockItem(block.get(), props), () -> properties.get().useBlockDescriptionPrefix());
    }
}
