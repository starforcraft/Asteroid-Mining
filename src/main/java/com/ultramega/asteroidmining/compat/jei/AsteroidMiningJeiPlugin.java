package com.ultramega.asteroidmining.compat.jei;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.compat.jei.asteroidmining.AsteroidMiningRecipeCategory;
import com.ultramega.asteroidmining.events.AsteroidReloadListener;
import com.ultramega.asteroidmining.gui.RocketControllerConfigurationScreen;
import com.ultramega.asteroidmining.gui.RocketControllerScreen;

import java.util.List;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;

@JeiPlugin
public final class AsteroidMiningJeiPlugin implements IModPlugin {
    private static final Identifier PLUGIN_UID = Identifier.fromNamespaceAndPath(AsteroidMining.MOD_ID, AsteroidMining.MOD_ID);

    @Override
    public void registerCategories(final IRecipeCategoryRegistration registration) {
        final IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();

        registration.addRecipeCategories(new AsteroidMiningRecipeCategory(guiHelper));
    }

    @Override
    public void registerRecipeCatalysts(final IRecipeCatalystRegistration registration) {
        //TODO: decide if we want to show the engines
        //registration.addRecipeCatalyst(new ItemStack(ModItems.RS25_ENGINE.get()), AsteroidMiningRecipeCategory.ASTEROID_MINING_TYPE);
    }

    @Override
    public void registerRecipes(final IRecipeRegistration registration) {
        registration.addRecipes(AsteroidMiningRecipeCategory.ASTEROID_MINING_TYPE, AsteroidReloadListener.INSTANCE.getData().values().stream().toList());
    }

    @Override
    public void registerGuiHandlers(final IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(RocketControllerScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(final RocketControllerScreen screen) {
                return List.of(new Rect2i(screen.getLeftPos() + screen.getImageWidth() + 3, screen.getTopPos() + 40, 97, 112));
            }
        });
        registration.addGuiContainerHandler(RocketControllerConfigurationScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(final RocketControllerConfigurationScreen screen) {
                return List.of(new Rect2i(screen.getLeftPos() + screen.getImageWidth() + 3, screen.getTopPos() + 30, 97, 112));
            }
        });
    }

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_UID;
    }
}
