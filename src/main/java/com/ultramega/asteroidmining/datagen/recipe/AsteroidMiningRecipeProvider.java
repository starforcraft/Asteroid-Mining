package com.ultramega.asteroidmining.datagen.recipe;

import com.ultramega.asteroidmining.AsteroidMining;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;

public abstract class AsteroidMiningRecipeProvider extends RecipeProvider {
    private final String name;

    protected AsteroidMiningRecipeProvider(final HolderLookup.Provider registries, final RecipeOutput output, final String name) {
        super(registries, output);
        this.name = name;
    }

    protected final String recipeId(final String id) {
        return AsteroidMining.makeId(this.name + "/" + id).toString();
    }

    @FunctionalInterface
    public interface RecipeProviderFactory {
        AsteroidMiningRecipeProvider create(HolderLookup.Provider registries, RecipeOutput output);
    }

    public static final class Runner extends RecipeProvider.Runner {
        private static final List<RecipeProviderFactory> PROVIDERS = List.of(
            HeatExchangeRecipes::new,
            DistillationRecipes::new
        );

        public Runner(final PackOutput packOutput, final CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(final HolderLookup.Provider registries, final RecipeOutput output) {
            return new RecipeProvider(registries, output) {
                @Override
                protected void buildRecipes() {
                    for (final RecipeProviderFactory provider : PROVIDERS) {
                        provider.create(this.registries, this.output).buildRecipes();
                    }
                }
            };
        }

        @Override
        public String getName() {
            return "Asteroid Mining Recipes";
        }
    }
}
