package com.ultramega.asteroidmining.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ServerConfig { //TODO: change to common? and balance all values
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue DISTILLATION_COLUMN_ENERGY_CAPACITY = BUILDER
        .comment("The energy capacity of the distillation column")
        .defineInRange("distillationColumnEnergyCapacity", 100_000, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue DISTILLATION_COLUMN_TANK_CAPACITY = BUILDER
        .comment("The tank capacity of the distillation column")
        .defineInRange("distillationColumnTankCapacity", 2_000, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue DISTILLATION_COLUMN_ENERGY_USAGE = BUILDER
        .comment("The energy usage of the distillation column")
        .defineInRange("distillationColumnEnergyUsage", 10_000, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue AIR_ABSORBER_ENERGY_CAPACITY = BUILDER
        .comment("The energy capacity of the air absorber")
        .defineInRange("airAbsorberEnergyCapacity", 100_000, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue AIR_ABSORBER_TANK_CAPACITY = BUILDER
        .comment("The tank capacity of the air absorber")
        .defineInRange("airAbsorberTankCapacity", 20_000, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue HEAT_EXCHANGER_ENERGY_CAPACITY = BUILDER
        .comment("The energy capacity of the heat exchanger")
        .defineInRange("heatExchangerEnergyCapacity", 500_000, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue HEAT_EXCHANGER_TANK_CAPACITY = BUILDER
        .comment("The tank capacity of the heat exchanger")
        .defineInRange("heatExchangerTankCapacity", 4_000, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue HEAT_EXCHANGER_ENERGY_USAGE = BUILDER
        .comment("The energy usage of the heat exchanger")
        .defineInRange("heatExchangerEnergyUsage", 50_000, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue ELECTROLYSIS_PLANT_ENERGY_CAPACITY = BUILDER
        .comment("The energy capacity of the electrolysis plant")
        .defineInRange("electrolysisPlantEnergyCapacity", 1_000_000, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue ELECTROLYSIS_PLANT_TANK_CAPACITY = BUILDER
        .comment("The tank capacity of the electrolysis plant")
        .defineInRange("electrolysisPlantTankCapacity", 10_000, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue ELECTROLYSIS_PLANT_ENERGY_USAGE = BUILDER
        .comment("The energy usage of the electrolysis plant")
        .defineInRange("electrolysisPlantEnergyUsage", 100_000, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue ELECTROLYSIS_PLANT_RECIPE_DURATION = BUILDER
        .comment("The duration (in ticks) required to transform water into hydrogen in the the electrolysis plant")
        .defineInRange("electrolysisPlantRecipeDuration", 20, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue BIOGAS_PLANT_ENERGY_CAPACITY = BUILDER
        .comment("The energy capacity of the biogas plant")
        .defineInRange("biogasPlantEnergyCapacity", 100_000, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue BIOGAS_PLANT_TANK_CAPACITY = BUILDER
        .comment("The tank capacity of the biogas plant")
        .defineInRange("biogasPlantTankCapacity", 2_000, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue BIOGAS_PLANT_ENERGY_USAGE = BUILDER
        .comment("The energy usage of the biogas plant")
        .defineInRange("biogasPlantEnergyUsage", 5_000, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ServerConfig() {
    }
}
