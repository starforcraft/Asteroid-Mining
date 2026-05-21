package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.gui.widgets.AbstractMovableWidget;
import com.ultramega.asteroidmining.utils.ClientUtils;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;

public abstract class AbstractMovableWidgetContainerScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    private final List<AbstractMovableWidget> movableWidgets = new ArrayList<>();

    protected AbstractMovableWidgetContainerScreen(final M menu,
                                                   final Inventory inventory,
                                                   final Component title,
                                                   final int imageWidth,
                                                   final int imageHeight) {
        super(menu, inventory, title, imageWidth, imageHeight);
    }

    @Override
    protected void init() {
        super.init();
        this.movableWidgets.clear();
    }

    protected <T extends AbstractMovableWidget> T addTopLayerWidget(final T widget) {
        this.movableWidgets.add(widget);
        return this.addWidget(widget);
    }

    @Override
    public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractContents(graphics, mouseX, mouseY, partialTicks);

        for (final AbstractMovableWidget widget : this.movableWidgets) {
            if (widget.visible) {
                widget.extractRenderState(graphics, mouseX, mouseY, partialTicks);
            }
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        for (int i = this.movableWidgets.size() - 1; i >= 0; --i) {
            if (this.movableWidgets.get(i).mouseClicked(event, doubleClick)) {
                this.setFocused(this.movableWidgets.get(i));
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        for (int i = this.movableWidgets.size() - 1; i >= 0; --i) {
            if (this.movableWidgets.get(i).mouseReleased(event)) {
                return true;
            }
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        for (int i = this.movableWidgets.size() - 1; i >= 0; --i) {
            if (this.movableWidgets.get(i).mouseDragged(event, dragX, dragY)) {
                return true;
            }
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        for (int i = this.movableWidgets.size() - 1; i >= 0; --i) {
            if (this.movableWidgets.get(i).mouseScrolled(x, y, scrollX, scrollY)) {
                return true;
            }
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    protected final void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        if (this.isMouseOverMovableWidget(mouseX, mouseY)) {
            return;
        }

        super.extractTooltip(graphics, mouseX, mouseY);
        this.drawTooltip(graphics, mouseX, mouseY);
    }

    @Nullable
    @Override
    public Slot getHoveredSlot(final double x, final double y) {
        if (this.isMouseOverMovableWidget(x, y)) {
            return null;
        }
        return super.getHoveredSlot(x, y);
    }

    protected final boolean isMouseOverMovableWidget(final double mouseX, final double mouseY) {
        for (final AbstractMovableWidget widget : this.movableWidgets) {
            if (widget.visible && ClientUtils.isMouseOver(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), mouseX, mouseY)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void removed() {
        for (final AbstractMovableWidget widget : this.movableWidgets) {
            widget.savePosition();
        }

        super.removed();
    }

    protected abstract void drawTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY);
}
