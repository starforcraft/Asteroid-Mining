package com.ultramega.asteroidmining.registry;

import com.ultramega.asteroidmining.AsteroidMining;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, AsteroidMining.MOD_ID);

    //TODO: improve sounds
    public static final Holder<SoundEvent> ROCKET_START = SOUND_EVENTS.register("rocket_start", SoundEvent::createVariableRangeEvent);

    public static final Holder<SoundEvent> LAUNCH_T_MINUS = SOUND_EVENTS.register("launch_t_minus", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> COUNTDOWN = SOUND_EVENTS.register("launch_countdown", SoundEvent::createVariableRangeEvent);

    private ModSounds() {
    }
}
