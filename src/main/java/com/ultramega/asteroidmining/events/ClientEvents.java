package com.ultramega.asteroidmining.events;

import com.ultramega.asteroidmining.blockentities.renderer.RocketEngineBlockEntityRenderer;
import com.ultramega.asteroidmining.blocks.RocketEngineBlock;
import com.ultramega.asteroidmining.camera.CameraHandler;
import com.ultramega.asteroidmining.entities.renderer.BlockStructureEntityRenderer;
import com.ultramega.asteroidmining.gui.AirAbsorberScreen;
import com.ultramega.asteroidmining.gui.BiogasPlantScreen;
import com.ultramega.asteroidmining.gui.DistillationColumnScreen;
import com.ultramega.asteroidmining.gui.ElectrolysisPlantScreen;
import com.ultramega.asteroidmining.gui.HeatExchangerScreen;
import com.ultramega.asteroidmining.gui.LaunchPadBuilderScreen;
import com.ultramega.asteroidmining.gui.ObservatoryScreen;
import com.ultramega.asteroidmining.gui.RocketControllerConfigurationScreen;
import com.ultramega.asteroidmining.gui.RocketControllerScreen;
import com.ultramega.asteroidmining.gui.RocketStorageViewerScreen;
import com.ultramega.asteroidmining.gui.renderer.ScenePictureInPictureRenderer;
import com.ultramega.asteroidmining.particles.BigSmokeParticle;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.registry.ModEntityTypes;
import com.ultramega.asteroidmining.registry.ModFluids;
import com.ultramega.asteroidmining.registry.ModMenuTypes;
import com.ultramega.asteroidmining.registry.ModParticles;
import com.ultramega.asteroidmining.registry.ModRenderPipelines;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void cancelRenderingGuiLayers(final RenderGuiLayerEvent.Pre event) {
        if (CameraHandler.isCameraOutsideOfPlayer()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void cancelRenderingHand(final RenderHandEvent event) {
        if (CameraHandler.isCameraOutsideOfPlayer()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void registerScreens(final RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.DISTILLATION_COLUMN.get(), DistillationColumnScreen::new);
        event.register(ModMenuTypes.AIR_ABSORBER.get(), AirAbsorberScreen::new);
        event.register(ModMenuTypes.HEAT_EXCHANGER.get(), HeatExchangerScreen::new);
        event.register(ModMenuTypes.ELECTROLYSIS_PLANT.get(), ElectrolysisPlantScreen::new);
        event.register(ModMenuTypes.BIOGAS_PLANT.get(), BiogasPlantScreen::new);
        event.register(ModMenuTypes.ROCKET_CONTROLLER.get(), RocketControllerScreen::new);
        event.register(ModMenuTypes.SELECT_CONFIGURATION.get(), RocketControllerConfigurationScreen::new);
        event.register(ModMenuTypes.ROCKET_STORAGE_VIEWER.get(), RocketStorageViewerScreen::new);
        event.register(ModMenuTypes.LAUNCH_PAD_BUILDER.get(), LaunchPadBuilderScreen::new);
        event.register(ModMenuTypes.SMALL_OBSERVATORY.get(), ObservatoryScreen::new);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        for (final RocketEngineBlock.Type type : RocketEngineBlock.Type.values()) {
            event.registerBlockEntityRenderer(type.getBlockEntity().get(), (ctx) -> new RocketEngineBlockEntityRenderer());
        }
        event.registerEntityRenderer(ModEntityTypes.BLOCK_STRUCTURE_ENTITY.get(), BlockStructureEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(final RegisterClientExtensionsEvent event) {
        event.registerFluidType(ModFluids.STILL_EXTENSION.apply("air"), ModFluids.AIR_TYPE.get());
        event.registerFluidType(ModFluids.STILL_EXTENSION.apply("liquid_air"), ModFluids.LIQUID_AIR_TYPE.get());
        event.registerFluidType(ModFluids.STILL_EXTENSION.apply("liquid_oxygen"), ModFluids.LIQUID_OXYGEN_TYPE.get());
        event.registerFluidType(ModFluids.STILL_EXTENSION.apply("methane"), ModFluids.METHANE_TYPE.get());
        event.registerFluidType(ModFluids.STILL_EXTENSION.apply("liquid_methane"), ModFluids.LIQUID_METHANE_TYPE.get());
        event.registerFluidType(ModFluids.STILL_EXTENSION.apply("hydrogen"), ModFluids.HYDROGEN_TYPE.get());
        event.registerFluidType(ModFluids.STILL_EXTENSION.apply("liquid_hydrogen"), ModFluids.LIQUID_HYDROGEN_TYPE.get());
        event.registerFluidType(ModFluids.LIQUID_EXTENSION.apply("petroleum"), ModFluids.PETROLEUM_TYPE.get());
        event.registerFluidType(ModFluids.LIQUID_EXTENSION.apply("kerosene"), ModFluids.KEROSENE_TYPE.get());
        event.registerBlock(ModBlocks.crack(), ModBlocks.BOUNDING_BOX.get());
    }

    @SubscribeEvent
    public static void registerFluidModels(final RegisterFluidModelsEvent event) {
        event.register(ModFluids.NON_PLACEABLE_FLUID_UNBAKED_MODEL.apply("air"), ModFluids.AIR.get());
        event.register(ModFluids.NON_PLACEABLE_FLUID_UNBAKED_MODEL.apply("liquid_oxygen"), ModFluids.LIQUID_OXYGEN.get());
        event.register(ModFluids.NON_PLACEABLE_FLUID_UNBAKED_MODEL.apply("liquid_air"), ModFluids.LIQUID_AIR.get());
        event.register(ModFluids.NON_PLACEABLE_FLUID_UNBAKED_MODEL.apply("methane"), ModFluids.METHANE.get());
        event.register(ModFluids.NON_PLACEABLE_FLUID_UNBAKED_MODEL.apply("liquid_methane"), ModFluids.LIQUID_METHANE.get());
        event.register(ModFluids.NON_PLACEABLE_FLUID_UNBAKED_MODEL.apply("hydrogen"), ModFluids.HYDROGEN.get());
        event.register(ModFluids.NON_PLACEABLE_FLUID_UNBAKED_MODEL.apply("liquid_hydrogen"), ModFluids.LIQUID_HYDROGEN.get());
        event.register(ModFluids.NON_PLACEABLE_FLUID_UNBAKED_MODEL.apply("rocket_propellant"), ModFluids.ROCKET_PROPELLANT.get());
        event.register(ModFluids.FLUID_UNBAKED_MODEL.apply("petroleum"), ModFluids.PETROLEUM_SOURCE.get(), ModFluids.PETROLEUM_FLOWING.get());
        event.register(ModFluids.FLUID_UNBAKED_MODEL.apply("kerosene"), ModFluids.KEROSENE_SOURCE.get(), ModFluids.KEROSENE_FLOWING.get());
    }

    @SubscribeEvent
    public static void registerParticleProviders(final RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.BIG_SMOKE_PARTICLE.get(), BigSmokeParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerPipRenderers(final RegisterPictureInPictureRenderersEvent event) {
        event.register(ScenePictureInPictureRenderer.State.class, ScenePictureInPictureRenderer::new);
    }

    @SubscribeEvent
    public static void registerPipelines(final RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ModRenderPipelines.ORBIT_LINES);
        event.registerPipeline(ModRenderPipelines.ROCKET_FLAME);
    }
}
