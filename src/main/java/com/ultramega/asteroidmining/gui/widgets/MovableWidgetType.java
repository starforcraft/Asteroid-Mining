package com.ultramega.asteroidmining.gui.widgets;

public enum MovableWidgetType {
    SIDE_CONFIG("side_config"),
    ROCKET_VIEWER("rocket_viewer"),
    ROCKET_CONFIGURATION("rocket_configuration"),
    SPACE_PORT_ERRORS("space_port_errors");

    private final String savePath;

    MovableWidgetType(final String savePath) {
        this.savePath = savePath;
    }

    public String savePath() {
        return this.savePath;
    }
}
