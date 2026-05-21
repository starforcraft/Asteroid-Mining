package com.ultramega.asteroidmining.gui.widgets;

import com.ultramega.asteroidmining.utils.ClientUtils;
import com.ultramega.asteroidmining.utils.LaunchError;
import com.ultramega.asteroidmining.utils.TextColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import static com.ultramega.asteroidmining.utils.ClientUtils.createTooltip;

public class SpacePortErrorsWidget extends AbstractMovableWidget {
    public static final int WIDTH = 170;
    public static final int HEIGHT = 112;

    private static final int CONTENT_PADDING = 5;
    private static final int ROW_HEIGHT = 14;
    private static final int SCROLLBAR_WIDTH = 12;

    private final List<LaunchError> errors;
    private final ScrollbarWidget scrollbar;

    public SpacePortErrorsWidget(final int defaultX,
                                 final int defaultY,
                                 final IntSupplier screenWidth,
                                 final IntSupplier screenHeight,
                                 final List<LaunchError> errors) {
        super(MovableWidgetType.SPACE_PORT_ERRORS, defaultX, defaultY, WIDTH, HEIGHT, screenWidth, screenHeight, false);
        this.errors = errors;
        this.scrollbar = new ScrollbarWidget(0, 0, this.getListHeight());
        this.scrollbar.setScrollAmount(ROW_HEIGHT);
        this.scrollbar.setListener(value -> this.updateScrollbarState());
    }

    @Override
    protected void extractMovableContents(final GuiGraphicsExtractor graphics,
                                          final int mouseX,
                                          final int mouseY,
                                          final float partialTicks) {
        this.layoutScrollbar();
        this.updateScrollbarState();

        this.scrollbar.extractWidgetBackground(graphics);

        final Font font = Minecraft.getInstance().font;
        final int listX = this.getListX();
        final int listY = this.getListY();
        final int listWidth = this.getListWidth();
        final int listHeight = this.getListHeight();

        graphics.enableScissor(listX, listY, listX + listWidth, listY + listHeight);

        for (int i = 0; i < this.errors.size(); i++) {
            final int rowY = listY + i * ROW_HEIGHT - (int) this.scrollbar.getOffset();
            if (rowY + ROW_HEIGHT < listY || rowY > listY + listHeight) {
                continue;
            }

            final Component errorText = this.getErrorText(this.errors.get(i));
            final String fittedText = this.fitToWidth(font, errorText.getString(), listWidth - 8);

            // TODO: not 100% happy of this right now
            graphics.text(font, "-", listX, rowY + 3, TextColors.RED.getHexCode(), false);
            graphics.text(font, fittedText, listX + 8, rowY + 3, TextColors.RED.getHexCode(), false);
        }

        graphics.disableScissor();

        this.scrollbar.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void extractTooltips(final GuiGraphicsExtractor graphics,
                                   final Font font,
                                   final int mouseX,
                                   final int mouseY) {
        if (!ClientUtils.isMouseOver(this.getListX(), this.getListY(), this.getListWidth(), this.getListHeight(), mouseX, mouseY)) {
            return;
        }

        final int index = (int) ((mouseY - this.getListY() + this.scrollbar.getOffset()) / ROW_HEIGHT);
        if (index < 0 || index >= this.errors.size()) {
            return;
        }

        graphics.tooltip(font, createTooltip(this.getErrorTooltip(this.errors.get(index))), mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
    }

    private void updateScrollbarState() {
        final int maxOffset = this.errors.size() * ROW_HEIGHT - this.getListHeight();
        this.scrollbar.setMaxOffset(maxOffset);
        this.scrollbar.setEnabled(maxOffset > 0);
    }

    private void layoutScrollbar() {
        this.scrollbar.setX(this.getX() + this.getWidth() - CONTENT_PADDING - SCROLLBAR_WIDTH);
        this.scrollbar.setY(this.getListY());
    }

    @Override
    protected boolean mouseClickedInside(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.scrollbar.mouseClicked(event, doubleClick)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        if (this.scrollbar.mouseReleased(event)) {
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        if (this.scrollbar.isMouseOver(event.x(), event.y()) || this.scrollbar.isClicked()) {
            this.scrollbar.mouseMoved(event.x(), event.y());
            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(final double mouseX,
                                 final double mouseY,
                                 final double scrollX,
                                 final double scrollY) {
        if (!ClientUtils.isMouseOver(this.getX(), this.getY(), this.getWidth(), this.getHeight(), mouseX, mouseY)) {
            return false;
        }

        return this.scrollbar.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int getListX() {
        return this.getX() + CONTENT_PADDING;
    }

    private int getListY() {
        return this.getY() + this.getHeaderHeight() + CONTENT_PADDING;
    }

    private int getListWidth() {
        return this.getWidth() - CONTENT_PADDING * 3 - SCROLLBAR_WIDTH;
    }

    private int getListHeight() {
        return this.getHeight() - this.getHeaderHeight() - CONTENT_PADDING * 2;
    }

    @Override
    protected Component getTitle() {
        return Component.translatable("gui.asteroidmining.rocket_controller.errors.title");
    }

    private Component getErrorText(final LaunchError error) {
        final Component base = Component.translatable(
            "gui.asteroidmining.rocket_controller.error." + error.type().name().toLowerCase(Locale.ROOT)
        );

        if (error.pos() == null) {
            return base;
        }

        return Component.empty()
            .append(base)
            .append(" ")
            .append(Component.literal("(" + error.pos().getX() + ", " + error.pos().getY() + ", " + error.pos().getZ() + ")"));
    }

    private List<Component> getErrorTooltip(final LaunchError error) {
        final List<Component> tooltip = new ArrayList<>();

        tooltip.add(Component.translatable(
            "gui.asteroidmining.rocket_controller.error." + error.type().name().toLowerCase(Locale.ROOT)
        ));

        if (error.pos() != null) {
            tooltip.add(Component.translatable(
                "gui.asteroidmining.rocket_controller.error.position",
                error.pos().getX(),
                error.pos().getY(),
                error.pos().getZ()
            ));
        }

        if (error.expectedState() != null) {
            tooltip.add(Component.translatable(
                "gui.asteroidmining.rocket_controller.error.expected",
                error.expectedState().getBlock().getName()
            ));
        }

        if (error.actualState() != null) {
            tooltip.add(Component.translatable(
                "gui.asteroidmining.rocket_controller.error.actual",
                error.actualState().getBlock().getName()
            ));
        }

        if (!error.relatedPositions().isEmpty()) {
            tooltip.add(Component.translatable(
                "gui.asteroidmining.rocket_controller.error.related_positions",
                error.relatedPositions().size()
            ));
        }

        return tooltip;
    }

    private String fitToWidth(final Font font, final String text, final int width) {
        if (font.width(text) <= width) {
            return text;
        }

        final String ellipsis = "...";
        String fitted = text;

        while (!fitted.isEmpty() && font.width(fitted + ellipsis) > width) {
            fitted = fitted.substring(0, fitted.length() - 1);
        }

        return fitted.isEmpty() ? ellipsis : fitted + ellipsis;
    }
}
