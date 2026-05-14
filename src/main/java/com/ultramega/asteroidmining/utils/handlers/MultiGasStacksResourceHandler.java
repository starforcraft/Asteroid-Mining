package com.ultramega.asteroidmining.utils.handlers;

import net.neoforged.neoforge.transfer.fluid.FluidResource;

// TODO: decide if we want to create and switch to our own gas registry. Though this will inevitably result in custom pipes being needed
public class MultiGasStacksResourceHandler extends MultiFluidStacksResourceHandler {
    public MultiGasStacksResourceHandler(final int[] capacity) {
        super(capacity);
        this.capacity = capacity;
    }

    @Override
    public boolean isValid(final int index, final FluidResource resource) {
        return !super.isValid(index, resource);
    }
}
