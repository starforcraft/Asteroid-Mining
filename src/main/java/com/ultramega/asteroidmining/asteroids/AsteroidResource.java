package com.ultramega.asteroidmining.asteroids;

import com.ultramega.asteroidmining.utils.ClientUtils;
import com.ultramega.asteroidmining.utils.CommonUtils;
import com.ultramega.asteroidmining.utils.CoreValidations;
import com.ultramega.asteroidmining.utils.FluidContainerUtil;
import com.ultramega.asteroidmining.utils.TextColors;

import java.util.List;
import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

import static com.ultramega.asteroidmining.utils.ClientUtils.renderAmount;
import static com.ultramega.asteroidmining.utils.ClientUtils.renderFluidStackTooltip;
import static com.ultramega.asteroidmining.utils.ClientUtils.renderItemStackTooltip;

public sealed interface AsteroidResource permits AsteroidResource.ItemEntry, AsteroidResource.FluidEntry {
    Codec<AsteroidResource> CODEC = EntryType.CODEC.dispatch("type", AsteroidResource::type, EntryType::mapCodec);

    Codec<List<AsteroidResource>> LIST_CODEC = CODEC.listOf();

    StreamCodec<RegistryFriendlyByteBuf, AsteroidResource> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AsteroidResource decode(final RegistryFriendlyByteBuf buf) {
            final EntryType type = EntryType.values()[buf.readVarInt()];
            return type.decode(buf);
        }

        @Override
        public void encode(final RegistryFriendlyByteBuf buf, final AsteroidResource resource) {
            buf.writeVarInt(resource.type().ordinal());
            resource.encode(buf);
        }
    };

    EntryType type();

    long amount();

    AsteroidResource withAmount(long amount);

    boolean isEmpty();

    void encode(RegistryFriendlyByteBuf buf);

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

    record ItemEntry(ItemStackTemplate resource, long amount) implements AsteroidResource {
        public static final MapCodec<ItemEntry> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStackTemplate.CODEC.fieldOf("resource").forGetter(ItemEntry::resource),
            positiveLongCodec().fieldOf("amount").forGetter(ItemEntry::amount)
        ).apply(instance, ItemEntry::new));

        public ItemEntry(final Item item, final long amount) {
            this(new ItemStackTemplate(item), amount);
        }

        public ItemEntry {
            Objects.requireNonNull(resource, "Stored item template cannot be null");

            resource = resource.withCount(1);

            CoreValidations.validateFalse(resource.is(Items.AIR), "Stored item template cannot be air");
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
            return this.resource.is(Items.AIR) || this.amount <= 0;
        }

        @Override
        public void encode(final RegistryFriendlyByteBuf buf) {
            ItemStackTemplate.STREAM_CODEC.encode(buf, this.resource());
            buf.writeVarLong(this.amount());
        }

        public static ItemEntry decode(final RegistryFriendlyByteBuf buf) {
            return new ItemEntry(ItemStackTemplate.STREAM_CODEC.decode(buf), buf.readVarLong());
        }

        @Override
        public void drawTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
            renderItemStackTooltip(graphics, this.resource.create(), this.amount, mouseX, mouseY);
        }

        @Override
        public void drawResourceWithAmount(final GuiGraphicsExtractor graphics, final Font font, final int x, final int y) {
            graphics.item(this.resource.create(), x, y);
            renderAmount(graphics, font, x, y, CommonUtils.formatWithUnits(this.amount()), TextColors.WHITE.getHexCode());
        }
    }

    record FluidEntry(FluidStackTemplate resource, long amount) implements AsteroidResource {
        private static final Codec<FluidStackTemplate> FLUID_STACK_TEMPLATE_NO_AMOUNT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("id").forGetter(template -> template.fluid().value()),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(FluidStackTemplate::components)
        ).apply(instance, (fluid, components) -> new FluidStackTemplate(fluid, 1, components)));

        public static final MapCodec<FluidEntry> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FLUID_STACK_TEMPLATE_NO_AMOUNT_CODEC.fieldOf("resource").forGetter(FluidEntry::resource),
            positiveLongCodec().fieldOf("amount").forGetter(FluidEntry::amount)
        ).apply(instance, FluidEntry::new));

        public FluidEntry(final Fluid fluid, final long amount) {
            this(new FluidStackTemplate(fluid, 1), amount);
        }

        public FluidEntry {
            Objects.requireNonNull(resource, "Stored fluid resource cannot be null");

            resource = resource.withAmount(1);

            CoreValidations.validateFalse(resource.is(Fluids.EMPTY), "Stored fluid resource cannot be empty");
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
            return this.resource.is(Fluids.EMPTY) || this.amount <= 0;
        }

        @Override
        public void encode(final RegistryFriendlyByteBuf buf) {
            FluidStackTemplate.STREAM_CODEC.encode(buf, this.resource());
            buf.writeVarLong(this.amount());
        }

        public static FluidEntry decode(final RegistryFriendlyByteBuf buf) {
            return new FluidEntry(FluidStackTemplate.STREAM_CODEC.decode(buf), buf.readVarLong());
        }

        @Override
        public void drawTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
            renderFluidStackTooltip(graphics, this.resource.create(), this.amount, mouseX, mouseY);
        }

        @Override
        public void drawResourceWithAmount(final GuiGraphicsExtractor graphics, final Font font, final int x, final int y) {
            FluidContainerUtil.renderTiledFluid(graphics, this.resource.create(), 0, 0, x, y, 16, 16);
            renderAmount(graphics, font, x, y, CommonUtils.formatWithUnitsFluid(this.amount()), TextColors.WHITE.getHexCode());
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

        public AsteroidResource decode(final RegistryFriendlyByteBuf buf) {
            return switch (this) {
                case ITEM -> ItemEntry.decode(buf);
                case FLUID -> FluidEntry.decode(buf);
            };
        }
    }
}
