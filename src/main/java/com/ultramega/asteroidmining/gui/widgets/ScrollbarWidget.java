package com.ultramega.asteroidmining.gui.widgets;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.utils.ClientUtils;

import java.util.function.DoubleConsumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public class ScrollbarWidget extends AbstractWidget {
    private static final Identifier BACKGROUND = AsteroidMining.makeId("scrollbar_background");
    private static final Identifier SCROLLBAR = AsteroidMining.makeId("widget/scrollbar");
    private static final Identifier SCROLLBAR_CLICKED = AsteroidMining.makeId("widget/scrollbar_clicked");
    private static final Identifier SCROLLBAR_DISABLED = AsteroidMining.makeId("widget/scrollbar_disabled");

    private static final int SCROLLER_HEIGHT = 15;

    private double offset;
    private double maxOffset;
    private double scrollAmount = 1.0;
    private boolean enabled = true;
    private boolean clicked;

    @Nullable
    private DoubleConsumer listener;

    public ScrollbarWidget(final int x, final int y, final int height) {
        super(x, y, 12, height, Component.empty());
    }

    public void extractWidgetBackground(final GuiGraphicsExtractor graphics) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.getX() - 1, this.getY() - 1, this.getWidth() + 2, this.getHeight() + 2);
    }

    @Override
    public void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        graphics.blitSprite(
            GUI_TEXTURED,
            this.getTexture(),
            this.getX(),
            this.getY() + (int) ((float) this.offset / (float) this.maxOffset * (this.height - SCROLLER_HEIGHT)),
            this.getWidth(),
            SCROLLER_HEIGHT
        );
    }

    @Override
    public void mouseMoved(final double mouseX, final double mouseY) {
        final boolean inBounds = mouseY >= this.getY() && mouseY <= this.getY() + this.height;
        if (this.clicked && inBounds) {
            this.updateOffset(mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (!this.isActive()) {
            return false;
        }
        final double mouseX = event.x();
        final double mouseY = event.y();
        final boolean inBounds = ClientUtils.isMouseOver(this.getX(), this.getY(), this.width, this.height, mouseX, mouseY);
        if (event.button() == 0 && inBounds) {
            this.updateOffset(mouseY);
            this.clicked = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        if (this.clicked) {
            this.clicked = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        if (this.enabled) {
            final int scrollDirection = Math.clamp(-(int) scrollY, -1, 1);
            this.setOffset(this.offset + scrollDirection * this.scrollAmount);
            return true;
        }
        return false;
    }

    public void setScrollAmount(final double scrollAmount) {
        this.scrollAmount = Math.max(0.0, scrollAmount);
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public void setListener(@Nullable final DoubleConsumer listener) {
        this.listener = listener;
    }

    public boolean isClicked() {
        return this.clicked;
    }

    private Identifier getTexture() {
        if (!this.enabled) {
            return SCROLLBAR_DISABLED;
        }
        return this.clicked ? SCROLLBAR_CLICKED : SCROLLBAR;
    }

    public void setMaxOffset(final double maxOffset) {
        this.maxOffset = Math.max(0, maxOffset);
        if (this.offset > this.maxOffset) {
            this.offset = this.maxOffset;
            if (this.listener != null) {
                this.listener.accept(this.offset);
            }
        }
    }

    public double getOffset() {
        return this.offset;
    }

    public void setOffset(final double offset) {
        this.offset = Math.clamp(offset, 0, this.maxOffset);
        if (this.listener != null) {
            this.listener.accept(this.offset);
        }
    }

    private void updateOffset(final double mouseY) {
        this.setOffset(Math.floor((mouseY - this.getY()) / (this.height - SCROLLER_HEIGHT) * this.maxOffset));
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput narrationElementOutput) {
    }
}

