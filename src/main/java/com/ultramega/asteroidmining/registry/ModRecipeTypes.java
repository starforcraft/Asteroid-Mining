package com.ultramega.asteroidmining.registry;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.recipe.DistillationRecipe;
import com.ultramega.asteroidmining.recipe.HeatExchangeRecipe;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.asteroidmining.AsteroidMining.makeId;

public final class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, AsteroidMining.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, AsteroidMining.MOD_ID);

    public static final Supplier<RecipeType<HeatExchangeRecipe>> HEAT_EXCHANGE = RECIPE_TYPES.register("heat_exchange", () ->
        RecipeType.simple(makeId("heat_exchange")));

    public static final Supplier<RecipeType<DistillationRecipe>> DISTILLATION = RECIPE_TYPES.register("distillation", () ->
        RecipeType.simple(makeId("distillation")));

    public static final Supplier<RecipeSerializer<HeatExchangeRecipe>> HEAT_EXCHANGE_SERIALIZER = RECIPE_SERIALIZERS.register("heat_exchange", () ->
        new RecipeSerializer<>(HeatExchangeRecipe.CODEC, HeatExchangeRecipe.STREAM_CODEC));

    public static final Supplier<RecipeSerializer<DistillationRecipe>> DISTILLATION_SERIALIZER = RECIPE_SERIALIZERS.register("distillation", () ->
        new RecipeSerializer<>(DistillationRecipe.CODEC, DistillationRecipe.STREAM_CODEC));

    private ModRecipeTypes() {
    }
}
