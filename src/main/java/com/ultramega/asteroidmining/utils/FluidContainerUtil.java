package com.ultramega.asteroidmining.utils;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;

import static com.ultramega.asteroidmining.utils.ClientUtils.createTooltip;

public final class FluidContainerUtil {
    private FluidContainerUtil() {
    }

    public static void renderFluidTank(final GuiGraphicsExtractor graphics,
                                       final AbstractContainerScreen<?> screen,
                                       final FluidStack stack,
                                       final int capacity,
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
        if (stack.isEmpty() || capacity <= 0 || amount <= 0 || width <= 0 || height <= 0) {
            return;
        }

        final int fluidHeight = Math.min(height, (int) ((long) height * amount / capacity));
        if (fluidHeight <= 0) {
            return;
        }

        final int density = stack.getFluid().getFluidType().getDensity(stack);
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
        if (stack.isEmpty() || width <= 0 || height <= 0) {
            return;
        }

        final FluidStateModelSet fluidStateModelSet = Minecraft.getInstance().getModelManager().getFluidStateModelSet();
        final FluidModel fluidModel = fluidStateModelSet.get(stack.getFluid().defaultFluidState());
        final TextureAtlasSprite sprite = getFluidSprite(fluidModel);
        final int tint = ensureOpaque(getTint(fluidModel, stack));

        renderTiledTextureAtlas(graphics, sprite, guiLeft, guiTop, x, y, width, height, tint);
    }

    public static void renderTiledTextureAtlas(final GuiGraphicsExtractor graphics,
                                               final TextureAtlasSprite sprite,
                                               final int guiLeft,
                                               final int guiTop,
                                               final int x,
                                               final int y,
                                               final int width,
                                               final int height,
                                               final int color) {
        if (width <= 0 || height <= 0) {
            return;
        }

        final int spriteWidth = sprite.contents().width();
        final int spriteHeight = sprite.contents().height();
        if (spriteWidth <= 0 || spriteHeight <= 0) {
            return;
        }

        final int startX = guiLeft + x;
        final int startY = guiTop + y;

        final int xTileCount = width / spriteWidth;
        final int xRemainder = width - xTileCount * spriteWidth;

        final int yTileCount = height / spriteHeight;
        final int yRemainder = height - yTileCount * spriteHeight;

        for (int tileX = 0; tileX <= xTileCount; tileX++) {
            final int tileWidth = tileX == xTileCount ? xRemainder : spriteWidth;
            if (tileWidth <= 0) {
                continue;
            }

            final int drawX = startX + tileX * spriteWidth;
            final int maskRight = spriteWidth - tileWidth;

            for (int tileY = 0; tileY <= yTileCount; tileY++) {
                final int tileHeight = tileY == yTileCount ? yRemainder : spriteHeight;
                if (tileHeight <= 0) {
                    continue;
                }

                final int drawY = startY + height - (tileY + 1) * spriteHeight;
                final int maskTop = spriteHeight - tileHeight;

                drawTextureWithMasking(graphics, drawX, drawY, sprite, spriteWidth, spriteHeight, maskTop, maskRight, color);
            }
        }
    }

    private static void drawTextureWithMasking(final GuiGraphicsExtractor graphics,
                                               final int x,
                                               final int y,
                                               final TextureAtlasSprite sprite,
                                               final int spriteWidth,
                                               final int spriteHeight,
                                               final int maskTop,
                                               final int maskRight,
                                               final int color) {
        final float minU = sprite.getU0();
        float maxU = sprite.getU1();
        float minV = sprite.getV0();
        final float maxV = sprite.getV1();

        maxU -= maskRight / (float) spriteWidth * (maxU - minU);
        minV += maskTop / (float) spriteHeight * (maxV - minV);

        graphics.innerBlit(RenderPipelines.GUI_TEXTURED, sprite.atlasLocation(), x, x + spriteWidth - maskRight, y + maskTop, y + spriteHeight, minU, maxU, minV, maxV, color);
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

    private static int ensureOpaque(final int color) {
        return (color & 0xFF000000) == 0 ? 0xFF000000 | color : color;
    }

    public static void renderFluidTooltip(final GuiGraphicsExtractor graphics, final Font font, final FluidStack stack, final int mouseX, final int mouseY) {
        final List<ClientTooltipComponent> tooltip;
        if (stack.isEmpty()) {
            tooltip = createTooltip(Component.translatable("gui.asteroidmining.fluid_empty"));
        } else {
            final Minecraft mc = Minecraft.getInstance();
            final TooltipFlag flag = mc.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL;

            final List<Component> lines = stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, flag);
            lines.add(Component.translatable("gui.asteroidmining.fluid_amount", stack.getAmount()));

            tooltip = createTooltip(lines);
        }

        graphics.tooltip(font, tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
    }
}
