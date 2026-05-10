package com.ultramega.asteroidmining.gui.widgets;

import com.ultramega.asteroidmining.container.AbstractSideConfigContainerMenu;
import com.ultramega.asteroidmining.network.c2s.SetSideConfigPayload;
import com.ultramega.asteroidmining.utils.ClientUtils;
import com.ultramega.asteroidmining.utils.sides.SideConfigType;
import com.ultramega.asteroidmining.utils.sides.SideIoMode;

import java.util.function.IntSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

// TODO: don't keep this open all the time but make it openable/closeable with a side tab button instead
public class SideConfigWidget extends AbstractMovableWidget {
    public static final int WIDTH = 112;
    public static final int HEIGHT = 81;

    private static final int TAB_WIDTH = 20;
    private static final int TAB_HEIGHT = 20;
    private static final int TAB_ICON_SIZE = 16;
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
        super(MovableWidgetType.SIDE_CONFIG, x, y, WIDTH, HEIGHT, Component.translatable("gui.asteroidmining.side_configuration.title"), screenWidth, screenHeight);
        this.menu = menu;

        this.ensureActiveTypeSupported();
    }

    @Override
    protected void extractMovableContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        this.ensureActiveTypeSupported();

        final Font font = Minecraft.getInstance().font;
        final int x = this.getX();
        final int y = this.getY() + this.headerHeight();

        for (final SideConfigType type : SideConfigType.values()) {
            final int tabX = x + 2;
            final int tabY = y + type.ordinal() * (TAB_HEIGHT + 2) + 2;

            final boolean supported = this.menu.supportsSideConfig(type);
            final boolean selected = supported && type == this.activeType;

            final int color = !supported ? 0xFF181818 : selected ? 0xFF585859 : 0xFF242424;

            graphics.fill(tabX, tabY, tabX + TAB_WIDTH, tabY + TAB_HEIGHT, color);
            graphics.outline(tabX, tabY, TAB_WIDTH, TAB_HEIGHT, 0xFF707070);

            final int iconX = tabX + (TAB_WIDTH - TAB_ICON_SIZE) / 2;
            final int iconY = tabY + (TAB_HEIGHT - TAB_ICON_SIZE) / 2;

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, type.tabIcon(), iconX, iconY, TAB_ICON_SIZE, TAB_ICON_SIZE);
        }

        graphics.text(font, this.activeType.title(), x + TAB_WIDTH + 6, y + 4, 0xFFFFFFFF, false);

        final int cellsX = x + TAB_WIDTH + 4;
        final int cellsY = y + CELL_HEIGHT;

        for (final SideCellLayout cell : SIDE_CELL_LAYOUTS) {
            this.drawSideCell(
                graphics,
                cell.direction(),
                cellsX + cell.col() * (CELL_WIDTH + 1),
                cellsY + cell.row() * (CELL_HEIGHT + 1),
                cell.label()
            );
        }
    }

    private void drawSideCell(final GuiGraphicsExtractor graphics, final Direction side, final int x, final int y, final String sideLabel) {
        final var font = Minecraft.getInstance().font;
        final SideIoMode mode = this.menu.getSideConfig(this.activeType, side);

        final int color = switch (mode) {
            case NONE -> 0xFF303030;
            case INPUT -> 0xFF1E5A8A;
            case OUTPUT -> 0xFF8A4A1E;
            case BOTH -> 0xFF3E7A3E;
        };

        graphics.fill(x, y, x + CELL_WIDTH, y + CELL_HEIGHT, color);
        graphics.outline(x, y, CELL_WIDTH, CELL_HEIGHT, 0xFF909090);

        graphics.text(font, sideLabel, x + 3, y + 4, 0xFFFFFFFF, false);
        graphics.text(font, mode.label(), x + 12, y + 4, 0xFFFFFFFF, false);
    }

    @Override
    protected boolean mouseClickedInside(final MouseButtonEvent event, final boolean doubleClick) {
        final int mouseX = (int) event.x();
        final int mouseY = (int) event.y();

        final int x = this.getX();
        final int y = this.getY() + this.headerHeight();

        for (final SideConfigType type : SideConfigType.values()) {
            final int tabX = x + 2;
            final int tabY = y + type.ordinal() * (TAB_HEIGHT + 2) + 2;
            if (ClientUtils.isMouseOver(tabX, tabY, TAB_WIDTH, TAB_HEIGHT, mouseX, mouseY)) {
                if (this.menu.supportsSideConfig(type)) {
                    this.activeType = type;
                }
                return true;
            }
        }

        final Direction clickedSide = this.getClickedSide(mouseX, mouseY);
        if (clickedSide == null) {
            return false;
        }

        final SideIoMode current = this.menu.getSideConfig(this.activeType, clickedSide);
        final SideIoMode next = event.button() == 1 ? current.previous() : current.next();

        this.menu.setClientSideConfig(this.activeType, clickedSide, next);

        ClientPacketDistributor.sendToServer(new SetSideConfigPayload(this.menu.blockEntity.getBlockPos(), this.activeType, clickedSide, next));

        return true;
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
    private Direction getClickedSide(final int mouseX, final int mouseY) {
        final int x = this.getX();
        final int y = this.getY() + this.headerHeight();

        final int cellsX = x + TAB_WIDTH + 4;
        final int cellsY = y + CELL_HEIGHT;

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
