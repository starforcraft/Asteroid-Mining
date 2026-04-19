package com.ultramega.asteroidmining.registry;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.recipe.HeatExchangerRecipe;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, AsteroidMining.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, AsteroidMining.MOD_ID);

    public static final Supplier<RecipeType<HeatExchangerRecipe>> HEAT_EXCHANGER = RECIPE_TYPES.register(
        "heat_exchanger",
        () -> RecipeType.simple(Identifier.fromNamespaceAndPath(AsteroidMining.MOD_ID, "heat_exchanger")));

    public static final Supplier<RecipeSerializer<HeatExchangerRecipe>> HEAT_EXCHANGER_SERIALIZER =
        RECIPE_SERIALIZERS.register("heat_exchanger", () -> new RecipeSerializer<>(HeatExchangerRecipe.CODEC, HeatExchangerRecipe.STREAM_CODEC));

    private ModRecipeTypes() {
    }
}
