package com.ultramega.asteroidmining.datagen.recipe;

import com.ultramega.asteroidmining.datagen.recipe.builder.HeatExchangeRecipeBuilder;
import com.ultramega.asteroidmining.registry.ModFluids;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

public class HeatExchangeRecipes extends AsteroidMiningRecipeProvider {
    protected HeatExchangeRecipes(final HolderLookup.Provider registries, final RecipeOutput output) {
        super(registries, output, "heat_exchange");
    }

    @Override
    protected void buildRecipes() {
        new HeatExchangeRecipeBuilder(
            SizedFluidIngredient.of(ModFluids.AIR.get(), 70),
            SizedFluidIngredient.of(ModFluids.LIQUID_AIR.get(), 10),
            40
        ).save(this.output, this.recipeId("air"));
        new HeatExchangeRecipeBuilder(
            SizedFluidIngredient.of(ModFluids.HYDROGEN.get(), 85),
            SizedFluidIngredient.of(ModFluids.LIQUID_HYDROGEN.get(), 10),
            60
        ).save(this.output, this.recipeId("hydrogen"));
        new HeatExchangeRecipeBuilder(
            SizedFluidIngredient.of(ModFluids.METHANE.get(), 60),
            SizedFluidIngredient.of(ModFluids.LIQUID_METHANE.get(), 10),
            20
        ).save(this.output, this.recipeId("methane"));
    }
}
