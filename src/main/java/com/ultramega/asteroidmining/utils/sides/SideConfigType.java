package com.ultramega.asteroidmining.utils.sides;

import java.util.Locale;

import net.minecraft.resources.Identifier;

import static com.ultramega.asteroidmining.AsteroidMining.makeId;

public enum SideConfigType {
    ENERGY("Energy"),
    ITEMS("Items"),
    FLUIDS("Fluids");

    private final String title;

    SideConfigType(final String title) {
        this.title = title;
    }

    public Identifier tabIcon() {
        return makeId(this.title.toLowerCase(Locale.ROOT));
    }

    public String title() {
        return this.title;
    }
}
