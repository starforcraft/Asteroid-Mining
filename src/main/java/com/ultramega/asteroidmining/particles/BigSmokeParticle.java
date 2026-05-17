package com.ultramega.asteroidmining.particles;

import com.ultramega.asteroidmining.utils.CommonUtils;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class BigSmokeParticle extends SingleQuadParticle {
    public BigSmokeParticle(final ClientLevel level,
                            final double x,
                            final double y,
                            final double z,
                            final double speedX,
                            final double speedY,
                            final double speedZ,
                            final SpriteSet sprites) {
        super(level, x, y, z, speedX, speedY, speedZ, sprites.first());

        this.setSprite(sprites.get(this.random));
        this.scale(6.0F + CommonUtils.randomOffset(this.random, 2));
        this.friction = 0.95F;
        this.lifetime = 8000;
    }

    @Override
    public void tick() {
        super.tick();
        this.quadSize += 0.003F;

        this.alpha -= 0.01F;
        if (this.alpha <= 0.0F) {
            this.remove();
        }
    }

    @Override
    public Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(final SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(final SimpleParticleType options,
                                       final ClientLevel level,
                                       final double x,
                                       final double y,
                                       final double z,
                                       final double auxX,
                                       final double auxY,
                                       final double auxZ,
                                       final RandomSource random) {
            return new BigSmokeParticle(level, x, y, z, auxX, auxY, auxZ, this.sprites);
        }
    }
}
