package com.ultramega.asteroidmining.config;

import com.ultramega.asteroidmining.gui.widgets.MovableWidgetType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;

    public static final Map<MovableWidgetType, WindowPosition> LAST_WIDGET_POSITIONS = new EnumMap<>(MovableWidgetType.class);

    private static final int UNSET = Integer.MAX_VALUE;

    static {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Last positions of movable GUI widgets.").push("lastWidgetPositions");

        for (final MovableWidgetType type : MovableWidgetType.values()) {
            builder.push(type.savePath());

            final ModConfigSpec.IntValue x = builder
                .comment("Last X position for this movable widget.")
                .defineInRange("x", UNSET, Integer.MIN_VALUE, Integer.MAX_VALUE);

            final ModConfigSpec.IntValue y = builder
                .comment("Last Y position for this movable widget.")
                .defineInRange("y", UNSET, Integer.MIN_VALUE, Integer.MAX_VALUE);

            LAST_WIDGET_POSITIONS.put(type, new WindowPosition(x, y));

            builder.pop();
        }

        builder.pop();

        SPEC = builder.build();
    }

    private ClientConfig() {
    }

    public record SavedPosition(int x, int y) {
    }

    public record WindowPosition(ModConfigSpec.IntValue x, ModConfigSpec.IntValue y) {
        public Optional<SavedPosition> get() {
            final int savedX = this.x.get();
            final int savedY = this.y.get();

            if (savedX == UNSET || savedY == UNSET) {
                return Optional.empty();
            }

            return Optional.of(new SavedPosition(savedX, savedY));
        }

        public void set(final int x, final int y) {
            this.x.set(x);
            this.y.set(y);

            if (ClientConfig.SPEC.isLoaded()) {
                ClientConfig.SPEC.save();
            }
        }
    }

    public static Optional<SavedPosition> getWidgetPosition(final MovableWidgetType type) {
        final WindowPosition position = LAST_WIDGET_POSITIONS.get(type);
        return position == null ? Optional.empty() : position.get();
    }

    public static void setWidgetPosition(final MovableWidgetType type, final int x, final int y) {
        final WindowPosition position = LAST_WIDGET_POSITIONS.get(type);
        if (position != null) {
            position.set(x, y);
        }
    }
}
