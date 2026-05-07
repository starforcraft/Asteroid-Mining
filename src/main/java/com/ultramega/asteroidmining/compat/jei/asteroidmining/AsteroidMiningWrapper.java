package com.ultramega.asteroidmining.compat.jei.asteroidmining;

import com.ultramega.asteroidmining.asteroids.AsteroidConfig;
import com.ultramega.asteroidmining.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.recipe.category.extensions.IRecipeCategoryExtension;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public class AsteroidMiningWrapper implements IRecipeCategoryExtension<AsteroidConfig> { //TODO: put this into the Recipe Category instead? Or should it stay separated?
    @Override
    public void drawInfo(final AsteroidConfig asteroid,
                         final int recipeWidth,
                         final int recipeHeight,
                         final GuiGraphicsExtractor graphics,
                         final double mouseX,
                         final double mouseY) {
        final int size = 32;
        graphics.blitSprite(GUI_TEXTURED, asteroid.getTexture(), size, size, 0, 0, (recipeWidth - size) / 2 - 40, (recipeHeight - size) / 2, size, size);
    }

    public List<Component> getTooltip(final AsteroidConfig asteroid,
                                      final int recipeWidth,
                                      final int recipeHeight,
                                      final double mouseX,
                                      final double mouseY) {
        final List<Component> tooltip = new ArrayList<>();
        final int size = 32;

        if (Utils.isMouseOver((recipeWidth / 2 - size) / 2, (recipeHeight - size) / 2, size, size, mouseX, mouseY)) {
            tooltip.add(Component.literal(asteroid.getName()));
        }

        return tooltip;
    }
}
