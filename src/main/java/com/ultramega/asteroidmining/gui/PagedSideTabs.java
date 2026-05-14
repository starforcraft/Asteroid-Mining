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
    public static final int TAB_WIDTH = 32;
    public static final int TAB_HEIGHT = 26;

    private static final Identifier SELECTED_TAB_TOP = AsteroidMining.makeId("selected_tab_top");
    private static final Identifier SELECTED_TAB_BOTTOM = AsteroidMining.makeId("selected_tab_bottom");
    private static final Identifier SELECTED_TAB = AsteroidMining.makeId("selected_tab");
    private static final Identifier UNSELECTED_TAB = AsteroidMining.makeId("unselected_tab");

    private static final int TAB_HIT_X_OFFSET = 3;
    private static final int TAB_HIT_Y_OFFSET = 2;
    private static final int TAB_HIT_WIDTH = 24;
    private static final int TAB_HIT_HEIGHT = 22;

    private final int maxShownTabs;

    private int selectedIndex = -1;
    private int currentPage;
    private int totalPages;

    public PagedSideTabs(final int maxShownTabs) {
        this.maxShownTabs = maxShownTabs;
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

    public void renderTabs(final GuiGraphicsExtractor graphics,
                           final List<T> items,
                           final int x,
                           final int y,
                           final int mouseX,
                           final int mouseY,
                           final TabRenderer<T> tabRenderer) {
        this.update(items.size());

        if (items.isEmpty()) {
            return;
        }

        final int startIndex = this.currentPage * this.maxShownTabs;
        final int endIndex = Math.min(startIndex + this.maxShownTabs, items.size());
        final int lastSlotOnPage = endIndex - startIndex - 1;

        for (int i = startIndex; i < endIndex; i++) {
            final int slot = i - startIndex;
            final int tabY = y + TAB_HEIGHT * slot;

            final T item = items.get(i);
            final boolean selected = i == this.selectedIndex;

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, selected ? this.selectedSprite(slot, lastSlotOnPage) : UNSELECTED_TAB, x, tabY, TAB_WIDTH, TAB_HEIGHT);
            tabRenderer.render(graphics, item, x + 9, tabY + 5, mouseX, mouseY, this.isMouseOverTab(x, tabY, mouseX, mouseY));
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
            final int tabY = y + TAB_HEIGHT * slot;

            if (this.isMouseOverTab(x, tabY, mouseX, mouseY)) {
                return i;
            }
        }

        return -1;
    }

    private boolean isMouseOverTab(final int tabX,
                                   final int tabY,
                                   final double mouseX,
                                   final double mouseY) {
        return ClientUtils.isMouseOver(tabX + TAB_HIT_X_OFFSET, tabY + TAB_HIT_Y_OFFSET, TAB_HIT_WIDTH, TAB_HIT_HEIGHT, mouseX, mouseY);
    }

    private Identifier selectedSprite(final int slot, final int lastSlotOnPage) {
        if (slot == 0) {
            return SELECTED_TAB_TOP;
        }
        if (slot == lastSlotOnPage) {
            return SELECTED_TAB_BOTTOM;
        }
        return SELECTED_TAB;
    }

    public int getMaxShownTabs() {
        return this.maxShownTabs;
    }

    public int getCurrentPage() {
        return this.currentPage;
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
