package com.ultramega.asteroidmining.utils;

import com.ultramega.asteroidmining.AsteroidMining;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

public record CoolantData(int duration, int temperature) {
    public static final Codec<CoolantData> CODEC = RecordCodecBuilder.create(builder -> builder
        .group(
            Codec.INT.fieldOf("duration").forGetter(CoolantData::duration),
            Codec.intRange(-273, 0).fieldOf("temperature").forGetter(CoolantData::temperature))
        .apply(builder, CoolantData::new));

    public static final DataMapType<Item, CoolantData> COOLANT_DATA = DataMapType.builder(AsteroidMining.makeId("coolant"), Registries.ITEM, CODEC)
        .synced(CODEC, true)
        .build();
}
