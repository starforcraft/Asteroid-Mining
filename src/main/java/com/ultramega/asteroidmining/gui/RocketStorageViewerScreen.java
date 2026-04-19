package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.container.RocketStorageViewerContainerMenu;
import com.ultramega.asteroidmining.gui.widgets.ScrollbarWidget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

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
    private boolean clicked;

    public RocketStorageViewerScreen(final RocketStorageViewerContainerMenu container, final Inventory inventory, final Component title) {
        super(container, inventory, title, 193, 226);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        this.scrollbar = new ScrollbarWidget(
            this.leftPos + 174,
            this.topPos + 20,
            106
        );
        // TODO
        final int slots = 0; //this.menu.getInventoryHandler() != null ? this.menu.getInventoryHandler().getSlots() : 0;
        final int overflowingRows = Mth.ceil((double) slots / COLUMNS_DISPLAYED - ROWS_DISPLAYED);
        final int maxOffset = overflowingRows * ROWS_DISPLAYED;
        this.scrollbar.setMaxOffset(maxOffset);
        this.scrollbar.setEnabled(maxOffset > 0);
        this.scrollbar.setListener(value -> this.updateWidgets());
        this.addWidget(this.scrollbar);
    }

    //TODO: likely wrong method
    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        this.scrollbar.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        this.extractTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.getLeftPos(), this.getTopPos(), 0, 0, this.getImageWidth(), this.getImageHeight(), 256, 256);
        this.renderInventoryContent(graphics, mouseX, mouseY);
    }

    private void renderInventoryContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        final int scrollbarOffset = (int) this.scrollbar.getOffset();
        final int x = (this.width - this.imageWidth) / 2;
        final int y = (this.height - this.imageHeight) / 2;
        // TODO
        final int slots = 0; //this.menu.getInventoryHandler() != null ? this.menu.getInventoryHandler().getSlots() : 0;

        for (int row = 0; row < Math.max(slots, ROWS_DISPLAYED); ++row) {
            final int rowY = y + INV_START_Y + (row * ROW_SIZE) - scrollbarOffset * ROW_SIZE;
            final boolean isOutOfFrame = (rowY < y + INV_START_Y)
                || (rowY > y + INV_START_Y + (ROW_SIZE * ROWS_DISPLAYED) - ROW_SIZE);
            if (isOutOfFrame) {
                continue;
            }

            this.renderInventoryRow(graphics, x + INV_START_X, rowY, row, mouseX, mouseY, slots);
        }
    }

    private void renderInventoryRow(final GuiGraphicsExtractor graphics,
                                    final int rowX,
                                    final int rowY,
                                    final int row,
                                    final int mouseX,
                                    final int mouseY,
                                    final int slots) {
        for (int column = 0; column < COLUMNS_DISPLAYED; ++column) {
            final int index = column + row * COLUMNS_DISPLAYED;
            final int slotX = rowX + 1 + (column * ROW_SIZE);
            final int slotY = rowY + 1;
            final boolean isSlotHovered = this.isHovering(slotX - this.leftPos, slotY - this.topPos, ROW_SIZE - 2, ROW_SIZE - 2, mouseX, mouseY);

            if (index >= slots) {
                if (isSlotHovered) {
                    drawSlotHighlight(graphics, slotX, slotY);
                }
                continue;
            }
            // TODO
//            if (this.menu.getInventoryHandler() == null) {
//                continue;
//            }
//            final ItemFluidStack stack = this.menu.getInventoryHandler().getItemFluidStackInSlot(index);
//            final ItemStack itemStack = stack.getItemStack();
//            final FluidStack fluidStack = stack.getFluidStack();
//
//            if (itemStack != null) {
//                graphics.item(itemStack, slotX, slotY);
//                Utils.renderAmount(graphics, this.font, slotX, slotY, Utils.formatWithUnits(itemStack.getCount()), 16777215);
//            } else if (fluidStack != null) {
//                FluidContainerUtil.renderTiledFluid(graphics, this, fluidStack,
//                    slotX - this.leftPos, slotY - this.topPos, 16, 16);
//                Utils.renderAmount(graphics, this.font, slotX, slotY, Utils.formatWithUnitsFluid(fluidStack.getAmount()), 16777215);
//            }
//
//            if (isSlotHovered) {
//                drawSlotHighlight(graphics, slotX, slotY);
//                Utils.renderResourceTooltip(graphics, stack, mouseX, mouseY);
//
//                if (this.clicked) {
//                    final boolean shiftDown = Minecraft.getInstance().hasShiftDown();
//                    if (itemStack != null) {
//                        ClientPacketDistributor.sendToServer(new TryExtractRocketStorageMessage(index,
//                            Math.min(itemStack.getCount(), itemStack.getMaxStackSize()), shiftDown));
//                    } else if (fluidStack != null) {
//                        ClientPacketDistributor.sendToServer(new TryExtractRocketStorageMessage(index, 1000, shiftDown));
//                    }
//                    this.clicked = false;
//                }
//            }
        }
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
        super.extractLabels(graphics, xm, ym);

        graphics.text(this.font, this.title, 8, 6, -12566464, false);
    }

    private void updateWidgets() {
        // TODO
        final int totalRows = 0; //Mth.ceil((double) this.menu.getInventoryHandler().getSlots() / COLUMNS_DISPLAYED);
        final double maxOffset = totalRows - ROWS_DISPLAYED;
        this.scrollbar.setMaxOffset(maxOffset);
        this.scrollbar.setEnabled(maxOffset > 0);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        this.clicked = true;

        if (this.scrollbar.mouseClicked(event, doubleClick)) {
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void mouseMoved(final double mouseX, final double mouseY) {
        this.scrollbar.mouseMoved(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        this.clicked = false;

        if (this.scrollbar.mouseReleased(event)) {
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        return this.scrollbar.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
