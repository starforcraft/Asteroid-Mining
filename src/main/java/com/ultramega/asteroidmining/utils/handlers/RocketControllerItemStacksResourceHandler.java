package com.ultramega.asteroidmining.utils.handlers;

import com.ultramega.asteroidmining.blockentities.RocketControllerBlockEntity;
import com.ultramega.asteroidmining.events.PreviewClientEvents;
import com.ultramega.asteroidmining.network.s2c.SendLaunchPreviewDataPayload;
import com.ultramega.asteroidmining.registry.ModDataComponentTypes;
import com.ultramega.asteroidmining.storage.ConfigurationSavedData;
import com.ultramega.asteroidmining.storage.NetworkConfiguration;
import com.ultramega.asteroidmining.utils.CommonUtils;
import com.ultramega.asteroidmining.utils.PreviewInfo;

import java.util.List;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class RocketControllerItemStacksResourceHandler extends ItemStacksResourceHandler {
    private final RocketControllerBlockEntity blockEntity;

    public RocketControllerItemStacksResourceHandler(final int size, final RocketControllerBlockEntity blockEntity) {
        super(size);
        this.blockEntity = blockEntity;
    }

    @Override
    public void onContentsChanged(final int index, final ItemStack previousContents) {
        this.blockEntity.setChanged();

        final int selectedIndex = this.blockEntity.getSelectedConfigurationIndex();
        if (selectedIndex == -1) {
            return;
        }
        final ItemResource stack = this.getResource(selectedIndex);
        if (!stack.has(ModDataComponentTypes.CONFIGURATION_PATH_DATA.get())) {
            if (this.blockEntity.getLevel() != null && this.blockEntity.getLevel().isClientSide()) {
                PreviewClientEvents.LAUNCH_PAD_BUILDER_POS.remove(this.blockEntity.getBlockPos());
                PreviewClientEvents.LAUNCH_PAD_PREVIEW_BLOCKS.remove(this.blockEntity.getBlockPos());
            }
        } else {
            if (this.blockEntity.getLevel() instanceof ServerLevel serverLevel) {
                final UUID uuid = stack.get(ModDataComponentTypes.CONFIGURATION_PATH_DATA.get());
                if (uuid == null) {
                    return;
                }
                final NetworkConfiguration configuration = ConfigurationSavedData.getConfigurationData(serverLevel).get(uuid);
                if (configuration != null) {
                    final List<PreviewInfo> previewInfos = CommonUtils.calculateSpacePort(this.blockEntity.getLevel(),
                        configuration.launchPadConfiguration(), false);
                    PacketDistributor.sendToAllPlayers(new SendLaunchPreviewDataPayload(this.blockEntity.getBlockPos(), uuid, previewInfos));
                }
            }
        }
    }

    @Override
    public void deserialize(final ValueInput input) {
        super.deserialize(input);
        this.triggerContentsChanged();
    }

    public void triggerContentsChanged() {
        this.onContentsChanged(-1, ItemStack.EMPTY);
    }
}
