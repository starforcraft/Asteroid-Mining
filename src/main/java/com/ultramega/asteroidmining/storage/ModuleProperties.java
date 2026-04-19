package com.ultramega.asteroidmining.storage;

import com.ultramega.asteroidmining.utils.ItemFluidStack;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

public record ModuleProperties(Optional<Identifier> selectedAsteroid, NonNullList<ItemFluidStack> inventory) {
    public static final Codec<ModuleProperties> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Identifier.CODEC.optionalFieldOf("selectedAsteroid").forGetter(ModuleProperties::selectedAsteroid),
            ItemFluidStack.LIST_CODEC.fieldOf("inventory").forGetter(ModuleProperties::inventory)
        ).apply(instance, ModuleProperties::new)
    );

    public static final StreamCodec<FriendlyByteBuf, ModuleProperties> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC.apply(ByteBufCodecs::optional), ModuleProperties::selectedAsteroid,
        ItemFluidStack.LIST_STREAM_CODEC, ModuleProperties::inventory,
        ModuleProperties::new
    );

    public void addItemFluidStack(final ItemFluidStack stack) {
        if (stack.getItemStack() != null) {
            this.addItemStack(stack.getItemStack());
        } else if (stack.getFluidStack() != null) {
            this.addFluidStack(stack.getFluidStack());
        }
    }

    public void addItemStack(final ItemStack stack) {
        for (final ItemFluidStack existingStack : this.inventory) {
            if (existingStack.getItemStack() != null) {
                if (ItemStack.isSameItemSameComponents(existingStack.getItemStack(), stack)) {
                    existingStack.getItemStack().grow(stack.getCount());

                    return;
                }
            }
        }

        this.inventory.add(new ItemFluidStack(ItemStackTemplate.fromNonEmptyStack(stack)));
    }

    public void addFluidStack(final FluidStack stack) {
        for (final ItemFluidStack existingStack : this.inventory) {
            if (existingStack.getFluidStack() != null) {
                if (FluidStack.isSameFluidSameComponents(existingStack.getFluidStack(), stack)) {
                    existingStack.getFluidStack().grow(stack.getAmount());
                    return;
                }
            }
        }

        this.inventory.add(new ItemFluidStack(FluidStackTemplate.fromNonEmptyStack(stack)));
    }
}
