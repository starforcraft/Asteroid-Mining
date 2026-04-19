package com.ultramega.asteroidmining.datagen.builder;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.advancements.Criterion;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jspecify.annotations.Nullable;

public abstract class SimpleRecipeBuilder implements RecipeBuilder {
    protected final List<ICondition> conditions = new ArrayList<>();
    protected final ItemStack result;

    public SimpleRecipeBuilder(final ItemStack result) {
        this.result = result;
    }

    public SimpleRecipeBuilder addCondition(final ICondition condition) {
        this.conditions.add(condition);
        return this;
    }

    @Override
    public RecipeBuilder unlockedBy(final String s, final Criterion<?> criterion) {
        return this;
    }

    @Override
    public RecipeBuilder group(final @Nullable String s) {
        return this;
    }

    @Override
    public ResourceKey<Recipe<?>> defaultId() {
        return RecipeBuilder.getDefaultRecipeId(this.result);
    }
}
