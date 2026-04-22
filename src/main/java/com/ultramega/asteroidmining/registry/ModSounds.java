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
    public static final Holder<SoundEvent> LAUNCH_10 = SOUND_EVENTS.register("launch_ten", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> LAUNCH_9 = SOUND_EVENTS.register("launch_nine", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> LAUNCH_8 = SOUND_EVENTS.register("launch_eight", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> LAUNCH_7 = SOUND_EVENTS.register("launch_seven", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> LAUNCH_6 = SOUND_EVENTS.register("launch_six", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> LAUNCH_5 = SOUND_EVENTS.register("launch_five", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> LAUNCH_4 = SOUND_EVENTS.register("launch_four", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> LAUNCH_3 = SOUND_EVENTS.register("launch_three", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> LAUNCH_2 = SOUND_EVENTS.register("launch_two", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> LAUNCH_1 = SOUND_EVENTS.register("launch_one", SoundEvent::createVariableRangeEvent);

    private ModSounds() {
    }
}
