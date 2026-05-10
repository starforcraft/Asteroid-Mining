package com.ultramega.asteroidmining.gui.widgets;

import com.ultramega.asteroidmining.config.ClientConfig;
import com.ultramega.asteroidmining.utils.ClientUtils;

import java.util.function.IntSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public abstract class AbstractMovableWidget extends AbstractWidget {
    private static final int HEADER_HEIGHT = 13;

    private final MovableWidgetType widgetType;
    private final IntSupplier screenWidth;
    private final IntSupplier screenHeight;

    private boolean dragging;
    private int lastSavedX;
    private int lastSavedY;

    protected AbstractMovableWidget(final MovableWidgetType widgetType,
                                    final int defaultX,
                                    final int defaultY,
                                    final int width,
                                    final int height,
                                    final Component title,
                                    final IntSupplier screenWidth,
                                    final IntSupplier screenHeight) {
        final int x = ClientConfig.getWidgetPosition(widgetType).map(ClientConfig.SavedPosition::x).orElse(defaultX);
        final int y = ClientConfig.getWidgetPosition(widgetType).map(ClientConfig.SavedPosition::y).orElse(defaultY);
        super(x, y, width, height, title);

        this.widgetType = widgetType;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        this.clampToScreen();

        this.lastSavedX = this.getX();
        this.lastSavedY = this.getY();
    }

    @Override
    protected final void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        final int x = this.getX();
        final int y = this.getY();
        final Font font = Minecraft.getInstance().font;

        graphics.fill(x, y, x + this.getWidth(), y + this.getHeight(), 0xDD101010);
        graphics.fill(x, y, x + this.getWidth(), y + HEADER_HEIGHT, 0xEE303030);
        graphics.outline(x, y, this.getWidth(), this.getHeight(), this.isHovered() ? 0xFFFFFFFF : 0xFF707070);

        graphics.text(font, this.getMessage(), x + 4, y + 3, 0xFFFFFFFF, false);

        this.extractMovableContents(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (!this.visible || !this.active || !this.isMouseOver(event.x(), event.y())) {
            return false;
        }

        if (event.button() == 0 && ClientUtils.isMouseOver(this.getX(), this.getY(), this.getWidth(), HEADER_HEIGHT, event.x(), event.y())) {
            this.dragging = true;
            return true;
        }

        final boolean click = this.mouseClickedInside(event, doubleClick);
        if (click) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
        }
        return click;
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy) {
        if (!this.dragging) {
            return false;
        }

        this.setX(this.getX() + (int) Math.round(dx));
        this.setY(this.getY() + (int) Math.round(dy));
        this.clampToScreen();
        return true;
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        if (this.dragging) {
            this.dragging = false;
            this.savePosition();
            return true;
        }

        return false;
    }

    public final void savePosition() {
        if (this.getX() == this.lastSavedX && this.getY() == this.lastSavedY) {
            return;
        }

        this.lastSavedX = this.getX();
        this.lastSavedY = this.getY();

        ClientConfig.setWidgetPosition(this.widgetType, this.getX(), this.getY());
    }

    private void clampToScreen() {
        this.setX(Mth.clamp(this.getX(), 0, Math.max(0, this.screenWidth.getAsInt() - this.getWidth())));
        this.setY(Mth.clamp(this.getY(), 0, Math.max(0, this.screenHeight.getAsInt() - this.getHeight())));
    }

    protected int headerHeight() {
        return HEADER_HEIGHT;
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    protected abstract void extractMovableContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks);

    protected abstract boolean mouseClickedInside(MouseButtonEvent event, boolean doubleClick);
}
