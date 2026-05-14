package com.ultramega.asteroidmining.registry;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.container.AirAbsorberContainerMenu;
import com.ultramega.asteroidmining.container.BiogasPlantContainerMenu;
import com.ultramega.asteroidmining.container.DistillationColumnContainerMenu;
import com.ultramega.asteroidmining.container.ElectrolysisPlantContainerMenu;
import com.ultramega.asteroidmining.container.HeatExchangerContainerMenu;
import com.ultramega.asteroidmining.container.LaunchPadBuilderContainerMenu;
import com.ultramega.asteroidmining.container.ObservatoryContainerMenu;
import com.ultramega.asteroidmining.container.RocketControllerConfigurationContainerMenu;
import com.ultramega.asteroidmining.container.RocketControllerContainerMenu;
import com.ultramega.asteroidmining.container.RocketStorageViewerContainerMenu;

import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(BuiltInRegistries.MENU, AsteroidMining.MOD_ID);

    public static final Supplier<MenuType<DistillationColumnContainerMenu>> DISTILLATION_COLUMN = MENU_TYPES.register("distillation_column", () ->
        IMenuTypeExtension.create(DistillationColumnContainerMenu::new));
    public static final Supplier<MenuType<AirAbsorberContainerMenu>> AIR_ABSORBER = MENU_TYPES.register("air_absorber", () ->
        IMenuTypeExtension.create(AirAbsorberContainerMenu::new));
    public static final Supplier<MenuType<HeatExchangerContainerMenu>> HEAT_EXCHANGER = MENU_TYPES.register("heat_exchanger", () ->
        IMenuTypeExtension.create(HeatExchangerContainerMenu::new));
    public static final Supplier<MenuType<ElectrolysisPlantContainerMenu>> ELECTROLYSIS_PLANT = MENU_TYPES.register("electrolysis_plant", () ->
        IMenuTypeExtension.create(ElectrolysisPlantContainerMenu::new));
    public static final Supplier<MenuType<BiogasPlantContainerMenu>> BIOGAS_PLANT = MENU_TYPES.register("biogas_plant", () ->
        IMenuTypeExtension.create(BiogasPlantContainerMenu::new));

    public static final Supplier<MenuType<RocketControllerContainerMenu>> ROCKET_CONTROLLER = MENU_TYPES.register("rocket_controller", () ->
        IMenuTypeExtension.create(RocketControllerContainerMenu::new));
    public static final Supplier<MenuType<RocketControllerConfigurationContainerMenu>> SELECT_CONFIGURATION = MENU_TYPES.register("select_configuration", () ->
        IMenuTypeExtension.create(RocketControllerConfigurationContainerMenu::new));
    public static final Supplier<MenuType<RocketStorageViewerContainerMenu>> ROCKET_STORAGE_VIEWER = MENU_TYPES.register("rocket_storage_viewer", () ->
        IMenuTypeExtension.create(RocketStorageViewerContainerMenu::new));
    public static final Supplier<MenuType<LaunchPadBuilderContainerMenu>> LAUNCH_PAD_BUILDER = MENU_TYPES.register("launch_pad_builder", () ->
        IMenuTypeExtension.create(LaunchPadBuilderContainerMenu::new));
    public static final Supplier<MenuType<ObservatoryContainerMenu>> SMALL_OBSERVATORY = MENU_TYPES.register("small_observatory", () ->
        IMenuTypeExtension.create(ObservatoryContainerMenu::new));

    private ModMenuTypes() {
    }
}
