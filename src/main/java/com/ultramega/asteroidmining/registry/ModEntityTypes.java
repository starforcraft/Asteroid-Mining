package com.ultramega.asteroidmining.registry;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.entities.BlockStructureEntity;

import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, AsteroidMining.MOD_ID);

    public static final Supplier<EntityType<BlockStructureEntity>> BLOCK_STRUCTURE_ENTITY = ENTITY_TYPES.register("block_structure", () ->
        EntityType.Builder.<BlockStructureEntity>of((type, level) -> new BlockStructureEntity(level), MobCategory.MISC)
            .sized(1.0F, 1.0F)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, AsteroidMining.makeId("block_structure"))));

    private ModEntityTypes() {
    }
}
