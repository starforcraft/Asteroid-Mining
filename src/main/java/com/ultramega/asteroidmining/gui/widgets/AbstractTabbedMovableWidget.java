package com.ultramega.asteroidmining.gui.widgets;

import com.ultramega.asteroidmining.gui.PagedSideTabs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.util.Mth;

public abstract class AbstractTabbedMovableWidget<T> extends AbstractMovableWidget {
    private final PagedSideTabs<T> tabs;

    protected AbstractTabbedMovableWidget(final MovableWidgetType widgetType,
                                          final int defaultX,
                                          final int defaultY,
                                          final int width,
                                          final int height,
                                          final IntSupplier screenWidth,
                                          final IntSupplier screenHeight) {
        super(widgetType, defaultX, defaultY, width, height, screenWidth, screenHeight);
        this.tabs = new PagedSideTabs<>(height / PagedSideTabs.TAB_HEIGHT);
    }

    protected final PagedSideTabs<T> tabs() {
        return this.tabs;
    }

    @Override
    protected final void extractMovableContents(final GuiGraphicsExtractor graphics,
                                                final int mouseX,
                                                final int mouseY,
                                                final float partialTicks) {
        final List<T> items = this.getTabItems();
        this.tabs.renderTabs(graphics, items, this.getTabX(), this.getTabY(), mouseX, mouseY, this::renderTab);

        this.extractTabbedMovableContents(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected final boolean mouseClickedInside(final MouseButtonEvent event, final boolean doubleClick) {
        final List<T> items = this.getTabItems();
        if (this.tabs.mouseClickedTab(event, items, this.getTabX(), this.getTabY(), this::onTabClicked)) {
            return true;
        }

        return this.mouseClickedInsideTabbed(event, doubleClick);
    }

    @Override
    protected boolean isMouseOverWidgetArea(final double mouseX, final double mouseY) {
        final List<T> items = this.getTabItems();
        return super.isMouseOverWidgetArea(mouseX, mouseY) || this.tabs.isMouseOverTabs(items.size(), this.getTabX(), this.getTabY(), mouseX, mouseY);
    }

    @Override
    protected void clampToScreen() {
        final int minX = this.getX() - this.getTabX();
        final int minY = this.getY() - this.getTabY();

        final int maxX = Math.max(minX, this.getScreenWidth().getAsInt() - this.getWidth());
        final int maxY = Math.max(minY, this.getScreenHeight().getAsInt() - this.getHeight());

        this.setX(Mth.clamp(this.getX(), minX, maxX));
        this.setY(Mth.clamp(this.getY(), minY, maxY));
    }

    protected int getTabX() {
        return this.getX() - 28;
    }

    protected int getTabY() {
        return this.getY();
    }

    @Override
    public List<Rect2i> getGuiExtraAreas() {
        if (!this.visible) {
            return List.of();
        }

        final List<Rect2i> areas = new ArrayList<>(super.getGuiExtraAreas());

        final List<T> items = this.getTabItems();
        final int itemCount = items.size();
        if (itemCount == 0) {
            return areas;
        }

        final int startIndex = this.tabs.getCurrentPage() * this.tabs.getMaxShownTabs();
        final int visibleTabs = Math.min(this.tabs.getMaxShownTabs(), itemCount - startIndex);
        if (visibleTabs > 0) {
            areas.add(new Rect2i(this.getTabX(), this.getTabY(), PagedSideTabs.TAB_WIDTH, visibleTabs * PagedSideTabs.TAB_HEIGHT));
        }

        return areas;
    }

    protected void renderTab(final GuiGraphicsExtractor graphics, final T item, final int x, final int y, final int mouseX, final int mouseY, final boolean hovered) {
    }

    protected abstract List<T> getTabItems();

    protected abstract void onTabClicked(int index, T item);

    protected abstract void extractTabbedMovableContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks);

    protected abstract boolean mouseClickedInsideTabbed(MouseButtonEvent event, boolean doubleClick);
}
