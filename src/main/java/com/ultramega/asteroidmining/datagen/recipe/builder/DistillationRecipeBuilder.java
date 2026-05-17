package com.ultramega.asteroidmining.datagen.recipe.builder;

import com.ultramega.asteroidmining.recipe.DistillationRecipe;

import java.util.Optional;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jspecify.annotations.Nullable;

public class DistillationRecipeBuilder extends SimpleRecipeBuilder {
    private final SizedFluidIngredient input;
    @Nullable
    private final SizedFluidIngredient reagent;
    private final FluidStackTemplate output;
    private final int duration;
    private final int minTemperature;
    private final int maxTemperature;

    public DistillationRecipeBuilder(final SizedFluidIngredient input,
                                     @Nullable final SizedFluidIngredient reagent,
                                     final FluidStackTemplate output,
                                     final int duration,
                                     final int minTemperature,
                                     final int maxTemperature) {
        super(ItemStack.EMPTY);
        this.input = input;
        this.reagent = reagent;
        this.output = output;
        this.duration = duration;
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
    }

    public static DistillationRecipeBuilder of(final SizedFluidIngredient input,
                                               final FluidStackTemplate output,
                                               final int duration,
                                               final int minTemperature,
                                               final int maxTemperature) {
        return new DistillationRecipeBuilder(input, null, output, duration, minTemperature, maxTemperature);
    }

    public static DistillationRecipeBuilder withReagent(final SizedFluidIngredient input,
                                                        final SizedFluidIngredient reagent,
                                                        final FluidStackTemplate output,
                                                        final int duration,
                                                        final int minTemperature,
                                                        final int maxTemperature) {
        return new DistillationRecipeBuilder(input, reagent, output, duration, minTemperature, maxTemperature);
    }

    @Override
    public void save(final RecipeOutput recipeOutput, final ResourceKey<Recipe<?>> resourceKey) {
        final DistillationRecipe recipe =
            new DistillationRecipe(this.input, Optional.ofNullable(this.reagent), this.output, this.duration, this.minTemperature, this.maxTemperature);
        recipeOutput.accept(resourceKey, recipe, null, this.conditions.toArray(new ICondition[0]));
    }
}
