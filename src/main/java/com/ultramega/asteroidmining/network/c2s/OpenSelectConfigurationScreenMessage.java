package com.ultramega.asteroidmining.network.c2s;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.blockentities.RocketControllerBlockEntity;
import com.ultramega.asteroidmining.container.SelectConfigurationContainerMenu;
import com.ultramega.asteroidmining.utils.SimpleMenuProvider;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenSelectConfigurationScreenMessage(BlockPos controllerPos) implements CustomPacketPayload {
    public static final Type<OpenSelectConfigurationScreenMessage> TYPE = new Type<>(AsteroidMining.makeId("open_select_configuration_screen"));
    public static final StreamCodec<ByteBuf, OpenSelectConfigurationScreenMessage> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, OpenSelectConfigurationScreenMessage::controllerPos,
        OpenSelectConfigurationScreenMessage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final OpenSelectConfigurationScreenMessage data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            final Player player = context.player();
            if (player.level().getBlockEntity(data.controllerPos()) instanceof RocketControllerBlockEntity blockEntity) {
                player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, player2) ->
                        new SelectConfigurationContainerMenu(containerId, playerInventory, blockEntity,
                            ContainerLevelAccess.create(player.level(), data.controllerPos()),
                            blockEntity.nextLaunchCooldown, blockEntity.launchCooldownOverlay, blockEntity.launchCooldownCommentator),
                    Component.translatable("gui.asteroidmining.select_configuration.title")
                ), (buf) -> {
                    buf.writeBlockPos(data.controllerPos());
                    buf.writeInt(blockEntity.nextLaunchCooldown);
                    buf.writeBoolean(blockEntity.launchCooldownOverlay);
                    buf.writeBoolean(blockEntity.launchCooldownCommentator);
                });
            }
        });
    }
}
