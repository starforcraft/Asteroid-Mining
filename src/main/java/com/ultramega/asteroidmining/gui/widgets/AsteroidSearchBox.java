package com.ultramega.asteroidmining.gui.widgets;

import com.ultramega.asteroidmining.asteroids.AsteroidConfig;
import com.ultramega.asteroidmining.events.AsteroidReloadListener;
import com.ultramega.asteroidmining.utils.ClientUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class AsteroidSearchBox extends PlaceholderEditBox {
    private static final int MAX_RESULTS = 50;
    private static final int VISIBLE_RESULTS = 8;
    private final Font font;
    private final Consumer<AsteroidConfig> selectedAsteroid;
    private final List<AsteroidConfig> suggestions = new ArrayList<>();
    private int selectedIndex;
    private int offset;

    public AsteroidSearchBox(final Font font, final int x, final int y, final int width,
                             final int height, final int maxLength, final Component placeholder,
                             final Consumer<AsteroidConfig> selectedAsteroid) {
        super(font, x, y, width, height, maxLength, placeholder, Component.empty());
        this.font = font;
        this.selectedAsteroid = selectedAsteroid;
    }

    @Override
    public void onValueChange(final String newText) {
        super.onValueChange(newText);
        // EditBox invokes this from its constructor.
        if (this.suggestions != null) {
            this.refreshSuggestions();
        }
    }

    public void refreshSuggestions() {
        this.offset = 0;
        this.selectedIndex = 0;
        this.suggestions.clear();
        this.suggestions.addAll(AsteroidReloadListener.INSTANCE.findAsteroids(this.getValue(), MAX_RESULTS));
    }

    @Override
    public void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTicks);
        if (!this.isVisible() || !this.isFocused()) {
            return;
        }
        final int rows = this.shownSuggestions();
        for (int row = 0; row < rows; row++) {
            final int index = this.offset + row;
            final int y = this.getY() + this.getHeight() * (row + 1);
            graphics.fill(this.getX(), y, this.getX() + this.getWidth(), y + this.getHeight(),
                index == this.selectedIndex ? 0xFF385676 : 0xFF202A38);
            graphics.text(this.font, this.font.plainSubstrByWidth(this.suggestions.get(index).getName(), this.getWidth() - 8),
                this.getX() + 4, y + 4, index == this.selectedIndex ? 0xFFFFFF55 : -1);
        }
        final int footerY = this.getY() + this.getHeight() * (rows + 1);
        graphics.fill(this.getX(), footerY, this.getX() + this.getWidth(), footerY + this.getHeight(), 0xFF202A38);
        final Component footer = this.suggestions.isEmpty() && !this.getValue().isBlank()
            ? Component.translatable("gui.asteroidmining.asteroid.search_empty")
            : Component.translatable("gui.asteroidmining.asteroid.search_help");
        graphics.text(this.font, this.font.plainSubstrByWidth(footer.getString(), this.getWidth() - 8),
            this.getX() + 4, footerY + 4, 0xFFB6C7D9);
    }

    @Override
    public void mouseMoved(final double mouseX, final double mouseY) {
        if (this.isVisible() && this.isFocused() && this.isInBounds(mouseX, mouseY)) {
            final int row = (int) ((mouseY - this.getY()) / this.getHeight()) - 1;
            if (row >= 0 && row < this.shownSuggestions()) {
                this.selectedIndex = this.offset + row;
            }
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (!this.isVisible()) {
            return false;
        }
        // Handle the dropdown before EditBox defocuses itself for an outside click.
        if (this.isFocused() && this.isInBounds(event.x(), event.y()) && !this.isMouseOver(event.x(), event.y())) {
            final int row = (int) ((event.y() - this.getY()) / this.getHeight()) - 1;
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && row >= 0 && row < this.shownSuggestions()) {
                this.choose(this.offset + row);
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        if (this.isVisible() && this.isFocused() && this.isInBounds(mouseX, mouseY)) {
            this.offset = Mth.clamp(this.offset - (int) Math.signum(scrollY), 0,
                Math.max(0, this.suggestions.size() - this.shownSuggestions()));
            this.selectedIndex = Mth.clamp(this.selectedIndex, this.offset,
                Math.max(this.offset, this.offset + this.shownSuggestions() - 1));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (!this.isVisible() || !this.isFocused()) {
            return false;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            this.choose(this.selectedIndex);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_DOWN) {
            if (!this.suggestions.isEmpty()) {
                this.selectedIndex = Mth.clamp(this.selectedIndex + (event.key() == GLFW.GLFW_KEY_UP ? -1 : 1),
                    0, this.suggestions.size() - 1);
                this.offset = Mth.clamp(this.offset, Math.max(0, this.selectedIndex - this.shownSuggestions() + 1), this.selectedIndex);
            }
            return true;
        }
        return super.keyPressed(event);
    }

    private void choose(final int index) {
        if (index >= 0 && index < this.suggestions.size()) {
            final AsteroidConfig asteroid = this.suggestions.get(index);
            this.selectedAsteroid.accept(asteroid);
            this.setFocused(false);
        }
    }

    public boolean isInBounds(final double mouseX, final double mouseY) {
        return this.isVisible() && ClientUtils.isMouseOver(this.getX(), this.getY(), this.getWidth(),
            this.getHeight() * (1 + (this.isFocused() ? this.shownSuggestions() + 1 : 0)), mouseX, mouseY);
    }

    private int shownSuggestions() {
        return Math.min(VISIBLE_RESULTS, this.suggestions.size());
    }
}
