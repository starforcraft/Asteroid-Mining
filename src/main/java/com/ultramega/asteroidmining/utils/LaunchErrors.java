package com.ultramega.asteroidmining.utils;

// TODO: implement this into NetworkConfiguration
public enum LaunchErrors {
    WRONG_BLOCK,
    UNMOVABLE_BLOCK,
    NO_DESTINATION_SELECTED,
    ROCKET_HAS_AIR_GAP,
    NOT_ENOUGH_THRUST_FORCE, // or TOO_HEAVY
    NOT_ENOUGH_FUEL,
    MISSING_ENGINE,
    MISSING_ITEM_STORAGE_OR_FLUID_TANK,
    BLOCKS_ABOVE_LAUNCH_PAD
}
