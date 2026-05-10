package com.ultramega.asteroidmining.utils.handlers;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

public class MultiFluidStacksResourceHandler extends FluidStacksResourceHandler {
    protected int[] capacity;

    public MultiFluidStacksResourceHandler(final int size, final int[] capacity) {
        super(size, 0);
        this.capacity = capacity;
    }

    public FluidStack getStackInTank(final int index) {
        return this.getStackFrom(this.getResource(index), this.getAmountAsInt(index));
    }

    public int getRemainingSpace(final int index) {
        return Math.max(0, this.getCapacityAsInt(index) - this.getAmountAsInt(index));
    }

    public int getCapacityAsInt(final int index) {
        return this.getCapacityAsInt(index, this.getResource(index));
    }

    @Override
    protected int getCapacity(final int index, final FluidResource resource) {
        return this.capacity[index];
    }
}
