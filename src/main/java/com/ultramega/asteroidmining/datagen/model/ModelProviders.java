package com.ultramega.asteroidmining.datagen.model;

import com.ultramega.asteroidmining.registry.ModItems;

import java.util.stream.Stream;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import static com.ultramega.asteroidmining.AsteroidMining.MOD_ID;

public class ModelProviders extends ModelProvider {
    public ModelProviders(final PackOutput output) {
        super(output, MOD_ID);
    }

    @Override
    protected void registerModels(final BlockModelGenerators blockModels, final ItemModelGenerators itemModels) {
        this.registerSimpleItems(itemModels);
    }

    private void registerSimpleBlockItem(final ItemModelGenerators itemModels) {
    }

    private void registerSimpleItems(final ItemModelGenerators itemModels) {
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

    private void registerPlainItem(final ItemModelGenerators itemModels, final Block block) {
//        final Identifier blockModel = ModelTemplates.CUBE_ALL.create()
//        itemModels.itemModelOutput.accept(block.asItem(), ItemModelUtils.plainModel(blockModel));
    }

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return Stream.of();
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return Stream.of();
    }
}
