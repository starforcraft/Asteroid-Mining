package com.ultramega.asteroidmining.blockentities;

import com.ultramega.asteroidmining.config.ServerConfig;
import com.ultramega.asteroidmining.container.ElectrolysisPlantContainerMenu;
import com.ultramega.asteroidmining.registry.ModBlockEntityTypes;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.registry.ModFluids;
import com.ultramega.asteroidmining.utils.handlers.MultiFluidStacksResourceHandler;
import com.ultramega.asteroidmining.utils.handlers.MutableEnergy;
import com.ultramega.asteroidmining.utils.PreserveData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

public class ElectrolysisPlantBlockEntity extends AbstractDataPreservingBlockEntity implements MenuProvider, Nameable, PreserveData {
    public final MutableEnergy energyStorage = new MutableEnergy(ServerConfig.ELECTROLYSIS_PLANT_ENERGY_CAPACITY.get());
    public final MultiFluidStacksResourceHandler fluidTank = new MultiFluidStacksResourceHandler(2, new int[] {
        ServerConfig.ELECTROLYSIS_PLANT_TANK_CAPACITY.get(),
        ServerConfig.ELECTROLYSIS_PLANT_TANK_CAPACITY.get()
    }) {
        @Override
        protected void onContentsChanged(final int index, final FluidStack previousContents) {
            if (this.getAmountAsInt(0) == 0 && ElectrolysisPlantBlockEntity.this.level instanceof ServerLevel) {
                ElectrolysisPlantBlockEntity.this.recipeProgress = ServerConfig.ELECTROLYSIS_PLANT_RECIPE_DURATION.get();
            }
            ElectrolysisPlantBlockEntity.this.setChanged();
        }
    };
    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(final int index) {
            return switch (index) {
                case 0 -> ElectrolysisPlantBlockEntity.this.energyStorage.getAmountAsInt();
                case 1 -> ElectrolysisPlantBlockEntity.this.energyStorage.getCapacityAsInt();
                case 2 -> ElectrolysisPlantBlockEntity.this.recipeProgress;
                default -> throw new IllegalStateException("Unexpected value: " + index);
            };
        }

        @Override
        public void set(final int index, final int value) {
            switch (index) {
                case 0:
                    ElectrolysisPlantBlockEntity.this.energyStorage.set(value);
                    break;
                case 1:
                    ElectrolysisPlantBlockEntity.this.energyStorage.setCapacity(value);
                    break;
                case 2:
                    ElectrolysisPlantBlockEntity.this.recipeProgress = value;
                    break;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    private int recipeProgress;

    //TODO: add jei support
    public ElectrolysisPlantBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntityTypes.ELECTROLYSIS_PLANT.get(), pos, blockState);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final ElectrolysisPlantBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel) || blockEntity.cannotOperate()) {
            return;
        }
        if (blockEntity.fluidTank.getRemainingSpace(1) == 0) {
            return;
        }

        if (blockEntity.fluidTank.getRemainingSpace(1) >= 10) {
            final FluidStack inputStack = new FluidStack(Fluids.WATER, 10);
            final FluidStack outputStack = new FluidStack(ModFluids.HYDROGEN.get(), 8);
            try (Transaction tx = Transaction.openRoot()) {
                if (blockEntity.fluidTank.insert(1, FluidResource.of(outputStack), outputStack.getAmount(), tx) <= 0) {
                    return;
                }
            }
            if (--blockEntity.recipeProgress > 0) {
                return;
            }

            try (Transaction tx = Transaction.openRoot()) {
                blockEntity.fluidTank.extract(FluidResource.of(inputStack), inputStack.getAmount(), tx);
                blockEntity.fluidTank.insert(1, FluidResource.of(outputStack), outputStack.getAmount(), tx);
                blockEntity.energyStorage.extract(ServerConfig.ELECTROLYSIS_PLANT_ENERGY_USAGE.get(), tx);
                tx.commit();
            }

            blockEntity.recipeProgress = ServerConfig.ELECTROLYSIS_PLANT_RECIPE_DURATION.get();
        }
    }

    public boolean cannotOperate() {
        return this.energyStorage.getAmountAsInt() < ServerConfig.ELECTROLYSIS_PLANT_ENERGY_USAGE.get();
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);

        this.energyStorage.deserialize(input.childOrEmpty("energy"));
        this.fluidTank.deserialize(input.childOrEmpty("fluidTank"));
        this.recipeProgress = input.getInt("recipeProgress").orElse(0);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);

        this.energyStorage.serialize(output.child("energy"));
        this.fluidTank.serialize(output.child("fluidTank"));
        output.putInt("recipeProgress", this.recipeProgress);
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

    @Override
    public Component getName() {
        return Component.translatable(ModBlocks.ELECTROLYSIS_PLANT.get().getDescriptionId());
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new ElectrolysisPlantContainerMenu(containerId, inventory, this, ContainerLevelAccess.create(this.level, this.getBlockPos()), this.containerData);
    }
}
