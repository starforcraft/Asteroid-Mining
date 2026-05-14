package com.ultramega.asteroidmining.blockentities;

import com.ultramega.asteroidmining.container.RocketStorageViewerContainerMenu;
import com.ultramega.asteroidmining.registry.ModBlockEntityTypes;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.storage.ClientConfigurationSavedData;
import com.ultramega.asteroidmining.storage.ConfigurationSavedData;
import com.ultramega.asteroidmining.storage.NetworkConfiguration;
import com.ultramega.asteroidmining.utils.handlers.UnlimitedResourceStore;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

public class RocketStorageViewerBlockEntity extends AbstractModuleBlockEntity implements MenuProvider, Nameable {
    private final ResourceHandler<ItemResource> itemStorage = UnlimitedResourceStore.items(
        () -> {
            final NetworkConfiguration configuration = this.getConfiguration();
            return configuration == null ? null : configuration.moduleProperties();
        },
        properties -> {
            final NetworkConfiguration configuration = this.getConfiguration();
            if (configuration == null) {
                return;
            }
            this.setConfiguration(configuration.withModuleProperties(properties));
        }, this::setChanged);

    private final ResourceHandler<FluidResource> fluidStorage = UnlimitedResourceStore.fluids(
        () -> {
            final NetworkConfiguration configuration = this.getConfiguration();
            return configuration == null ? null : configuration.moduleProperties();
        },
        properties -> {
            final NetworkConfiguration configuration = this.getConfiguration();
            if (configuration == null) {
                return;
            }
            this.setConfiguration(configuration.withModuleProperties(properties));
        }, this::setChanged);

    public RocketStorageViewerBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntityTypes.ROCKET_STORAGE_VIEWER.get(), pos, blockState);
    }

    @Override
    public Component getName() {
        return Component.translatable(ModBlocks.ROCKET_STORAGE_VIEWER.get().getDescriptionId());
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Override
    public void setChanged() {
        super.setChanged();

        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        final var menu = new RocketStorageViewerContainerMenu(containerId, inventory, this, ContainerLevelAccess.create(this.level, this.getBlockPos()));
        menu.setOverwriteStillValid(this.overwriteStillValid);
        return menu;
    }

    @Nullable
    private NetworkConfiguration getConfiguration() {
        final UUID id = this.getSelectedConfigurationUUID();
        if (id == null) {
            return null;
        }

        if (this.getLevel() instanceof ServerLevel serverLevel) {
            return ConfigurationSavedData.getConfigurationData(serverLevel).get(id);
        } else {
            return ClientConfigurationSavedData.INSTANCE.get(id);
        }
    }

    private void setConfiguration(final NetworkConfiguration configuration) {
        final UUID id = this.getSelectedConfigurationUUID();
        if (id == null) {
            return;
        }

        if (this.getLevel() instanceof ServerLevel serverLevel) {
            ConfigurationSavedData.getConfigurationData(serverLevel).set(id, configuration);
        } else {
            ClientConfigurationSavedData.INSTANCE.put(id, configuration);
        }
    }

    public ResourceHandler<ItemResource> getItemHandler(@Nullable final Direction side) {
        return this.itemStorage;
    }

    public ResourceHandler<FluidResource> getFluidHandler(@Nullable final Direction side) {
        return this.fluidStorage;
    }
}
