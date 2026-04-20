package com.ultramega.asteroidmining.mixin;

import com.ultramega.asteroidmining.utils.CameraHandler;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera {
    @Inject(method = "update", at = @At("RETURN"))
    public void update(final CallbackInfo ci) {
        final Entity entity = this.entity();
        if (entity != null) {
            CameraHandler.cameraTick((Camera) (Object) this, entity.getRandom());
        }
    }

    @Shadow
    public abstract @Nullable Entity entity();
}
