package com.ultramega.asteroidmining.gui.widgets;

import com.ultramega.asteroidmining.asteroids.AsteroidConfig;
import com.ultramega.asteroidmining.events.AsteroidReloadListener;
import com.ultramega.asteroidmining.utils.TextColors;
import com.ultramega.asteroidmining.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class AsteroidSearchBox extends PlaceholderEditBox {
    private static final int PADDING = 15;
    private static final int MAX_WIDTH = 200;
    private static final int MAX_SUGGESTIONS = 10;

    private final Font font;
    private final int defaultWidth;
    private final boolean resizeable;
    private final Consumer<String> selectedAsteroid;
    private final Map<Identifier, AsteroidConfig> asteroids;

    @Nullable
    private final List<String> currentSuggestions = new ArrayList<>();

    private int suggestionsWidth = 0;
    private boolean updateSelectedIndex = false;
    private int lastSelectedIndex = -1;
    private int selectedIndex = -1;
    private int offset = 0;

    public AsteroidSearchBox(final Font font,
                             final int x,
                             final int y,
                             final int width,
                             final int height,
                             final int maxLength,
                             final boolean resizeable,
                             final Component placeholder,
                             final Consumer<String> selectedAsteroid) {
        super(font, x, y, width, height, maxLength, placeholder, Component.empty());
        this.font = font;
        this.defaultWidth = width;
        this.resizeable = resizeable;
        this.selectedAsteroid = selectedAsteroid;
        this.asteroids = AsteroidReloadListener.INSTANCE.getData();
    }

    @Override
    public void onValueChange(final String newText) {
        super.onValueChange(newText);

        // currentSuggestions can be null because of PlaceholderEditBox#setValue
        if (this.currentSuggestions != null) {
            this.offset = 0;
            this.selectedIndex = -1;
            this.currentSuggestions.clear();

            for (final AsteroidConfig asteroid : this.asteroids.values()) {
                if (asteroid.getName().toLowerCase(Locale.ROOT).contains(newText.toLowerCase(Locale.ROOT))) {
                    this.currentSuggestions.add(asteroid.getName());
                }
            }
        }

        this.updateSize(newText);
    }

    @Override
    public void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTicks);
        if (!this.isFocused()) {
            return;
        }

        boolean isHovering = false;
        this.suggestionsWidth = 0;

        for (int i = this.offset; i < Math.min(this.offset + this.shownSuggestions(), this.currentSuggestions.size()); i++) {
            final String suggestion = this.currentSuggestions.get(i);
            final int index = i - this.offset;

            final int minY = this.getY() + this.getHeight() + this.getHeight() * index;
            final boolean hovered = index == this.selectedIndex;

            if (Utils.isMouseOver(this.getX(), minY, this.getWidth(), this.getHeight(), mouseX, mouseY)) {
                this.selectedIndex = index;
                this.lastSelectedIndex = index;
                isHovering = true;
            }

            graphics.fill(this.getX(), minY, this.getX() + this.getWidth(), minY + this.getHeight(), hovered ? 0xAA919492 : 0xAA676b68);
            graphics.text(this.font, Component.literal(suggestion), this.getX() + 4, minY + 4, hovered ? TextColors.YELLOW.getHexCode() : -1);

            this.suggestionsWidth = Math.max(this.suggestionsWidth, this.font.width(suggestion));
        }

        if (!isHovering && this.updateSelectedIndex) {
            this.selectedIndex = -1;
            this.updateSelectedIndex = false;
        }

        this.updateSize(this.getValue());
    }

    private void updateSize(final String text) {
        if (!this.resizeable) {
            return;
        }

        final int textWidth = Math.max(this.font.width(text), this.suggestionsWidth);
        final int newWidth = Math.min(Math.max(textWidth + PADDING, this.defaultWidth), MAX_WIDTH);

        if (newWidth != this.getWidth()) {
            this.setWidth(newWidth);
        }
    }

    @Override
    public void mouseMoved(final double mouseX, final double mouseY) {
        this.updateSelectedIndex = true;
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        final boolean clicked = super.mouseClicked(event, doubleClick);
        final double mouseX = event.x();
        final double mouseY = event.y();
        if (this.isInBounds(mouseX, mouseY) && !this.isMouseOver(mouseX, mouseY)) {
            if (this.selectedIndex != -1 && this.currentSuggestions.size() > this.offset + this.selectedIndex) {
                final String suggestion = this.currentSuggestions.get(this.offset + this.selectedIndex);
                if (!suggestion.isEmpty()) {
                    this.setValue(suggestion);
                    this.selectedAsteroid.accept(suggestion);
                    this.setFocused(false);
                    return false;
                }
            }
        }

        if (!this.isMouseOver(mouseX, mouseY)) {
            this.setFocused(false);
            this.selectedIndex = -1;
            this.lastSelectedIndex = -1;
        }

        return clicked;
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        if (this.isInBounds(mouseX, mouseY)) {
            this.offset = (int) Mth.clamp((double) this.offset - scrollY, 0.0, Math.max(this.currentSuggestions.size() - MAX_SUGGESTIONS, 0));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER && this.selectedIndex != -1 && this.currentSuggestions.size() > this.offset + this.selectedIndex) {
            final String suggestion = this.currentSuggestions.get(this.offset + this.selectedIndex);
            if (!suggestion.isEmpty()) {
                this.setValue(suggestion);
                this.selectedAsteroid.accept(suggestion);
                this.setFocused(false);
                return true;
            }
            return true;
        } else if (event.key() == GLFW.GLFW_KEY_UP) {
            this.offsetSuggestions(this.lastSelectedIndex - 1);
            return true;
        } else if (event.key() == GLFW.GLFW_KEY_DOWN) {
            this.offsetSuggestions(this.lastSelectedIndex + 1);
            return true;
        }

        return super.keyPressed(event);
    }

    public boolean isInBounds(final double mouseX, final double mouseY) {
        return Utils.isMouseOver(this.getX(), this.getY(), this.getWidth(), this.getHeight() + this.getHeight() * this.shownSuggestions(), mouseX, mouseY);
    }

    private int shownSuggestions() {
        return Math.min(MAX_SUGGESTIONS, this.currentSuggestions.size());
    }

    private void offsetSuggestions(final int offset) {
        final int newOffset = Mth.clamp(offset, 0, this.shownSuggestions() - 1);
        final int halfSuggestions = MAX_SUGGESTIONS / 2;
        final int currentItem = this.offset + newOffset;

        final int minOffset = Math.max(currentItem - halfSuggestions, 0);
        final int maxOffset = Math.max(this.currentSuggestions.size() - MAX_SUGGESTIONS, 0);

        this.offset = Math.min(minOffset, maxOffset);
        this.selectedIndex = currentItem - this.offset;
        this.lastSelectedIndex = this.selectedIndex;
    }
}
