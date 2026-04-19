package com.ultramega.asteroidmining.datagen;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.datagen.builder.HeatExchangeRecipeBuilder;
import com.ultramega.asteroidmining.registry.ModFluids;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

public class HeatExchangeRecipeProvider extends RecipeProvider {
    protected HeatExchangeRecipeProvider(final HolderLookup.Provider registries, final RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        new HeatExchangeRecipeBuilder(
            SizedFluidIngredient.of(ModFluids.AIR.get(), 70), //TODO: update to the new NeoForge fluid ingredient(?)
            SizedFluidIngredient.of(ModFluids.LIQUID_AIR.get(), 10),
            40
        ).save(this.output, recipeId("air"));
        new HeatExchangeRecipeBuilder(
            SizedFluidIngredient.of(ModFluids.HYDROGEN.get(), 85),
            SizedFluidIngredient.of(ModFluids.LIQUID_HYDROGEN.get(), 10),
            60
        ).save(this.output, recipeId("hydrogen"));
        new HeatExchangeRecipeBuilder(
            SizedFluidIngredient.of(ModFluids.METHANE.get(), 60),
            SizedFluidIngredient.of(ModFluids.LIQUID_METHANE.get(), 10),
            20
        ).save(this.output, recipeId("methane"));
    }

    private static String recipeId(final String id) {
        return AsteroidMining.makeId(id).toString();
    }

    public static final class Runner extends RecipeProvider.Runner {
        public Runner(final PackOutput packOutput, final CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(final HolderLookup.Provider registries,
                                                      final RecipeOutput output) {
            return new HeatExchangeRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Asteroid Mining heat exchange recipes";
        }
    }
}
