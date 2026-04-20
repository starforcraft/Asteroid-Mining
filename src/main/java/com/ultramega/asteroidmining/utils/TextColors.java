package com.ultramega.asteroidmining.utils;

public enum TextColors { // ARGB
    RED(0xFFFF0000),
    GREEN(0xFF00FF00),
    YELLOW(0xFFFFFF00),
    GOLD(0xFFFFD700),
    WHITE(0xFFFFFFFF),
    BLACK(0xFF000000);

    private final int hexCode;

    TextColors(final int hexCode) {
        this.hexCode = hexCode;
    }

    public int getHexCode() {
        return this.hexCode;
    }
}
