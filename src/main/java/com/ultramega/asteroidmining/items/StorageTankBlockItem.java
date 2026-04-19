package com.ultramega.asteroidmining.items;

import com.ultramega.asteroidmining.blocks.StorageTankBlock;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class StorageTankBlockItem extends BaseBlockItem {
    private final StorageTankBlock.Type type;
    private final StorageTankBlock.Capacity capacity;

    public StorageTankBlockItem(final StorageTankBlock block, final Properties properties) {
        super(block, properties);
        this.type = block.getType();
        this.capacity = block.getCapacity();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(final ItemStack stack,
                                final TooltipContext context,
                                final TooltipDisplay display,
                                final Consumer<Component> builder,
                                final TooltipFlag tooltipFlag) {
        builder.accept(Component.translatable("tooltip.asteroidmining.storage_tank.capacity").withStyle(ChatFormatting.GOLD)
            .append(Component.literal(this.capacity.getName() + " ").withStyle(ChatFormatting.WHITE)
                .append(this.type == StorageTankBlock.Type.ITEMS
                    ? Component.translatable("tooltip.asteroidmining.storage_tank.items") //TODO: if fluid add B (Buckets) to the capacity
                    : Component.translatable("tooltip.asteroidmining.storage_tank.fluids").withStyle(ChatFormatting.WHITE))));
    }
}
