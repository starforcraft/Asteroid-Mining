package com.ultramega.asteroidmining.items;

import com.ultramega.asteroidmining.registry.ModDataComponentTypes;
import com.ultramega.asteroidmining.storage.ClientConfigurationSavedData;
import com.ultramega.asteroidmining.storage.NetworkConfiguration;

import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ConfigurationCardItem extends Item {
    public ConfigurationCardItem(final Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(final ItemStack stack,
                                final TooltipContext context,
                                final TooltipDisplay display,
                                final Consumer<Component> builder,
                                final TooltipFlag tooltipFlag) {
        if (stack.has(ModDataComponentTypes.CONFIGURATION_PATH_DATA)) {
            final UUID uuid = stack.get(ModDataComponentTypes.CONFIGURATION_PATH_DATA);
            final NetworkConfiguration configuration = ClientConfigurationSavedData.INSTANCE.get(uuid);
            if (configuration != null) {
                builder.accept(Component.translatable("tooltip.asteroidmining.configuration_card.saved_configuration", configuration.launchPadConfiguration().name())
                    .withStyle(ChatFormatting.AQUA));
            }
        }
    }
}
