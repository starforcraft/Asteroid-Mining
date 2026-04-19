package com.ultramega.asteroidmining.datagen.loot;

import com.ultramega.asteroidmining.registry.ModBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.registries.DeferredHolder;

public class BlockDropsProvider extends BlockLootSubProvider {
    public BlockDropsProvider(final HolderLookup.Provider provider) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
    }

    @Override
    protected void generate() {
        for (final DeferredHolder<Block, ? extends Block> holder : ModBlocks.BLOCKS.getEntries()) {
            final Block block = holder.get();
            if (block.getLootTable().isPresent() && block.asItem() != Items.AIR) {
                this.drop(block);
            }
        }
    }

    private void drop(final Block block) {
        this.add(block, this.createSingleItemTable(block)
            .apply(copyName()));
    }

    private static CopyComponentsFunction.Builder copyName() {
        return CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
            .include(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        final List<Block> blocks = new ArrayList<>();

        for (final DeferredHolder<Block, ? extends Block> holder : ModBlocks.BLOCKS.getEntries()) {
            final Block block = holder.get();
            if (block.getLootTable().isPresent() && block.asItem() != Items.AIR) {
                blocks.add(block);
            }
        }

        return blocks;
    }
}
