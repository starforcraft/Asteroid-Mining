package com.ultramega.asteroidmining.blockentities;

import com.ultramega.asteroidmining.config.ServerConfig;
import com.ultramega.asteroidmining.container.DistillationColumnContainerMenu;
import com.ultramega.asteroidmining.recipe.DistillationInput;
import com.ultramega.asteroidmining.recipe.DistillationRecipe;
import com.ultramega.asteroidmining.registry.ModBlockEntityTypes;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.registry.ModRecipeTypes;
import com.ultramega.asteroidmining.utils.CoolantData;
import com.ultramega.asteroidmining.utils.handlers.ItemStacksResourceHandler;
import com.ultramega.asteroidmining.utils.handlers.MultiFluidStacksResourceHandler;
import com.ultramega.asteroidmining.utils.handlers.MultiGasStacksResourceHandler;
import com.ultramega.asteroidmining.utils.handlers.MutableEnergy;

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
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

public class DistillationColumnBlockEntity extends AbstractSideConfigurableBlockEntity implements MenuProvider, Nameable {
    public static final int RECIPE_DURATION = 40; //TODO: delete because this is wrong. Get it instead from activeRecipe.duration()
    public static final int MAX_TEMPERATURE = 500;
    public static final int MIN_TEMPERATURE = -273;

    public static final int MACHINE_DATA_COUNT = 8;

    private static final String TAG_ENERGY = "energy";
    private static final String TAG_INVENTORY = "inventory";
    private static final String TAG_FLUID_TANK = "fluidTank";
    private static final String TAG_GAS_TANK = "gasTank";
    private static final String TAG_RECIPE_PROGRESS = "recipeProgress";
    private static final String TAG_LIT_TIME = "litTime";
    private static final String TAG_LIT_DURATION = "litDuration";
    private static final String TAG_COOLING_TIME = "coolingTime";
    private static final String TAG_COOLING_DURATION = "coolingDuration";
    private static final String TAG_TEMPERATURE = "temperature";
    private static final String TAG_TEMPERATURE_COOLDOWN = "temperatureCooldown";

    //private static final Map<String, Optional<RecipeHolder<DistillationRecipe>>> RECIPE_CACHE = new HashMap<>();

    public final MutableEnergy energyStorage = new MutableEnergy(ServerConfig.DISTILLATION_COLUMN_ENERGY_CAPACITY.get());
    public final ItemStacksResourceHandler inventoryHandler = new ItemStacksResourceHandler(1) {
        @Override
        protected void onContentsChanged(final int index, final ItemStack previousContents) {
            DistillationColumnBlockEntity.this.recipeProgress = RECIPE_DURATION;
            DistillationColumnBlockEntity.this.setChanged();
        }
    };
    public final MultiFluidStacksResourceHandler fluidTank = new MultiFluidStacksResourceHandler(new int[] {
        ServerConfig.DISTILLATION_COLUMN_TANK_CAPACITY.get(),
        ServerConfig.DISTILLATION_COLUMN_TANK_CAPACITY.get()
    }) {
        @Override
        protected void onContentsChanged(final int index, final FluidStack previousContents) {
            if (index == 0) { //TODO: only remove the recipe if the required ingredients are gone
                DistillationColumnBlockEntity.this.recipeProgress = 0;
                DistillationColumnBlockEntity.this.activeRecipe = null;
            }

            DistillationColumnBlockEntity.this.setChanged();
        }
    };
    public final MultiGasStacksResourceHandler gasTank = new MultiGasStacksResourceHandler(new int[] {
        ServerConfig.DISTILLATION_COLUMN_TANK_CAPACITY.get()
    }) {
        @Override
        protected void onContentsChanged(final int index, final FluidStack previousContents) {
            //TODO: only remove the recipe if the required ingredients are gone
            DistillationColumnBlockEntity.this.recipeProgress = 0;
            DistillationColumnBlockEntity.this.activeRecipe = null;

            DistillationColumnBlockEntity.this.setChanged();
        }
    };

    @Nullable
    private DistillationRecipe activeRecipe;
    private int recipeProgress = RECIPE_DURATION;

    private int litTime;
    private int litDuration;
    private int coolingTime;
    private int coolingDuration;
    private int temperature;
    private int temperatureCooldown;

    //TODO: add jei support
    public DistillationColumnBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntityTypes.DISTILLATION_COLUMN.get(), pos, blockState);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final DistillationColumnBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean changed = false;

        if (blockEntity.cannotOperate()) {
            changed |= blockEntity.autoEjectEnergyToConfiguredOutputs(ServerConfig.DISTILLATION_COLUMN_ENERGY_CAPACITY.get() / 20);
            if (changed) {
                blockEntity.setChanged();
            }
            return;
        }

        changed |= tickHeatTimers(blockEntity);
        changed |= tickTemperature(blockEntity);

        changed |= blockEntity.autoEjectEnergyToConfiguredOutputs(ServerConfig.DISTILLATION_COLUMN_ENERGY_CAPACITY.get() / 20);

        if (!blockEntity.isLit() && !blockEntity.isCooling()) {
            changed |= tryConsumeFuelOrCoolant(level, blockEntity);
        }

        changed |= tickDistillation(serverLevel, level, blockEntity);

        if (changed) {
            blockEntity.setChanged();
        }
    }

    private static boolean tickHeatTimers(final DistillationColumnBlockEntity blockEntity) {
        boolean changed = false;

        if (blockEntity.isLit()) {
            --blockEntity.litTime;
            changed = true;
        }

        if (blockEntity.isCooling()) {
            --blockEntity.coolingTime;
            changed = true;
        }

        return changed;
    }

    private static boolean tickTemperature(final DistillationColumnBlockEntity blockEntity) {
        final int oldTemperature = blockEntity.temperature;
        final int oldCooldown = blockEntity.temperatureCooldown;

        --blockEntity.temperatureCooldown;

        if (blockEntity.temperatureCooldown > 0) {
            return oldCooldown != blockEntity.temperatureCooldown;
        }

        updateTemperature(blockEntity);
        blockEntity.temperatureCooldown = getNextTemperatureCooldown(blockEntity);

        return oldTemperature != blockEntity.temperature || oldCooldown != blockEntity.temperatureCooldown;
    }

    private static void updateTemperature(final DistillationColumnBlockEntity blockEntity) {
        if (blockEntity.isLit()) {
            if (blockEntity.temperature < MAX_TEMPERATURE) {
                ++blockEntity.temperature;
            }
            return;
        }

        if (blockEntity.isCooling()) {
            if (blockEntity.temperature > MIN_TEMPERATURE) {
                --blockEntity.temperature;
            }
            return;
        }

        if (blockEntity.temperature > 0) {
            --blockEntity.temperature;
        } else if (blockEntity.temperature < 0) {
            ++blockEntity.temperature;
        }
    }

    private static int getNextTemperatureCooldown(final DistillationColumnBlockEntity blockEntity) {
        if (blockEntity.isLit()) {
            return blockEntity.temperature / 20 + 2;
        }

        if (blockEntity.isCooling()) {
            return -blockEntity.temperature / 20 + 2;
        }

        return Math.abs(blockEntity.temperature) / 70 + 2;
    }

    private static boolean tryConsumeFuelOrCoolant(final Level level, final DistillationColumnBlockEntity blockEntity) {
        final ItemResource resource = blockEntity.inventoryHandler.getResource(0);
        if (resource.isEmpty()) {
            return false;
        }

        final int burnTime = blockEntity.inventoryHandler.getStack(0).getBurnTime(null, level.fuelValues());
        if (burnTime > 0) {
            if (!consumeOneInputItem(blockEntity, resource)) {
                return false;
            }

            final int duration = burnTime / 10;
            blockEntity.litTime = duration;
            blockEntity.litDuration = duration;
            return true;
        }

        final CoolantData coolantData = resource.typeHolder().getData(CoolantData.COOLANT_DATA);
        if (coolantData == null || !consumeOneInputItem(blockEntity, resource)) {
            return false;
        }

        final int duration = coolantData.duration() / 10;
        blockEntity.coolingTime = duration;
        blockEntity.coolingDuration = duration;
        return true;
    }

    private static boolean consumeOneInputItem(final DistillationColumnBlockEntity blockEntity, final ItemResource resource) {
        try (Transaction tx = Transaction.openRoot()) {
            final long extracted = blockEntity.inventoryHandler.extract(0, resource, 1, tx);
            if (extracted != 1) {
                return false;
            }

            tx.commit();
            return true;
        }
    }

    private static boolean tickDistillation(final ServerLevel serverLevel, final Level level, final DistillationColumnBlockEntity blockEntity) {
        if (blockEntity.fluidTank.getAmountAsInt(1) >= blockEntity.fluidTank.getCapacityAsInt(1)) {
            return false;
        }

        final FluidStack inputStack = blockEntity.fluidTank.getStackInTank(0);
        final FluidStack reagentStack = blockEntity.gasTank.getStackInTank(0);

        final DistillationInput recipeInput = new DistillationInput(inputStack, reagentStack, blockEntity.temperature);

        // TODO: cache via RECIPE_CACHE / getRecipeHolderFromInput
        final Optional<RecipeHolder<DistillationRecipe>> recipeHolder = serverLevel.recipeAccess().getRecipeFor(ModRecipeTypes.DISTILLATION.get(), recipeInput, level);
        if (recipeHolder.isEmpty()) {
            if (blockEntity.recipeProgress == 0 && blockEntity.activeRecipe == null) {
                return false;
            }

            blockEntity.recipeProgress = 0;
            blockEntity.activeRecipe = null;
            return true;
        }

        final int energyUsage = ServerConfig.DISTILLATION_COLUMN_ENERGY_USAGE.get();
        if (blockEntity.energyStorage.getAmountAsInt() < energyUsage) {
            return false;
        }

        final DistillationRecipe recipe = recipeHolder.get().value();

        try (Transaction tx = Transaction.openRoot()) {
            final long inserted = blockEntity.fluidTank.insert(1, FluidResource.of(recipe.output().fluid().value()), recipe.output().amount(), tx);
            if (inserted != recipe.output().amount()) {
                return false;
            }
        }

        if (blockEntity.activeRecipe != recipe || blockEntity.recipeProgress <= 0 || blockEntity.recipeProgress > recipe.duration()) {
            blockEntity.activeRecipe = recipe;
            blockEntity.recipeProgress = recipe.duration();
        }

        --blockEntity.recipeProgress;
        if (blockEntity.recipeProgress > 0) {
            return true;
        }

        if (blockEntity.craftRecipe(recipe, inputStack, reagentStack, energyUsage)) {
            blockEntity.recipeProgress = recipe.duration();
        } else {
            blockEntity.recipeProgress = 1;
        }

        return true;
    }

    private boolean craftRecipe(final DistillationRecipe recipe, final FluidStack inputStack, final FluidStack reagentStack, final int energyUsage) {
        try (Transaction tx = Transaction.openRoot()) {
            final long extractedInput = this.fluidTank.extract(0, FluidResource.of(inputStack.getFluid()), recipe.input().amount(), tx);
            if (extractedInput != recipe.input().amount()) {
                return false;
            }

            if (recipe.reagent().isPresent()) {
                final SizedFluidIngredient reagent = recipe.reagent().get();

                final long extractedReagent = this.gasTank.extract(0, FluidResource.of(reagentStack.getFluid()), reagent.amount(), tx);
                if (extractedReagent != reagent.amount()) {
                    return false;
                }
            }

            final FluidStackTemplate output = recipe.output();
            final long insertedOutput = this.fluidTank.insert(1, FluidResource.of(output.fluid().value()), output.amount(), tx);
            if (insertedOutput != output.amount()) {
                return false;
            }

            final long extractedEnergy = this.energyStorage.extract(energyUsage, tx);

            if (extractedEnergy != energyUsage) {
                return false;
            }

            tx.commit();
            return true;
        }
    }

    public boolean cannotOperate() {
        return this.energyStorage.getAmountAsInt() < ServerConfig.DISTILLATION_COLUMN_ENERGY_USAGE.get();
    }

    private boolean isLit() {
        return this.litTime > 0;
    }

    private boolean isCooling() {
        return this.coolingTime > 0;
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.energyStorage.deserialize(input.childOrEmpty(TAG_ENERGY));
        this.inventoryHandler.deserialize(input.childOrEmpty(TAG_INVENTORY));
        this.fluidTank.deserialize(input.childOrEmpty(TAG_FLUID_TANK));
        this.gasTank.deserialize(input.childOrEmpty(TAG_GAS_TANK));

        this.recipeProgress = input.getInt(TAG_RECIPE_PROGRESS).orElse(RECIPE_DURATION);
        this.litTime = input.getInt(TAG_LIT_TIME).orElse(0);
        this.litDuration = input.getInt(TAG_LIT_DURATION).orElse(0);
        this.coolingTime = input.getInt(TAG_COOLING_TIME).orElse(0);
        this.coolingDuration = input.getInt(TAG_COOLING_DURATION).orElse(0);
        this.temperature = input.getInt(TAG_TEMPERATURE).orElse(0);
        this.temperatureCooldown = input.getInt(TAG_TEMPERATURE_COOLDOWN).orElse(0);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        this.energyStorage.serialize(output.child(TAG_ENERGY));
        this.inventoryHandler.serialize(output.child(TAG_INVENTORY));
        this.fluidTank.serialize(output.child(TAG_FLUID_TANK));
        this.gasTank.serialize(output.child(TAG_GAS_TANK));

        output.putInt(TAG_RECIPE_PROGRESS, this.recipeProgress);
        output.putInt(TAG_LIT_TIME, this.litTime);
        output.putInt(TAG_LIT_DURATION, this.litDuration);
        output.putInt(TAG_COOLING_TIME, this.coolingTime);
        output.putInt(TAG_COOLING_DURATION, this.coolingDuration);
        output.putInt(TAG_TEMPERATURE, this.temperature);
        output.putInt(TAG_TEMPERATURE_COOLDOWN, this.temperatureCooldown);
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
        return Component.translatable(ModBlocks.DISTILLATION_COLUMN.get().getDescriptionId());
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Override
    protected int getMachineDataCount() {
        return MACHINE_DATA_COUNT;
    }

    @Override
    protected int getMachineData(final int index) {
        return switch (index) {
            case 0 -> this.energyStorage.getAmountAsInt();
            case 1 -> this.energyStorage.getCapacityAsInt();
            case 2 -> this.recipeProgress;
            case 3 -> this.litTime;
            case 4 -> this.litDuration;
            case 5 -> this.coolingTime;
            case 6 -> this.coolingDuration;
            case 7 -> this.temperature;
            default -> throw new IllegalStateException("Unexpected machine data index: " + index);
        };
    }

    @Override
    protected void setMachineData(final int index, final int value) {
        switch (index) {
            case 0 -> this.energyStorage.set(value);
            case 1 -> this.energyStorage.setCapacity(value);
            case 2 -> this.recipeProgress = value;
            case 3 -> this.litTime = value;
            case 4 -> this.litDuration = value;
            case 5 -> this.coolingTime = value;
            case 6 -> this.coolingDuration = value;
            case 7 -> this.temperature = value;
            default -> throw new IllegalStateException("Unexpected machine data index: " + index);
        }
    }

    @Override
    protected EnergyHandler getEnergyStorageForSideConfig() {
        return this.energyStorage;
    }

    @Override
    protected ResourceHandler<ItemResource> getItemHandlerForSideConfig() {
        return this.inventoryHandler;
    }

    @Override
    protected ResourceHandler<FluidResource> getFluidHandlerForSideConfig() {
        return this.fluidTank;
    }

    @Override
    protected ResourceHandler<FluidResource> getGasHandlerForSideConfig() {
        return this.gasTank;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        if (this.level == null) {
            return null;
        }
        return new DistillationColumnContainerMenu(containerId, inventory, this, ContainerLevelAccess.create(this.level, this.getBlockPos()), this.getContainerData());
    }
}
