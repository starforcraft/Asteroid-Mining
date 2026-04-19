package com.ultramega.asteroidmining.datagen;

import com.ultramega.asteroidmining.utils.CoolantData;
import com.ultramega.asteroidmining.utils.ITags;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.DataMapProvider;

public class DataMapProviderImpl extends DataMapProvider {
    protected DataMapProviderImpl(final PackOutput packOutput,
                                  final CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather(final HolderLookup.Provider provider) {
        this.builder(CoolantData.COOLANT_DATA) //TODO: balance values
            .add(item(Blocks.SNOW_BLOCK), new CoolantData(48, -3), false)
            .add(item(Items.SNOWBALL), new CoolantData(12, -3), false)
            .add(ITags.Items.ICES_ICE, new CoolantData(48, -5), false)
            .add(ITags.Items.ICES_PACKED, new CoolantData(192, -8), false)
            .add(ITags.Items.ICES_BLUE, new CoolantData(568, -17), false)
            .build();
    }

    private static Holder<Item> item(final ItemLike item) {
        return BuiltInRegistries.ITEM.wrapAsHolder(item.asItem());
    }
}
