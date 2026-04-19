package com.ultramega.asteroidmining.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class BaseBlockItem extends BlockItem {
    private final Block block;

    public BaseBlockItem(final Block block, final Properties properties) {
        super(block, properties);
        this.block = block;
    }

    @Override
    public Component getName(final ItemStack itemStack) {
        return this.block.getName();
    }
}
