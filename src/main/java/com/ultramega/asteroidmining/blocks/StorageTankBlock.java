package com.ultramega.asteroidmining.blocks;

import net.minecraft.world.level.block.Block;

public class StorageTankBlock extends Block {
    private final Type type;
    private final Capacity capacity;

    public StorageTankBlock(final Type type, final Capacity capacity, final Properties properties) {
        super(properties);
        this.type = type;
        this.capacity = capacity;
    }

    public Type getType() {
        return this.type;
    }

    public Capacity getCapacity() {
        return this.capacity;
    }

    public enum Capacity {
        TIER_1("8k", 8192),
        TIER_2("32k", 32768),
        TIER_3("64k", 65536),
        TIER_4("262k", 262144);

        private final String name;
        private final int capacity;

        Capacity(final String name, final int capacity) {
            this.name = name;
            this.capacity = capacity;
        }

        public String getName() {
            return this.name;
        }

        public int getCapacity() {
            return this.capacity;
        }
    }

    public enum Type {
        ITEMS,
        FLUIDS
    }
}
