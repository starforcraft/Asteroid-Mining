package com.ultramega.asteroidmining.blockentities;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.registry.ModDataComponentTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;

public abstract class AbstractDataPreservingBlockEntity extends BlockEntity {
    public AbstractDataPreservingBlockEntity(final BlockEntityType<?> type,
                                             final BlockPos pos,
                                             final BlockState blockState) {
        super(type, pos, blockState);
    }

    public void setPlacedBy(final ItemStack stack) {
        final CustomData storedState = stack.get(ModDataComponentTypes.STORED_BLOCK_ENTITY_DATA);
        if (storedState != null) {
            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(this.problemPath(), AsteroidMining.LOGGER)) {
                this.loadAdditional(TagValueInput.create(reporter, this.level.registryAccess(), storedState.copyTag()));
            }
        }
    }

    public ItemStack storeToStack(final ItemStack stack) {
        final CompoundTag tag = this.getUpdateTag(this.level.registryAccess());
        if (!tag.isEmpty()) {
            stack.set(ModDataComponentTypes.STORED_BLOCK_ENTITY_DATA.get(), CustomData.of(tag));
        }

        return stack;
    }
}
