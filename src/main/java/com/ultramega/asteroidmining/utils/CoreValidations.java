package com.ultramega.asteroidmining.utils;

import org.jspecify.annotations.Nullable;

public final class CoreValidations { //TODO: delete this class?
    private CoreValidations() {
    }

    public static <T> T validateNotNull(@Nullable final T value, final String message) {
        if (value == null) {
            throw new NullPointerException(message);
        }
        return value;
    }
}
