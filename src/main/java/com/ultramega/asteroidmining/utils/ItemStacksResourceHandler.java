package com.ultramega.asteroidmining.utils;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public class ItemStacksResourceHandler extends net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler {
    public ItemStacksResourceHandler(final int size) {
        super(size);
    }

    public ItemStacksResourceHandler(final NonNullList<ItemStack> stacks) {
        super(stacks);
    }

    public ItemStack getStack(final int index) {
        return super.getStackFrom(this.getResource(index), this.getAmountAsInt(index));
    }
}
