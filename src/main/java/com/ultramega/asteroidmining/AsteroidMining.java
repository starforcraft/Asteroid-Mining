package com.ultramega.asteroidmining;

import com.ultramega.asteroidmining.config.ClientConfig;
import com.ultramega.asteroidmining.config.ServerConfig;
import com.ultramega.asteroidmining.launch.RocketLaunchManager;
import com.ultramega.asteroidmining.registry.ModBlockEntityTypes;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.registry.ModCreativeTabs;
import com.ultramega.asteroidmining.registry.ModDataComponentTypes;
import com.ultramega.asteroidmining.registry.ModEntityDataSerializers;
import com.ultramega.asteroidmining.registry.ModEntityTypes;
import com.ultramega.asteroidmining.registry.ModFluids;
import com.ultramega.asteroidmining.registry.ModItems;
import com.ultramega.asteroidmining.registry.ModMenuTypes;
import com.ultramega.asteroidmining.registry.ModParticles;
import com.ultramega.asteroidmining.registry.ModRecipeTypes;
import com.ultramega.asteroidmining.registry.ModSounds;

import com.mojang.logging.LogUtils;
import guideme.Guide;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(AsteroidMining.MOD_ID)
public final class AsteroidMining {
    public static final String MOD_ID = "asteroidmining";

    public static final Logger LOGGER = LogUtils.getLogger();

    public AsteroidMining(final IEventBus modEventBus, final ModContainer modContainer) {
        NeoForge.EVENT_BUS.addListener(RocketLaunchManager::onServerTick);

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModDataComponentTypes.DATA_COMPONENT_TYPE.register(modEventBus);
        ModEntityDataSerializers.ENTITY_DATA_SERIALIZERS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModRecipeTypes.RECIPE_TYPES.register(modEventBus);
        ModRecipeTypes.RECIPE_SERIALIZERS.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            // TODO: add lang entries for all the configs
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }

        this.createGuide();
    }

    private void createGuide() {
        Guide.builder(makeId("guide"))
            .build();
    }

    public static Identifier makeId(final String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
