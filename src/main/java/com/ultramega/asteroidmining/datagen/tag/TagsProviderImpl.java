package com.ultramega.asteroidmining.datagen.tag;

import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.registry.ModFluids;
import com.ultramega.asteroidmining.utils.ITags;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.data.BlockTagCopyingItemTagProvider;

import static com.ultramega.asteroidmining.AsteroidMining.MOD_ID;

public class TagsProviderImpl {
    public static class Blocks extends IntrinsicHolderTagsProvider<Block> {
        public static final TagKey<Block> MINEABLE = TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace("mineable/pickaxe"));

        @SuppressWarnings("deprecation")
        public Blocks(final PackOutput packOutput, final CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, Registries.BLOCK, registries, block -> block.builtInRegistryHolder().key(), MOD_ID);
        }

        @Override
        protected void addTags(final HolderLookup.Provider provider) {
            ModBlocks.BLOCKS.getEntries().forEach(block ->
                this.tag(MINEABLE).add(block.get()));

            // Remove ice if NeoForge adds them in the future
            this.tag(ITags.Blocks.ICES).addTag(ITags.Blocks.ICES_ICE).addTag(ITags.Blocks.ICES_PACKED).addTag(ITags.Blocks.ICES_BLUE);
            this.tag(ITags.Blocks.ICES_ICE).add(net.minecraft.world.level.block.Blocks.ICE);
            this.tag(ITags.Blocks.ICES_PACKED).add(net.minecraft.world.level.block.Blocks.PACKED_ICE);
            this.tag(ITags.Blocks.ICES_BLUE).add(net.minecraft.world.level.block.Blocks.BLUE_ICE);
        }
    }

    public static class Items extends BlockTagCopyingItemTagProvider {
        public Items(final PackOutput packOutput,
                     final CompletableFuture<HolderLookup.Provider> registries,
                     final CompletableFuture<TagLookup<Block>> blockTagsProvider) {
            super(packOutput, registries, blockTagsProvider, MOD_ID);
        }

        @Override
        protected void addTags(final HolderLookup.Provider provider) {
            // Remove ice if NeoForge adds them in the future
            this.copy(ITags.Blocks.ICES, ITags.Items.ICES);
            this.copy(ITags.Blocks.ICES_ICE, ITags.Items.ICES_ICE);
            this.copy(ITags.Blocks.ICES_PACKED, ITags.Items.ICES_PACKED);
            this.copy(ITags.Blocks.ICES_BLUE, ITags.Items.ICES_BLUE);
        }
    }

    public static class Fluids extends IntrinsicHolderTagsProvider<Fluid> {
        @SuppressWarnings("deprecation")
        public Fluids(final PackOutput packOutput, final CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, Registries.FLUID, registries, fluid -> fluid.builtInRegistryHolder().key(), MOD_ID);
        }

        @Override
        protected void addTags(final HolderLookup.Provider provider) {
            this.tag(ITags.Fluids.GASES)
                .add(ModFluids.AIR.get())
                .add(ModFluids.METHANE.get())
                .add(ModFluids.HYDROGEN.get());
        }
    }
}
