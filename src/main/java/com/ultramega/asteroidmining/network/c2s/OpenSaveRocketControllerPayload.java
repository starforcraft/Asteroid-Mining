package com.ultramega.asteroidmining.network.c2s;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.blockentities.RocketControllerBlockEntity;
import com.ultramega.asteroidmining.container.RocketControllerContainerMenu;
import com.ultramega.asteroidmining.utils.SimpleMenuProvider;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenSaveRocketControllerPayload(BlockPos controllerPos, int launchCooldown, boolean launchCooldownOverlay, boolean launchCooldownCommentator)
    implements CustomPacketPayload {
    public static final Type<OpenSaveRocketControllerPayload> TYPE = new Type<>(AsteroidMining.makeId("open_save_rocket_controller"));
    public static final StreamCodec<ByteBuf, OpenSaveRocketControllerPayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, OpenSaveRocketControllerPayload::controllerPos,
        ByteBufCodecs.INT, OpenSaveRocketControllerPayload::launchCooldown,
        ByteBufCodecs.BOOL, OpenSaveRocketControllerPayload::launchCooldownOverlay,
        ByteBufCodecs.BOOL, OpenSaveRocketControllerPayload::launchCooldownCommentator,
        OpenSaveRocketControllerPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final OpenSaveRocketControllerPayload data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            final Player player = context.player();
            if (player.level().getBlockEntity(data.controllerPos()) instanceof RocketControllerBlockEntity blockEntity) {
                blockEntity.nextLaunchCooldown = data.launchCooldown();
                blockEntity.launchCooldownOverlay = data.launchCooldownOverlay();
                blockEntity.launchCooldownCommentator = data.launchCooldownCommentator();
                blockEntity.setChanged();

                player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, player2) ->
                        new RocketControllerContainerMenu(containerId, playerInventory, blockEntity,
                            ContainerLevelAccess.create(player.level(), data.controllerPos())),
                    blockEntity.getDisplayName()
                ), data.controllerPos());
            }
        });
    }
}
