package com.ultramega.asteroidmining.utils.handlers;

import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

public class MutableEnergy extends SimpleEnergyHandler {
    public MutableEnergy(final int capacity) {
        super(capacity);
    }

    public MutableEnergy(final int capacity, final int maxTransfer) {
        super(capacity, maxTransfer);
    }

    public MutableEnergy(final int capacity, final int maxReceive, final int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    public MutableEnergy(final int capacity, final int maxReceive, final int maxExtract, final int energy) {
        super(capacity, maxReceive, maxExtract, energy);
    }

    public void setCapacity(final int capacity) {
        this.capacity = capacity;
    }
}
