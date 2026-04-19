package com.ultramega.asteroidmining.items;

import com.ultramega.asteroidmining.blocks.RocketEngineBlock;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class RocketEngineBlockItem extends BaseBlockItem {
    private final RocketEngineBlock.Type type;

    public RocketEngineBlockItem(final RocketEngineBlock block, final Properties properties) {
        super(block, properties);
        this.type = block.getType();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(final ItemStack stack,
                                final TooltipContext context,
                                final TooltipDisplay display,
                                final Consumer<Component> builder,
                                final TooltipFlag tooltipFlag) {
        builder.accept(Component.translatable("tooltip.asteroidmining.rocket_engine.fuel").withStyle(ChatFormatting.GOLD)
            .append(Component.translatable(this.type.getFuel().getFluidType().getDescriptionId()).withStyle(ChatFormatting.WHITE)));
        builder.accept(Component.translatable("tooltip.asteroidmining.rocket_engine.oxidizer").withStyle(ChatFormatting.GOLD)
            .append(Component.translatable(this.type.getOxidizer().getFluidType().getDescriptionId()).withStyle(ChatFormatting.WHITE)));
    }
}
