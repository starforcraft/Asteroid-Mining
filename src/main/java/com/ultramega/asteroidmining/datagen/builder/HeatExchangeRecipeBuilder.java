package com.ultramega.asteroidmining.datagen.builder;

import com.ultramega.asteroidmining.recipe.HeatExchangerRecipe;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

public class HeatExchangeRecipeBuilder extends SimpleRecipeBuilder {
    private final SizedFluidIngredient input;
    private final SizedFluidIngredient output;
    private final int duration;

    public HeatExchangeRecipeBuilder(final SizedFluidIngredient input, final SizedFluidIngredient output, final int duration) {
        super(ItemStack.EMPTY);
        this.input = input;
        this.output = output;
        this.duration = duration;
    }

    @Override
    public void save(final RecipeOutput recipeOutput, final ResourceKey<Recipe<?>> resourceKey) {
        final HeatExchangerRecipe recipe = new HeatExchangerRecipe(this.input, this.output, this.duration);
        recipeOutput.accept(resourceKey, recipe, null, this.conditions.toArray(new ICondition[0]));
    }
}
