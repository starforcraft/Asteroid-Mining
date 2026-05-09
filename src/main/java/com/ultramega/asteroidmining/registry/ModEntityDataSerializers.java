package com.ultramega.asteroidmining.registry;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.utils.CommonUtils;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModEntityDataSerializers {
    public static final DeferredRegister<EntityDataSerializer<?>> ENTITY_DATA_SERIALIZERS =
        DeferredRegister.create(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS, AsteroidMining.MOD_ID);

    public static final Supplier<EntityDataSerializer<List<StructureTemplate.StructureBlockInfo>>> STRUCTURE_BLOCK_INFO_LIST_REGISTER =
        ENTITY_DATA_SERIALIZERS.register("structure_block_info_list", () -> EntityDataSerializer.forValueType(CommonUtils.STRUCTURE_BLOCK_INFO_STREAM_CODEC_LIST));

    private ModEntityDataSerializers() {
    }
}
