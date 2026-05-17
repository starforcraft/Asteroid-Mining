package com.ultramega.asteroidmining.config;

import com.ultramega.asteroidmining.gui.widgets.MovableWidgetType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.util.Mth;
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

            final ModConfigSpec.EnumValue<HorizontalAnchor> horizontalAnchor = builder
                .comment("Horizontal anchor for this movable widget.")
                .defineEnum("horizontalAnchor", HorizontalAnchor.LEFT);

            final ModConfigSpec.EnumValue<VerticalAnchor> verticalAnchor = builder
                .comment("Vertical anchor for this movable widget.")
                .defineEnum("verticalAnchor", VerticalAnchor.TOP);

            final ModConfigSpec.IntValue offsetX = builder
                .comment("X offset from the horizontal anchor.")
                .defineInRange("offsetX", UNSET, Integer.MIN_VALUE, Integer.MAX_VALUE);

            final ModConfigSpec.IntValue offsetY = builder
                .comment("Y offset from the vertical anchor.")
                .defineInRange("offsetY", UNSET, Integer.MIN_VALUE, Integer.MAX_VALUE);

            LAST_WIDGET_POSITIONS.put(type, new WindowPosition(horizontalAnchor, verticalAnchor, offsetX, offsetY));

            builder.pop();
        }

        builder.pop();

        SPEC = builder.build();
    }

    private ClientConfig() {
    }

    public static Optional<SavedPosition> getWidgetPosition(final MovableWidgetType type,
                                                            final int screenWidth,
                                                            final int screenHeight,
                                                            final int widgetWidth,
                                                            final int widgetHeight) {
        final WindowPosition position = LAST_WIDGET_POSITIONS.get(type);
        return position == null
            ? Optional.empty()
            : position.get(screenWidth, screenHeight, widgetWidth, widgetHeight);
    }

    public static void setWidgetPosition(final MovableWidgetType type,
                                         final int x,
                                         final int y,
                                         final int screenWidth,
                                         final int screenHeight,
                                         final int widgetWidth,
                                         final int widgetHeight) {
        final WindowPosition position = LAST_WIDGET_POSITIONS.get(type);
        if (position != null) {
            position.set(x, y, screenWidth, screenHeight, widgetWidth, widgetHeight);
        }
    }

    public enum HorizontalAnchor {
        LEFT,
        CENTER,
        RIGHT;

        private int getBaseX(final int screenWidth, final int widgetWidth) {
            final int maxX = Math.max(0, screenWidth - widgetWidth);

            return switch (this) {
                case LEFT -> 0;
                case CENTER -> maxX / 2;
                case RIGHT -> maxX;
            };
        }

        private static HorizontalAnchor closestTo(final int x, final int screenWidth, final int widgetWidth) {
            HorizontalAnchor closest = LEFT;
            int closestDistance = Integer.MAX_VALUE;

            for (final HorizontalAnchor anchor : values()) {
                final int distance = Math.abs(x - anchor.getBaseX(screenWidth, widgetWidth));
                if (distance < closestDistance) {
                    closest = anchor;
                    closestDistance = distance;
                }
            }

            return closest;
        }
    }

    public enum VerticalAnchor {
        TOP,
        CENTER,
        BOTTOM;

        private int getBaseY(final int screenHeight, final int widgetHeight) {
            final int maxY = Math.max(0, screenHeight - widgetHeight);

            return switch (this) {
                case TOP -> 0;
                case CENTER -> maxY / 2;
                case BOTTOM -> maxY;
            };
        }

        private static VerticalAnchor closestTo(final int y, final int screenHeight, final int widgetHeight) {
            VerticalAnchor closest = TOP;
            int closestDistance = Integer.MAX_VALUE;

            for (final VerticalAnchor anchor : values()) {
                final int distance = Math.abs(y - anchor.getBaseY(screenHeight, widgetHeight));
                if (distance < closestDistance) {
                    closest = anchor;
                    closestDistance = distance;
                }
            }

            return closest;
        }
    }

    public record SavedPosition(int x, int y) {
    }

    public record WindowPosition(ModConfigSpec.EnumValue<HorizontalAnchor> horizontalAnchor,
                                 ModConfigSpec.EnumValue<VerticalAnchor> verticalAnchor,
                                 ModConfigSpec.IntValue offsetX,
                                 ModConfigSpec.IntValue offsetY) {
        public Optional<SavedPosition> get(final int screenWidth,
                                           final int screenHeight,
                                           final int widgetWidth,
                                           final int widgetHeight) {
            final int savedOffsetX = this.offsetX.get();
            final int savedOffsetY = this.offsetY.get();

            if (savedOffsetX == UNSET || savedOffsetY == UNSET) {
                return Optional.empty();
            }

            final int maxX = Math.max(0, screenWidth - widgetWidth);
            final int maxY = Math.max(0, screenHeight - widgetHeight);

            final int x = Mth.clamp(this.horizontalAnchor.get().getBaseX(screenWidth, widgetWidth) + savedOffsetX, 0, maxX);
            final int y = Mth.clamp(this.verticalAnchor.get().getBaseY(screenHeight, widgetHeight) + savedOffsetY, 0, maxY);

            return Optional.of(new SavedPosition(x, y));
        }

        public void set(final int x,
                        final int y,
                        final int screenWidth,
                        final int screenHeight,
                        final int widgetWidth,
                        final int widgetHeight) {
            final int maxX = Math.max(0, screenWidth - widgetWidth);
            final int maxY = Math.max(0, screenHeight - widgetHeight);

            final int clampedX = Mth.clamp(x, 0, maxX);
            final int clampedY = Mth.clamp(y, 0, maxY);

            final HorizontalAnchor newHorizontalAnchor = HorizontalAnchor.closestTo(clampedX, screenWidth, widgetWidth);
            final VerticalAnchor newVerticalAnchor = VerticalAnchor.closestTo(clampedY, screenHeight, widgetHeight);

            this.horizontalAnchor.set(newHorizontalAnchor);
            this.verticalAnchor.set(newVerticalAnchor);

            this.offsetX.set(clampedX - newHorizontalAnchor.getBaseX(screenWidth, widgetWidth));
            this.offsetY.set(clampedY - newVerticalAnchor.getBaseY(screenHeight, widgetHeight));

            if (ClientConfig.SPEC.isLoaded()) {
                ClientConfig.SPEC.save();
            }
        }
    }
}
