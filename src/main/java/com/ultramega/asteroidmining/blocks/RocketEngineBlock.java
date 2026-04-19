package com.ultramega.asteroidmining.blocks;

import com.ultramega.asteroidmining.blockentities.RocketEngineBlockEntity;
import com.ultramega.asteroidmining.registry.ModBlockEntityTypes;
import com.ultramega.asteroidmining.registry.ModFluids;
import com.ultramega.asteroidmining.utils.Utils;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class RocketEngineBlock extends AbstractMultiblockBlock implements EntityBlock {
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");

    private final Type type;

    public RocketEngineBlock(final Type type, final Properties properties) {
        super(properties, type.getWidth(), type.getHeight());
        this.type = type;
        this.registerDefaultState(this.stateDefinition.any().setValue(RUNNING, false));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RUNNING);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new RocketEngineBlockEntity(this.type, pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> blockEntityType) {
        return level.isClientSide() ? Utils.createTickerHelper(
            blockEntityType, this.type.getBlockEntity().get(), RocketEngineBlockEntity::clientTick) : null;
    }

    public Type getType() {
        return this.type;
    }

    public enum Type { //TODO: update consumption for fuel and oxidizer (kg/s)
        F1("F-1",
            1, 1, 8400, 6770,
            ModFluids.ROCKET_PROPELLANT.get(), 788,
            ModFluids.LIQUID_OXYGEN.get(), 1789,
            ModBlockEntityTypes.ROCKET_ENGINE),
        RS25("RS-25",
            2, 2, 3177, 2279,
            ModFluids.LIQUID_HYDROGEN.get(), 0,
            ModFluids.LIQUID_OXYGEN.get(), 0,
            ModBlockEntityTypes.RS25_ENGINE),
        RD180("RD-180",
            1, 1, 5480, 4150,
            ModFluids.KEROSENE_SOURCE.get(), 0,
            ModFluids.LIQUID_OXYGEN.get(), 0,
            ModBlockEntityTypes.ROCKET_ENGINE),
        RD170("RD-170",
            1, 1, 9750, 7900,
            ModFluids.KEROSENE_SOURCE.get(), 0,
            ModFluids.LIQUID_OXYGEN.get(), 0,
            ModBlockEntityTypes.ROCKET_ENGINE),
        RAPTOR3("Raptor 3",
            1, 1, 1525, 2750,
            ModFluids.LIQUID_METHANE.get(), 0,
            ModFluids.LIQUID_OXYGEN.get(), 0,
            ModBlockEntityTypes.ROCKET_ENGINE),
        VULCAIN2("Vulcain 2",
            1, 1, 1800, 1359,
            ModFluids.LIQUID_HYDROGEN.get(), 0,
            ModFluids.LIQUID_OXYGEN.get(), 0,
            ModBlockEntityTypes.ROCKET_ENGINE),
        BE4("BE-4",
            1, 1, 2500, 2400,
            ModFluids.LIQUID_METHANE.get(), 0,
            ModFluids.LIQUID_OXYGEN.get(), 0,
            ModBlockEntityTypes.ROCKET_ENGINE);

        private final String name; //TODO: is this required?
        private final int width;
        private final int height;
        private final int weight;
        private final int thrustForce;
        private final Fluid fuel;
        private final int fuelConsumptionRate;
        private final Fluid oxidizer;
        private final int oxidizerConsumptionRate;
        private final Supplier<BlockEntityType<RocketEngineBlockEntity>> blockEntity;

        Type(final String name,
             final int width,
             final int height,
             final int weight, // in kg (dry mass)
             final int thrustForce, // in kN (currently in vacuum, will maybe change later)
             final Fluid fuel,
             final int fuelConsumptionRate, // in kg/s
             final Fluid oxidizer,
             final int oxidizerConsumptionRate, // in kg/s
             final Supplier<BlockEntityType<RocketEngineBlockEntity>> blockEntity
        ) {
            this.name = name;
            this.width = width;
            this.height = height;
            this.weight = weight;
            this.thrustForce = thrustForce;
            this.fuel = fuel;
            this.fuelConsumptionRate = fuelConsumptionRate;
            this.oxidizer = oxidizer;
            this.oxidizerConsumptionRate = oxidizerConsumptionRate;
            this.blockEntity = blockEntity;
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }

        public int getWeight() {
            return this.weight;
        }

        public int getThrustForce() {
            return this.thrustForce;
        }

        public Fluid getFuel() {
            return this.fuel;
        }

        public int getFuelConsumptionRate() {
            return this.fuelConsumptionRate;
        }

        public Fluid getOxidizer() {
            return this.oxidizer;
        }

        public int getOxidizerConsumptionRate() {
            return this.oxidizerConsumptionRate;
        }

        public Supplier<BlockEntityType<RocketEngineBlockEntity>> getBlockEntity() {
            return this.blockEntity;
        }
    }
}
