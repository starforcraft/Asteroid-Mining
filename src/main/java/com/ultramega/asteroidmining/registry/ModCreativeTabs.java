package com.ultramega.asteroidmining.registry;

import com.ultramega.asteroidmining.AsteroidMining;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AsteroidMining.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ASTEROID_MINING_TAB =
        CREATIVE_MODE_TABS.register(AsteroidMining.MOD_ID + "_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + AsteroidMining.MOD_ID))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModItems.ROCKET_STORAGE_VIEWER.get().getDefaultInstance())
            .displayItems(ModItems.ITEMS.getEntries())
            .build());

    private ModCreativeTabs() {
    }
}
