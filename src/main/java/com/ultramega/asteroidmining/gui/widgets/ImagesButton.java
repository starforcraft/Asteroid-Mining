package com.ultramega.asteroidmining.gui.widgets;

import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public class ImagesButton extends Button {
    public boolean value = false;

    @Nullable
    private final Identifier sprite;
    @Nullable
    private final WidgetSprites activeSprites;
    @Nullable
    private final WidgetSprites deactiveSprites;

    public ImagesButton(final int x,
                        final int y,
                        final int width,
                        final int height,
                        final Identifier sprite,
                        final Consumer<ImagesButton> onPress) {
        this(x, y, width, height, sprite, null, null, onPress);
    }

    public ImagesButton(final int x,
                        final int y,
                        final int width,
                        final int height,
                        final WidgetSprites activeSprites,
                        final WidgetSprites deactiveSprites,
                        final Consumer<ImagesButton> onPress) {
        this(x, y, width, height, null, activeSprites, deactiveSprites, onPress);
    }

    private ImagesButton(final int x, final int y, final int width, final int height,
                         @Nullable final Identifier sprite,
                         @Nullable final WidgetSprites activeSprites,
                         @Nullable final WidgetSprites deactiveSprites,
                         final Consumer<ImagesButton> onPress) {
        super(x, y, width, height, Component.empty(), button -> onPress.accept((ImagesButton) button), DEFAULT_NARRATION);
        this.sprite = sprite;
        this.activeSprites = activeSprites;
        this.deactiveSprites = deactiveSprites;
    }

    @Override
    protected void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
        final Identifier texture = (this.activeSprites != null && this.deactiveSprites != null)
            ? (this.value ? this.activeSprites.get(this.isActive(), this.isHovered()) : this.deactiveSprites.get(this.isActive(), this.isHovered()))
            : this.sprite;

        if (texture != null) {
            graphics.blitSprite(GUI_TEXTURED, texture, this.getX(), this.getY(), this.width, this.height);
        }
    }
}
