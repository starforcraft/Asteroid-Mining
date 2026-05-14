package com.ultramega.asteroidmining.utils.sides;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import static com.ultramega.asteroidmining.AsteroidMining.makeId;

public enum SideConfigType {
    ENERGY("energy",
        Component.translatable("gui.asteroidmining.side_io_mode.energy_config"),
        Component.translatable("tooltip.asteroidmining.energy"),
        0xFFE53935,
        0xFFFF7043),
    ITEMS("items",
        Component.translatable("gui.asteroidmining.side_io_mode.items_config"),
        Component.translatable("tooltip.asteroidmining.items"),
        0xFFFFB300,
        0xFFFFD54F),
    FLUIDS("fluids",
        Component.translatable("gui.asteroidmining.side_io_mode.fluids_config"),
        Component.translatable("tooltip.asteroidmining.fluids"),
        0xFF1E88E5,
        0xFF64B5F6),
    GASES("gases",
        Component.translatable("gui.asteroidmining.side_io_mode.gas_config"),
        Component.translatable("tooltip.asteroidmining.gases"),
        0xFF1E88E5,
        0xFF64B5F6);

    private final Component title;
    private final Identifier tabIcon;
    private final Component tooltip;
    private final int defaultColor;
    private final int selectedColor;

    SideConfigType(final String id, final Component title, final Component tooltip, final int defaultColor, final int selectedColor) {
        this.title = title;
        this.tabIcon = makeId(id);
        this.tooltip = tooltip;
        this.defaultColor = defaultColor;
        this.selectedColor = selectedColor;
    }

    public Component getTitle() {
        return this.title;
    }

    public Identifier getTabIcon() {
        return this.tabIcon;
    }

    public Component getTooltip() {
        return this.tooltip;
    }

    public int getDefaultColor() {
        return this.defaultColor;
    }

    public int getSelectedColor() {
        return this.selectedColor;
    }
}
