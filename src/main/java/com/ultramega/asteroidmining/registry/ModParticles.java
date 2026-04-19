package com.ultramega.asteroidmining.registry;

import com.ultramega.asteroidmining.AsteroidMining;

import java.util.function.Supplier;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, AsteroidMining.MOD_ID);

    public static final Supplier<SimpleParticleType> BIG_SMOKE_PARTICLE = PARTICLE_TYPES.register("big_smoke_particle", () ->
        new SimpleParticleType(false));

    private ModParticles() {
    }
}
