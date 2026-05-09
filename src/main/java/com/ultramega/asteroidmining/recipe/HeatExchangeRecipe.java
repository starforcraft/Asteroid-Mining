package com.ultramega.asteroidmining.recipe;

import com.ultramega.asteroidmining.registry.ModRecipeTypes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import static com.ultramega.asteroidmining.AsteroidMining.MOD_ID;

public record HeatExchangeRecipe(SizedFluidIngredient input, SizedFluidIngredient output, int duration) implements Recipe<HeatExchangeInput> {
    public static final MapCodec<HeatExchangeRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        SizedFluidIngredient.CODEC.fieldOf("input").forGetter(HeatExchangeRecipe::input),
        SizedFluidIngredient.CODEC.fieldOf("output").forGetter(HeatExchangeRecipe::output),
        Codec.INT.fieldOf("duration").orElse(20).forGetter(HeatExchangeRecipe::duration)
    ).apply(inst, HeatExchangeRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, HeatExchangeRecipe> STREAM_CODEC = StreamCodec.composite(
        SizedFluidIngredient.STREAM_CODEC, HeatExchangeRecipe::input,
        SizedFluidIngredient.STREAM_CODEC, HeatExchangeRecipe::output,
        ByteBufCodecs.INT, HeatExchangeRecipe::duration,
        HeatExchangeRecipe::new
    );

    @Override
    public boolean matches(final HeatExchangeInput input, final Level level) {
        return this.input.ingredient().fluids().getFirst().value().isSame(input.input().getFluid());
    }

    @Override
    public ItemStack assemble(final HeatExchangeInput input) {
        return ItemStack.EMPTY;
    }

    public FluidStack getInputFluid() {
        return new FluidStack(this.input.ingredient().fluids().getFirst().value(), this.input.amount());
    }

    public FluidStack getOutputFluid() {
        return new FluidStack(this.output.ingredient().fluids().getFirst().value(), this.output.amount());
    }

    @Override
    public RecipeSerializer<? extends Recipe<HeatExchangeInput>> getSerializer() {
        return ModRecipeTypes.HEAT_EXCHANGE_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<HeatExchangeInput>> getType() {
        return ModRecipeTypes.HEAT_EXCHANGE.get();
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
        return MOD_ID + "_heat_exchange";
    }
}
