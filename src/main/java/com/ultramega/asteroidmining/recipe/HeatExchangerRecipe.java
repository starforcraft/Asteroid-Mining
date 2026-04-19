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

public record HeatExchangerRecipe(SizedFluidIngredient input, SizedFluidIngredient output, int duration) implements Recipe<HeatExchangerInput> {
    public static final MapCodec<HeatExchangerRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        SizedFluidIngredient.CODEC.fieldOf("input").forGetter(HeatExchangerRecipe::input),
        SizedFluidIngredient.CODEC.fieldOf("output").forGetter(HeatExchangerRecipe::output),
        Codec.INT.fieldOf("duration").orElse(20).forGetter(HeatExchangerRecipe::duration)
    ).apply(inst, HeatExchangerRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, HeatExchangerRecipe> STREAM_CODEC = StreamCodec.composite(
        SizedFluidIngredient.STREAM_CODEC, HeatExchangerRecipe::input,
        SizedFluidIngredient.STREAM_CODEC, HeatExchangerRecipe::output,
        ByteBufCodecs.INT, HeatExchangerRecipe::duration,
        HeatExchangerRecipe::new
    );

    @Override
    public boolean matches(final HeatExchangerInput heatExchangerInput, final Level level) {
        return this.input.ingredient().fluids().getFirst().value().isSame(heatExchangerInput.input().getFluid());
    }

    @Override
    public ItemStack assemble(final HeatExchangerInput heatExchangerInput) {
        return ItemStack.EMPTY;
    }

    public FluidStack getInputFluid() {
        return new FluidStack(this.input.ingredient().fluids().getFirst().value(), this.input.amount());
    }

    public FluidStack getOutputFluid() {
        return new FluidStack(this.output.ingredient().fluids().getFirst().value(), this.output.amount());
    }

    @Override
    public RecipeSerializer<? extends Recipe<HeatExchangerInput>> getSerializer() {
        return ModRecipeTypes.HEAT_EXCHANGER_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<HeatExchangerInput>> getType() {
        return ModRecipeTypes.HEAT_EXCHANGER.get();
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
        return MOD_ID + "_heat_exchanger";
    }
}
