package com.ultramega.asteroidmining.utils;

import java.util.function.Function;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import org.jspecify.annotations.Nullable;

// TODO: change into interface ResourceStack and FluidResourceStack/ItemResourceStack
public class ItemFluidStack {
    public static final Codec<ItemFluidStack> CODEC = Codec.either(FluidStackTemplate.CODEC, ItemStackTemplate.CODEC)
        .xmap(
            either -> either.map(ItemFluidStack::new, ItemFluidStack::new),
            stack -> {
                if (stack.fluidStack != null) {
                    return Either.left(stack.fluidStack);
                } else if (stack.itemStack != null) {
                    return Either.right(stack.itemStack);
                }
                throw new IllegalStateException("ItemFluidStack must have either an ItemStack or a FluidStack");
            }
        );

    public static final Codec<NonNullList<ItemFluidStack>> LIST_CODEC =
        ItemFluidStack.CODEC.listOf().xmap(
            list -> {
                final NonNullList<ItemFluidStack> stackList = NonNullList.create();
                stackList.addAll(list);
                return stackList;
            },
            Function.identity()
        );

    public static final StreamCodec<FriendlyByteBuf, NonNullList<ItemFluidStack>> LIST_STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public NonNullList<ItemFluidStack> decode(final FriendlyByteBuf buf) {
                final CompoundTag compound = buf.readNbt();
                final Tag listTag = compound.getList("list").get();

                return ItemFluidStack.LIST_CODEC.parse(NbtOps.INSTANCE, listTag)
                    .resultOrPartial(error -> {
                        throw new IllegalStateException("Failed to decode list: " + error);
                    })
                    .orElseThrow();
            }

            @Override
            public void encode(final FriendlyByteBuf buf, final NonNullList<ItemFluidStack> value) {
                final Tag listTag = ItemFluidStack.LIST_CODEC.encodeStart(NbtOps.INSTANCE, value)
                    .resultOrPartial(error -> {
                        throw new IllegalStateException("Failed to encode list: " + error);
                    })
                    .orElseThrow();

                final CompoundTag compound = new CompoundTag();
                compound.put("list", listTag);
                buf.writeNbt(compound);
            }
        };

    @Nullable
    private FluidStackTemplate fluidStack;
    @Nullable
    private ItemStackTemplate itemStack;

    public ItemFluidStack(final ItemStackTemplate itemStack) {
        this.itemStack = itemStack;
    }

    public ItemFluidStack(final FluidStackTemplate fluidStack) {
        this.fluidStack = fluidStack;
    }

    public ItemFluidStack copyWithCount(final int amount) {
        if (this.itemStack != null) {
            return new ItemFluidStack(this.itemStack.withCount(amount));
        } else if (this.fluidStack != null) {
            return new ItemFluidStack(this.fluidStack.withAmount(amount));
        }

        throw new IllegalStateException();
    }

    public int getCount() {
        if (this.itemStack != null) {
            return this.itemStack.count();
        } else if (this.fluidStack != null) {
            return this.fluidStack.amount();
        }

        return -1;
    }

    @Nullable
    public ItemStack getItemStack() {
        return this.itemStack.create();
    }

    @Nullable
    public FluidStack getFluidStack() {
        return this.fluidStack.create();
    }

    public static ItemFluidStack of(final ItemStackTemplate itemStack, final FluidStackTemplate fluidStack) {
        if (itemStack != null) {
            return new ItemFluidStack(itemStack);
        } else if (fluidStack != null) {
            return new ItemFluidStack(fluidStack);
        }

        return null;
    }
}
