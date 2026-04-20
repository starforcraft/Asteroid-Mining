package com.ultramega.asteroidmining.compat.jei.asteroidmining;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.utils.AsteroidConfig;
import com.ultramega.asteroidmining.utils.ItemFluidStack;

import java.util.List;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.placement.VerticalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IScrollGridWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public class AsteroidMiningRecipeCategory implements IRecipeCategory<AsteroidConfig> {
    public static final IRecipeType<AsteroidConfig> ASTEROID_MINING_TYPE = IRecipeType.create(AsteroidMining.MOD_ID, "asteroid_mining", AsteroidConfig.class);
    private static final Identifier BACKGROUND = AsteroidMining.makeId("textures/gui/jei/asteroid_mining.png");

    private final IDrawable icon;
    private final AsteroidMiningWrapper recipeCategoryExtension;

    public AsteroidMiningRecipeCategory(final IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.RS25_ENGINE)); //TODO: change icon?
        this.recipeCategoryExtension = new AsteroidMiningWrapper();
    }

    @Override
    public void setRecipe(final IRecipeLayoutBuilder builder, final AsteroidConfig recipe, final IFocusGroup focuses) {
        for (int i = 0; i < recipe.getCompositionStacks().size(); i++) {
            final ItemFluidStack stack = recipe.getCompositionStacks().get(i).copyWithCount(1);

            if (stack.getItemStackTemplate() != null) {
                builder.addOutputSlot(0, 0)
                    .add(stack.getItemStackTemplate());
            } else if (stack.getFluidStackTemplate() != null) {
                builder.addOutputSlot(0, 0)
                    .add(stack.getFluidStackTemplate().fluid().value());
            }
        }
    }

    @Override
    public void createRecipeExtras(final IRecipeExtrasBuilder builder, final AsteroidConfig recipe, final IFocusGroup focuses) {
        final List<IRecipeSlotDrawable> outputSlots = builder.getRecipeSlots().getSlots(RecipeIngredientRole.OUTPUT);

        final IScrollGridWidget scrollGridWidget = builder.addScrollGridWidget(outputSlots, 2, 4);
        scrollGridWidget.setPosition(37, 0, this.getWidth(), this.getHeight(), HorizontalAlignment.CENTER, VerticalAlignment.BOTTOM);
    }

    @Override
    public void draw(final AsteroidConfig recipe, final IRecipeSlotsView recipeSlotsView, final GuiGraphicsExtractor graphics, final double mouseX, final double mouseY) {
        graphics.blit(GUI_TEXTURED, BACKGROUND, 0, 0, 0, 0, this.getWidth(), this.getHeight(), 256, 256);
        this.recipeCategoryExtension.drawInfo(recipe, this.getWidth(), this.getHeight(), graphics, mouseX, mouseY);
    }

    @Override
    public void getTooltip(final ITooltipBuilder tooltip, final AsteroidConfig recipe, final IRecipeSlotsView recipeSlotsView, final double mouseX, final double mouseY) {
        tooltip.addAll(this.recipeCategoryExtension.getTooltip(recipe, this.getWidth(), this.getHeight(), mouseX, mouseY));
    }

    @Override
    public IRecipeType<AsteroidConfig> getRecipeType() {
        return ASTEROID_MINING_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("itemGroup.asteroidmining");
    }

    @Override
    public int getWidth() {
        return 126;
    }

    @Override
    public int getHeight() {
        return 70;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }
}
