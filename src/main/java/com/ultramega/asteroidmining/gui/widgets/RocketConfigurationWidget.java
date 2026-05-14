package com.ultramega.asteroidmining.gui.widgets;

import com.ultramega.asteroidmining.container.RocketControllerConfigurationContainerMenu;

import java.util.function.IntSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.widget.ExtendedSlider;

public class RocketConfigurationWidget extends AbstractMovableWidget {
    public static final int WIDTH = 97;
    public static final int HEIGHT = 85;

    private final ExtendedSlider cooldownSlider;
    private final Checkbox overlayCheckbox;
    private final Checkbox commentatorCheckbox;

    public RocketConfigurationWidget(final RocketControllerConfigurationContainerMenu menu,
                                     final int defaultX,
                                     final int defaultY,
                                     final IntSupplier screenWidth,
                                     final IntSupplier screenHeight) {
        super(MovableWidgetType.ROCKET_CONFIGURATION, defaultX, defaultY, WIDTH, HEIGHT, screenWidth, screenHeight);

        final Font font = Minecraft.getInstance().font;
        this.cooldownSlider = new ExtendedSlider(0, 0, 82, 16,
            Component.empty(), Component.literal("s"), 0, 120, (int) (menu.getLaunchCooldown() / 20), 1, 0, true);
        //TODO: find better name
        this.overlayCheckbox = Checkbox.builder(Component.translatable("gui.asteroidmining.rocket_controller.configuration.overlay"), font)
            .selected(menu.isLaunchCooldownOverlay())
            .build();
        this.overlayCheckbox.textWidget.setFGColor(-1);

        //TODO: find better name (Sound/Announcement?)
        this.commentatorCheckbox = Checkbox.builder(Component.translatable("gui.asteroidmining.rocket_controller.configuration.commentator"), font)
            .selected(menu.isLaunchCooldownCommentator())
            .build();
        this.commentatorCheckbox.textWidget.setFGColor(-1);
    }

    @Override
    protected void extractMovableContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        this.layoutControls();

        // TODO: rename to only "Cooldown"?
        graphics.text(Minecraft.getInstance().font, Component.translatable("gui.asteroidmining.rocket_controller.configuration.cooldown_settings"),
            this.getX() + 4, this.getY() + 5, -12566464, false);

        this.cooldownSlider.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        this.overlayCheckbox.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        this.commentatorCheckbox.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void extractTooltips(final GuiGraphicsExtractor graphics, final Font font, final int mouseX, final int mouseY) {
    }

    private void layoutControls() {
        final int controlX = this.getX() + 7;

        this.cooldownSlider.setX(controlX);
        this.cooldownSlider.setY(this.getY() + 20);

        this.overlayCheckbox.setX(controlX);
        this.overlayCheckbox.setY(this.getY() + 40);

        this.commentatorCheckbox.setX(controlX);
        this.commentatorCheckbox.setY(this.getY() + 60);
    }

    @Override
    protected boolean mouseClickedInside(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.cooldownSlider.isMouseOver(event.x(), event.y())) {
            return this.cooldownSlider.mouseClicked(event, doubleClick);
        }
        if (this.overlayCheckbox.isMouseOver(event.x(), event.y())) {
            return this.overlayCheckbox.mouseClicked(event, doubleClick);
        }
        if (this.commentatorCheckbox.isMouseOver(event.x(), event.y())) {
            return this.commentatorCheckbox.mouseClicked(event, doubleClick);
        }

        return false;
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        if (this.cooldownSlider.isMouseOver(event.x(), event.y()) && this.cooldownSlider.mouseReleased(event)) {
            return true;
        }

        if (this.overlayCheckbox.isMouseOver(event.x(), event.y()) && this.overlayCheckbox.mouseReleased(event)) {
            return true;
        }

        if (this.commentatorCheckbox.isMouseOver(event.x(), event.y()) && this.commentatorCheckbox.mouseReleased(event)) {
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        if (this.cooldownSlider.isMouseOver(event.x(), event.y())) {
            return this.cooldownSlider.mouseDragged(event, dragX, dragY);
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    public int getLaunchCooldownTicks() {
        return (int) Math.round(this.cooldownSlider.getValue()) * 20;
    }

    public boolean isLaunchOverlaySelected() {
        return this.overlayCheckbox.selected();
    }

    public boolean isLaunchCommentatorSelected() {
        return this.commentatorCheckbox.selected();
    }
}
