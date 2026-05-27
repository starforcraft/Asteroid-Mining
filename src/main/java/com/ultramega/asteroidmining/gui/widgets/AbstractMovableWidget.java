package com.ultramega.asteroidmining.gui.widgets;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.config.ClientConfig;
import com.ultramega.asteroidmining.utils.ClientUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import static com.ultramega.asteroidmining.utils.ClientUtils.createTooltip;

public abstract class AbstractMovableWidget extends AbstractWidget {
    private static final Identifier BACKGROUND = AsteroidMining.makeId("white_side_panel");

    private static final Identifier CLOSE = AsteroidMining.makeId("close_small");
    private static final int CLOSE_SIZE = 7;
    private static final int CLOSE_OFFSET = 5;

    private static final int HEADER_HEIGHT = 13;

    private static final Map<MovableWidgetType, Boolean> VISIBILITY_STATES = new EnumMap<>(MovableWidgetType.class);

    private final MovableWidgetType widgetType;
    private final IntSupplier screenWidth;
    private final IntSupplier screenHeight;
    private final boolean isCloseable;

    private int lastScreenWidth;
    private int lastScreenHeight;

    private boolean dragging;
    private int lastSavedX;
    private int lastSavedY;

    protected AbstractMovableWidget(final MovableWidgetType widgetType,
                                    final int defaultX,
                                    final int defaultY,
                                    final int width,
                                    final int height,
                                    final IntSupplier screenWidth,
                                    final IntSupplier screenHeight,
                                    final boolean defaultVisible) {
        this(widgetType, ClientConfig.getWidgetPosition(widgetType, screenWidth.getAsInt(), screenHeight.getAsInt(), width, height)
                .orElse(new ClientConfig.SavedPosition(defaultX, defaultY)), width, height, screenWidth, screenHeight, defaultVisible);
    }

    private AbstractMovableWidget(final MovableWidgetType widgetType,
                                  final ClientConfig.SavedPosition initialPosition,
                                  final int width,
                                  final int height,
                                  final IntSupplier screenWidth,
                                  final IntSupplier screenHeight,
                                  final boolean defaultVisible) {
        super(initialPosition.x(), initialPosition.y(), width, height, Component.empty());

        this.widgetType = widgetType;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.visible = VISIBILITY_STATES.getOrDefault(widgetType, defaultVisible);
        this.isCloseable = !defaultVisible;

        this.lastScreenWidth = screenWidth.getAsInt();
        this.lastScreenHeight = screenHeight.getAsInt();

        this.clampToScreen();

        this.lastSavedX = this.getX();
        this.lastSavedY = this.getY();
    }

    @Override
    protected final void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        this.updatePositionAfterScreenResize();

        final int x = this.getX();
        final int y = this.getY();
        final Font font = Minecraft.getInstance().font;

        this.extractBehindMovableContents(graphics, mouseX, mouseY, partialTicks);

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, this.getWidth(), this.getHeight());
        if (this.isCloseable) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, CLOSE, x + this.getWidth() - CLOSE_SIZE - CLOSE_OFFSET, y + CLOSE_OFFSET, CLOSE_SIZE, CLOSE_SIZE);
        }
        graphics.text(font, this.getTitle(), x + (this.getWidth() - font.width(this.getTitle())) / 2, y + 5, -12566464, false);

        this.extractMovableContents(graphics, mouseX, mouseY, partialTicks);
        this.extractAllTooltips(graphics, Minecraft.getInstance().font, mouseX, mouseY);
    }

    private void extractAllTooltips(final GuiGraphicsExtractor graphics, final Font font, final int mouseX, final int mouseY) {
        if (this.isCloseable
            && ClientUtils.isMouseOver(this.getX() + this.getWidth() - CLOSE_SIZE - CLOSE_OFFSET, this.getY() + CLOSE_OFFSET, CLOSE_SIZE, CLOSE_SIZE, mouseX, mouseY)) {
            graphics.tooltip(font, createTooltip(Component.translatable("gui.asteroidmining.close")), mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
            return;
        }
        this.extractTooltips(graphics, font, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (!this.visible || !this.active || !this.isMouseOverWidgetArea(event.x(), event.y())) {
            return false;
        }

        if (this.isCloseable
            && ClientUtils.isMouseOver(this.getX() + this.getWidth() - CLOSE_SIZE - CLOSE_OFFSET, this.getY() + CLOSE_OFFSET, CLOSE_SIZE, CLOSE_SIZE, event.x(), event.y())) {
            this.setMovableVisible(false);
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
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

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (this.isCloseable && this.visible && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.setMovableVisible(false);
            return true;
        }

        return false;
    }

    private void updatePositionAfterScreenResize() {
        if (this.dragging) {
            return;
        }

        final int currentScreenWidth = this.screenWidth.getAsInt();
        final int currentScreenHeight = this.screenHeight.getAsInt();

        if (currentScreenWidth == this.lastScreenWidth && currentScreenHeight == this.lastScreenHeight) {
            return;
        }

        this.lastScreenWidth = currentScreenWidth;
        this.lastScreenHeight = currentScreenHeight;

        ClientConfig.getWidgetPosition(this.widgetType, currentScreenWidth, currentScreenHeight, this.width, this.height)
            .ifPresentOrElse(position -> {
                this.setX(position.x());
                this.setY(position.y());
            }, this::clampToScreen);

        this.lastSavedX = this.getX();
        this.lastSavedY = this.getY();
    }

    public final void savePosition() {
        if (this.getX() == this.lastSavedX && this.getY() == this.lastSavedY) {
            return;
        }

        this.lastSavedX = this.getX();
        this.lastSavedY = this.getY();

        ClientConfig.setWidgetPosition(this.widgetType, this.getX(), this.getY(), this.screenWidth.getAsInt(), this.screenHeight.getAsInt(), this.width, this.height);
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

    public void openOrClose() {
        this.setMovableVisible(!this.visible);
    }

    public final void setMovableVisible(final boolean visible) {
        this.visible = visible;
        VISIBILITY_STATES.put(this.widgetType, visible);
    }

    protected void extractBehindMovableContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
    }

    protected abstract void extractMovableContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks);

    protected abstract void extractTooltips(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY);

    protected abstract boolean mouseClickedInside(MouseButtonEvent event, boolean doubleClick);
}
