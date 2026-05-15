package com.ultramega.asteroidmining.gui.widgets;

import com.ultramega.asteroidmining.container.AbstractSideConfigContainerMenu;
import com.ultramega.asteroidmining.network.c2s.SetSideConfigPayload;
import com.ultramega.asteroidmining.utils.ClientUtils;
import com.ultramega.asteroidmining.utils.sides.SideConfigType;
import com.ultramega.asteroidmining.utils.sides.SideIoMode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import static com.ultramega.asteroidmining.utils.ClientUtils.createTooltip;

// TODO: don't keep this open all the time but make it openable/closeable with a side tab button instead
// TODO: also make the tabs smaller
public class SideConfigWidget extends AbstractTabbedMovableWidget<SideConfigType> {
    public static final int WIDTH = 100;
    public static final int HEIGHT = 20 * 4;

    private static final int TAB_ICON_SIZE = 12;
    private static final int CELL_WIDTH = 28;
    private static final int CELL_HEIGHT = 16;

    private static final SideCellLayout[] SIDE_CELL_LAYOUTS = {
        new SideCellLayout(Direction.NORTH, 1, 0, "N"),
        new SideCellLayout(Direction.UP, 2, 0, "U"),

        new SideCellLayout(Direction.WEST, 0, 1, "W"),
        new SideCellLayout(Direction.EAST, 2, 1, "E"),

        new SideCellLayout(Direction.DOWN, 0, 2, "D"),
        new SideCellLayout(Direction.SOUTH, 1, 2, "S")
    };

    private final AbstractSideConfigContainerMenu<?> menu;
    private SideConfigType activeType = SideConfigType.ENERGY;

    public SideConfigWidget(final AbstractSideConfigContainerMenu<?> menu,
                            final int x,
                            final int y,
                            final IntSupplier screenWidth,
                            final IntSupplier screenHeight) {
        super(MovableWidgetType.SIDE_CONFIG, x, y, WIDTH, HEIGHT, screenWidth, screenHeight, true, TAB_ICON_SIZE);
        this.menu = menu;

        this.ensureActiveTypeSupported();
        this.syncSelectedTabToActiveType();
    }

    @Override
    protected void extractTabbedMovableContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        this.ensureActiveTypeSupported();

        // TODO hover highlight the respective slots.
        // TODO: separate fluid tank and gas tank? Or give all tanks a number if more than one?

        final int cellsX = this.getX() + 8;
        final int cellsY = this.getY() + this.getHeaderHeight() + CELL_HEIGHT / 2;
        for (final SideCellLayout cell : SIDE_CELL_LAYOUTS) {
            this.drawSideCell(graphics, cell.direction(), cellsX + cell.col() * (CELL_WIDTH + 1), cellsY + cell.row() * (CELL_HEIGHT + 1), cell.label());
        }
    }

    private void drawSideCell(final GuiGraphicsExtractor graphics, final Direction side, final int x, final int y, final String sideLabel) {
        final Font font = Minecraft.getInstance().font;
        final SideIoMode mode = this.menu.getSideConfig(this.activeType, side);

        graphics.fill(x, y, x + CELL_WIDTH, y + CELL_HEIGHT, mode.getColor());
        graphics.outline(x, y, CELL_WIDTH, CELL_HEIGHT, 0xFF909090);

        graphics.text(font, sideLabel, x + 2, y + 4, 0xFFFFFFFF, false);
        graphics.text(font, mode.getAbbreviation(), x + 10, y + 4, 0xFFFFFFFF, false);
    }

    @Override
    protected void renderTab(final GuiGraphicsExtractor graphics,
                             final SideConfigType type,
                             final int x,
                             final int y,
                             final int mouseX,
                             final int mouseY,
                             final boolean hovered) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, type.getTabIcon(), x, y, TAB_ICON_SIZE, TAB_ICON_SIZE);

        if (hovered) {
            final Font font = Minecraft.getInstance().font;
            graphics.tooltip(font, createTooltip(List.of(type.getTooltip())), mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        }
    }

    @Override
    protected boolean mouseClickedInsideTabbed(final MouseButtonEvent event, final boolean doubleClick) {
        final Direction clickedSide = this.getClickedSide(event.x(), event.y());
        if (clickedSide == null) {
            return false;
        }

        final SideIoMode current = this.menu.getSideConfig(this.activeType, clickedSide);
        final SideIoMode next = event.button() == 1 ? current.previous() : current.next();

        this.menu.setClientSideConfig(this.activeType, clickedSide, next);

        ClientPacketDistributor.sendToServer(new SetSideConfigPayload(this.menu.blockEntity.getBlockPos(), this.activeType, clickedSide, next));
        return true;
    }

    @Override
    protected void onTabClicked(final int index, final SideConfigType type) {
        this.setActiveType(type);
    }

    @Override
    protected void extractTooltips(final GuiGraphicsExtractor graphics, final Font font, final int mouseX, final int mouseY) { //TODO: refactor
        final int cellsX = this.getX() + 8;
        final int cellsY = this.getY() + this.getHeaderHeight() + CELL_HEIGHT / 2;
        for (final SideCellLayout cell : SIDE_CELL_LAYOUTS) {
            if (ClientUtils.isMouseOver(cellsX + cell.col() * (CELL_WIDTH + 1), cellsY + cell.row() * (CELL_HEIGHT + 1), CELL_WIDTH, CELL_HEIGHT, mouseX, mouseY)) {
                final SideIoMode mode = this.menu.getSideConfig(this.activeType, cell.direction());
                final List<Component> tooltips = new ArrayList<>();
                tooltips.add(Component.literal(cell.direction().getName()));
                tooltips.add(mode.getName());
                graphics.tooltip(font, createTooltip(tooltips), mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
            }
        }
    }

    @Override
    protected Component getTitle() {
        return this.activeType.getTitle();
    }

    @Override
    protected List<SideConfigType> getTabItems() {
        final List<SideConfigType> supportedTypes = new ArrayList<>();

        for (final SideConfigType type : SideConfigType.values()) {
            if (this.menu.supportsSideConfig(type)) {
                supportedTypes.add(type);
            }
        }

        return supportedTypes;
    }

    private void setActiveType(final SideConfigType type) {
        this.activeType = type;
        this.syncSelectedTabToActiveType();
    }

    private void syncSelectedTabToActiveType() {
        this.tabs().setSelectedIndex(this.getTabItems().indexOf(this.activeType));
    }

    private void ensureActiveTypeSupported() {
        if (this.menu.supportsSideConfig(this.activeType)) {
            return;
        }

        for (final SideConfigType type : SideConfigType.values()) {
            if (this.menu.supportsSideConfig(type)) {
                this.activeType = type;
                return;
            }
        }

        this.activeType = SideConfigType.ENERGY;
    }

    @Nullable
    private Direction getClickedSide(final double mouseX, final double mouseY) {
        final int x = this.getX();
        final int y = this.getY() + this.getHeaderHeight();

        final int cellsX = x + 8;
        final int cellsY = y + CELL_HEIGHT / 2;

        for (final SideCellLayout cell : SIDE_CELL_LAYOUTS) {
            final int cellX = cellsX + cell.col() * (CELL_WIDTH + 1);
            final int cellY = cellsY + cell.row() * (CELL_HEIGHT + 1);

            if (ClientUtils.isMouseOver(cellX, cellY, CELL_WIDTH, CELL_HEIGHT, mouseX, mouseY)) {
                return cell.direction();
            }
        }

        return null;
    }

    private record SideCellLayout(Direction direction, int col, int row, String label) {
    }
}
