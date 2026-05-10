package com.ultramega.asteroidmining.blockentities;

import com.ultramega.asteroidmining.container.LaunchPadBuilderContainerMenu;
import com.ultramega.asteroidmining.events.PreviewClientEvents;
import com.ultramega.asteroidmining.network.c2s.SetConfigurationStackPayload;
import com.ultramega.asteroidmining.registry.ModBlockEntityTypes;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.registry.ModDataComponentTypes;
import com.ultramega.asteroidmining.storage.ClientConfigurationSavedData;
import com.ultramega.asteroidmining.storage.NetworkConfiguration;
import com.ultramega.asteroidmining.utils.PreserveData;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;

public class LaunchPadBuilderBlockEntity extends AbstractDataPreservingBlockEntity implements MenuProvider, Nameable, PreserveData {
    public final ItemStacksResourceHandler inventoryHandler = new ItemStacksResourceHandler(1) {
        @Override
        protected void onContentsChanged(final int index, final ItemStack previousContents) {
            super.onContentsChanged(index, previousContents);

            LaunchPadBuilderBlockEntity.super.setChanged();
            LaunchPadBuilderBlockEntity.this.updateConfigurationSettings();
        }
    };

    public LaunchPadBuilderBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntityTypes.LAUNCH_PAD_BUILDER.get(), pos, blockState);
    }

    protected void updateConfigurationSettings() {
        final ItemResource resource = this.inventoryHandler.getResource(0);
        if (!resource.has(ModDataComponentTypes.CONFIGURATION_PATH_DATA.get())) {
            if (this.level != null && this.level.isClientSide()) {
                PreviewClientEvents.LAUNCH_PAD_BUILDER_POS.remove(this.getBlockPos());
                PreviewClientEvents.LAUNCH_PAD_PREVIEW_BLOCKS.remove(this.getBlockPos());
            }
            return;
        }

        final UUID uuid = resource.get(ModDataComponentTypes.CONFIGURATION_PATH_DATA.get());
        if (uuid != null) {
            final NetworkConfiguration configuration = ClientConfigurationSavedData.INSTANCE.get(uuid);
            if (configuration != null) {
                ClientPacketDistributor.sendToServer(new SetConfigurationStackPayload(this.getBlockPos(), configuration.launchPadConfiguration()));
            }
        }
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);

        this.inventoryHandler.deserialize(input.childOrEmpty("inventory"));
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);

        this.inventoryHandler.serialize(output.child("inventory"));
    }

    @Override
    public void onLoad() {
        super.onLoad();

        this.updateConfigurationSettings();
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
    public Component getName() {
        return Component.translatable(ModBlocks.LAUNCH_PAD_BUILDER.get().getDescriptionId());
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new LaunchPadBuilderContainerMenu(containerId, inventory, this, ContainerLevelAccess.create(this.level, this.getBlockPos()));
    }
}
