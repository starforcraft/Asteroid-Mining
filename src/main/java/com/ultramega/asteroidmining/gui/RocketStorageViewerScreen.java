package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.asteroids.AsteroidResource;
import com.ultramega.asteroidmining.container.RocketStorageViewerContainerMenu;
import com.ultramega.asteroidmining.gui.widgets.ScrollbarWidget;
import com.ultramega.asteroidmining.network.c2s.TryExtractRocketStorageMessage;
import com.ultramega.asteroidmining.utils.Utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
import org.jspecify.annotations.Nullable;

import static com.ultramega.asteroidmining.utils.Utils.drawSlotHighlight;
import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public class RocketStorageViewerScreen extends AbstractModuleScreen<RocketStorageViewerContainerMenu> {
    private static final Identifier BACKGROUND = AsteroidMining.makeId("textures/gui/rocket_storage_viewer.png");

    private static final int INV_START_X = 7;
    private static final int INV_START_Y = 19;
    private static final int COLUMNS_DISPLAYED = 9;
    private static final int ROWS_DISPLAYED = 6;
    private static final int ROW_SIZE = 18;

    private ScrollbarWidget scrollbar;

    public RocketStorageViewerScreen(final RocketStorageViewerContainerMenu container, final Inventory inventory, final Component title) {
        super(container, inventory, title, 193, 226);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        this.scrollbar = new ScrollbarWidget(this.leftPos + 174, this.topPos + 20, 106);
        this.scrollbar.setListener(value -> this.updateWidgets());
        this.addWidget(this.scrollbar);

        this.updateWidgets();
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        this.scrollbar.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.getLeftPos(), this.getTopPos(), 0, 0, this.getImageWidth(), this.getImageHeight(), 256, 256);
    }

    @Override
    public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractContents(graphics, mouseX, mouseY, a);
        this.extractInventoryContent(graphics, mouseX, mouseY, false);
    }

    @Override
    protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        this.extractInventoryContent(graphics, mouseX, mouseY, true);
    }

    private void extractInventoryContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean tooltip) {
        final int firstRow = (int) this.scrollbar.getOffset();
        final int displayedSlots = this.getDisplayedEntryCount();
        final int totalRows = this.getTotalRows();

        final int x = this.leftPos + INV_START_X;
        final int y = this.topPos + INV_START_Y;

        for (int visibleRow = 0; visibleRow < ROWS_DISPLAYED; visibleRow++) {
            final int row = firstRow + visibleRow;
            if (row >= totalRows) {
                break;
            }

            final int rowY = y + visibleRow * ROW_SIZE;
            this.extractInventoryRow(graphics, x, rowY, row, mouseX, mouseY, displayedSlots, tooltip);
        }
    }

    private void extractInventoryRow(final GuiGraphicsExtractor graphics,
                                     final int rowX,
                                     final int rowY,
                                     final int row,
                                     final int mouseX,
                                     final int mouseY,
                                     final int displayedSlots,
                                     final boolean tooltip) {
        for (int column = 0; column < COLUMNS_DISPLAYED; column++) {
            final int displayIndex = column + row * COLUMNS_DISPLAYED;
            final int slotX = rowX + 1 + column * ROW_SIZE;
            final int slotY = rowY + 1;

            final boolean hovered = this.isHovering(slotX - this.leftPos, slotY - this.topPos, ROW_SIZE - 2, ROW_SIZE - 2, mouseX, mouseY);

            if (displayIndex >= displayedSlots) {
                if (hovered && !tooltip) {
                    drawSlotHighlight(graphics, slotX, slotY);
                }
                continue;
            }

            final DisplayedResource displayedResource = this.getDisplayedResource(displayIndex);
            if (displayedResource == null) {
                continue;
            }

            if (!tooltip) {
                Utils.renderResource(graphics, this.font, slotX, slotY, displayedResource.resource());
            }

            if (hovered) {
                if (tooltip) {
                    displayedResource.resource().drawTooltip(graphics, mouseX, mouseY);
                } else {
                    drawSlotHighlight(graphics, slotX, slotY);
                }
            }
        }
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
        super.extractLabels(graphics, xm, ym);
        graphics.text(this.font, this.title, 8, 6, -12566464, false);
    }

    private void updateWidgets() {
        final int totalRows = this.getTotalRows();
        final int maxOffset = Math.max(0, totalRows - ROWS_DISPLAYED);

        this.scrollbar.setMaxOffset(maxOffset);
        this.scrollbar.setEnabled(maxOffset > 0);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.scrollbar.mouseClicked(event, doubleClick)) {
            return true;
        }

        if (this.tryClickStorage(event.x(), event.y())) {
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private boolean tryClickStorage(final double mouseX, final double mouseY) {
        final int displayIndex = this.getHoveredDisplayIndex(mouseX, mouseY);
        if (displayIndex < 0) {
            return false;
        }

        final DisplayedResource displayedResource = this.getDisplayedResource(displayIndex);
        if (displayedResource == null) {
            return false;
        }

        final boolean shiftDown = Minecraft.getInstance().hasShiftDown();

        // TODO: not happy about this either
        switch (displayedResource.resource()) {
            case AsteroidResource.ItemEntry item -> {
                final int amount = Math.min(clampToPositiveInt(displayedResource.resource().amount()), item.resource().getMaxStackSize());
                ClientPacketDistributor.sendToServer(new TryExtractRocketStorageMessage(displayedResource.handlerIndex(), item, amount, shiftDown));
            }

            case AsteroidResource.FluidEntry fluid -> {
                final int amount = Math.min(clampToPositiveInt(displayedResource.resource().amount()), FluidType.BUCKET_VOLUME);
                ClientPacketDistributor.sendToServer(new TryExtractRocketStorageMessage(displayedResource.handlerIndex(), fluid, amount, shiftDown));
            }
        }

        return true;
    }

    private int getHoveredDisplayIndex(final double mouseX, final double mouseY) {
        final int relX = Mth.floor(mouseX) - (this.leftPos + INV_START_X + 1);
        final int relY = Mth.floor(mouseY) - (this.topPos + INV_START_Y + 1);
        if (relX < 0 || relY < 0) {
            return -1;
        }

        if (relX >= COLUMNS_DISPLAYED * ROW_SIZE || relY >= ROWS_DISPLAYED * ROW_SIZE
            || relX % ROW_SIZE >= ROW_SIZE - 2 || relY % ROW_SIZE >= ROW_SIZE - 2) {
            return -1;
        }

        final int column = relX / ROW_SIZE;
        final int visibleRow = relY / ROW_SIZE;
        final int row = visibleRow + (int) this.scrollbar.getOffset();

        final int index = column + row * COLUMNS_DISPLAYED;

        return index < this.getDisplayedEntryCount() ? index : -1;
    }

    @Override
    public void mouseMoved(final double mouseX, final double mouseY) {
        this.scrollbar.mouseMoved(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        if (this.scrollbar.mouseReleased(event)) {
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        return this.scrollbar.mouseScrolled(mouseX, mouseY, scrollX, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int getDisplayedEntryCount() {
        return countNonEmpty(this.menu.getItemHandler()) + countNonEmpty(this.menu.getFluidHandler());
    }

    private int getTotalRows() {
        return Math.max(ROWS_DISPLAYED, Mth.ceil((double) this.getDisplayedEntryCount() / COLUMNS_DISPLAYED));
    }

    private static <R extends Resource> int countNonEmpty(final ResourceHandler<R> handler) {
        int count = 0;

        for (int i = 0; i < handler.size(); i++) {
            final R resource = handler.getResource(i);
            final long amount = handler.getAmountAsLong(i);

            if (!resource.isEmpty() && amount > 0L) {
                count++;
            }
        }

        return count;
    }

    @Nullable
    private DisplayedResource getDisplayedResource(final int displayIndex) {
        int current = 0;

        // TODO: this is shit and not extensible
        final ResourceHandler<ItemResource> items = this.menu.getItemHandler();
        for (int i = 0; i < items.size(); i++) {
            final ItemResource resource = items.getResource(i);
            final long amount = items.getAmountAsLong(i);

            if (resource.isEmpty() || amount <= 0L) {
                continue;
            }

            if (current++ == displayIndex) {
                return new DisplayedResource(i, new AsteroidResource.ItemEntry(resource, amount));
            }
        }

        final ResourceHandler<FluidResource> fluids = this.menu.getFluidHandler();
        for (int i = 0; i < fluids.size(); i++) {
            final FluidResource resource = fluids.getResource(i);
            final long amount = fluids.getAmountAsLong(i);

            if (resource.isEmpty() || amount <= 0L) {
                continue;
            }

            if (current++ == displayIndex) {
                return new DisplayedResource(i, new AsteroidResource.FluidEntry(resource, amount));
            }
        }

        return null;
    }

    private static int clampToPositiveInt(final long amount) {
        return (int) Math.clamp(amount, 1L, Integer.MAX_VALUE);
    }

    private record DisplayedResource(int handlerIndex, AsteroidResource resource) {
    }
}
