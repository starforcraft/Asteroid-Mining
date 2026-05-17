package com.ultramega.asteroidmining.blockentities;

import com.ultramega.asteroidmining.blocks.BoundingBoxBlock;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.utils.PreserveData;
import com.ultramega.asteroidmining.utils.handlers.SidedEnergyHandler;
import com.ultramega.asteroidmining.utils.handlers.SidedResourceHandler;
import com.ultramega.asteroidmining.utils.sides.SideConfigType;
import com.ultramega.asteroidmining.utils.sides.SideIoMode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

public abstract class AbstractSideConfigurableBlockEntity extends AbstractDataPreservingBlockEntity implements PreserveData {
    public static final int SIDE_COUNT = Direction.values().length;
    public static final int SIDE_CONFIG_DATA_COUNT = SideConfigType.values().length * SIDE_COUNT;

    private final SideIoMode[][] sideConfigs = new SideIoMode[SideConfigType.values().length][SIDE_COUNT];

    @Nullable
    private final EnergyHandler[] sidedEnergyHandlers = new EnergyHandler[SIDE_COUNT];

    @SuppressWarnings("unchecked")
    @Nullable
    private final ResourceHandler<ItemResource>[] sidedItemHandlers = (ResourceHandler<ItemResource>[]) new ResourceHandler<?>[SIDE_COUNT];

    @SuppressWarnings("unchecked")
    @Nullable
    private final ResourceHandler<FluidResource>[] sidedFluidHandlers = (ResourceHandler<FluidResource>[]) new ResourceHandler<?>[SIDE_COUNT];

    @SuppressWarnings("unchecked")
    @Nullable
    private final ResourceHandler<FluidResource>[] sidedGasesHandlers = (ResourceHandler<FluidResource>[]) new ResourceHandler<?>[SIDE_COUNT];

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(final int index) {
            final int machineDataCount = AbstractSideConfigurableBlockEntity.this.getMachineDataCount();
            final int sideConfigEnd = machineDataCount + SIDE_CONFIG_DATA_COUNT;

            if (index < machineDataCount) {
                return AbstractSideConfigurableBlockEntity.this.getMachineData(index);
            }

            if (index < sideConfigEnd) {
                final int relative = index - machineDataCount;
                final SideConfigType type = SideConfigType.values()[relative / SIDE_COUNT];
                final Direction side = Direction.values()[relative % SIDE_COUNT];

                return AbstractSideConfigurableBlockEntity.this.getSideConfig(type, side).ordinal();
            }

            throw new IllegalStateException("Unexpected container data index: " + index);
        }

        @Override
        public void set(final int index, final int value) {
            final int machineDataCount = AbstractSideConfigurableBlockEntity.this.getMachineDataCount();
            final int sideConfigEnd = machineDataCount + SIDE_CONFIG_DATA_COUNT;

            if (index < machineDataCount) {
                AbstractSideConfigurableBlockEntity.this.setMachineData(index, value);
                return;
            }

            if (index < sideConfigEnd) {
                final int relative = index - machineDataCount;
                final SideConfigType type = SideConfigType.values()[relative / SIDE_COUNT];
                final Direction side = Direction.values()[relative % SIDE_COUNT];

                AbstractSideConfigurableBlockEntity.this.sideConfigs[type.ordinal()][side.ordinal()] = SideIoMode.byId(value);
                return;
            }

            throw new IllegalStateException("Unexpected container data index: " + index);
        }

        @Override
        public int getCount() {
            return AbstractSideConfigurableBlockEntity.this.getContainerDataCount();
        }
    };

    public AbstractSideConfigurableBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState blockState) {
        super(type, pos, blockState);

        for (final SideConfigType sideType : SideConfigType.values()) {
            for (final Direction side : Direction.values()) {
                this.sideConfigs[sideType.ordinal()][side.ordinal()] = this.getDefaultSideConfig(sideType, side);
            }
        }
    }

    protected boolean autoEjectEnergyToConfiguredOutputs(final int maxEnergyPerSide) {
        if (this.level == null || this.level.isClientSide()) {
            return false;
        }

        if (maxEnergyPerSide <= 0) {
            return false;
        }

        final EnergyHandler source = this.getEnergyStorageForSideConfig();
        if (source == null || source.getAmountAsLong() <= 0) {
            return false;
        }

        boolean changed = false;
        for (final Direction side : Direction.values()) {
            if (!this.getSideConfig(SideConfigType.ENERGY, side).canOutput()) {
                continue;
            }

            if (source.getAmountAsLong() <= 0) {
                break;
            }

            for (final EnergyHandler target : this.getEnergyOutputTargets(side)) {
                final int amountToTry = (int) Math.min(maxEnergyPerSide, source.getAmountAsLong());
                if (amountToTry <= 0) {
                    break;
                }

                final int accepted;
                try (Transaction tx = Transaction.openRoot()) {
                    final int extracted = source.extract(amountToTry, tx);
                    accepted = target.insert(extracted, tx);
                }
                if (accepted <= 0) {
                    continue;
                }

                try (Transaction tx = Transaction.openRoot()) {
                    final int extracted = source.extract(accepted, tx);
                    if (extracted != accepted) {
                        continue;
                    }

                    final int inserted = target.insert(extracted, tx);
                    if (inserted != extracted) {
                        continue;
                    }

                    tx.commit();
                    changed = true;
                }
            }
        }

        return changed;
    }

    private List<EnergyHandler> getEnergyOutputTargets(final Direction side) {
        if (this.level == null) {
            return List.of();
        }

        final List<EnergyHandler> targets = new ArrayList<>();
        final Set<BlockPos> checkedTargets = new HashSet<>();
        for (final BlockPos outputPartPos : this.getConnectedOutputPartPositions()) {
            final BlockPos targetPos = outputPartPos.relative(side);
            if (targetPos.equals(this.getBlockPos()) || this.isConnectedBoundingBox(targetPos)) {
                continue;
            }
            if (!checkedTargets.add(targetPos)) {
                continue;
            }

            final EnergyHandler target = this.level.getCapability(Capabilities.Energy.BLOCK, targetPos, side.getOpposite());
            if (target != null) {
                targets.add(target);
            }
        }

        return targets;
    }

    private Set<BlockPos> getConnectedOutputPartPositions() {
        final Set<BlockPos> parts = new HashSet<>();
        final Queue<BlockPos> queue = new ArrayDeque<>();

        final BlockPos mainPos = this.getBlockPos();
        parts.add(mainPos);
        queue.add(mainPos);

        while (!queue.isEmpty()) {
            final BlockPos current = queue.remove();
            for (final Direction direction : Direction.values()) {
                final BlockPos next = current.relative(direction);
                if (parts.contains(next)) {
                    continue;
                }

                if (!this.isConnectedBoundingBox(next)) {
                    continue;
                }

                parts.add(next);
                queue.add(next);
            }
        }

        return parts;
    }

    private boolean isConnectedBoundingBox(final BlockPos pos) {
        if (this.level == null || !this.level.getBlockState(pos).is(ModBlocks.BOUNDING_BOX.get())) {
            return false;
        }

        final BlockPos mainPos = BoundingBoxBlock.getMainBlockPos(this.level, pos);
        return mainPos != null && mainPos.equals(this.getBlockPos());
    }

    protected SideIoMode getDefaultSideConfig(final SideConfigType type, final Direction side) {
        return switch (type) {
            case ENERGY -> SideIoMode.INPUT;
            case ITEMS -> SideIoMode.BOTH;
            case FLUIDS -> side != Direction.UP ? SideIoMode.INPUT : SideIoMode.NONE;
            case GASES -> side != Direction.DOWN ? SideIoMode.INPUT : SideIoMode.NONE;
        };
    }

    protected abstract int getMachineDataCount();

    protected abstract int getMachineData(int index);

    protected abstract void setMachineData(int index, int value);

    @Nullable
    protected EnergyHandler getEnergyStorageForSideConfig() {
        return null;
    }

    @Nullable
    protected ResourceHandler<ItemResource> getItemHandlerForSideConfig() {
        return null;
    }

    @Nullable
    protected ResourceHandler<FluidResource> getFluidHandlerForSideConfig() {
        return null;
    }

    @Nullable
    protected ResourceHandler<FluidResource> getGasHandlerForSideConfig() {
        return null;
    }

    public final ContainerData getContainerData() {
        return this.containerData;
    }

    public final int getContainerDataCount() {
        return this.getMachineDataCount() + SIDE_CONFIG_DATA_COUNT;
    }

    public boolean supportsSideConfig(final SideConfigType type) {
        return switch (type) {
            case ENERGY -> this.getEnergyStorageForSideConfig() != null;
            case ITEMS -> this.getItemHandlerForSideConfig() != null;
            case FLUIDS -> this.getFluidHandlerForSideConfig() != null;
            case GASES -> this.getGasHandlerForSideConfig() != null;
        };
    }

    public SideIoMode getSideConfig(final SideConfigType type, final Direction side) {
        return this.sideConfigs[type.ordinal()][side.ordinal()];
    }

    public void setSideConfig(final SideConfigType type, final Direction side, final SideIoMode mode) {
        if (!this.supportsSideConfig(type)) {
            return;
        }

        if (this.getSideConfig(type, side) == mode) {
            return;
        }

        this.sideConfigs[type.ordinal()][side.ordinal()] = mode;
        this.setChanged();

        if (this.level != null && !this.level.isClientSide()) {
            this.level.invalidateCapabilities(this.getBlockPos());
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Nullable
    public EnergyHandler getEnergyCapability(@Nullable final Direction side) {
        final EnergyHandler delegate = this.getEnergyStorageForSideConfig();
        if (delegate == null) {
            return null;
        }

        if (side == null) {
            return delegate;
        }

        if (this.getSideConfig(SideConfigType.ENERGY, side) == SideIoMode.NONE) {
            return null;
        }

        final int index = side.ordinal();
        if (this.sidedEnergyHandlers[index] == null) {
            this.sidedEnergyHandlers[index] = new SidedEnergyHandler(
                delegate,
                () -> this.getSideConfig(SideConfigType.ENERGY, side).canInput(),
                () -> this.getSideConfig(SideConfigType.ENERGY, side).canOutput()
            );
        }

        return this.sidedEnergyHandlers[index];
    }

    @Nullable
    public ResourceHandler<ItemResource> getItemCapability(@Nullable final Direction side) {
        final ResourceHandler<ItemResource> delegate = this.getItemHandlerForSideConfig();
        if (delegate == null) {
            return null;
        }

        if (side == null) {
            return delegate;
        }

        if (this.getSideConfig(SideConfigType.ITEMS, side) == SideIoMode.NONE) {
            return null;
        }

        final int index = side.ordinal();
        if (this.sidedItemHandlers[index] == null) {
            this.sidedItemHandlers[index] = new SidedResourceHandler<>(
                delegate,
                () -> this.getSideConfig(SideConfigType.ITEMS, side).canInput(),
                () -> this.getSideConfig(SideConfigType.ITEMS, side).canOutput()
            );
        }

        return this.sidedItemHandlers[index];
    }

    @Nullable
    public ResourceHandler<FluidResource> getFluidCapability(@Nullable final Direction side) {
        final ResourceHandler<FluidResource> delegate = this.getFluidHandlerForSideConfig();
        if (delegate == null) {
            return null;
        }

        if (side == null) {
            return delegate;
        }

        if (this.getSideConfig(SideConfigType.FLUIDS, side) == SideIoMode.NONE) {
            return null;
        }

        final int index = side.ordinal();
        if (this.sidedFluidHandlers[index] == null) {
            this.sidedFluidHandlers[index] = new SidedResourceHandler<>(
                delegate,
                () -> this.getSideConfig(SideConfigType.FLUIDS, side).canInput(),
                () -> this.getSideConfig(SideConfigType.FLUIDS, side).canOutput()
            );
        }

        return this.sidedFluidHandlers[index];
    }

    @Nullable
    public ResourceHandler<FluidResource> getGasCapability(@Nullable final Direction side) {
        final ResourceHandler<FluidResource> delegate = this.getGasHandlerForSideConfig();
        if (delegate == null) {
            return null;
        }

        if (side == null) {
            return delegate;
        }

        if (this.getSideConfig(SideConfigType.GASES, side) == SideIoMode.NONE) {
            return null;
        }

        final int index = side.ordinal();
        if (this.sidedGasesHandlers[index] == null) {
            this.sidedGasesHandlers[index] = new SidedResourceHandler<>(
                delegate,
                () -> this.getSideConfig(SideConfigType.GASES, side).canInput(),
                () -> this.getSideConfig(SideConfigType.GASES, side).canOutput()
            );
        }

        return this.sidedGasesHandlers[index];
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);

        for (final SideConfigType type : SideConfigType.values()) {
            for (final Direction side : Direction.values()) {
                this.sideConfigs[type.ordinal()][side.ordinal()] = SideIoMode.byId(input.getInt(sideConfigKey(type, side))
                    .orElse(this.getDefaultSideConfig(type, side).ordinal()));
            }
        }
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);

        for (final SideConfigType type : SideConfigType.values()) {
            for (final Direction side : Direction.values()) {
                output.putInt(sideConfigKey(type, side), this.getSideConfig(type, side).ordinal());
            }
        }
    }

    private static String sideConfigKey(final SideConfigType type, final Direction side) {
        return "sideConfig_" + type.ordinal() + "_" + side.ordinal();
    }
}
