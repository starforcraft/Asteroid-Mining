package com.ultramega.asteroidmining.utils.handlers;

import java.util.function.BooleanSupplier;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class TankRestrictedFluidHandler implements ResourceHandler<FluidResource> {
    private final MultiFluidStacksResourceHandler delegate;
    private final int tank;
    private final BooleanSupplier canInput;
    private final BooleanSupplier canOutput;

    public TankRestrictedFluidHandler(final MultiFluidStacksResourceHandler delegate,
                                      final int tank,
                                      final BooleanSupplier canInput,
                                      final BooleanSupplier canOutput) {
        this.delegate = delegate;
        this.tank = tank;
        this.canInput = canInput;
        this.canOutput = canOutput;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(final int index) {
        return index == 0 ? this.delegate.getResource(this.tank) : FluidResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(final int index) {
        return index == 0 ? this.delegate.getAmountAsLong(this.tank) : 0;
    }

    @Override
    public long getCapacityAsLong(final int index, final FluidResource resource) {
        return index == 0 ? this.delegate.getCapacityAsLong(this.tank, resource) : 0;
    }

    @Override
    public boolean isValid(final int index, final FluidResource resource) {
        return index == 0 && this.canInput.getAsBoolean()
            && this.delegate.isValid(this.tank, resource);
    }

    @Override
    public int insert(final int index, final FluidResource resource, final int amount, final TransactionContext tx) {
        if (index != 0 || !this.canInput.getAsBoolean()) {
            return 0;
        }
        return this.delegate.insert(this.tank, resource, amount, tx);
    }

    @Override
    public int insert(final FluidResource resource, final int amount, final TransactionContext tx) {
        if (!this.canInput.getAsBoolean()) {
            return 0;
        }
        return this.delegate.insert(this.tank, resource, amount, tx);
    }

    @Override
    public int extract(final int index, final FluidResource resource, final int amount, final TransactionContext tx) {
        if (index != 0 || !this.canOutput.getAsBoolean()) {
            return 0;
        }
        return this.delegate.extract(this.tank, resource, amount, tx);
    }

    @Override
    public int extract(final FluidResource resource, final int amount, final TransactionContext tx) {
        if (!this.canOutput.getAsBoolean()) {
            return 0;
        }
        return this.delegate.extract(this.tank, resource, amount, tx);
    }
}
