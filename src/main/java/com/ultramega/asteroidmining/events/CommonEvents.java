package com.ultramega.asteroidmining.events;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.blockentities.BoundingBoxBlockEntity;
import com.ultramega.asteroidmining.network.AsteroidDataMessage;
import com.ultramega.asteroidmining.registry.ModBlockEntityTypes;
import com.ultramega.asteroidmining.storage.ConfigurationSavedData;
import com.ultramega.asteroidmining.utils.CameraHandler;
import com.ultramega.asteroidmining.utils.CoolantData;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

@EventBusSubscriber
public class CommonEvents {
    private CommonEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        // Sync all entries to the client
        //TODO: remove this if request response system is implemented
        if (event.getEntity().level() instanceof ServerLevel serverLevel) {
            ConfigurationSavedData.getConfigurationData(serverLevel).setDirty();
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
        CameraHandler.clearScreenShakes();
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        CameraHandler.clearScreenShakes();
    }

    @SubscribeEvent
    public static void onServerStarting(final AddServerReloadListenersEvent event) {
        AsteroidReloadListener.INSTANCE.context = event.getConditionContext();
        event.addListener(AsteroidMining.makeId("asteroids"), AsteroidReloadListener.INSTANCE);
    }

    @SubscribeEvent
    public static void onDataSync(final OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) { //TODO: is this really required?
            PacketDistributor.sendToAllPlayers(new AsteroidDataMessage(AsteroidReloadListener.INSTANCE.getData()));
        } else {
            PacketDistributor.sendToPlayer(event.getPlayer(), new AsteroidDataMessage(AsteroidReloadListener.INSTANCE.getData()));
        }
    }

    @SubscribeEvent
    public static void onCommonEvent(final FMLCommonSetupEvent event) {
        AsteroidReloadListener.loadAsteroidsFromConfig();
    }

    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.Energy.BLOCK,
            ModBlockEntityTypes.DISTILLATION_COLUMN.get(),
            (blockEntity, side) -> blockEntity.energyStorage
        );
        event.registerBlockEntity(
            Capabilities.Item.BLOCK,
            ModBlockEntityTypes.DISTILLATION_COLUMN.get(),
            (blockEntity, side) -> blockEntity.inventoryHandler
        );
        event.registerBlockEntity(
            Capabilities.Fluid.BLOCK,
            ModBlockEntityTypes.DISTILLATION_COLUMN.get(),
            (blockEntity, side) -> blockEntity.fluidTank
        );
        event.registerBlockEntity(
            Capabilities.Energy.BLOCK,
            ModBlockEntityTypes.AIR_ABSORBER.get(),
            (blockEntity, side) -> blockEntity.energyStorage
        );
        event.registerBlockEntity(
            Capabilities.Fluid.BLOCK,
            ModBlockEntityTypes.AIR_ABSORBER.get(),
            (blockEntity, side) -> blockEntity.fluidTank
        );
        event.registerBlockEntity(
            Capabilities.Energy.BLOCK,
            ModBlockEntityTypes.HEAT_EXCHANGER.get(),
            (blockEntity, side) -> blockEntity.energyStorage
        );
        event.registerBlockEntity(
            Capabilities.Fluid.BLOCK,
            ModBlockEntityTypes.HEAT_EXCHANGER.get(),
            (blockEntity, side) -> blockEntity.fluidTank
        );
        event.registerBlockEntity(
            Capabilities.Energy.BLOCK,
            ModBlockEntityTypes.ELECTROLYSIS_PLANT.get(),
            (blockEntity, side) -> blockEntity.energyStorage
        );
        event.registerBlockEntity(
            Capabilities.Fluid.BLOCK,
            ModBlockEntityTypes.ELECTROLYSIS_PLANT.get(),
            (blockEntity, side) -> blockEntity.fluidTank
        );
        event.registerBlockEntity(
            Capabilities.Energy.BLOCK,
            ModBlockEntityTypes.BIOGAS_PLANT.get(),
            (blockEntity, side) -> blockEntity.energyStorage
        );
        event.registerBlockEntity(
            Capabilities.Item.BLOCK,
            ModBlockEntityTypes.BIOGAS_PLANT.get(),
            (blockEntity, side) -> blockEntity.inventoryHandler
        );
        event.registerBlockEntity(
            Capabilities.Fluid.BLOCK,
            ModBlockEntityTypes.BIOGAS_PLANT.get(),
            (blockEntity, side) -> blockEntity.fluidTank
        ); //TODO
//        event.registerBlockEntity(
//            Capabilities.Item.BLOCK,
//            ModBlockEntityTypes.ROCKET_STORAGE_VIEWER.get(),
//            (blockEntity, side) -> blockEntity.itemFluidHandler
//        );
//        event.registerBlockEntity(
//            Capabilities.Fluid.BLOCK,
//            ModBlockEntityTypes.ROCKET_STORAGE_VIEWER.get(),
//            (blockEntity, side) -> blockEntity.itemFluidHandler
//        );
        event.registerBlockEntity(
            Capabilities.Item.BLOCK,
            ModBlockEntityTypes.ROCKET_CONTROLLER.get(),
            (blockEntity, side) -> blockEntity.inventoryHandler
        );
        event.registerBlockEntity(
            Capabilities.Item.BLOCK,
            ModBlockEntityTypes.LAUNCH_PAD_BUILDER.get(),
            (blockEntity, side) -> blockEntity.inventoryHandler
        );
        BoundingBoxBlockEntity.redirectCapability(event, Capabilities.Energy.BLOCK);
        BoundingBoxBlockEntity.redirectCapability(event, Capabilities.Fluid.BLOCK);
        BoundingBoxBlockEntity.redirectCapability(event, Capabilities.Item.BLOCK);
    }

    @SubscribeEvent
    private static void registerDataMapTypes(final RegisterDataMapTypesEvent event) {
        event.register(CoolantData.COOLANT_DATA);
    }

    /*@SubscribeEvent
    public static void registerCommands(final RegisterCommandsEvent event) {
        //TODO
        ModCommands.register(event.getDispatcher());
    }*/
}
