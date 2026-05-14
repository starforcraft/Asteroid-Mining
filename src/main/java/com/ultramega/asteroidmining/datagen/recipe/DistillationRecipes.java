package com.ultramega.asteroidmining.datagen.recipe;

import com.ultramega.asteroidmining.datagen.recipe.builder.DistillationRecipeBuilder;
import com.ultramega.asteroidmining.registry.ModFluids;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

public class DistillationRecipes extends AsteroidMiningRecipeProvider {
    protected DistillationRecipes(final HolderLookup.Provider registries, final RecipeOutput output) {
        super(registries, output, "distillation");
    }

    @Override
    protected void buildRecipes() {
        DistillationRecipeBuilder.withReagent(
            SizedFluidIngredient.of(ModFluids.PETROLEUM_SOURCE.get(), 200),
            SizedFluidIngredient.of(ModFluids.HYDROGEN.get(), 1),
            new FluidStackTemplate(ModFluids.KEROSENE_SOURCE.get(), 20),
            40,
            150,
            300
        ).save(this.output, this.recipeId("kerosene"));

        DistillationRecipeBuilder.of(
            SizedFluidIngredient.of(ModFluids.LIQUID_AIR.get(), 850),
            new FluidStackTemplate(ModFluids.LIQUID_OXYGEN.get(), 1),
            40,
            -196,
            -183
        ).save(this.output, this.recipeId("liquid_oxygen"));
    }
}
