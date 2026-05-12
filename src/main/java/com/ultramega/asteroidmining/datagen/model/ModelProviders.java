package com.ultramega.asteroidmining.datagen.model;

import com.ultramega.asteroidmining.blocks.AbstractModuleBlock;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.registry.ModItems;

import java.util.stream.Stream;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import static com.ultramega.asteroidmining.AsteroidMining.MOD_ID;
import static com.ultramega.asteroidmining.AsteroidMining.makeId;
import static net.minecraft.client.data.models.BlockModelGenerators.ROTATION_HORIZONTAL_FACING;
import static net.minecraft.client.data.models.BlockModelGenerators.createSimpleBlock;
import static net.minecraft.client.data.models.BlockModelGenerators.plainVariant;
import static net.minecraft.client.data.models.model.TextureSlot.FRONT;
import static net.minecraft.client.data.models.model.TextureSlot.PARTICLE;
import static net.minecraft.client.data.models.model.TextureSlot.SIDE;
import static net.minecraft.client.data.models.model.TextureSlot.TOP;

public class ModelProviders extends ModelProvider {
    public ModelProviders(final PackOutput output) {
        super(output, MOD_ID);
    }

    @Override
    protected void registerModels(final BlockModelGenerators blockModels, final ItemModelGenerators itemModels) {
        this.registerModelBlockItems(blockModels, itemModels);
        this.registerSimpleBlockItems(blockModels, itemModels);
        this.registerFluids(blockModels);
        this.registerSimpleItems(itemModels);
    }

    private void registerModelBlockItems(final BlockModelGenerators blockModels, final ItemModelGenerators itemModels) {
        this.registerWithParentBlockItem(itemModels, ModBlocks.TELESCOPE.get());
        this.registerWithParentBlockItem(itemModels, ModBlocks.SMALL_OBSERVATORY.get());
        this.registerWithParentBlockItem(itemModels, ModBlocks.DISTILLATION_COLUMN.get());
        this.registerWithParentBlockItem(itemModels, ModBlocks.BIOGAS_PLANT.get());
        this.registerWithParentBlockItem(itemModels, ModBlocks.ROCKET_CONTROLLER.get());
        this.registerWithParentBlockItem(itemModels, ModBlocks.LAUNCH_PAD_BUILDER.get());
        this.registerWithParentBlockItem(itemModels, ModBlocks.ROCKET_ENGINE.get());
        this.registerWithParentBlockItem(itemModels, ModBlocks.RS25_ENGINE.get());
        this.registerWithParentBlockItem(itemModels, ModBlocks.METAL_SCAFFOLDING.get());
        this.registerWithParentBlockItem(itemModels, ModBlocks.IRON_ROCKET_DRILL.get());
        this.registerWithParentBlockItem(itemModels, ModBlocks.DIAMOND_ROCKET_DRILL.get());
        this.registerWithParentBlockItem(itemModels, ModBlocks.EMERALD_ROCKET_DRILL.get());
        this.registerWithParentBlockItem(itemModels, ModBlocks.NETHERITE_ROCKET_DRILL.get());

        this.registerWithFrontBlockAndItem(blockModels, itemModels, ModBlocks.AIR_ABSORBER.get());
        this.registerWithFrontBlockAndItem(blockModels, itemModels, ModBlocks.ROCKET_STORAGE_VIEWER.get());
    }

    private void registerSimpleBlockItems(final BlockModelGenerators blockModels, final ItemModelGenerators itemModels) {
        this.registerCubeAllBlockItem(blockModels, itemModels, ModBlocks.HEAT_EXCHANGER.get());
        this.registerCubeAllBlockItem(blockModels, itemModels, ModBlocks.ELECTROLYSIS_PLANT.get());
        this.registerCubeAllBlockItem(blockModels, itemModels, ModBlocks.TRANSFORMER.get());
        this.registerCubeAllBlockItem(blockModels, itemModels, ModBlocks.ROCKET_BASE.get());
        this.registerCubeAllBlockItem(blockModels, itemModels, ModBlocks.ITEM_STORAGE_TIER_1.get());
        this.registerCubeAllBlockItem(blockModels, itemModels, ModBlocks.ITEM_STORAGE_TIER_2.get());
        this.registerCubeAllBlockItem(blockModels, itemModels, ModBlocks.ITEM_STORAGE_TIER_3.get());
        this.registerCubeAllBlockItem(blockModels, itemModels, ModBlocks.ITEM_STORAGE_TIER_4.get());
        this.registerCubeAllBlockItem(blockModels, itemModels, ModBlocks.FLUID_TANK_TIER_1.get());
        this.registerCubeAllBlockItem(blockModels, itemModels, ModBlocks.FLUID_TANK_TIER_2.get());
        this.registerCubeAllBlockItem(blockModels, itemModels, ModBlocks.FLUID_TANK_TIER_3.get());
        this.registerCubeAllBlockItem(blockModels, itemModels, ModBlocks.FLUID_TANK_TIER_4.get());
    }

    private void registerFluids(final BlockModelGenerators blockModels) {
        this.registerParticleOnlyBlock(blockModels, ModBlocks.PETROLEUM.get());
        this.registerParticleOnlyBlock(blockModels, ModBlocks.KEROSENE.get());
    }

    private void registerSimpleItems(final ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(ModItems.CONFIGURATION_CARD.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.AIR_BUCKET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.LIQUID_AIR_BUCKET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.LIQUID_OXYGEN_BUCKET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.METHANE_BUCKET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.LIQUID_METHANE_BUCKET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.HYDROGEN_BUCKET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.LIQUID_HYDROGEN_BUCKET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.ROCKET_PROPELLANT_BUCKET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.PETROLEUM_BUCKET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.KEROSENE_BUCKET.get(), ModelTemplates.FLAT_ITEM);
    }

    private void registerCubeAllBlockItem(final BlockModelGenerators blockModels, final ItemModelGenerators itemModels, final Block block) {
        final Identifier id = this.getBlockId(block);
        final Identifier blockModel = ModelTemplates.CUBE_ALL.create(block, TextureMapping.cube(texture(id)), blockModels.modelOutput);
        itemModels.itemModelOutput.accept(block.asItem(), ItemModelUtils.plainModel(blockModel));
    }

    private void registerWithParentBlockItem(final ItemModelGenerators itemModels, final Block block) {
        final Identifier id = this.getBlockId(block);
        itemModels.itemModelOutput.accept(block.asItem(), ItemModelUtils.plainModel(id));
    }

    private void registerWithFrontBlockAndItem(final BlockModelGenerators blockModels, final ItemModelGenerators itemModels, final Block block) {
        final Identifier id = this.getBlockId(block);
        final Identifier frontTexture = id.withSuffix("_front");
        final Identifier sideTexture = id.withSuffix("_side");

        final Identifier blockModel = ModelTemplates.CUBE_ORIENTABLE.create(
            block,
            new TextureMapping()
                .put(PARTICLE, texture(sideTexture))
                .put(FRONT, texture(frontTexture))
                .put(TOP, texture(sideTexture))
                .put(SIDE, texture(sideTexture)),
            blockModels.modelOutput
        );

        MultiVariantGenerator variant = MultiVariantGenerator.dispatch(block, plainVariant(blockModel))
            .with(ROTATION_HORIZONTAL_FACING);
        if (block instanceof AbstractModuleBlock) {
            variant.with(AbstractModuleBlock.ACTIVE); //TODO
        }
        blockModels.blockStateOutput.accept(variant);
        itemModels.itemModelOutput.accept(block.asItem(), ItemModelUtils.plainModel(blockModel));
    }

    private void registerParticleOnlyBlock(final BlockModelGenerators blockModels, final Block block) {
        final String id = this.getBlockId(block).getPath().replace("block/", "fluid/");

        final MultiVariant model = plainVariant(ModelTemplates.PARTICLE_ONLY.create(block,
            TextureMapping.particle(texture(makeId(id + "_still"))), blockModels.modelOutput));
        blockModels.blockStateOutput.accept(createSimpleBlock(block, model));
    }

    private Identifier getBlockId(final Block block) {
        final String[] splitId = block.getDescriptionId().split("\\.");
        return makeId("block/" + splitId[splitId.length - 1]);
    }

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return Stream.of();
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return Stream.of();
    }

    private static Material texture(final Identifier location) {
        return new Material(location);
    }
}
