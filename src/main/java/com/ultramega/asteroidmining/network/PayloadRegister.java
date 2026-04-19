package com.ultramega.asteroidmining.network;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.network.c2s.LaunchRocketMessage;
import com.ultramega.asteroidmining.network.c2s.OpenSaveRocketControllerMessage;
import com.ultramega.asteroidmining.network.c2s.OpenSelectConfigurationScreenMessage;
import com.ultramega.asteroidmining.network.c2s.OpenTabModuleMessage;
import com.ultramega.asteroidmining.network.c2s.SelectAsteroidMessage;
import com.ultramega.asteroidmining.network.c2s.SetConfigurationStackMessage;
import com.ultramega.asteroidmining.network.c2s.SetSelectConfigurationMessage;
import com.ultramega.asteroidmining.network.c2s.TryExtractRocketStorageMessage;
import com.ultramega.asteroidmining.network.s2c.HidePreviewBlocksMessage;
import com.ultramega.asteroidmining.network.s2c.OpenAsteroidEditScreenMessage;
import com.ultramega.asteroidmining.network.s2c.SendLaunchPreviewDataMessage;
import com.ultramega.asteroidmining.network.s2c.SetCursorMessage;
import com.ultramega.asteroidmining.network.s2c.UpdateClientConfigurationDataMessage;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber
public class PayloadRegister {
    private PayloadRegister() {
    }

    @SubscribeEvent
    public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(AsteroidMining.MOD_ID).versioned("1.0");
        registrar.playToClient(
            OpenAsteroidEditScreenMessage.TYPE,
            OpenAsteroidEditScreenMessage.STREAM_CODEC,
            OpenAsteroidEditScreenMessage::handle
        );
        registrar.playToClient(
            UpdateClientConfigurationDataMessage.TYPE,
            UpdateClientConfigurationDataMessage.STREAM_CODEC,
            UpdateClientConfigurationDataMessage::handle
        );
        registrar.playToClient(
            SendLaunchPreviewDataMessage.TYPE,
            SendLaunchPreviewDataMessage.STREAM_CODEC,
            SendLaunchPreviewDataMessage::handle
        );
        registrar.playToClient(
            HidePreviewBlocksMessage.TYPE,
            HidePreviewBlocksMessage.STREAM_CODEC,
            HidePreviewBlocksMessage::handle
        );
        registrar.playToClient(
            SetCursorMessage.TYPE,
            SetCursorMessage.STREAM_CODEC,
            SetCursorMessage::handle
        );

        registrar.playToServer(
            LaunchRocketMessage.TYPE,
            LaunchRocketMessage.STREAM_CODEC,
            LaunchRocketMessage::handle
        );
        registrar.playToServer(
            SetConfigurationStackMessage.TYPE,
            SetConfigurationStackMessage.STREAM_CODEC,
            SetConfigurationStackMessage::handle
        );
        registrar.playToServer(
            TryExtractRocketStorageMessage.TYPE,
            TryExtractRocketStorageMessage.STREAM_CODEC,
            TryExtractRocketStorageMessage::handle
        );
        registrar.playToServer(
            OpenSaveRocketControllerMessage.TYPE,
            OpenSaveRocketControllerMessage.STREAM_CODEC,
            OpenSaveRocketControllerMessage::handle
        );
        registrar.playToServer(
            OpenSelectConfigurationScreenMessage.TYPE,
            OpenSelectConfigurationScreenMessage.STREAM_CODEC,
            OpenSelectConfigurationScreenMessage::handle
        );
        registrar.playToServer(
            SetSelectConfigurationMessage.TYPE,
            SetSelectConfigurationMessage.STREAM_CODEC,
            SetSelectConfigurationMessage::handle
        );
        registrar.playToServer(
            SelectAsteroidMessage.TYPE,
            SelectAsteroidMessage.STREAM_CODEC,
            SelectAsteroidMessage::handle
        );
        registrar.playToServer(
            OpenTabModuleMessage.TYPE,
            OpenTabModuleMessage.STREAM_CODEC,
            OpenTabModuleMessage::handle
        );

        registrar.playBidirectional(
            AsteroidDataMessage.TYPE,
            AsteroidDataMessage.STREAM_CODEC,
            AsteroidDataMessage::handle,
            AsteroidDataMessage::handle
        );
    }
}
