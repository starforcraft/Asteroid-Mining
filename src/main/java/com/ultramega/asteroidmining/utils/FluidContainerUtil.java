package com.ultramega.asteroidmining.utils;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

public final class FluidContainerUtil {
    private FluidContainerUtil() {
    }

    public static void renderFluidTank(final GuiGraphicsExtractor graphics,
                                       final AbstractContainerScreen<?> screen,
                                       final FluidStack stack, final int capacity,
                                       final int x,
                                       final int y,
                                       final int width,
                                       final int height,
                                       final int movementY) {
        renderFluidTank(graphics, screen, stack, stack.getAmount(), capacity, x, y, width, height, movementY);
    }

    public static void renderFluidTank(final GuiGraphicsExtractor graphics,
                                       final AbstractContainerScreen<?> screen,
                                       final FluidStack stack,
                                       final int amount,
                                       final int capacity,
                                       final int x,
                                       final int y,
                                       final int width,
                                       final int height,
                                       final int movementY) {
        if (!stack.isEmpty() && capacity > 0) {
            final int density = stack.getFluid().getFluidType().getDensity(stack);
            final int fluidHeight = Math.min(height * amount / capacity, height);

            if (density < 0) {
                // Gas-like fluid: fill from top down
                final int drawY = y + movementY;
                renderTiledFluid(graphics, screen, stack, x, drawY, width, fluidHeight);
            } else {
                // Liquid: fill from bottom up
                final int maxY = y + height - movementY;
                final int drawY = maxY - fluidHeight;
                renderTiledFluid(graphics, screen, stack, x, drawY, width, fluidHeight);
            }
        }
    }

    public static void renderTiledFluid(final GuiGraphicsExtractor graphics,
                                        final AbstractContainerScreen<?> screen,
                                        final FluidStack stack,
                                        final int x,
                                        final int y,
                                        final int width,
                                        final int height) {
        renderTiledFluid(graphics, stack, screen.getLeftPos(), screen.getTopPos(), x, y, width, height);
    }

    public static void renderTiledFluid(final GuiGraphicsExtractor graphics,
                                        final FluidStack stack,
                                        final int guiLeft,
                                        final int guiTop,
                                        final int x,
                                        final int y,
                                        final int width,
                                        final int height) {
        if (!stack.isEmpty()) {
            final FluidStateModelSet fluidStateModelSet = Minecraft.getInstance().getModelManager().getFluidStateModelSet();
            final FluidModel fluidModel = fluidStateModelSet.get(stack.getFluid().defaultFluidState());
            final TextureAtlasSprite sprite = getFluidSprite(fluidModel);
            final int tint = getTint(fluidModel, stack);
            renderTiledTextureAtlas(graphics, sprite, guiLeft, guiTop, x, y, width, height); //TODO fluid rendering in gui
        }
    }

    public static void renderTiledTextureAtlas(final GuiGraphicsExtractor graphics,
                                               final TextureAtlasSprite sprite,
                                               final int guiLeft,
                                               final int guiTop,
                                               final int x,
                                               final int y,
                                               final int width,
                                               final int height) {
//        // start drawing sprites
//        bindTexture(sprite.atlasLocation());
//
//        final int spriteHeight = sprite.contents().height();
//        final int spriteWidth = sprite.contents().width();
//        // tile vertically
//        final int startX = x + guiLeft;
//        final int startY = y + guiTop;
//
//        final Matrix4f matrix = graphics.pose().last().pose();
//
//        final int xTileCount = width / spriteWidth;
//        final int xRemainder = width - (xTileCount * spriteWidth);
//        final long yTileCount = height / spriteHeight;
//        final long yRemainder = height - (yTileCount * spriteHeight);
//
//        for (int tileX = 0; tileX <= xTileCount; tileX++) {
//            for (int tileY = 0; tileY <= yTileCount; tileY++) {
//                final int widthLeft = (tileX == xTileCount) ? xRemainder : spriteWidth;
//                final long heightLeft = (tileY == yTileCount) ? yRemainder : spriteHeight;
//                final int x2 = startX + (tileX * spriteWidth);
//                final int y2 = startY + height - ((tileY + 1) * spriteHeight);
//                if (widthLeft > 0 && heightLeft > 0) {
//                    final long maskTop = spriteHeight - heightLeft;
//                    final int maskRight = spriteWidth - widthLeft;
//
//                    drawTextureWithMasking(matrix, x2, y2, sprite, maskTop, maskRight, 100);
//                }
//            }
//        }
    }

    private static void drawTextureWithMasking(final Matrix4f matrix,
                                               final float coordX,
                                               final float coordY,
                                               final TextureAtlasSprite textureSprite,
                                               final long maskTop,
                                               final long maskRight,
                                               final float levelZ) {
//        final float minU = textureSprite.getU0();
//        float maxU = textureSprite.getU1();
//        final float minV = textureSprite.getV0();
//        float maxV = textureSprite.getV1();
//        maxU = maxU - (maskRight / 16F * (maxU - minU));
//        maxV = maxV - (maskTop / 16F * (maxV - minV));
//
//        RenderSystem.setShader(GameRenderer::getPositionTexShader);
//
//        final BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
//        buffer.addVertex(matrix, coordX, coordY + 16, levelZ).setUv(minU, maxV);
//        buffer.addVertex(matrix, coordX + 16 - maskRight, coordY + 16, levelZ).setUv(maxU, maxV);
//        buffer.addVertex(matrix, coordX + 16 - maskRight, coordY + maskTop, levelZ).setUv(maxU, minV);
//        buffer.addVertex(matrix, coordX, coordY + maskTop, levelZ).setUv(minU, minV);
//        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static TextureAtlasSprite getFluidSprite(final FluidModel fluidModel) {
        final Material.Baked stillMaterial = fluidModel.stillMaterial();
        return stillMaterial.sprite();
    }

    private static int getTint(final FluidModel model, final FluidStack stack) {
        final FluidTintSource tintSource = model.fluidTintSource();
        if (tintSource == null) {
            return 0xFFFFFFFF;
        }
        return tintSource.colorAsStack(stack);
    }

    public static void renderFluidTooltip(final GuiGraphicsExtractor graphics, final Font font, final FluidStack stack, final int mouseX, final int mouseY) {
        final Minecraft mc = Minecraft.getInstance();
        final List<Component> tooltip = stack.getTooltipLines(Item.TooltipContext.EMPTY, mc.player, mc.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL);
        graphics.tooltip(font, Utils.createTooltip(tooltip), mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
    }
}
