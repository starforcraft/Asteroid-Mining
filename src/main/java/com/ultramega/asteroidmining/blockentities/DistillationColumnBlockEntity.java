package com.ultramega.asteroidmining.blockentities;

import com.ultramega.asteroidmining.config.ServerConfig;
import com.ultramega.asteroidmining.container.DistillationColumnContainerMenu;
import com.ultramega.asteroidmining.registry.ModBlockEntityTypes;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.registry.ModFluids;
import com.ultramega.asteroidmining.utils.CoolantData;
import com.ultramega.asteroidmining.utils.ItemStacksResourceHandler;
import com.ultramega.asteroidmining.utils.MultiFluidStacksResourceHandler;
import com.ultramega.asteroidmining.utils.MutableEnergy;
import com.ultramega.asteroidmining.utils.PreserveData;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

public class DistillationColumnBlockEntity extends AbstractDataPreservingBlockEntity implements MenuProvider, Nameable, PreserveData {
    public static final int RECIPE_DURATION = 40;

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

    //TODO: full recipe system?
    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final DistillationColumnBlockEntity blockEntity) {
        if (blockEntity.cannotOperate()) {
            return;
        }
        if (blockEntity.isLit()) {
            --blockEntity.litTime;
        }
        if (blockEntity.isCooling()) {
            --blockEntity.coolingTime;
        }

        --blockEntity.temperatureCooldown;
        if (blockEntity.temperatureCooldown <= 0) {
            if (blockEntity.isLit()) {
                // Increase temperature with a cap of 500°C
                if (blockEntity.temperature < 500) {
                    ++blockEntity.temperature;
                }
            } else if (blockEntity.isCooling()) {
                // Decrease temperature with a cap of -273°C
                if (blockEntity.temperature > -273) { //TODO: update calculation (take temperature into account)
                    --blockEntity.temperature;
                }
            } else {
                // Slowly go back to 0°C temperature
                if (blockEntity.temperature > 0) {
                    --blockEntity.temperature;
                } else if (blockEntity.temperature < 0) {
                    ++blockEntity.temperature;
                }
            }
            if (blockEntity.isLit()) {
                blockEntity.temperatureCooldown = (int) ((double) (blockEntity.temperature / 20) + 2);
            } else if (blockEntity.isCooling()) {
                blockEntity.temperatureCooldown = (int) ((double) (-blockEntity.temperature / 20) + 2);
            } else {
                blockEntity.temperatureCooldown = (int) ((double) (Math.abs(blockEntity.temperature) / 70) + 2);
            }
        }

        if (!blockEntity.isLit() && !blockEntity.isCooling()) {
            // Consume burnable fuels or coolants
            final ItemResource resource = blockEntity.inventoryHandler.getResource(0);
            if (!resource.isEmpty()) {
                final int burnTime = blockEntity.inventoryHandler.getStack(0).getBurnTime(null, level.fuelValues());
                if (burnTime > 0) {
                    blockEntity.litTime = burnTime / 10;
                    blockEntity.litDuration = blockEntity.litTime;
                    try (Transaction tx = Transaction.openRoot()) {
                        blockEntity.inventoryHandler.extract(resource, 1, tx);
                        tx.commit();
                    }
                } else {
                    final CoolantData data = resource.typeHolder().getData(CoolantData.COOLANT_DATA);
                    if (data != null) {
                        blockEntity.coolingTime = data.duration() / 10;
                        blockEntity.coolingDuration = blockEntity.coolingTime;
                        try (Transaction tx = Transaction.openRoot()) {
                            blockEntity.inventoryHandler.extract(resource, 1, tx);
                            tx.commit();
                        }
                    }
                }
            }
        }

        if (blockEntity.fluidTank.getAmountAsInt(2) >= blockEntity.fluidTank.getCapacityAsInt(2)) {
            return;
        }

        final FluidResource extractFluid = blockEntity.fluidTank.getResource(1);
        final FluidResource hydrotreatingFluid = blockEntity.fluidTank.getResource(0);
        // Kerosene Production
        if (blockEntity.temperature > 300
            && extractFluid.is(ModFluids.PETROLEUM_SOURCE) && blockEntity.fluidTank.getAmountAsInt(1) > 200
            && hydrotreatingFluid.is(ModFluids.HYDROGEN) && blockEntity.fluidTank.getAmountAsInt(0) > 1) {
            if (--blockEntity.recipeProgress > 0) {
                return;
            }

            try (Transaction tx = Transaction.openRoot()) {
                blockEntity.fluidTank.extract(1, extractFluid, 200, tx);
                blockEntity.fluidTank.extract(0, hydrotreatingFluid, 1, tx);
                blockEntity.fluidTank.insert(2, FluidResource.of(ModFluids.KEROSENE_SOURCE), 20, tx);
                blockEntity.energyStorage.extract(ServerConfig.DISTILLATION_COLUMN_ENERGY_USAGE.get(), tx);
                tx.commit();
            }

            blockEntity.recipeProgress = RECIPE_DURATION;
        }
        // Liquid Oxygen Production
        else if (blockEntity.temperature < -185
            && extractFluid.is(ModFluids.AIR) && blockEntity.fluidTank.getAmountAsInt(1) > 850
            /*&& hydrotreatingFluid.isEmpty()*/) { //TODO: re-enable
            if (--blockEntity.recipeProgress > 0) {
                return;
            }

            try (Transaction tx = Transaction.openRoot()) {
                blockEntity.fluidTank.extract(1, extractFluid, 850, tx);
                blockEntity.fluidTank.insert(2, FluidResource.of(ModFluids.LIQUID_OXYGEN), 1, tx);
                blockEntity.energyStorage.extract(ServerConfig.DISTILLATION_COLUMN_ENERGY_USAGE.get(), tx);
                tx.commit();
            }

            blockEntity.recipeProgress = RECIPE_DURATION;
        }
    }

    public boolean cannotOperate() {
        return energyStorage.getAmountAsInt() < ServerConfig.DISTILLATION_COLUMN_ENERGY_USAGE.get();
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
        return new DistillationColumnContainerMenu(containerId, inventory, this, ContainerLevelAccess.create(level, getBlockPos()), containerData);
    }
}
