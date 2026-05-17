package com.ultramega.asteroidmining.utils.sides;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum SideIoMode {
    NONE(false,
        false,
        Component.literal("—"),
        Component.translatable("gui.asteroidmining.side_io_mode.none"),
        0xFF303030),
    INPUT(true,
        false,
        Component.translatable("gui.asteroidmining.side_io_mode.input.short"),
        Component.translatable("gui.asteroidmining.side_io_mode.input"),
        0xFF1E5A8A),
    OUTPUT(false,
        true,
        Component.translatable("gui.asteroidmining.side_io_mode.output.short"),
        Component.translatable("gui.asteroidmining.side_io_mode.output"),
        0xFF8A4A1E),
    BOTH(true,
        true,
        Component.translatable("gui.asteroidmining.side_io_mode.input_output.short"),
        Component.translatable("gui.asteroidmining.side_io_mode.input_output"),
        0xFF3E7A3E);

    private final boolean input;
    private final boolean output;
    private final Component abbreviation;
    private final MutableComponent name;
    private final int color;

    SideIoMode(final boolean input, final boolean output, final Component abbreviation, final MutableComponent name, final int color) {
        this.input = input;
        this.output = output;
        this.abbreviation = abbreviation;
        this.name = name.withColor(color);
        this.color = color;
    }

    public boolean canInput() {
        return this.input;
    }

    public boolean canOutput() {
        return this.output;
    }

    public Component getAbbreviation() {
        return this.abbreviation;
    }

    public Component getName() {
        return this.name;
    }

    public int getColor() {
        return this.color;
    }

    public SideIoMode next() {
        return values()[(this.ordinal() + 1) % values().length];
    }

    public SideIoMode previous() {
        return values()[(this.ordinal() + values().length - 1) % values().length];
    }

    public static SideIoMode byId(final int id) {
        final SideIoMode[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }
}
