package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.gui.widgets.ImageButton;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public class RocketControllerErrorScreen extends Screen {
    private static final Identifier BACKGROUND = AsteroidMining.makeId("textures/gui/rocket_controller.png");
    private static final Identifier RETURN = AsteroidMining.makeId("return");

    private static final int BACKGROUND_WIDTH = 223;
    private static final int BACKGROUND_HEIGHT = 182;

    private int leftPos;
    private int topPos;

    public RocketControllerErrorScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - BACKGROUND_WIDTH) / 2;
        this.topPos = (this.height - BACKGROUND_HEIGHT) / 2;

        final ImageButton returnButton = new ImageButton(this.leftPos + BACKGROUND_WIDTH - 26, this.topPos - 12, 20, 20, 4, 4,
            RETURN, (button) -> this.onClose());
        returnButton.setActiveTooltip(Component.translatable("gui.asteroidmining.return"));
        this.addRenderableWidget(returnButton);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);

        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, 256, 256);

        // TODO: show all errors
    }

    @Override
    protected void extractBlurredBackground(final GuiGraphicsExtractor graphics) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
