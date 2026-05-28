package com.ultramega.asteroidmining.blockentities;

import com.ultramega.asteroidmining.config.ServerConfig;
import com.ultramega.asteroidmining.container.BiogasPlantContainerMenu;
import com.ultramega.asteroidmining.registry.ModBlockEntityTypes;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.registry.ModFluids;
import com.ultramega.asteroidmining.utils.PreserveData;
import com.ultramega.asteroidmining.utils.handlers.MutableEnergy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

public class BiogasPlantBlockEntity extends AbstractDataPreservingBlockEntity implements MenuProvider, Nameable, PreserveData {
    public static final int RECIPE_DURATION = 20;

    private static final String TAG_ENERGY = "energy";
    private static final String TAG_INVENTORY = "inventory";
    private static final String TAG_FLUID_TANK = "fluidTank";
    private static final String TAG_RECIPE_PROGRESS = "recipeProgress";

    public final MutableEnergy energyStorage = new MutableEnergy(ServerConfig.BIOGAS_PLANT_ENERGY_CAPACITY.get());
    public final ItemStacksResourceHandler inventoryHandler = new ItemStacksResourceHandler(1) {
        @Override
        protected void onContentsChanged(final int index, final ItemStack previousContents) {
            super.onContentsChanged(index, previousContents);

            BiogasPlantBlockEntity.this.recipeProgress = RECIPE_DURATION;
            BiogasPlantBlockEntity.this.setChanged();
        }
    };
    public final FluidStacksResourceHandler fluidTank = new FluidStacksResourceHandler(1, ServerConfig.BIOGAS_PLANT_TANK_CAPACITY.get()) {
        @Override
        protected void onContentsChanged(final int index, final FluidStack previousContents) {
            super.onContentsChanged(index, previousContents);
            BiogasPlantBlockEntity.this.setChanged();
        }
    };
    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(final int index) {
            return switch (index) {
                case 0 -> BiogasPlantBlockEntity.this.energyStorage.getAmountAsInt();
                case 1 -> BiogasPlantBlockEntity.this.energyStorage.getCapacityAsInt();
                case 2 -> BiogasPlantBlockEntity.this.recipeProgress;
                default -> throw new IllegalStateException("Unexpected value: " + index);
            };
        }

        @Override
        public void set(final int index, final int value) {
            switch (index) {
                case 0:
                    BiogasPlantBlockEntity.this.energyStorage.set(value);
                    break;
                case 1:
                    BiogasPlantBlockEntity.this.energyStorage.setCapacity(value);
                    break;
                case 2:
                    BiogasPlantBlockEntity.this.recipeProgress = value;
                    break;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    private int recipeProgress = RECIPE_DURATION;

    //TODO: add jei support
    public BiogasPlantBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntityTypes.BIOGAS_PLANT.get(), pos, blockState);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final BiogasPlantBlockEntity blockEntity) {
        if (blockEntity.cannotOperate()) {
            return;
        }
        if (blockEntity.fluidTank.getAmountAsInt(0) >= blockEntity.fluidTank.getCapacityAsInt(0, blockEntity.fluidTank.getResource(0))) {
            return;
        }

        final ItemResource resource = blockEntity.inventoryHandler.getResource(0);
        if (resource.is(Tags.Items.FOODS)) {
            final FoodProperties foodProperties = resource.get(DataComponents.FOOD);
            if (foodProperties == null) {
                return;
            }
            if (--blockEntity.recipeProgress > 0) {
                return;
            }

            final int amount = Math.round(foodProperties.saturation() * foodProperties.nutrition()); //TODO: update formula?

            try (Transaction tx = Transaction.openRoot()) {
                blockEntity.fluidTank.insert(FluidResource.of(ModFluids.METHANE.get()), amount, tx); //TODO: add 1/3 carbon dioxide
                blockEntity.energyStorage.extract(ServerConfig.BIOGAS_PLANT_ENERGY_USAGE.get(), tx);
                blockEntity.inventoryHandler.extract(resource, 1, tx);
                tx.commit();
            }

            blockEntity.recipeProgress = RECIPE_DURATION;
        }
    }

    public boolean cannotOperate() {
        return this.energyStorage.getAmountAsInt() < ServerConfig.BIOGAS_PLANT_ENERGY_USAGE.get();
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.energyStorage.deserialize(input.childOrEmpty(TAG_ENERGY));
        this.inventoryHandler.deserialize(input.childOrEmpty(TAG_INVENTORY));
        this.fluidTank.deserialize(input.childOrEmpty(TAG_FLUID_TANK));

        this.recipeProgress = input.getInt(TAG_RECIPE_PROGRESS).orElse(RECIPE_DURATION);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        this.energyStorage.serialize(output.child(TAG_ENERGY));
        this.inventoryHandler.serialize(output.child(TAG_INVENTORY));
        this.fluidTank.serialize(output.child(TAG_FLUID_TANK));

        output.putInt(TAG_RECIPE_PROGRESS, this.recipeProgress);
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
        return Component.translatable(ModBlocks.BIOGAS_PLANT.get().getDescriptionId());
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        if (this.level == null) {
            return null;
        }
        return new BiogasPlantContainerMenu(containerId, inventory, this, ContainerLevelAccess.create(this.level, this.getBlockPos()), this.containerData);
    }
}
