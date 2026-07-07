package com.ultramega.asteroidmining.network;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.network.c2s.LaunchRocketPayload;
import com.ultramega.asteroidmining.network.c2s.OpenSaveRocketControllerPayload;
import com.ultramega.asteroidmining.network.c2s.OpenSelectConfigurationScreenPayload;
import com.ultramega.asteroidmining.network.c2s.OpenTabModulePayload;
import com.ultramega.asteroidmining.network.c2s.SelectAsteroidPayload;
import com.ultramega.asteroidmining.network.c2s.SetConfigurationStackPayload;
import com.ultramega.asteroidmining.network.c2s.SetSelectConfigurationPayload;
import com.ultramega.asteroidmining.network.c2s.SetSideConfigPayload;
import com.ultramega.asteroidmining.network.c2s.TryExtractRocketStoragePayload;
import com.ultramega.asteroidmining.network.s2c.HidePreviewBlocksPayload;
import com.ultramega.asteroidmining.network.s2c.OpenAsteroidEditScreenPayload;
import com.ultramega.asteroidmining.network.s2c.SendLaunchPreviewDataPayload;
import com.ultramega.asteroidmining.network.s2c.SetCursorPayload;
import com.ultramega.asteroidmining.network.s2c.UpdateClientConfigurationDataPayload;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber
public final class PayloadRegister {
    private PayloadRegister() {
    }

    @SubscribeEvent
    public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(AsteroidMining.MOD_ID).versioned("1.0");

        registrar.playToClient(
                OpenAsteroidEditScreenPayload.TYPE,
                OpenAsteroidEditScreenPayload.STREAM_CODEC,
                OpenAsteroidEditScreenPayload::handle
        );
        registrar.playToClient(
                UpdateClientConfigurationDataPayload.TYPE,
                UpdateClientConfigurationDataPayload.STREAM_CODEC,
                UpdateClientConfigurationDataPayload::handle
        );
        registrar.playToClient(
                SendLaunchPreviewDataPayload.TYPE,
                SendLaunchPreviewDataPayload.STREAM_CODEC,
                SendLaunchPreviewDataPayload::handle
        );
        registrar.playToClient(
                HidePreviewBlocksPayload.TYPE,
                HidePreviewBlocksPayload.STREAM_CODEC,
                HidePreviewBlocksPayload::handle
        );
        registrar.playToClient(
                SetCursorPayload.TYPE,
                SetCursorPayload.STREAM_CODEC,
                SetCursorPayload::handle
        );

        registrar.playToServer(
                LaunchRocketPayload.TYPE,
                LaunchRocketPayload.STREAM_CODEC,
                LaunchRocketPayload::handle
        );
        registrar.playToServer(
                SetConfigurationStackPayload.TYPE,
                SetConfigurationStackPayload.STREAM_CODEC,
                SetConfigurationStackPayload::handle
        );
        registrar.playToServer(
                TryExtractRocketStoragePayload.TYPE,
                TryExtractRocketStoragePayload.STREAM_CODEC,
                TryExtractRocketStoragePayload::handle
        );
        registrar.playToServer(
                OpenSaveRocketControllerPayload.TYPE,
                OpenSaveRocketControllerPayload.STREAM_CODEC,
                OpenSaveRocketControllerPayload::handle
        );
        registrar.playToServer(
                OpenSelectConfigurationScreenPayload.TYPE,
                OpenSelectConfigurationScreenPayload.STREAM_CODEC,
                OpenSelectConfigurationScreenPayload::handle
        );
        registrar.playToServer(
                SetSelectConfigurationPayload.TYPE,
                SetSelectConfigurationPayload.STREAM_CODEC,
                SetSelectConfigurationPayload::handle
        );
        registrar.playToServer(
                SelectAsteroidPayload.TYPE,
                SelectAsteroidPayload.STREAM_CODEC,
                SelectAsteroidPayload::handle
        );
        registrar.playToServer(
                OpenTabModulePayload.TYPE,
                OpenTabModulePayload.STREAM_CODEC,
                OpenTabModulePayload::handle
        );
        registrar.playToServer(
                SetSideConfigPayload.TYPE,
                SetSideConfigPayload.STREAM_CODEC,
                SetSideConfigPayload::handle
        );

        registrar.playBidirectional(
                AsteroidDataPayload.TYPE,
                AsteroidDataPayload.STREAM_CODEC,
                AsteroidDataPayload::handle,
                AsteroidDataPayload::handle
        );
    }
}
