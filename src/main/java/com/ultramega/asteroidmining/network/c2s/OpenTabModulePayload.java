package com.ultramega.asteroidmining.network.c2s;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.blockentities.AbstractModuleBlockEntity;
import com.ultramega.asteroidmining.network.s2c.SetCursorPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenTabModulePayload(BlockPos pos, int cursorX, int cursorY) implements CustomPacketPayload {
    public static final Type<OpenTabModulePayload> TYPE = new Type<>(AsteroidMining.makeId("open_tab_module"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTabModulePayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, OpenTabModulePayload::pos,
        ByteBufCodecs.INT, OpenTabModulePayload::cursorX,
        ByteBufCodecs.INT, OpenTabModulePayload::cursorY,
        OpenTabModulePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final OpenTabModulePayload data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            final Player player = context.player();
            if (player.level().getBlockEntity(data.pos()) instanceof MenuProvider menu && menu instanceof AbstractModuleBlockEntity module) {
                module.setOverwriteStillValid(true);

                player.openMenu(menu, data.pos());
                
                if (player instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, new SetCursorPayload(data.cursorX(), data.cursorY()));
                }
            }
        });
    }
}
