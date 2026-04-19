package com.ultramega.asteroidmining.blockentities;

import com.ultramega.asteroidmining.blocks.BoundingBoxBlock;
import com.ultramega.asteroidmining.registry.ModBlockEntityTypes;
import com.ultramega.asteroidmining.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jspecify.annotations.Nullable;

public class BoundingBoxBlockEntity extends BlockEntity implements Nameable {
    @Nullable
    private BlockPos mainBlockPos;

    public BoundingBoxBlockEntity(final BlockPos pos,
                                  final BlockState blockState) {
        super(ModBlockEntityTypes.BOUNDING_BOX.get(), pos, blockState);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);

        input.read("mainPos", BlockPos.CODEC).ifPresent(pos -> this.mainBlockPos = pos);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);

        if (this.mainBlockPos != null) {
            output.store("mainPos", BlockPos.CODEC, this.mainBlockPos);
        }
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public boolean hasCustomName() {
        return this.getMainBlockEntity() instanceof Nameable blockEntity && blockEntity.hasCustomName();
    }

    @Override
    public Component getName() {
        return this.hasCustomName() ? this.getCustomName() : Component.empty();
    }

    @Override
    public Component getDisplayName() {
        return this.getMainBlockEntity() instanceof Nameable blockEntity ? blockEntity.getDisplayName() : Component.empty();
    }

    @Override
    public @Nullable Component getCustomName() {
        return this.getMainBlockEntity() instanceof Nameable blockEntity ? blockEntity.getCustomName() : null;
    }

    public static <T, C> void redirectCapability(final RegisterCapabilitiesEvent event, final BlockCapability<T, C> capability) {
        event.registerBlock(capability, (level, pos, state, blockEntity, context) -> {
            final BlockPos mainPos = BoundingBoxBlock.getMainBlockPos(level, pos);
            return mainPos == null ? null : level.getCapability(capability, mainPos, context);
        }, ModBlocks.BOUNDING_BOX.get());
    }

    @Nullable
    private BlockEntity getMainBlockEntity() {
        if (this.mainBlockPos != null) {
            return this.level.getBlockEntity(this.mainBlockPos);
        }

        return null;
    }

    public void setMainBlockPos(final BlockPos mainPos) {
        this.mainBlockPos = mainPos;
        this.setChanged();
    }

    @Nullable
    public BlockPos getMainBlockPos() {
        return this.mainBlockPos;
    }
}
