package com.ultramega.asteroidmining.registry;

import com.ultramega.asteroidmining.AsteroidMining;

import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponentTypes {
    public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPE = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, AsteroidMining.MOD_ID);

    // TODO: test if these items work in refined storage (https://refinedmods.com/refined-storage/items-cannot-be-extracted-from-refined-storage.html#_for_mod_developers)
    public static final Supplier<DataComponentType<CustomData>> STORED_BLOCK_ENTITY_DATA =
        DATA_COMPONENT_TYPE.registerComponentType("stored_block_entity_data", builder ->
            builder.persistent(CustomData.CODEC));
    public static final Supplier<DataComponentType<UUID>> CONFIGURATION_PATH_DATA =
        DATA_COMPONENT_TYPE.registerComponentType("configuration_path", builder ->
            builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    private ModDataComponentTypes() {
    }
}
