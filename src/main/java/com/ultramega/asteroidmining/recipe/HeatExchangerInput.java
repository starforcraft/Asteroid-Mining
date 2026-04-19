package com.ultramega.asteroidmining.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

public record HeatExchangerInput(FluidStack input) implements RecipeInput {
    @Override
    public ItemStack getItem(final int slot) {
        return Items.AIR.getDefaultInstance();
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.input.isEmpty();
    }
}
