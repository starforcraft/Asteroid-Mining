package com.ultramega.asteroidmining.network.c2s;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.blockentities.LaunchPadBuilderBlockEntity;
import com.ultramega.asteroidmining.network.s2c.SendLaunchPreviewDataMessage;
import com.ultramega.asteroidmining.registry.ModDataComponentTypes;
import com.ultramega.asteroidmining.storage.ConfigurationSavedData;
import com.ultramega.asteroidmining.storage.LaunchPadConfiguration;
import com.ultramega.asteroidmining.storage.ModuleProperties;
import com.ultramega.asteroidmining.storage.NetworkConfiguration;
import com.ultramega.asteroidmining.utils.PreviewInfo;
import com.ultramega.asteroidmining.utils.Utils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.transfer.item.ItemResource;

public record SetConfigurationStackMessage(BlockPos launchPadBuilderPos, LaunchPadConfiguration launchPadConfiguration) implements CustomPacketPayload {
    public static final Type<SetConfigurationStackMessage> TYPE = new Type<>(AsteroidMining.makeId("set_configuration_stack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetConfigurationStackMessage> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SetConfigurationStackMessage::launchPadBuilderPos,
        LaunchPadConfiguration.STREAM_CODEC, SetConfigurationStackMessage::launchPadConfiguration,
        SetConfigurationStackMessage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SetConfigurationStackMessage data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            final Level level = context.player().level();
            if (!(level instanceof ServerLevel serverLevel)) {
                return;
            }

            final BlockEntity blockEntity = level.getBlockEntity(data.launchPadBuilderPos());
            if (blockEntity instanceof LaunchPadBuilderBlockEntity launchPadBlockEntity) {
                final ItemResource resource = launchPadBlockEntity.inventoryHandler.getResource(0);

                final UUID uuid;
                if (!resource.has(ModDataComponentTypes.CONFIGURATION_PATH_DATA.get())) {
                    uuid = UUID.randomUUID();
                    final ItemResource newResource = resource.with(ModDataComponentTypes.CONFIGURATION_PATH_DATA.get(), uuid);

                    if (newResource != resource) {
                        launchPadBlockEntity.inventoryHandler.set(0, newResource, launchPadBlockEntity.inventoryHandler.getAmountAsInt(0));
                    }
                } else {
                    uuid = resource.get(ModDataComponentTypes.CONFIGURATION_PATH_DATA.get());
                }
                ConfigurationSavedData.getConfigurationData(serverLevel).set(uuid,
                    new NetworkConfiguration(data.launchPadConfiguration(), Optional.empty(), new ModuleProperties(Optional.empty(), NonNullList.create())));
                final List<PreviewInfo> previewInfos = Utils.calculateSpacePort(level, data.launchPadConfiguration(), true);
                PacketDistributor.sendToAllPlayers(new SendLaunchPreviewDataMessage(data.launchPadBuilderPos(), uuid, previewInfos));
                launchPadBlockEntity.setChanged();
            }
        });
    }
}
