package com.ultramega.asteroidmining.utils;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;

public record PreviewInfo(BlockPos pos, Optional<Block> expectedBlock) {
    public static final StreamCodec<RegistryFriendlyByteBuf, PreviewInfo> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, PreviewInfo::pos,
        ByteBufCodecs.registry(BuiltInRegistries.BLOCK.key()).apply(ByteBufCodecs::optional), PreviewInfo::expectedBlock,
        PreviewInfo::new
    );
}
