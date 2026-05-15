package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.utils.ClientUtils;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class PagedSideTabs<T> {
    private static final int TAB_WIDTH = 32;
    private static final int TAB_HEIGHT = 26;
    private static final int TAB_WIDTH_SMALL = 24;
    private static final int TAB_HEIGHT_SMALL = 20;

    private static final int TAB_SCREEN_OVERLAP = 4;

    private static final Identifier SELECTED_TAB_TOP = AsteroidMining.makeId("selected_tab_top");
    private static final Identifier SELECTED_TAB_BOTTOM = AsteroidMining.makeId("selected_tab_bottom");
    private static final Identifier SELECTED_TAB = AsteroidMining.makeId("selected_tab");
    private static final Identifier UNSELECTED_TAB = AsteroidMining.makeId("unselected_tab");
    private static final Identifier SELECTED_TAB_TOP_SMALL = AsteroidMining.makeId("selected_tab_top_small");
    private static final Identifier SELECTED_TAB_BOTTOM_SMALL = AsteroidMining.makeId("selected_tab_bottom_small");
    private static final Identifier SELECTED_TAB_SMALL = AsteroidMining.makeId("selected_tab_small");
    private static final Identifier UNSELECTED_TAB_SMALL = AsteroidMining.makeId("unselected_tab_small");

    private final boolean smallTabs;
    private final int tabIconSize;
    private final int maxShownTabs;

    private int selectedIndex = -1;
    private int currentPage;
    private int totalPages;

    public PagedSideTabs(final int guiHeight, final boolean smallTabs, final int tabIconSize) {
        this.smallTabs = smallTabs;
        this.tabIconSize = tabIconSize;
        this.maxShownTabs = guiHeight / this.getTabHeight();
    }

    public void setSelectedIndex(final int selectedIndex) {
        this.selectedIndex = selectedIndex;
        if (selectedIndex >= 0) {
            this.currentPage = selectedIndex / this.maxShownTabs;
        }
    }

    public Component pageLabel() {
        return Component.translatable("%s / %s", this.currentPage + 1, this.totalPages);
    }

    public boolean shouldShowPageControls() {
        return this.totalPages > 1;
    }

    public boolean canPageUp() {
        return this.shouldShowPageControls() && this.currentPage > 0;
    }

    public boolean canPageDown() {
        return this.shouldShowPageControls() && this.currentPage < this.totalPages - 1;
    }

    public void previousPage() {
        this.currentPage = Math.max(0, this.currentPage - 1);
    }

    public void nextPage() {
        this.currentPage = Math.clamp(this.totalPages - 1, 0, this.currentPage + 1);
    }

    public void update(final int itemCount) {
        this.totalPages = itemCount <= 0 ? 0 : (int) Math.ceil((double) itemCount / this.maxShownTabs);
        this.currentPage = Mth.clamp(this.currentPage, 0, Math.max(this.totalPages - 1, 0));

        if (itemCount <= 0) {
            this.selectedIndex = -1;
        } else if (this.selectedIndex >= itemCount) {
            this.selectedIndex = itemCount - 1;
        }
    }

    public void renderUnselectedTabs(final GuiGraphicsExtractor graphics,
                                     final List<T> items,
                                     final int x,
                                     final int y,
                                     final int mouseX,
                                     final int mouseY,
                                     final TabRenderer<T> tabRenderer) {
        this.renderTabs(graphics, items, x, y, mouseX, mouseY, tabRenderer, TabRenderLayer.UNSELECTED);
    }

    public void renderSelectedTab(final GuiGraphicsExtractor graphics,
                                  final List<T> items,
                                  final int x,
                                  final int y,
                                  final int mouseX,
                                  final int mouseY,
                                  final TabRenderer<T> tabRenderer) {
        this.renderTabs(graphics, items, x, y, mouseX, mouseY, tabRenderer, TabRenderLayer.SELECTED);
    }

    private void renderTabs(final GuiGraphicsExtractor graphics,
                            final List<T> items,
                            final int x,
                            final int y,
                            final int mouseX,
                            final int mouseY,
                            final TabRenderer<T> tabRenderer,
                            final TabRenderLayer renderLayer) {
        this.update(items.size());

        if (items.isEmpty()) {
            return;
        }

        final int startIndex = this.currentPage * this.maxShownTabs;
        final int endIndex = Math.min(startIndex + this.maxShownTabs, items.size());
        final int lastSlotOnPage = endIndex - startIndex - 1;

        for (int i = startIndex; i < endIndex; i++) {
            final int slot = i - startIndex;
            final int tabX = x + TAB_SCREEN_OVERLAP;
            final int tabY = y + this.getTabHeight() * slot;

            final T item = items.get(i);
            final boolean selected = i == this.selectedIndex;

            if (renderLayer.shouldRender(selected)) {
                final Identifier texture = selected ? this.selectedSprite(slot, lastSlotOnPage) : !this.smallTabs ? UNSELECTED_TAB : UNSELECTED_TAB_SMALL;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, texture, tabX, tabY, this.getTabWidth(), this.getTabHeight());
            }

            if (renderLayer == TabRenderLayer.SELECTED) {
                final int iconX = tabX + (this.getTabWidth() - this.tabIconSize) / 2;
                final int iconY = tabY + (this.getTabHeight() - this.tabIconSize) / 2;
                tabRenderer.render(graphics, item, iconX, iconY, mouseX, mouseY, this.isMouseOverTab(tabX, tabY, mouseX, mouseY));
            }
        }
    }

    public boolean mouseClickedTab(final MouseButtonEvent event,
                                   final List<T> items,
                                   final int x,
                                   final int y,
                                   final TabClickHandler<T> handler) {
        if (event.button() != 0) {
            return false;
        }

        this.update(items.size());

        final int clickedIndex = this.getTabIndexAt(items.size(), x, y, event.x(), event.y());
        if (clickedIndex == -1) {
            return false;
        }

        this.selectedIndex = clickedIndex;
        handler.onClick(clickedIndex, items.get(clickedIndex));
        return true;
    }

    public boolean isMouseOverTabs(final int itemCount,
                                   final int x,
                                   final int y,
                                   final double mouseX,
                                   final double mouseY) {
        this.update(itemCount);
        return this.getTabIndexAt(itemCount, x, y, mouseX, mouseY) != -1;
    }

    private int getTabIndexAt(final int itemCount,
                              final int x,
                              final int y,
                              final double mouseX,
                              final double mouseY) {
        if (itemCount <= 0) {
            return -1;
        }

        final int startIndex = this.currentPage * this.maxShownTabs;
        final int endIndex = Math.min(startIndex + this.maxShownTabs, itemCount);

        for (int i = startIndex; i < endIndex; i++) {
            final int slot = i - startIndex;
            final int tabX = x + TAB_SCREEN_OVERLAP;
            final int tabY = y + this.getTabHeight() * slot;

            if (this.isMouseOverTab(tabX, tabY, mouseX, mouseY)) {
                return i;
            }
        }

        return -1;
    }

    private boolean isMouseOverTab(final int tabX,
                                   final int tabY,
                                   final double mouseX,
                                   final double mouseY) {
        return ClientUtils.isMouseOver(tabX, tabY, this.getTabWidth(), this.getTabHeight(), mouseX, mouseY);
    }

    private Identifier selectedSprite(final int slot, final int lastSlotOnPage) {
        if (slot == 0) {
            return !this.smallTabs ? SELECTED_TAB_TOP : SELECTED_TAB_TOP_SMALL;
        }
        if (slot == lastSlotOnPage) {
            return !this.smallTabs ? SELECTED_TAB_BOTTOM : SELECTED_TAB_BOTTOM_SMALL;
        }
        return !this.smallTabs ? SELECTED_TAB : SELECTED_TAB_SMALL;
    }

    public int getTabHeight() {
        return !this.smallTabs ? TAB_HEIGHT : TAB_HEIGHT_SMALL;
    }

    public int getTabWidth() {
        return !this.smallTabs ? TAB_WIDTH : TAB_WIDTH_SMALL;
    }

    public int getMaxShownTabs() {
        return this.maxShownTabs;
    }

    public int getCurrentPage() {
        return this.currentPage;
    }

    private enum TabRenderLayer {
        ALL, SELECTED, UNSELECTED;

        private boolean shouldRender(final boolean selected) {
            return this == ALL || (this == SELECTED && selected) || (this == UNSELECTED && !selected);
        }
    }

    @FunctionalInterface
    public interface TabRenderer<T> {
        void render(GuiGraphicsExtractor graphics, T item, int iconX, int iconY, int mouseX, int mouseY, boolean hovered);
    }

    @FunctionalInterface
    public interface TabClickHandler<T> {
        void onClick(int index, T item);
    }
}
