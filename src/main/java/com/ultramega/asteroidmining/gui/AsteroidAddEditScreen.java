/*
package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.events.AsteroidReloadListener;
import com.ultramega.asteroidmining.gui.widgets.PlaceholderEditBox;
import com.ultramega.asteroidmining.asteroids.AsteroidConfig;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class AsteroidAddEditScreen extends Screen {
    private static final ResourceLocation BACKGROUND = AsteroidMining.makeId("textures/gui/add_asteroid.png");
    private static final int BACKGROUND_WIDTH = 223;
    private static final int BACKGROUND_HEIGHT = 182;

    private final AsteroidConfig data;

    private PlaceholderEditBox nameEditBox;

    private int leftPos;
    private int topPos;

    protected AsteroidAddEditScreen(final AsteroidConfig data) {
        super(Component.translatable("gui.asteroidmining.asteroid.add_asteroid"));
        this.data = data;
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - BACKGROUND_WIDTH) / 2;
        this.topPos = (this.height - BACKGROUND_HEIGHT) / 2;

        this.nameEditBox = new PlaceholderEditBox(
            this.font,
            this.leftPos + 7,
            this.topPos + 17,
            209,
            18,
            64,
            Component.translatable("gui.asteroidmining.asteroid.name_asteroid"),
            Component.literal(this.data.getName())
        );
        this.addRenderableWidget(this.nameEditBox);
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        final PoseStack poseStack = graphics.pose();

        super.render(graphics, mouseX, mouseY, partialTick);

        poseStack.pushPose();
        poseStack.translate((float) this.leftPos, (float) this.topPos, 0.0F);

        this.renderLabels(graphics);

        poseStack.popPose();
    }

    @Override
    public void renderBackground(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
    }

    private void renderLabels(final GuiGraphics graphics) {
        graphics.drawString(this.font, this.title, 8, 6, -12566464, false);
    }

    @Override
    public void onClose() {
        final String newName = nameEditBox.getValue();
        this.data.setName(newName);
        AsteroidReloadListener.INSTANCE.updateData(this.data);

        super.onClose();
    }

    @Override
    protected void renderBlurredBackground(final float partialTick) {
    }

    @Override
    protected void renderMenuBackground(final GuiGraphics partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
*/
