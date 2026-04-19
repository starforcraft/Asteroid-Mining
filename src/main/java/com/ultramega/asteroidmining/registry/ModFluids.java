package com.ultramega.asteroidmining.registry;

import com.ultramega.asteroidmining.AsteroidMining;

import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.joml.Matrix4f;

public final class ModFluids {
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, AsteroidMining.MOD_ID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, AsteroidMining.MOD_ID);

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> AIR =
        FLUIDS.register("air", () -> new BaseFlowingFluid.Source(ModFluids.AIR_PROPERTIES.apply(false)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUID_AIR =
        FLUIDS.register("liquid_air", () -> new BaseFlowingFluid.Source(ModFluids.AIR_PROPERTIES.apply(true)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUID_OXYGEN =
        FLUIDS.register("liquid_oxygen", () -> new BaseFlowingFluid.Source(ModFluids.LIQUID_OXYGEN_PROPERTIES));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> METHANE =
        FLUIDS.register("methane", () -> new BaseFlowingFluid.Source(ModFluids.METHANE_PROPERTIES.apply(false)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUID_METHANE =
        FLUIDS.register("liquid_methane", () -> new BaseFlowingFluid.Source(ModFluids.METHANE_PROPERTIES.apply(true)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> HYDROGEN =
        FLUIDS.register("hydrogen", () -> new BaseFlowingFluid.Source(ModFluids.HYDROGEN_PROPERTIES.apply(false)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUID_HYDROGEN =
        FLUIDS.register("liquid_hydrogen", () -> new BaseFlowingFluid.Source(ModFluids.HYDROGEN_PROPERTIES.apply(true)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> ROCKET_PROPELLANT =
        FLUIDS.register("rocket_propellant", () -> new BaseFlowingFluid.Source(ModFluids.ROCKET_PROPELLANT_PROPERTIES));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> PETROLEUM_SOURCE =
        FLUIDS.register("petroleum_source", () -> new BaseFlowingFluid.Source(ModFluids.PETROLEUM_PROPERTIES));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> PETROLEUM_FLOWING =
        FLUIDS.register("petroleum_flowing", () -> new BaseFlowingFluid.Flowing(ModFluids.PETROLEUM_PROPERTIES));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> KEROSENE_SOURCE =
        FLUIDS.register("kerosene_source", () -> new BaseFlowingFluid.Source(ModFluids.KEROSENE_PROPERTIES));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> KEROSENE_FLOWING =
        FLUIDS.register("kerosene_flowing", () -> new BaseFlowingFluid.Flowing(ModFluids.KEROSENE_PROPERTIES));

    public static final DeferredHolder<FluidType, FluidType> AIR_TYPE = FLUID_TYPES.register("air", () -> new FluidType(
        FluidType.Properties.create()
            .descriptionId("block.asteroidmining.air")
            .density(0)) {
    });
    public static final DeferredHolder<FluidType, FluidType> LIQUID_OXYGEN_TYPE = FLUID_TYPES.register("liquid_oxygen", () -> new FluidType(
        FluidType.Properties.create()
            .descriptionId("block.asteroidmining.liquid_oxygen")
            .density(1141)
            .temperature(90)) { //-183°C
    });
    public static final DeferredHolder<FluidType, FluidType> LIQUID_AIR_TYPE = FLUID_TYPES.register("liquid_air", () -> new FluidType(
        FluidType.Properties.create()
            .descriptionId("block.asteroidmining.liquid_air")
            .density(870)
            .temperature(77)) { //-196°C
    });
    public static final DeferredHolder<FluidType, FluidType> METHANE_TYPE = FLUID_TYPES.register("methane", () -> new FluidType(
        FluidType.Properties.create()
            .descriptionId("block.asteroidmining.methane")
            .density(1)) {
    });
    public static final DeferredHolder<FluidType, FluidType> LIQUID_METHANE_TYPE = FLUID_TYPES.register("liquid_methane", () -> new FluidType(
        FluidType.Properties.create()
            .descriptionId("block.asteroidmining.liquid_methane")
            .density(426)
            .temperature(111)) { //-162°C
    });
    public static final DeferredHolder<FluidType, FluidType> HYDROGEN_TYPE = FLUID_TYPES.register("hydrogen", () -> new FluidType(
        FluidType.Properties.create()
            .descriptionId("block.asteroidmining.hydrogen")
            .density(15)) {
    });
    public static final DeferredHolder<FluidType, FluidType> LIQUID_HYDROGEN_TYPE = FLUID_TYPES.register("liquid_hydrogen", () -> new FluidType(
        FluidType.Properties.create()
            .descriptionId("block.asteroidmining.liquid_hydrogen")
            .density(71)
            .temperature(20)) { //-253°C
    });
    public static final DeferredHolder<FluidType, FluidType> ROCKET_PROPELLANT_TYPE = FLUID_TYPES.register("rocket_propellant", () -> new FluidType(
        FluidType.Properties.create()
            .descriptionId("block.asteroidmining.rocket_propellant")
            .density(1500)
            .temperature(293)) { //20°C
    });
    public static final DeferredHolder<FluidType, FluidType> PETROLEUM_TYPE = FLUID_TYPES.register("petroleum", () -> new FluidType(FluidType.Properties.create()
        .descriptionId("block.asteroidmining.petroleum")
        .canExtinguish(true)
        .fallDistanceModifier(0F)
        .density(900)
        .viscosity(3000)
        .temperature(293)) { //20°C
    });
    public static final DeferredHolder<FluidType, FluidType> KEROSENE_TYPE = FLUID_TYPES.register("kerosene", () -> new FluidType(FluidType.Properties.create()
        .descriptionId("block.asteroidmining.kerosene")
        .canExtinguish(true)
        .fallDistanceModifier(0F)
        .density(800)
        .viscosity(2500)
        .temperature(293)) { //20°C
    });

    public static final Function<Boolean, BaseFlowingFluid.Properties> AIR_PROPERTIES = (isLiquid) ->
        new BaseFlowingFluid.Properties(isLiquid ? LIQUID_AIR_TYPE::value : AIR_TYPE::value,
            isLiquid ? LIQUID_AIR::value : AIR::value,
            isLiquid ? LIQUID_AIR::value : AIR::value)
            .bucket(isLiquid ? ModItems.LIQUID_AIR_BUCKET : ModItems.AIR_BUCKET);
    public static final BaseFlowingFluid.Properties LIQUID_OXYGEN_PROPERTIES =
        new BaseFlowingFluid.Properties(LIQUID_OXYGEN_TYPE::value, LIQUID_OXYGEN::value, LIQUID_OXYGEN::value)
            .bucket(ModItems.LIQUID_OXYGEN_BUCKET);
    public static final Function<Boolean, BaseFlowingFluid.Properties> METHANE_PROPERTIES = (isLiquid) ->
        new BaseFlowingFluid.Properties(isLiquid ? LIQUID_METHANE_TYPE::value : METHANE_TYPE::value,
            isLiquid ? LIQUID_METHANE::value : METHANE::value,
            isLiquid ? LIQUID_METHANE::value : METHANE::value)
            .bucket(isLiquid ? ModItems.LIQUID_METHANE_BUCKET : ModItems.METHANE_BUCKET);
    public static final Function<Boolean, BaseFlowingFluid.Properties> HYDROGEN_PROPERTIES = (isLiquid) ->
        new BaseFlowingFluid.Properties(isLiquid ? LIQUID_HYDROGEN_TYPE::value : HYDROGEN_TYPE::value,
            isLiquid ? LIQUID_HYDROGEN::value : HYDROGEN::value,
            isLiquid ? LIQUID_HYDROGEN::value : HYDROGEN::value)
            .bucket(isLiquid ? ModItems.LIQUID_HYDROGEN_BUCKET : ModItems.HYDROGEN_BUCKET);
    public static final BaseFlowingFluid.Properties PETROLEUM_PROPERTIES =
        new BaseFlowingFluid.Properties(PETROLEUM_TYPE::value, PETROLEUM_SOURCE::value, PETROLEUM_FLOWING::value)
            .bucket(ModItems.PETROLEUM_BUCKET).block(ModBlocks.PETROLEUM);
    public static final BaseFlowingFluid.Properties KEROSENE_PROPERTIES =
        new BaseFlowingFluid.Properties(KEROSENE_TYPE::value, KEROSENE_SOURCE::value, KEROSENE_FLOWING::value)
            .bucket(ModItems.KEROSENE_BUCKET).block(ModBlocks.KEROSENE);
    public static final BaseFlowingFluid.Properties ROCKET_PROPELLANT_PROPERTIES =
        new BaseFlowingFluid.Properties(ROCKET_PROPELLANT_TYPE::value, ROCKET_PROPELLANT::value, ROCKET_PROPELLANT::value)
            .bucket(ModItems.ROCKET_PROPELLANT_BUCKET);

    public static final Function<String, FluidModel.Unbaked> FLUID_UNBAKED_MODEL = (name) -> new FluidModel.Unbaked(
        new Material(AsteroidMining.makeId("fluid/" + name + "_still")),
        new Material(AsteroidMining.makeId("fluid/" + name + "_flow")),
        new Material(AsteroidMining.makeId("fluid/" + name + "_overlay")),
        FluidTintSources.water() //TODO
    );

    public static final Function<String, IClientFluidTypeExtensions> STILL_EXTENSION = (name) -> new IClientFluidTypeExtensions() {
        @Override
        public Identifier getRenderOverlayTexture(final Minecraft mc) {
            return AsteroidMining.makeId("fluid/" + name);
        }
    };

    public static final Function<String, IClientFluidTypeExtensions> LIQUID_EXTENSION = (name) -> new IClientFluidTypeExtensions() {
        @Override
        public Identifier getRenderOverlayTexture(final Minecraft mc) {
            return AsteroidMining.makeId("textures/fluid/" + name + "_overlay.png");
        }

        /**
         * Copied and modified from {@link ScreenEffectRenderer#renderFluid(Minecraft, PoseStack, MultiBufferSource, Identifier)}
         */
        @Override
        public void renderOverlay(final Minecraft mc, final PoseStack poseStack, final MultiBufferSource buffers) {
            final Identifier texture = this.getRenderOverlayTexture(mc);
            if (mc.player == null) {
                return;
            }

            final BlockPos pos = BlockPos.containing(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
            final float br = Lightmap.getBrightness(mc.player.level().dimensionType(), mc.player.level().getMaxLocalRawBrightness(pos));
            final int color = ARGB.colorFromFloat(0.1F, br, br, br);
            final float uo = -mc.player.getYRot() / 64.0F;
            final float vo = mc.player.getXRot() / 64.0F;
            final Matrix4f pose = poseStack.last().pose();
            final VertexConsumer builder = buffers.getBuffer(RenderTypes.blockScreenEffect(texture));
            builder.addVertex(pose, -1.0F, -1.0F, -0.5F).setUv(4.0F + uo, 4.0F + vo).setColor(color);
            builder.addVertex(pose, 1.0F, -1.0F, -0.5F).setUv(0.0F + uo, 4.0F + vo).setColor(color);
            builder.addVertex(pose, 1.0F, 1.0F, -0.5F).setUv(0.0F + uo, 0.0F + vo).setColor(color);
            builder.addVertex(pose, -1.0F, 1.0F, -0.5F).setUv(4.0F + uo, 0.0F + vo).setColor(color);
        }
    };

    private ModFluids() {
    }
}
