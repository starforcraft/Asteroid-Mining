package com.ultramega.asteroidmining.blockentities;

import com.ultramega.asteroidmining.registry.ModDataComponentTypes;
import com.ultramega.asteroidmining.storage.ClientConfigurationSavedData;
import com.ultramega.asteroidmining.storage.ConfigurationSavedData;
import com.ultramega.asteroidmining.storage.NetworkConfiguration;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

public abstract class AbstractModuleBlockEntity extends AbstractDataPreservingBlockEntity {
    private static final String TAG_CONTROLLER_POS = "controllerPos";

    protected boolean overwriteStillValid;
    @Nullable
    private BlockPos controllerPos;

    public AbstractModuleBlockEntity(final BlockEntityType<?> type,
                                     final BlockPos pos,
                                     final BlockState blockState) {
        super(type, pos, blockState);
    }

    @Nullable
    public UUID getSelectedConfigurationUUID() {
        final SelectedConfigurationResult result = this.getSelectedConfigurationResult();
        return result.uuid();
    }

    public SelectedConfigurationResult getSelectedConfigurationResult() {
        if (this.level == null) {
            return SelectedConfigurationResult.error(Component.translatable("gui.asteroidmining.rocket_controller.invalid_configuration"));
        }

        final BlockPos targetPos = this.controllerPos != null ? this.controllerPos : this.getBlockPos();
        final BlockEntity blockEntity = this.level.getBlockEntity(targetPos);

        if (!(blockEntity instanceof RocketControllerBlockEntity controller)) {
            return SelectedConfigurationResult.error(Component.translatable("gui.asteroidmining.rocket_controller.invalid_configuration"));
        }

        final int selectedConfiguration = controller.getSelectedConfigurationIndex();
        if (selectedConfiguration == -1) {
            return SelectedConfigurationResult.error(Component.translatable("gui.asteroidmining.rocket_controller.no_configuration_selected"));
        }

        final ItemResource resource = controller.inventoryHandler.getResource(selectedConfiguration);
        if (resource.isEmpty() || !resource.has(ModDataComponentTypes.CONFIGURATION_PATH_DATA.get())) {
            return SelectedConfigurationResult.error(Component.translatable("gui.asteroidmining.rocket_controller.invalid_configuration"));
        }

        final UUID uuid = resource.get(ModDataComponentTypes.CONFIGURATION_PATH_DATA.get());
        if (uuid == null) {
            return SelectedConfigurationResult.error(Component.translatable("gui.asteroidmining.rocket_controller.invalid_configuration"));
        }

        return SelectedConfigurationResult.valid(uuid);
    }

    @Nullable
    public NetworkConfiguration getSelectedClientNetworkConfiguration() {
        final UUID uuid = this.getSelectedConfigurationUUID();
        if (uuid != null) {
            return ClientConfigurationSavedData.INSTANCE.get(uuid);
        }

        return null;
    }

    @Nullable
    public NetworkConfiguration getSelectedServerNetworkConfiguration() {
        if (this.level == null || !(this.level instanceof ServerLevel serverLevel)) {
            return null;
        }

        final UUID uuid = this.getSelectedConfigurationUUID();
        if (uuid != null) {
            return ConfigurationSavedData.getConfigurationData(serverLevel).get(uuid);
        }

        return null;
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        input.read(TAG_CONTROLLER_POS, BlockPos.CODEC).ifPresent(pos -> this.controllerPos = pos);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        if (this.controllerPos != null) {
            output.store(TAG_CONTROLLER_POS, BlockPos.CODEC, this.controllerPos);
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
    public void setChanged() {
        // Don't cause useless Level#updateNeighbourForOutputSignal update as we don't use any redstone logic at all
        if (this.level != null) {
            this.level.blockEntityChanged(this.worldPosition);
        }
    }

    public void setControllerPos(final BlockPos controllerPos) {
        this.controllerPos = controllerPos;
    }

    @Nullable
    public BlockPos getControllerPos() {
        return this.controllerPos;
    }

    public void setOverwriteStillValid(final boolean overwriteStillValid) {
        this.overwriteStillValid = overwriteStillValid;
    }

    public record SelectedConfigurationResult(@Nullable UUID uuid, @Nullable Component errorMessage) {
        public boolean isValid() {
            return this.uuid != null;
        }

        public boolean hasError() {
            return this.errorMessage != null;
        }

        public static SelectedConfigurationResult valid(final UUID uuid) {
            return new SelectedConfigurationResult(uuid, null);
        }

        public static SelectedConfigurationResult error(final Component message) {
            return new SelectedConfigurationResult(null, message);
        }
    }
}
