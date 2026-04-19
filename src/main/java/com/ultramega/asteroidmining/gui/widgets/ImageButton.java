package com.ultramega.asteroidmining.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import static com.ultramega.asteroidmining.utils.Utils.createTooltip;
import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public class ImageButton extends Button {
    private Identifier image;
    private final int paddingLeft;
    private final int paddingTop;

    @Nullable
    private Component activeTooltip;
    @Nullable
    private Component deactiveTooltip;

    private boolean renderBackground = true;

    public ImageButton(final int x,
                       final int y,
                       final int width,
                       final int height,
                       final Identifier image,
                       final OnPress onPress) {
        this(x, y, width, height, 0, 0, image, onPress);
    }

    public ImageButton(final int x,
                       final int y,
                       final int width,
                       final int height,
                       final int paddingLeft,
                       final int paddingTop,
                       final Identifier image,
                       final OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, Button.DEFAULT_NARRATION);
        this.image = image;
        this.paddingLeft = paddingLeft;
        this.paddingTop = paddingTop;
    }

    @Override
    protected void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
        if (this.renderBackground) {
            //TODO
//            super.extractContents(graphics, mouseX, mouseY, partialTick);
        }

        final int imageWidth = this.getWidth() - this.paddingLeft;
        final int imageHeight = this.getHeight() - this.paddingTop;

        final int x = this.getX() + (this.getWidth() - imageWidth) / 2;
        final int y = this.getY() + (this.getHeight() - imageHeight) / 2;

        if (this.renderBackground) {
            //TODO test
//            graphics.setColor(0.0f, 0.0f, 0.0f, 0.5f);
            graphics.blitSprite(GUI_TEXTURED, this.image, x + 1, y + 1, imageWidth, imageHeight, 0.5F);
        }

        graphics.blitSprite(GUI_TEXTURED, this.image, x, y, imageWidth, imageHeight);

        if (this.activeTooltip != null && this.isHovered()) {
            final Component tooltip = this.isActive() ? this.activeTooltip : (this.deactiveTooltip != null ? this.deactiveTooltip : this.activeTooltip);
            graphics.tooltip(Minecraft.getInstance().font, createTooltip(tooltip), mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        }
    }

    public void setImage(final Identifier image) {
        this.image = image;
    }

    public void setActiveTooltip(final Component activeTooltip) {
        this.activeTooltip = activeTooltip;
    }

    public void setDeactiveTooltip(final Component deactiveTooltip) {
        this.deactiveTooltip = deactiveTooltip;
    }

    public void setRenderBackground(final boolean renderBackground) {
        this.renderBackground = renderBackground;
    }
}
