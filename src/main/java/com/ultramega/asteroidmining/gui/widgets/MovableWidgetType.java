package com.ultramega.asteroidmining.gui.widgets;

public enum MovableWidgetType {
    SIDE_CONFIG("side_config");

    private final String savePath;

    MovableWidgetType(final String savePath) {
        this.savePath = savePath;
    }

    public String savePath() {
        return this.savePath;
    }
}
