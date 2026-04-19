package com.ultramega.asteroidmining.mixin;

import com.ultramega.asteroidmining.blocks.BoundingBoxBlock;
import com.ultramega.asteroidmining.registry.ModBlocks;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class MixinClientLevel extends Level {
    @Shadow
    @Final
    private LevelRenderer levelRenderer;

    protected MixinClientLevel(final WritableLevelData levelData,
                               final ResourceKey<Level> dimension,
                               final RegistryAccess registryAccess,
                               final Holder<DimensionType> dimensionTypeRegistration,
                               final boolean isClientSide,
                               final boolean isDebug,
                               final long biomeZoomSeed,
                               final int maxChainedNeighborUpdates) {
        super(levelData, dimension, registryAccess, dimensionTypeRegistration, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }

    @Inject(method = "destroyBlockProgress", at = @At("TAIL"))
    public void destroyBlockProgress(final int breakerId, final BlockPos pos, final int progress, final CallbackInfo ci) {
        if (this.getBlockState(pos).is(ModBlocks.BOUNDING_BOX.get())) {
            final BlockPos mainPos = BoundingBoxBlock.getMainBlockPos(this, pos);
            if (mainPos != null) {
                this.levelRenderer.destroyBlockProgress(breakerId, mainPos, progress);
            }
        }
    }
}
