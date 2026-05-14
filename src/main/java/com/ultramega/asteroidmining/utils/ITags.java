package com.ultramega.asteroidmining.utils;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public class ITags {
    public static final class Blocks {
        // Remove ice if NeoForge adds them in the future
        public static final TagKey<Block> ICES = tag("ices"); //TODO: delete this tag?
        public static final TagKey<Block> ICES_ICE = tag("ices/ice");
        public static final TagKey<Block> ICES_PACKED = tag("ices/packed");
        public static final TagKey<Block> ICES_BLUE = tag("ices/blue");

        private static TagKey<Block> tag(final String name) {
            return BlockTags.create(Identifier.fromNamespaceAndPath("c", name));
        }
    }

    public static final class Items {
        // Remove ice if NeoForge adds them in the future
        public static final TagKey<Item> ICES = tag("ices"); //TODO: delete this tag?
        public static final TagKey<Item> ICES_ICE = tag("ices/ice");
        public static final TagKey<Item> ICES_PACKED = tag("ices/packed");
        public static final TagKey<Item> ICES_BLUE = tag("ices/blue");

        private static TagKey<Item> tag(final String name) {
            return ItemTags.create(Identifier.fromNamespaceAndPath("c", name));
        }
    }

    public static final class Fluids {
        public static final TagKey<Fluid> GASES = tag("gases");

        private static TagKey<Fluid> tag(final String name) {
            return FluidTags.create(Identifier.fromNamespaceAndPath("c", name));
        }
    }
}
