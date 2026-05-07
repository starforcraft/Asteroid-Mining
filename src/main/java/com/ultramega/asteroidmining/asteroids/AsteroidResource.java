package com.ultramega.asteroidmining.asteroids;

import com.ultramega.asteroidmining.utils.CoreValidations;
import com.ultramega.asteroidmining.utils.FluidContainerUtil;
import com.ultramega.asteroidmining.utils.TextColors;
import com.ultramega.asteroidmining.utils.Utils;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import static com.ultramega.asteroidmining.utils.Utils.renderAmount;

public sealed interface AsteroidResource permits AsteroidResource.ItemEntry, AsteroidResource.FluidEntry {
    Codec<AsteroidResource> CODEC = EntryType.CODEC.dispatch("type", AsteroidResource::type, EntryType::mapCodec);

    Codec<List<AsteroidResource>> LIST_CODEC = CODEC.listOf();

    StreamCodec<RegistryFriendlyByteBuf, AsteroidResource> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AsteroidResource decode(final RegistryFriendlyByteBuf buf) {
            final EntryType type = EntryType.values()[buf.readVarInt()];

            return switch (type) {
                case ITEM -> new ItemEntry(ItemResource.STREAM_CODEC.decode(buf), buf.readVarInt());
                case FLUID -> new FluidEntry(FluidResource.STREAM_CODEC.decode(buf), buf.readVarInt());
            };
        }

        @Override
        public void encode(final RegistryFriendlyByteBuf buf, final AsteroidResource value) {
            buf.writeVarInt(value.type().ordinal());

            switch (value) {
                case ItemEntry item -> {
                    ItemResource.STREAM_CODEC.encode(buf, item.resource());
                    buf.writeVarLong(item.amount());
                }
                case FluidEntry fluid -> {
                    FluidResource.STREAM_CODEC.encode(buf, fluid.resource());
                    buf.writeVarLong(fluid.amount());
                }
            }
        }
    };

    EntryType type();

    long amount();

    AsteroidResource withAmount(long amount);

    boolean isEmpty();

    void drawTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY);

    void drawResourceWithAmount(GuiGraphicsExtractor graphics, Font font, int x, int y);

    default boolean sameResource(final AsteroidResource other) {
        return switch (this) {
            case ItemEntry item -> other instanceof ItemEntry otherItem && item.resource().equals(otherItem.resource());
            case FluidEntry fluid -> other instanceof FluidEntry otherFluid && fluid.resource().equals(otherFluid.resource());
        };
    }

    private static Codec<Long> positiveLongCodec() {
        return Codec.LONG.validate(value -> value > 0L
            ? DataResult.success(value)
            : DataResult.error(() -> "Amount must be positive"));
    }

    record ItemEntry(ItemResource resource, long amount) implements AsteroidResource {
        public static final MapCodec<ItemEntry> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemResource.CODEC.fieldOf("resource").forGetter(ItemEntry::resource),
            positiveLongCodec().fieldOf("amount").forGetter(ItemEntry::amount)
        ).apply(instance, ItemEntry::new));

        public ItemEntry {
            CoreValidations.validateFalse(resource.isEmpty(), "Stored item resource cannot be empty");
            CoreValidations.validateLargerThanZero(amount, "Stored item amount must be positive");
        }

        @Override
        public EntryType type() {
            return EntryType.ITEM;
        }

        @Override
        public AsteroidResource withAmount(final long amount) {
            return new ItemEntry(this.resource, amount);
        }

        @Override
        public boolean isEmpty() {
            return this.resource.isEmpty() || this.amount <= 0;
        }

        @Override
        public void drawTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
            Utils.renderItemResourceTooltip(graphics, this.resource, this.amount, mouseX, mouseY);
        }

        @Override
        public void drawResourceWithAmount(final GuiGraphicsExtractor graphics, final Font font, final int x, final int y) {
            graphics.item(this.resource.toStack(), x, y);
            renderAmount(graphics, font, x, y, Utils.formatWithUnits(this.amount()), TextColors.WHITE.getHexCode());
        }
    }

    record FluidEntry(FluidResource resource, long amount) implements AsteroidResource {
        public static final MapCodec<FluidEntry> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FluidResource.CODEC.fieldOf("resource").forGetter(FluidEntry::resource),
            positiveLongCodec().fieldOf("amount").forGetter(FluidEntry::amount)
        ).apply(instance, FluidEntry::new));

        public FluidEntry {
            CoreValidations.validateFalse(resource.isEmpty(), "Stored fluid resource cannot be empty");
            CoreValidations.validateLargerThanZero(amount, "Stored fluid amount must be positive");
        }

        @Override
        public EntryType type() {
            return EntryType.FLUID;
        }

        @Override
        public AsteroidResource withAmount(final long amount) {
            return new FluidEntry(this.resource, amount);
        }

        @Override
        public boolean isEmpty() {
            return this.resource.isEmpty() || this.amount <= 0;
        }

        @Override
        public void drawTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
            Utils.renderFluidResourceTooltip(graphics, this.resource, this.amount, mouseX, mouseY);
        }

        @Override
        public void drawResourceWithAmount(final GuiGraphicsExtractor graphics, final Font font, final int x, final int y) {
            FluidContainerUtil.renderTiledFluid(graphics, this.resource.toStack(1), 0, 0, x, y, 16, 16);
            renderAmount(graphics, font, x, y, Utils.formatWithUnitsFluid(this.amount()), TextColors.WHITE.getHexCode());
        }
    }

    enum EntryType implements StringRepresentable {
        ITEM("item"),
        FLUID("fluid");

        public static final Codec<EntryType> CODEC = StringRepresentable.fromEnum(EntryType::values);

        private final String serializedName;

        EntryType(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return this.serializedName;
        }

        public MapCodec<? extends AsteroidResource> mapCodec() {
            return switch (this) {
                case ITEM -> ItemEntry.MAP_CODEC;
                case FLUID -> FluidEntry.MAP_CODEC;
            };
        }
    }
}
