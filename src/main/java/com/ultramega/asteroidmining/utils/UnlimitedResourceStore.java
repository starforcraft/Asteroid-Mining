package com.ultramega.asteroidmining.utils;

import com.ultramega.asteroidmining.asteroids.AsteroidResource;
import com.ultramega.asteroidmining.storage.ModuleProperties;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public final class UnlimitedResourceStore<R extends Resource> extends SnapshotJournal<ModuleProperties.Storage> implements ResourceHandler<R> {
    private final ResourceAdapter<R> adapter;
    private final Supplier<ModuleProperties> propertiesGetter;
    private final Consumer<ModuleProperties> propertiesSetter;
    private final Runnable onChanged;
    private final boolean allowInsert;

    public UnlimitedResourceStore(final ResourceAdapter<R> adapter,
                                  final Supplier<ModuleProperties> propertiesGetter,
                                  final Consumer<ModuleProperties> propertiesSetter,
                                  final Runnable onChanged) {
        this(adapter, propertiesGetter, propertiesSetter, onChanged, true);
    }

    public UnlimitedResourceStore(final ResourceAdapter<R> adapter,
                                  final Supplier<ModuleProperties> propertiesGetter,
                                  final Consumer<ModuleProperties> propertiesSetter,
                                  final Runnable onChanged, final boolean allowInsert) {
        this.adapter = adapter;
        this.propertiesGetter = propertiesGetter;
        this.propertiesSetter = propertiesSetter;
        this.onChanged = onChanged;
        this.allowInsert = allowInsert;
    }

    public static UnlimitedResourceStore<ItemResource> items(final Supplier<ModuleProperties> propertiesGetter,
                                                             final Consumer<ModuleProperties> propertiesSetter,
                                                             final Runnable onChanged) {
        return new UnlimitedResourceStore<>(ResourceAdapter.ITEMS, propertiesGetter, propertiesSetter, onChanged);
    }

    public static UnlimitedResourceStore<FluidResource> fluids(final Supplier<ModuleProperties> propertiesGetter,
                                                               final Consumer<ModuleProperties> propertiesSetter,
                                                               final Runnable onChanged) {
        return new UnlimitedResourceStore<>(ResourceAdapter.FLUIDS, propertiesGetter, propertiesSetter, onChanged);
    }

    @Override
    public int size() {
        // +1 for inserting a new resource through slotted callers
        return this.countMatchingEntries() + 1;
    }

    @Override
    public R getResource(final int index) {
        final IndexedEntry entry = this.getIndexedEntry(index);

        if (entry == null) {
            return this.adapter.empty();
        }

        return this.adapter.resource(entry.entry());
    }

    @Override
    public long getAmountAsLong(final int index) {
        final IndexedEntry entry = this.getIndexedEntry(index);
        return entry == null ? 0L : entry.entry().amount();
    }

    @Override
    public long getCapacityAsLong(final int index, final R resource) {
        return this.isValid(index, resource) ? Long.MAX_VALUE : 0L;
    }

    @Override
    public boolean isValid(final int index, final R resource) {
        if (index < 0 || resource.isEmpty()) {
            return false;
        }

        final IndexedEntry entry = this.getIndexedEntry(index);
        if (entry != null) {
            return this.adapter.resource(entry.entry()).equals(resource);
        }

        return this.allowInsert && index == this.countMatchingEntries();
    }

    @Override
    public int insert(final int index, final R resource, final int amount, final TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        if (!this.allowInsert || amount == 0 || !this.isValid(index, resource)) {
            return 0;
        }

        return this.insert(resource, amount, transaction);
    }

    @Override
    public int insert(final R resource, final int amount, final TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        if (!this.allowInsert || amount == 0) {
            return 0;
        }

        final ModuleProperties.Storage storage = this.storage();

        long current = 0L;
        for (final AsteroidResource entry : storage.inventory()) {
            if (this.adapter.matches(entry) && this.adapter.resource(entry).equals(resource)) {
                current = entry.amount();
                break;
            }
        }

        final long room = Long.MAX_VALUE - current;
        final int inserted = (int) Math.clamp(room, 0L, amount);
        if (inserted == 0) {
            return 0;
        }

        this.updateSnapshots(transaction);

        this.storage().add(this.adapter.create(resource, inserted));
        this.writeStorage(this.storage());

        return inserted;
    }

    @Override
    public int extract(final int index, final R resource, final int amount, final TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        if (amount == 0) {
            return 0;
        }

        final IndexedEntry indexedEntry = this.getIndexedEntry(index);
        if (indexedEntry == null) {
            return 0;
        }

        final R storedResource = this.adapter.resource(indexedEntry.entry());
        if (!storedResource.equals(resource)) {
            return 0;
        }

        final int extracted = (int) Math.min(amount, indexedEntry.entry().amount());
        if (extracted <= 0) {
            return 0;
        }

        this.updateSnapshots(transaction);

        final ModuleProperties.Storage storage = this.storage();
        final long remaining = indexedEntry.entry().amount() - extracted;

        if (remaining <= 0L) {
            storage.remove(indexedEntry.storageIndex());
        } else {
            storage.set(indexedEntry.storageIndex(), indexedEntry.entry().withAmount(remaining));
        }

        this.writeStorage(storage);

        return extracted;
    }

    @Override
    public int extract(final R resource, final int amount, final TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        if (amount == 0) {
            return 0;
        }

        for (int index = 0; index < this.size(); index++) {
            final IndexedEntry entry = this.getIndexedEntry(index);

            if (entry != null && this.adapter.resource(entry.entry()).equals(resource)) {
                return this.extract(index, resource, amount, transaction);
            }
        }

        return 0;
    }

    public Map<R, Long> view() {
        final Map<R, Long> result = new LinkedHashMap<>();

        for (final AsteroidResource entry : this.storage().inventory()) {
            if (this.adapter.matches(entry)) {
                result.put(this.adapter.resource(entry), entry.amount());
            }
        }

        return Map.copyOf(result);
    }

    public void putLoaded(final R resource, final long amount) {
        if (resource.isEmpty() || amount <= 0L) {
            return;
        }

        final ModuleProperties.Storage storage = this.storage();
        storage.add(this.adapter.create(resource, amount));
        this.writeStorage(storage);
    }

    public void clear() {
        final ModuleProperties.Storage storage = this.storage();

        for (int i = storage.inventory().size() - 1; i >= 0; i--) {
            if (this.adapter.matches(storage.inventory().get(i))) {
                storage.remove(i);
            }
        }

        this.writeStorage(storage);
        this.onChanged.run();
    }

    @Override
    protected ModuleProperties.Storage createSnapshot() {
        return this.storage().copy();
    }

    @Override
    protected void revertToSnapshot(final ModuleProperties.Storage snapshot) {
        this.writeStorage(snapshot.copy());
    }

    @Override
    protected void onRootCommit(final ModuleProperties.Storage originalState) {
        this.onChanged.run();
    }

    private ModuleProperties.Storage storage() {
        final ModuleProperties properties = this.propertiesGetter.get();
        return properties.storage();
    }

    private void writeStorage(final ModuleProperties.Storage storage) {
        final ModuleProperties current = this.propertiesGetter.get();
        this.propertiesSetter.accept(current.withStorage(storage));
    }

    private int countMatchingEntries() {
        int count = 0;

        for (final AsteroidResource entry : this.storage().inventory()) {
            if (this.adapter.matches(entry) && !entry.isEmpty()) {
                count++;
            }
        }

        return count;
    }

    @Nullable
    private IndexedEntry getIndexedEntry(final int handlerIndex) {
        if (handlerIndex < 0) {
            throw new IndexOutOfBoundsException(handlerIndex);
        }

        int current = 0;

        final ModuleProperties.Storage storage = this.storage();

        for (int storageIndex = 0; storageIndex < storage.inventory().size(); storageIndex++) {
            final AsteroidResource entry = storage.inventory().get(storageIndex);
            if (!this.adapter.matches(entry) || entry.isEmpty() || entry.amount() <= 0L) {
                continue;
            }

            if (current == handlerIndex) {
                return new IndexedEntry(storageIndex, entry);
            }

            current++;
        }

        return null;
    }

    private record IndexedEntry(int storageIndex, AsteroidResource entry) {
    }

    public interface ResourceAdapter<R extends Resource> {
        ResourceAdapter<ItemResource> ITEMS = new ResourceAdapter<>() {
            @Override
            public ItemResource empty() {
                return ItemResource.EMPTY;
            }

            @Override
            public boolean matches(final AsteroidResource entry) {
                return entry instanceof AsteroidResource.ItemEntry;
            }

            @Override
            public ItemResource resource(final AsteroidResource entry) {
                return ItemResource.of(((AsteroidResource.ItemEntry) entry).resource());
            }

            @Override
            public AsteroidResource create(final ItemResource resource, final long amount) {
                return new AsteroidResource.ItemEntry(resource.getItem(), amount);
            }
        };

        ResourceAdapter<FluidResource> FLUIDS = new ResourceAdapter<>() {
            @Override
            public FluidResource empty() {
                return FluidResource.EMPTY;
            }

            @Override
            public boolean matches(final AsteroidResource entry) {
                return entry instanceof AsteroidResource.FluidEntry;
            }

            @Override
            public FluidResource resource(final AsteroidResource entry) {
                return FluidResource.of(((AsteroidResource.FluidEntry) entry).resource());
            }

            @Override
            public AsteroidResource create(final FluidResource resource, final long amount) {
                return new AsteroidResource.FluidEntry(resource.getFluid(), amount);
            }
        };

        R empty();

        boolean matches(AsteroidResource entry);

        R resource(AsteroidResource entry);

        AsteroidResource create(R resource, long amount);
    }
}

