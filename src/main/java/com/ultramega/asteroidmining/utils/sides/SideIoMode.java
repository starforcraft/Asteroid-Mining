package com.ultramega.asteroidmining.utils.sides;

public enum SideIoMode {
    NONE(false, false, "—"), //TODO
    INPUT(true, false, "IN"),
    OUTPUT(false, true, "OUT"),
    BOTH(true, true, "I/O");

    private final boolean input;
    private final boolean output;
    private final String label;

    SideIoMode(final boolean input, final boolean output, final String label) {
        this.input = input;
        this.output = output;
        this.label = label;
    }

    public boolean canInput() {
        return this.input;
    }

    public boolean canOutput() {
        return this.output;
    }

    public String label() {
        return this.label;
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
