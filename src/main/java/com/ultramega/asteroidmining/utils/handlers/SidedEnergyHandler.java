package com.ultramega.asteroidmining.utils.handlers;

import java.util.function.BooleanSupplier;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class SidedEnergyHandler implements EnergyHandler {
    private final EnergyHandler delegate;
    private final BooleanSupplier canInput;
    private final BooleanSupplier canOutput;

    public SidedEnergyHandler(final EnergyHandler delegate, final BooleanSupplier canInput, final BooleanSupplier canOutput) {
        this.delegate = delegate;
        this.canInput = canInput;
        this.canOutput = canOutput;
    }

    @Override
    public long getAmountAsLong() {
        return this.delegate.getAmountAsLong();
    }

    @Override
    public long getCapacityAsLong() {
        return this.delegate.getCapacityAsLong();
    }

    @Override
    public int insert(final int amount, final TransactionContext transaction) {
        return this.canInput.getAsBoolean() ? this.delegate.insert(amount, transaction) : 0;
    }

    @Override
    public int extract(final int amount, final TransactionContext transaction) {
        return this.canOutput.getAsBoolean() ? this.delegate.extract(amount, transaction) : 0;
    }
}
