package com.ultramega.asteroidmining.storage;

import com.ultramega.asteroidmining.utils.CoreValidations;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

public record ModuleProperties(Optional<Identifier> selectedAsteroid, Storage storage) {
    public static final Codec<ModuleProperties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.optionalFieldOf("selectedAsteroid").forGetter(ModuleProperties::selectedAsteroid),
        Storage.CODEC.optionalFieldOf("storage", new Storage()).forGetter(ModuleProperties::storage)
    ).apply(instance, ModuleProperties::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ModuleProperties> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ModuleProperties decode(final RegistryFriendlyByteBuf buf) {
            final Optional<Identifier> selectedAsteroid = buf.readBoolean() ? Optional.of(buf.readIdentifier()) : Optional.empty();
            final Storage storage = Storage.STREAM_CODEC.decode(buf);

            return new ModuleProperties(selectedAsteroid, storage);
        }

        @Override
        public void encode(final RegistryFriendlyByteBuf buf, final ModuleProperties value) {
            buf.writeBoolean(value.selectedAsteroid().isPresent());
            value.selectedAsteroid().ifPresent(buf::writeIdentifier);

            Storage.STREAM_CODEC.encode(buf, value.storage());
        }
    };

    public NonNullList<StorageEntry> inventory() {
        return this.storage.inventory();
    }

    public ModuleProperties withStorage(final Storage storage) {
        return new ModuleProperties(this.selectedAsteroid, storage);
    }

    public void addItem(final ItemResource resource, final int amount) {
        this.storage.addItem(resource, amount);
    }

    public void addFluid(final FluidResource resource, final int amount) {
        this.storage.addFluid(resource, amount);
    }

    public void addStorageEntry(final StorageEntry entry) {
        this.storage.add(entry);
    }

    public static long saturatingAdd(final long a, final long b) {
        if (Long.MAX_VALUE - a < b) {
            return Long.MAX_VALUE;
        }
        return a + b;
    }

    private static Codec<Long> positiveLongCodec() {
        return Codec.LONG.validate(value -> value > 0L
            ? DataResult.success(value)
            : DataResult.error(() -> "Amount must be positive"));
    }

    public static final class Storage {
        public static final Codec<Storage> CODEC = StorageEntry.CODEC.listOf().xmap(
            Storage::new,
            storage -> List.copyOf(storage.inventory)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, Storage> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public Storage decode(final RegistryFriendlyByteBuf buf) {
                    final int size = buf.readVarInt();
                    final NonNullList<StorageEntry> entries = NonNullList.create();

                    for (int i = 0; i < size; i++) {
                        entries.add(StorageEntry.STREAM_CODEC.decode(buf));
                    }

                    return new Storage(entries);
                }

                @Override
                public void encode(final RegistryFriendlyByteBuf buf, final Storage value) {
                    buf.writeVarInt(value.inventory.size());

                    for (final StorageEntry entry : value.inventory) {
                        StorageEntry.STREAM_CODEC.encode(buf, entry);
                    }
                }
            };

        private final NonNullList<StorageEntry> inventory;

        public Storage() {
            this.inventory = NonNullList.create();
        }

        public Storage(final Collection<StorageEntry> inventory) {
            this.inventory = NonNullList.create();
            this.inventory.addAll(inventory);
        }

        public NonNullList<StorageEntry> inventory() {
            return this.inventory;
        }

        public Storage copy() {
            return new Storage(this.inventory);
        }

        public void addItem(final ItemResource resource, final long amount) {
            if (resource.isEmpty() || amount <= 0L) {
                return;
            }

            this.add(new StoredItem(resource, amount));
        }

        public void addFluid(final FluidResource resource, final long amount) {
            if (resource.isEmpty() || amount <= 0L) {
                return;
            }

            this.add(new StoredFluid(resource, amount));
        }

        public void add(final StorageEntry entry) {
            if (entry.isEmpty() || entry.amount() <= 0L) {
                return;
            }

            for (int i = 0; i < this.inventory.size(); i++) {
                final StorageEntry existing = this.inventory.get(i);
                if (existing.sameResource(entry)) {
                    this.inventory.set(i, existing.withAmount(saturatingAdd(existing.amount(), entry.amount())));
                    return;
                }
            }

            this.inventory.add(entry);
        }

        public void set(final int index, final StorageEntry entry) {
            if (entry.amount() <= 0L || entry.isEmpty()) {
                this.inventory.remove(index);
            } else {
                this.inventory.set(index, entry);
            }
        }

        public void remove(final int index) {
            this.inventory.remove(index);
        }
    }

    public sealed interface StorageEntry permits StoredItem, StoredFluid {
        Codec<StorageEntry> CODEC = EntryType.CODEC.dispatch("type", StorageEntry::type, EntryType::mapCodec);

        StreamCodec<RegistryFriendlyByteBuf, StorageEntry> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public StorageEntry decode(final RegistryFriendlyByteBuf buf) {
                final int ordinal = buf.readVarInt();
                if (ordinal < 0 || ordinal >= EntryType.values().length) {
                    throw new IllegalStateException("Unknown storage entry type id: " + ordinal);
                }

                final EntryType type = EntryType.values()[ordinal];
                return switch (type) {
                    case ITEM -> new StoredItem(ItemResource.STREAM_CODEC.decode(buf), buf.readLong());
                    case FLUID -> new StoredFluid(FluidResource.STREAM_CODEC.decode(buf), buf.readLong());
                };
            }

            @Override
            public void encode(final RegistryFriendlyByteBuf buf, final StorageEntry value) {
                buf.writeVarInt(value.type().ordinal());

                switch (value) {
                    case StoredItem item -> {
                        ItemResource.STREAM_CODEC.encode(buf, item.resource());
                        buf.writeLong(item.amount());
                    }
                    case StoredFluid fluid -> {
                        FluidResource.STREAM_CODEC.encode(buf, fluid.resource());
                        buf.writeLong(fluid.amount());
                    }
                }
            }
        };

        EntryType type();

        long amount();

        StorageEntry withAmount(long amount);

        boolean isEmpty();

        default boolean sameResource(final StorageEntry other) {
            return switch (this) {
                case StoredItem item -> other instanceof StoredItem otherItem && item.resource().equals(otherItem.resource());
                case StoredFluid fluid -> other instanceof StoredFluid otherFluid && fluid.resource().equals(otherFluid.resource());
            };
        }
    }

    public record StoredItem(ItemResource resource, long amount) implements StorageEntry {
        public static final MapCodec<StoredItem> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemResource.CODEC.fieldOf("resource").forGetter(StoredItem::resource),
            positiveLongCodec().fieldOf("amount").forGetter(StoredItem::amount)
        ).apply(instance, StoredItem::new));

        public static final Codec<StoredItem> CODEC = MAP_CODEC.codec();

        public StoredItem {
            CoreValidations.validateFalse(resource.isEmpty(), "Stored item resource cannot be empty");
            CoreValidations.validateLargerThanZero(amount, "Stored item amount must be positive");
        }

        @Override
        public EntryType type() {
            return EntryType.ITEM;
        }

        @Override
        public StorageEntry withAmount(final long amount) {
            return new StoredItem(this.resource, amount);
        }

        @Override
        public boolean isEmpty() {
            return this.resource.isEmpty();
        }
    }

    public record StoredFluid(FluidResource resource, long amount) implements StorageEntry {
        public static final MapCodec<StoredFluid> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FluidResource.CODEC.fieldOf("resource").forGetter(StoredFluid::resource),
            positiveLongCodec().fieldOf("amount").forGetter(StoredFluid::amount)
        ).apply(instance, StoredFluid::new));

        public static final Codec<StoredFluid> CODEC = MAP_CODEC.codec();

        public StoredFluid {
            CoreValidations.validateFalse(resource.isEmpty(), "Stored fluid resource cannot be empty");
            CoreValidations.validateLargerThanZero(amount, "Stored fluid amount must be positive");
        }

        @Override
        public EntryType type() {
            return EntryType.FLUID;
        }

        @Override
        public StorageEntry withAmount(final long amount) {
            return new StoredFluid(this.resource, amount);
        }

        @Override
        public boolean isEmpty() {
            return this.resource.isEmpty();
        }
    }

    public enum EntryType implements StringRepresentable {
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

        public MapCodec<? extends StorageEntry> mapCodec() {
            return switch (this) {
                case ITEM -> StoredItem.MAP_CODEC;
                case FLUID -> StoredFluid.MAP_CODEC;
            };
        }
    }
}
