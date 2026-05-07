package com.ultramega.asteroidmining.storage;

import com.ultramega.asteroidmining.asteroids.AsteroidResource;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

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

    public NonNullList<AsteroidResource> inventory() {
        return this.storage.inventory();
    }

    public ModuleProperties withStorage(final Storage storage) {
        return new ModuleProperties(this.selectedAsteroid, storage);
    }

    public void addResources(final List<AsteroidResource> resources) {
        for (final AsteroidResource resource : resources) {
            this.storage.add(resource);
        }
    }

    public static long saturatingAdd(final long a, final long b) {
        if (Long.MAX_VALUE - a < b) {
            return Long.MAX_VALUE;
        }
        return a + b;
    }

    public static final class Storage {
        public static final Codec<Storage> CODEC = AsteroidResource.CODEC.listOf().xmap(
            Storage::new,
            storage -> List.copyOf(storage.inventory)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, Storage> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public Storage decode(final RegistryFriendlyByteBuf buf) {
                    final int size = buf.readVarInt();
                    final NonNullList<AsteroidResource> entries = NonNullList.create();

                    for (int i = 0; i < size; i++) {
                        entries.add(AsteroidResource.STREAM_CODEC.decode(buf));
                    }

                    return new Storage(entries);
                }

                @Override
                public void encode(final RegistryFriendlyByteBuf buf, final Storage value) {
                    buf.writeVarInt(value.inventory.size());

                    for (final AsteroidResource entry : value.inventory) {
                        AsteroidResource.STREAM_CODEC.encode(buf, entry);
                    }
                }
            };

        private final NonNullList<AsteroidResource> inventory;

        public Storage() {
            this.inventory = NonNullList.create();
        }

        public Storage(final Collection<AsteroidResource> inventory) {
            this.inventory = NonNullList.create();
            this.inventory.addAll(inventory);
        }

        public NonNullList<AsteroidResource> inventory() {
            return this.inventory;
        }

        public Storage copy() {
            return new Storage(this.inventory);
        }

        public void add(final AsteroidResource entry) {
            if (entry.isEmpty() || entry.amount() <= 0L) {
                return;
            }

            for (int i = 0; i < this.inventory.size(); i++) {
                final AsteroidResource existing = this.inventory.get(i);
                if (existing.sameResource(entry)) {
                    this.inventory.set(i, existing.withAmount(saturatingAdd(existing.amount(), entry.amount())));
                    return;
                }
            }

            this.inventory.add(entry);
        }

        public void set(final int index, final AsteroidResource entry) {
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
}
