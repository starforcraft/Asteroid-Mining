package com.ultramega.asteroidmining.utils.handlers;

import java.util.function.BooleanSupplier;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class SidedResourceHandler<T extends Resource> implements ResourceHandler<T> {
    private final ResourceHandler<T> delegate;
    private final BooleanSupplier canInput;
    private final BooleanSupplier canOutput;

    public SidedResourceHandler(final ResourceHandler<T> delegate, final BooleanSupplier canInput, final BooleanSupplier canOutput) {
        this.delegate = delegate;
        this.canInput = canInput;
        this.canOutput = canOutput;
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public T getResource(final int index) {
        return this.delegate.getResource(index);
    }

    @Override
    public long getAmountAsLong(final int index) {
        return this.delegate.getAmountAsLong(index);
    }

    @Override
    public long getCapacityAsLong(final int index, final T resource) {
        return this.delegate.getCapacityAsLong(index, resource);
    }

    @Override
    public boolean isValid(final int index, final T resource) {
        return this.canInput.getAsBoolean() && this.delegate.isValid(index, resource);
    }

    @Override
    public int insert(final int index, final T resource, final int amount, final TransactionContext transaction) {
        return this.canInput.getAsBoolean() ? this.delegate.insert(index, resource, amount, transaction) : 0;
    }

    @Override
    public int insert(final T resource, final int amount, final TransactionContext transaction) {
        return this.canInput.getAsBoolean() ? this.delegate.insert(resource, amount, transaction) : 0;
    }

    @Override
    public int extract(final int index, final T resource, final int amount, final TransactionContext transaction) {
        return this.canOutput.getAsBoolean() ? this.delegate.extract(index, resource, amount, transaction) : 0;
    }

    @Override
    public int extract(final T resource, final int amount, final TransactionContext transaction) {
        return this.canOutput.getAsBoolean() ? this.delegate.extract(resource, amount, transaction) : 0;
    }
}
