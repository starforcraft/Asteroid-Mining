package com.ultramega.asteroidmining.blockentities;

import com.ultramega.asteroidmining.config.ServerConfig;
import com.ultramega.asteroidmining.container.DistillationColumnContainerMenu;
import com.ultramega.asteroidmining.recipe.DistillationInput;
import com.ultramega.asteroidmining.recipe.DistillationRecipe;
import com.ultramega.asteroidmining.registry.ModBlockEntityTypes;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.registry.ModRecipeTypes;
import com.ultramega.asteroidmining.utils.CoolantData;
import com.ultramega.asteroidmining.utils.ItemStacksResourceHandler;
import com.ultramega.asteroidmining.utils.MultiFluidStacksResourceHandler;
import com.ultramega.asteroidmining.utils.MutableEnergy;
import com.ultramega.asteroidmining.utils.PreserveData;

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
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

public class DistillationColumnBlockEntity extends AbstractDataPreservingBlockEntity implements MenuProvider, Nameable, PreserveData {
    public static final int RECIPE_DURATION = 40; //TODO: delete?
    public static final int MAX_TEMPERATURE = 500;
    public static final int MIN_TEMPERATURE = -273;

    //private static final Map<String, Optional<RecipeHolder<DistillationRecipe>>> RECIPE_CACHE = new HashMap<>();

    public final MutableEnergy energyStorage = new MutableEnergy(ServerConfig.DISTILLATION_COLUMN_ENERGY_CAPACITY.get());
    public final ItemStacksResourceHandler inventoryHandler = new ItemStacksResourceHandler(1) {
        @Override
        protected void onContentsChanged(final int index, final ItemStack previousContents) {
            DistillationColumnBlockEntity.this.recipeProgress = RECIPE_DURATION;
            DistillationColumnBlockEntity.this.setChanged();
        }
    };
    //TODO: add a configuration screen like in Mekanism to specify input sides for each tank (To every block!)
    public final MultiFluidStacksResourceHandler fluidTank = new MultiFluidStacksResourceHandler(3, new int[] {
        ServerConfig.DISTILLATION_COLUMN_TANK_CAPACITY.get() / 10,
        ServerConfig.DISTILLATION_COLUMN_TANK_CAPACITY.get(),
        ServerConfig.DISTILLATION_COLUMN_TANK_CAPACITY.get()
    }) {
        @Override
        protected void onContentsChanged(final int index, final FluidStack previousContents) {
            if (index == 0 || index == 1) { //TODO: only remove the recipe if the required ingredients are gone
                DistillationColumnBlockEntity.this.recipeProgress = 0;
                DistillationColumnBlockEntity.this.activeRecipe = null;
            }

            DistillationColumnBlockEntity.this.setChanged();
        }
    };
    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(final int index) {
            return switch (index) {
                case 0 -> DistillationColumnBlockEntity.this.energyStorage.getAmountAsInt();
                case 1 -> DistillationColumnBlockEntity.this.energyStorage.getCapacityAsInt();
                case 2 -> DistillationColumnBlockEntity.this.recipeProgress;
                case 3 -> DistillationColumnBlockEntity.this.litTime;
                case 4 -> DistillationColumnBlockEntity.this.litDuration;
                case 5 -> DistillationColumnBlockEntity.this.coolingTime;
                case 6 -> DistillationColumnBlockEntity.this.coolingDuration;
                case 7 -> DistillationColumnBlockEntity.this.temperature;
                default -> throw new IllegalStateException("Unexpected value: " + index);
            };
        }

        @Override
        public void set(final int index, final int value) {
            switch (index) {
                case 0:
                    DistillationColumnBlockEntity.this.energyStorage.set(value);
                    break;
                case 1:
                    DistillationColumnBlockEntity.this.energyStorage.setCapacity(value);
                    break;
                case 2:
                    DistillationColumnBlockEntity.this.recipeProgress = value;
                    break;
                case 3:
                    DistillationColumnBlockEntity.this.litTime = value;
                    break;
                case 4:
                    DistillationColumnBlockEntity.this.litDuration = value;
                    break;
                case 5:
                    DistillationColumnBlockEntity.this.coolingTime = value;
                    break;
                case 6:
                    DistillationColumnBlockEntity.this.coolingDuration = value;
                    break;
                case 7:
                    DistillationColumnBlockEntity.this.temperature = value;
                    break;
            }
        }

        @Override
        public int getCount() {
            return 8;
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
        if (!(level instanceof ServerLevel serverLevel) || blockEntity.cannotOperate()) {
            return;
        }

        boolean changed = false;

        changed |= tickHeatTimers(blockEntity);
        changed |= tickTemperature(blockEntity);

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
        if (blockEntity.fluidTank.getAmountAsInt(2) >= blockEntity.fluidTank.getCapacityAsInt(2)) {
            return false;
        }

        final FluidStack inputStack = blockEntity.fluidTank.getStackInTank(1);
        final FluidStack reagentStack = blockEntity.fluidTank.getStackInTank(0);

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
            final long inserted = blockEntity.fluidTank.insert(2, FluidResource.of(recipe.output().fluid().value()), recipe.output().amount(), tx);
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
            final long extractedInput = this.fluidTank.extract(1, FluidResource.of(inputStack.getFluid()), recipe.input().amount(), tx);
            if (extractedInput != recipe.input().amount()) {
                return false;
            }

            if (recipe.reagent().isPresent()) {
                final SizedFluidIngredient reagent = recipe.reagent().get();

                final long extractedReagent = this.fluidTank.extract(0, FluidResource.of(reagentStack.getFluid()), reagent.amount(), tx);
                if (extractedReagent != reagent.amount()) {
                    return false;
                }
            }

            final FluidStackTemplate output = recipe.output();
            final long insertedOutput = this.fluidTank.insert(2, FluidResource.of(output.fluid().value()), output.amount(), tx);
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

        this.energyStorage.deserialize(input);
        this.inventoryHandler.deserialize(input);
        this.fluidTank.deserialize(input);

        this.recipeProgress = input.getInt("recipeProgress").orElse(RECIPE_DURATION);
        this.litTime = input.getInt("litTime").orElse(0);
        this.litDuration = input.getInt("litDuration").orElse(0);
        this.coolingTime = input.getInt("coolingTime").orElse(0);
        this.coolingDuration = input.getInt("coolingDuration").orElse(0);
        this.temperature = input.getInt("temperature").orElse(0);
        this.temperatureCooldown = input.getInt("temperatureCooldown").orElse(0);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);

        this.energyStorage.serialize(output);
        this.inventoryHandler.serialize(output);
        this.fluidTank.serialize(output);

        output.putInt("recipeProgress", this.recipeProgress);
        output.putInt("litTime", this.litTime);
        output.putInt("litDuration", this.litDuration);
        output.putInt("coolingTime", this.coolingTime);
        output.putInt("coolingDuration", this.coolingDuration);
        output.putInt("temperature", this.temperature);
        output.putInt("temperatureCooldown", this.temperatureCooldown);
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

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new DistillationColumnContainerMenu(containerId, inventory, this, ContainerLevelAccess.create(this.level, this.getBlockPos()), this.containerData);
    }
}
