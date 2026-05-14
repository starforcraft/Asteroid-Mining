package com.ultramega.asteroidmining.blockentities;

import com.ultramega.asteroidmining.config.ServerConfig;
import com.ultramega.asteroidmining.container.HeatExchangerContainerMenu;
import com.ultramega.asteroidmining.recipe.HeatExchangeInput;
import com.ultramega.asteroidmining.recipe.HeatExchangeRecipe;
import com.ultramega.asteroidmining.registry.ModBlockEntityTypes;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.registry.ModRecipeTypes;
import com.ultramega.asteroidmining.utils.PreserveData;
import com.ultramega.asteroidmining.utils.handlers.MultiFluidStacksResourceHandler;
import com.ultramega.asteroidmining.utils.handlers.MultiGasStacksResourceHandler;
import com.ultramega.asteroidmining.utils.handlers.MutableEnergy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

public class HeatExchangerBlockEntity extends AbstractDataPreservingBlockEntity implements MenuProvider, Nameable, PreserveData {
    private static final Map<String, Optional<RecipeHolder<HeatExchangeRecipe>>> RECIPE_CACHE = new HashMap<>();

    public final MutableEnergy energyStorage = new MutableEnergy(ServerConfig.HEAT_EXCHANGER_ENERGY_CAPACITY.get());
    public final MultiGasStacksResourceHandler gasTank = new MultiGasStacksResourceHandler(new int[] {ServerConfig.HEAT_EXCHANGER_TANK_CAPACITY.get()}) {
        @Override
        protected void onContentsChanged(final int index, final FluidStack previousContents) {
            if (this.getAmountAsInt(0) == 0 && HeatExchangerBlockEntity.this.level instanceof ServerLevel serverLevel) {
                final Optional<RecipeHolder<HeatExchangeRecipe>> recipeHolder = getRecipeHolderFromInput(this.getStackInTank(0), serverLevel);
                if (recipeHolder.isPresent()) {
                    HeatExchangerBlockEntity.this.recipeDuration = recipeHolder.get().value().duration();
                    HeatExchangerBlockEntity.this.recipeProgress = HeatExchangerBlockEntity.this.recipeDuration;
                } else {
                    HeatExchangerBlockEntity.this.recipeProgress = 0;
                }
            }
            HeatExchangerBlockEntity.this.setChanged();
        }
    };
    public final MultiFluidStacksResourceHandler fluidTank = new MultiFluidStacksResourceHandler(new int[] {ServerConfig.HEAT_EXCHANGER_TANK_CAPACITY.get()}) {
        @Override
        protected void onContentsChanged(final int index, final FluidStack previousContents) {
            HeatExchangerBlockEntity.this.setChanged();
        }
    };
    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(final int index) {
            return switch (index) {
                case 0 -> HeatExchangerBlockEntity.this.energyStorage.getAmountAsInt();
                case 1 -> HeatExchangerBlockEntity.this.energyStorage.getCapacityAsInt();
                case 2 -> HeatExchangerBlockEntity.this.recipeDuration;
                case 3 -> HeatExchangerBlockEntity.this.recipeProgress;
                default -> throw new IllegalStateException("Unexpected value: " + index);
            };
        }

        @Override
        public void set(final int index, final int value) {
            switch (index) {
                case 0:
                    HeatExchangerBlockEntity.this.energyStorage.set(value);
                    break;
                case 1:
                    HeatExchangerBlockEntity.this.energyStorage.setCapacity(value);
                    break;
                case 2:
                    HeatExchangerBlockEntity.this.recipeDuration = value;
                    break;
                case 3:
                    HeatExchangerBlockEntity.this.recipeProgress = value;
                    break;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    private int recipeDuration;
    private int recipeProgress;

    //TODO: add jei support
    public HeatExchangerBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntityTypes.HEAT_EXCHANGER.get(), pos, blockState);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final HeatExchangerBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel) || blockEntity.cannotOperate()) {
            return;
        }

        final Optional<RecipeHolder<HeatExchangeRecipe>> recipeHolder = getRecipeHolderFromInput(blockEntity.gasTank.getStackInTank(0), serverLevel);
        if (recipeHolder.isEmpty()) {
            blockEntity.recipeProgress = blockEntity.recipeDuration;
            return;
        }

        final HeatExchangeRecipe recipe = recipeHolder.get().value();

        final FluidStack inputStack = recipe.getInputFluid();
        final FluidStack outputStack = recipe.getOutputFluid();

        final FluidResource inputResource = FluidResource.of(inputStack);
        final FluidResource outputResource = FluidResource.of(outputStack);

        final int inputAmount = inputStack.getAmount();
        final int outputAmount = outputStack.getAmount();
        final int energyAmount = ServerConfig.HEAT_EXCHANGER_ENERGY_USAGE.get();

        try (Transaction tx = Transaction.openRoot()) {
            if (blockEntity.fluidTank.insert(0, outputResource, outputAmount, tx) != outputAmount) {
                return;
            }
        }

        if (--blockEntity.recipeProgress > 0) {
            return;
        }

        try (Transaction tx = Transaction.openRoot()) {
            if (blockEntity.gasTank.extract(0, inputResource, inputAmount, tx) != inputAmount) {
                return;
            }
            if (blockEntity.fluidTank.insert(0, outputResource, outputAmount, tx) != outputAmount) {
                return;
            }
            if (blockEntity.energyStorage.extract(energyAmount, tx) != energyAmount) {
                return;
            }

            tx.commit();
        }

        blockEntity.recipeDuration = recipe.duration();
        blockEntity.recipeProgress = blockEntity.recipeDuration;
        blockEntity.setChanged();
    }

    private static Optional<RecipeHolder<HeatExchangeRecipe>> getRecipeHolderFromInput(final FluidStack inputStack, final ServerLevel serverLevel) {
        final RecipeManager recipeManager = serverLevel.recipeAccess();
        if (!RECIPE_CACHE.containsKey(inputStack.getDescriptionId())) {
            final HeatExchangeInput input = new HeatExchangeInput(inputStack);
            final Optional<RecipeHolder<HeatExchangeRecipe>> recipe = recipeManager.getRecipeFor(ModRecipeTypes.HEAT_EXCHANGE.get(), input, serverLevel);
            RECIPE_CACHE.put(inputStack.getDescriptionId(), recipe);
        }

        return RECIPE_CACHE.getOrDefault(inputStack.getDescriptionId(), Optional.empty());
    }

    public boolean cannotOperate() {
        return this.energyStorage.getAmountAsInt() < ServerConfig.HEAT_EXCHANGER_ENERGY_USAGE.get();
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);

        this.energyStorage.deserialize(input.childOrEmpty("energy"));
        this.fluidTank.deserialize(input.childOrEmpty("fluidTank"));
        this.gasTank.deserialize(input.childOrEmpty("gasTank"));
        this.recipeProgress = input.getInt("recipeProgress").orElse(0);
        this.recipeDuration = input.getInt("recipeDuration").orElse(0);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);

        this.energyStorage.serialize(output.child("energy"));
        this.fluidTank.serialize(output.child("fluidTank"));
        this.gasTank.serialize(output.child("gasTank"));
        output.putInt("recipeProgress", this.recipeProgress);
        output.putInt("recipeDuration", this.recipeDuration);
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
        return Component.translatable(ModBlocks.HEAT_EXCHANGER.get().getDescriptionId());
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new HeatExchangerContainerMenu(containerId, inventory, this, ContainerLevelAccess.create(this.level, this.getBlockPos()), this.containerData);
    }
}
