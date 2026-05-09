package com.ultramega.asteroidmining.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

public record DistillationInput(FluidStack input, FluidStack reagent, int temperature) implements RecipeInput {
    @Override
    public ItemStack getItem(final int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return this.input.isEmpty() && this.reagent.isEmpty();
    }
}
