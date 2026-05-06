package com.ultramega.asteroidmining.utils;

import org.jspecify.annotations.Nullable;

public final class CoreValidations { //TODO: delete this class?
    private CoreValidations() {
    }

    public static <T> void validateNotNull(@Nullable final T value, final String message) {
        if (value == null) {
            throw new NullPointerException(message);
        }
    }

    public static <T> void validateNull(@Nullable final T value, final String message) {
        if (value != null) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void validateTrue(final boolean value, final String message) {
        if (!value) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void validateFalse(final boolean value, final String message) {
        if (value) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void validateLargerThanZero(final long value, final String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
