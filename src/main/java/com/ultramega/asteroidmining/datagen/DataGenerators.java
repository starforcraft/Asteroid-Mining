package com.ultramega.asteroidmining.datagen;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.datagen.loot.LootTableProviderImpl;
import com.ultramega.asteroidmining.datagen.model.ModelProviders;
import com.ultramega.asteroidmining.datagen.recipe.AsteroidMiningRecipeProvider;
import com.ultramega.asteroidmining.datagen.tag.TagsProviderImpl;

import net.minecraft.data.DataGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = AsteroidMining.MOD_ID)
public final class DataGenerators {
    private DataGenerators() {
    }

    @SubscribeEvent
    public static void onGatherData(final GatherDataEvent.Client e) { //TODO: block/item model generators
        final DataGenerator generator = e.getGenerator();
        final DataGenerator.PackGenerator pack = generator.getVanillaPack(true);

        pack.addProvider(ModelProviders::new);
        pack.addProvider(AsteroidProvider::new);
        pack.addProvider(output -> new AsteroidMiningRecipeProvider.Runner(output, e.getLookupProvider()));

        pack.addProvider(output -> new LootTableProviderImpl(output, e.getLookupProvider()));
        final TagsProviderImpl.Blocks blockTagsProvider = pack.addProvider(output -> new TagsProviderImpl.Blocks(output, e.getLookupProvider()));
        pack.addProvider(output -> new TagsProviderImpl.Items(output, e.getLookupProvider(), blockTagsProvider.contentsGetter()));
        pack.addProvider(output -> new TagsProviderImpl.Fluids(output, e.getLookupProvider()));

        pack.addProvider(output -> new DataMapProviderImpl(output, e.getLookupProvider()));
    }
}
