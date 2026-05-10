package com.ultramega.asteroidmining.container;

import com.ultramega.asteroidmining.blockentities.AbstractSideConfigurableBlockEntity;
import com.ultramega.asteroidmining.utils.sides.SideConfigType;
import com.ultramega.asteroidmining.utils.sides.SideIoMode;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import org.jspecify.annotations.Nullable;

public abstract class AbstractSideConfigContainerMenu<T extends AbstractSideConfigurableBlockEntity> extends AbstractContainerMenu {
    public final T blockEntity;

    protected final ContainerLevelAccess access;
    protected final ContainerData data;

    private final int machineDataCount;

    protected AbstractSideConfigContainerMenu(@Nullable final MenuType<?> menuType,
                                              final int containerId,
                                              final Inventory playerInventory,
                                              final T blockEntity,
                                              final ContainerLevelAccess access,
                                              final ContainerData data,
                                              final int machineDataCount) {
        super(menuType, containerId);

        this.blockEntity = blockEntity;
        this.access = access;
        this.data = data;
        this.machineDataCount = machineDataCount;

        checkContainerDataCount(data, machineDataCount + AbstractSideConfigurableBlockEntity.SIDE_CONFIG_DATA_COUNT);
    }

    public static SimpleContainerData createClientData(final int machineDataCount) {
        return new SimpleContainerData(machineDataCount + AbstractSideConfigurableBlockEntity.SIDE_CONFIG_DATA_COUNT);
    }

    protected final void addSideConfigDataSlots() {
        this.addDataSlots(this.data);
    }

    public final SideIoMode getSideConfig(final SideConfigType type, final Direction side) {
        return SideIoMode.byId(this.data.get(this.sideConfigDataIndex(type, side)));
    }

    public final void setClientSideConfig(final SideConfigType type, final Direction side, final SideIoMode mode) {
        this.data.set(this.sideConfigDataIndex(type, side), mode.ordinal());
    }

    public final boolean supportsSideConfig(final SideConfigType type) {
        return this.blockEntity.supportsSideConfig(type);
    }

    private int sideConfigDataIndex(final SideConfigType type, final Direction side) {
        return this.machineDataCount + type.ordinal() * AbstractSideConfigurableBlockEntity.SIDE_COUNT + side.ordinal();
    }
}
