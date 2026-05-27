package com.ultramega.asteroidmining.gui.widgets;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class PlaceholderEditBox extends EditBox {
    private final Component placeholder;

    public PlaceholderEditBox(final Font font,
                              final int x,
                              final int y,
                              final int width,
                              final int height,
                              final int maxLength,
                              final Component placeholder,
                              final @Nullable Component value) {
        super(font, x, y, width, height, placeholder);
        this.placeholder = placeholder;

        this.setMaxLength(maxLength);
        this.setSuggestion(placeholder.getString());
        if (value != null) {
            this.setValue(value.getString());
        }
    }

    @Override
    public void onValueChange(final String newText) {
        super.onValueChange(newText);

        this.setSuggestion(newText.isEmpty() ? this.placeholder.getString() : "");
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.setFocused(false);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (!this.isMouseOver(event.x(), event.y())) {
            this.setFocused(false);
        } else {
            this.setFocused(true);
        }
        return super.mouseClicked(event, doubleClick);
    }
}
