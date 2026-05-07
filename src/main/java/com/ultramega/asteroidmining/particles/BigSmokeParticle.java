package com.ultramega.asteroidmining.particles;

import com.ultramega.asteroidmining.utils.Utils;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BaseAshSmokeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class BigSmokeParticle extends BaseAshSmokeParticle {
    public BigSmokeParticle(final ClientLevel level,
                            final double x,
                            final double y,
                            final double z,
                            final double xa,
                            final double ya,
                            final double za,
                            final float scale,
                            final SpriteSet sprites) {
        super(level, x, y, z, 0.1F, 0.1F, 0.1F, xa, ya, za, scale, sprites, 0.3F, 8, -0.1F, true);

        this.setSprite(sprites.get(this.random));
        this.scale(6.0F + Utils.randomOffset(this.random, 2));
        this.friction = 0.95F;
        this.lifetime = 10000;
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
            return new BigSmokeParticle(level, x, y, z, auxX, auxY, auxZ, 1.0F, this.sprites);
        }
    }
}
