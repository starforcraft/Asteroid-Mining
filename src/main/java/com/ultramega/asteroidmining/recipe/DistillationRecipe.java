package com.ultramega.asteroidmining.recipe;

import com.ultramega.asteroidmining.registry.ModRecipeTypes;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import static com.ultramega.asteroidmining.AsteroidMining.MOD_ID;
import static com.ultramega.asteroidmining.blockentities.DistillationColumnBlockEntity.MAX_TEMPERATURE;
import static com.ultramega.asteroidmining.blockentities.DistillationColumnBlockEntity.MIN_TEMPERATURE;

public record DistillationRecipe(SizedFluidIngredient input,
                                 Optional<SizedFluidIngredient> reagent,
                                 FluidStackTemplate output,
                                 int duration,
                                 int minTemperature,
                                 int maxTemperature) implements Recipe<DistillationInput> {
    public static final MapCodec<DistillationRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        SizedFluidIngredient.CODEC.fieldOf("input").forGetter(DistillationRecipe::input),
        SizedFluidIngredient.CODEC.optionalFieldOf("reagent").forGetter(DistillationRecipe::reagent),
        FluidStackTemplate.CODEC.fieldOf("output").forGetter(DistillationRecipe::output),
        Codec.INT.fieldOf("duration").orElse(40).forGetter(DistillationRecipe::duration),
        ExtraCodecs.intRange(MIN_TEMPERATURE, MAX_TEMPERATURE - 1).fieldOf("min_temperature").orElse(MAX_TEMPERATURE).forGetter(DistillationRecipe::minTemperature),
        ExtraCodecs.intRange(MIN_TEMPERATURE + 1, MAX_TEMPERATURE).fieldOf("max_temperature").orElse(MIN_TEMPERATURE).forGetter(DistillationRecipe::maxTemperature)
    ).apply(inst, DistillationRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DistillationRecipe> STREAM_CODEC = StreamCodec.composite(
        SizedFluidIngredient.STREAM_CODEC, DistillationRecipe::input,
        ByteBufCodecs.optional(SizedFluidIngredient.STREAM_CODEC), DistillationRecipe::reagent,
        FluidStackTemplate.STREAM_CODEC, DistillationRecipe::output,
        ByteBufCodecs.INT, DistillationRecipe::duration,
        ByteBufCodecs.INT, DistillationRecipe::minTemperature,
        ByteBufCodecs.INT, DistillationRecipe::maxTemperature,
        DistillationRecipe::new
    );

    @Override
    public boolean matches(final DistillationInput input, final Level level) {
        if (input.temperature() < this.minTemperature || input.temperature() > this.maxTemperature) {
            return false;
        }

        if (!this.input.test(input.input())) {
            return false;
        }

        return this.reagent
            .map(reagent -> reagent.test(input.reagent()))
            .orElse(true);
    }

    @Override
    public ItemStack assemble(final DistillationInput input) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<? extends Recipe<DistillationInput>> getSerializer() {
        return ModRecipeTypes.DISTILLATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<DistillationInput>> getType() {
        return ModRecipeTypes.DISTILLATION.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return null;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return MOD_ID + "_distillation";
    }
}
