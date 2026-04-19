package com.ultramega.asteroidmining.blockentities;

import com.ultramega.asteroidmining.config.ServerConfig;
import com.ultramega.asteroidmining.container.AirAbsorberContainerMenu;
import com.ultramega.asteroidmining.registry.ModBlockEntityTypes;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.registry.ModFluids;
import com.ultramega.asteroidmining.utils.MutableEnergy;
import com.ultramega.asteroidmining.utils.PreserveData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

public class AirAbsorberBlockEntity extends AbstractDataPreservingBlockEntity implements MenuProvider, Nameable, PreserveData {
    public final MutableEnergy energyStorage = new MutableEnergy(ServerConfig.AIR_ABSORBER_ENERGY_CAPACITY.get());
    public final FluidStacksResourceHandler fluidTank = new FluidStacksResourceHandler(1, ServerConfig.AIR_ABSORBER_TANK_CAPACITY.get()) {
        @Override
        protected void onContentsChanged(final int index, final FluidStack previousContents) {
            AirAbsorberBlockEntity.this.setChanged();
        }
    };
    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(final int index) {
            return switch (index) {
                case 0 -> AirAbsorberBlockEntity.this.energyStorage.getAmountAsInt();
                case 1 -> AirAbsorberBlockEntity.this.energyStorage.getCapacityAsInt();
                default -> throw new IllegalStateException("Unexpected value: " + index);
            };
        }

        @Override
        public void set(final int index, final int value) {
            switch (index) {
                case 0:
                    AirAbsorberBlockEntity.this.energyStorage.set(value);
                    break;
                case 1:
                    AirAbsorberBlockEntity.this.energyStorage.setCapacity(value);
                    break;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    private int airAmount;

    public AirAbsorberBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntityTypes.AIR_ABSORBER.get(), pos, blockState);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final AirAbsorberBlockEntity blockEntity) {
        if (blockEntity.cannotOperate()) {
            return;
        }
        if (blockEntity.fluidTank.getAmountAsInt(0) >= blockEntity.fluidTank.getCapacityAsInt(0, blockEntity.fluidTank.getResource(0))) {
            return;
        }

        //TODO: add cooldown?
        try (Transaction tx = Transaction.openRoot()) {
            blockEntity.fluidTank.insert(FluidResource.of(ModFluids.AIR.get()), blockEntity.airAmount, tx);
            blockEntity.energyStorage.extract(blockEntity.airAmount * 20, tx);
            tx.commit();
        }
    }

    public boolean cannotOperate() {
        return this.energyStorage.getAmountAsInt() < this.airAmount * 20; //TODO: look into the energy usage
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);

        this.energyStorage.deserialize(input);
        this.fluidTank.deserialize(input);
        this.airAmount = input.getInt("airAmount").orElse(0);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);

        this.energyStorage.serialize(output);
        this.fluidTank.serialize(output);
        output.putInt("airAmount", this.airAmount);
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
        super.setChanged();

        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void neighborChanged() {
        int result = 0;

        for (final Direction direction : Direction.values()) {
            if (this.level.getBlockState(this.getBlockPos().relative(direction)).isAir()) {
                result++;
            }
        }

        this.airAmount = result;
    }

    @Override
    public Component getName() {
        return Component.translatable(ModBlocks.AIR_ABSORBER.get().getDescriptionId());
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new AirAbsorberContainerMenu(containerId, inventory, this, ContainerLevelAccess.create(this.level, this.getBlockPos()), this.containerData);
    }
}
