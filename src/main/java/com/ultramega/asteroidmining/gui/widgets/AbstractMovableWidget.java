package com.ultramega.asteroidmining.gui.widgets;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.config.ClientConfig;
import com.ultramega.asteroidmining.utils.ClientUtils;

import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public abstract class AbstractMovableWidget extends AbstractWidget {
    private static final Identifier BACKGROUND = AsteroidMining.makeId("white_side_panel");

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
                                    final IntSupplier screenWidth,
                                    final IntSupplier screenHeight) {
        final int x = ClientConfig.getWidgetPosition(widgetType).map(ClientConfig.SavedPosition::x).orElse(defaultX);
        final int y = ClientConfig.getWidgetPosition(widgetType).map(ClientConfig.SavedPosition::y).orElse(defaultY);
        super(x, y, width, height, Component.empty());

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

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, this.getWidth(), this.getHeight());
        graphics.text(font, this.getTitle(), x + (this.getWidth() - font.width(this.getTitle())) / 2, y + 4, -12566464, false);

        this.extractMovableContents(graphics, mouseX, mouseY, partialTicks);
        this.extractTooltips(graphics, Minecraft.getInstance().font, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (!this.visible || !this.active || !this.isMouseOverWidgetArea(event.x(), event.y())) {
            return false;
        }

        final boolean click = this.mouseClickedInside(event, doubleClick);
        if (click) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }

        if (event.button() == 0 && ClientUtils.isMouseOver(this.getX(), this.getY(), this.getWidth(), this.getHeaderHeight(), event.x(), event.y())) {
            this.dragging = true;
            return true;
        }

        return false;
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

    protected boolean isMouseOverWidgetArea(final double mouseX, final double mouseY) {
        return this.isMouseOver(mouseX, mouseY);
    }

    protected void clampToScreen() {
        this.setX(Mth.clamp(this.getX(), 0, Math.max(0, this.screenWidth.getAsInt() - this.getWidth())));
        this.setY(Mth.clamp(this.getY(), 0, Math.max(0, this.screenHeight.getAsInt() - this.getHeight())));
    }

    protected Component getTitle() {
        return Component.empty();
    }

    public IntSupplier getScreenWidth() {
        return this.screenWidth;
    }

    public IntSupplier getScreenHeight() {
        return this.screenHeight;
    }

    protected int getHeaderHeight() {
        return HEADER_HEIGHT;
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    public List<Rect2i> getGuiExtraAreas() {
        if (!this.visible) {
            return List.of();
        }

        return List.of(new Rect2i(this.getX(), this.getY(), this.getWidth(), this.getHeight()));
    }

    protected abstract void extractMovableContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks);

    protected abstract void extractTooltips(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY);

    protected abstract boolean mouseClickedInside(MouseButtonEvent event, boolean doubleClick);
}
