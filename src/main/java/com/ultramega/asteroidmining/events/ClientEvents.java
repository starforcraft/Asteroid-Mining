package com.ultramega.asteroidmining.events;

import com.ultramega.asteroidmining.blockentities.renderer.RocketEngineBlockEntityRenderer;
import com.ultramega.asteroidmining.blocks.RocketEngineBlock;
import com.ultramega.asteroidmining.entities.renderer.BlockStructureEntityRenderer;
import com.ultramega.asteroidmining.gui.AirAbsorberScreen;
import com.ultramega.asteroidmining.gui.BiogasPlantScreen;
import com.ultramega.asteroidmining.gui.DistillationColumnScreen;
import com.ultramega.asteroidmining.gui.ElectrolysisPlantScreen;
import com.ultramega.asteroidmining.gui.HeatExchangerScreen;
import com.ultramega.asteroidmining.gui.LaunchPadBuilderScreen;
import com.ultramega.asteroidmining.gui.ObservatoryScreen;
import com.ultramega.asteroidmining.gui.RocketControllerConfigurationScreen;
import com.ultramega.asteroidmining.gui.RocketControllerScreen;
import com.ultramega.asteroidmining.gui.RocketStorageViewerScreen;
import com.ultramega.asteroidmining.gui.renderer.ScenePictureInPictureRenderer;
import com.ultramega.asteroidmining.particles.BigSmokeParticle;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.registry.ModEntityTypes;
import com.ultramega.asteroidmining.registry.ModFluids;
import com.ultramega.asteroidmining.registry.ModMenuTypes;
import com.ultramega.asteroidmining.registry.ModParticles;
import com.ultramega.asteroidmining.utils.CameraHandler;
import com.ultramega.asteroidmining.utils.PreviewInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientEvents {
    public static final Map<BlockPos, UUID> LAUNCH_PAD_BUILDER_POS = new Object2ObjectOpenHashMap<>();
    public static final Map<BlockPos, List<PreviewInfo>> LAUNCH_PAD_PREVIEW_BLOCKS = new Object2ObjectOpenHashMap<>();
    // TODO: Investigate if this is a good way to go about this (Hides preview blocks on launch so that the chopsticks aren't visible)
    public static final Set<BlockPos> HIDE_PREVIEW_BLOCKS = new HashSet<>();

    private static final ThreadLocal<RandomSource> RANDOM = ThreadLocal.withInitial(RandomSource::create);

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage1(final RenderLevelStageEvent.AfterTranslucentBlocks event) {
        // Change transparency of preview blocks
        final Set<SectionPos> sections = new ObjectOpenHashSet<>();
        LAUNCH_PAD_PREVIEW_BLOCKS.forEach((key, value) -> {
            for (final PreviewInfo info : value) {
                sections.add(SectionPos.of(info.pos()));
            }
        });

        for (final SectionPos section : sections) {
            Minecraft.getInstance().levelRenderer.setSectionDirty(section.x(), section.y(), section.z());
        }
    }

    @SubscribeEvent
    public static void addGeometryEvent(final AddSectionGeometryEvent event) {
        if (LAUNCH_PAD_PREVIEW_BLOCKS.isEmpty()) {
            return;
        }

        final SectionPos section = SectionPos.of(event.getSectionOrigin());
        final Map<BlockPos, BlockState> previewBlocks = new Object2ObjectOpenHashMap<>();

        LAUNCH_PAD_PREVIEW_BLOCKS.forEach((key, previewList) -> {
            if (previewList != null) {
                for (final PreviewInfo info : previewList) {
                    if (SectionPos.of(info.pos()).equals(section)) {
                        if (info.expectedBlock().isPresent()) {
                            final BlockState state = info.expectedBlock().get().defaultBlockState();
                            if (HIDE_PREVIEW_BLOCKS.contains(key) && !state.isAir()) {
                                continue;
                            }
                            previewBlocks.put(info.pos(), state);
                        }
                    }
                }
            }
        });

        //TODO reimplement
//        event.addRenderer(context -> {
//            final BlockAndTintGetter level = context.getRegion();
//            final RandomSource random = RANDOM.get();
//
//            for (final Map.Entry<BlockPos, BlockState> entry : previewBlocks.entrySet()) {
//                random.setSeed(42L);
//
//                final BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
//                BlockState previewState = entry.getValue();
//                final BlockPos pos = entry.getKey();
//                final PoseStack poseStack = context.getPoseStack();
//
//                final BlockState currentState = level.getBlockState(pos);
//
//                final boolean isCorrectBlock = previewState.is(currentState.getBlock());
//                if (isCorrectBlock) {
//                    continue;
//                } else if (previewState.isAir()) {
//                    previewState = Blocks.RED_TERRACOTTA.defaultBlockState();
//                }
//
//                final BakedModel targetModel = blockRenderer.getBlockModel(previewState);
//                final ModelData modelData = targetModel.getModelData(level, pos, previewState, ModelData.EMPTY);
//
//                poseStack.pushPose();
//                poseStack.translate(SectionPos.sectionRelative(pos.getX()), SectionPos.sectionRelative(pos.getY()), SectionPos.sectionRelative(pos.getZ()));
//
//                poseStack.translate(0.5, 0.5, 0.5);
//                poseStack.scale(1.005F, 1.005F, 1.005F);
//                poseStack.translate(-0.5, -0.5, -0.5);
//
//                for (final RenderType renderType : targetModel.getRenderTypes(previewState, random, ModelData.EMPTY)) {
//                    VertexConsumer buffer = context.getOrCreateChunkBuffer(RenderType.translucent());
//                    buffer = new AlphaColorWrapper(buffer, !currentState.isAir());
//                    blockRenderer.renderBatched(previewState, pos, level, poseStack, buffer, true, random, modelData, renderType);
//                }
//
//                poseStack.popPose();
//            }
//        });
    }

    @SubscribeEvent
    public static void renderOutline(final ExtractBlockOutlineRenderStateEvent event) {
        if (event.getCamera().entity() instanceof LivingEntity entity) {
            final BlockHitResult hitResult = event.getHitResult();
            final BlockPos pos = hitResult.getBlockPos();
            final BlockState targetState = entity.level().getBlockState(pos);

            if (LAUNCH_PAD_PREVIEW_BLOCKS.values().stream()
                .anyMatch(infos -> infos.stream().anyMatch(info -> info.pos().equals(pos) && info.expectedBlock().isPresent()))
            ) {
                event.getLevelRenderState().blockOutlineRenderState = new BlockOutlineRenderState(
                    pos,
                    event.isInTranslucentPass(),
                    event.isHighContrast(),
                    targetState.getOcclusionShape(),
                    List.of()
                );
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage2(final RenderLevelStageEvent.AfterOpaqueBlocks event) {
        // TODO: reimplement
//        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
//            return;
//        }
//
//        final Minecraft mc = Minecraft.getInstance();
//        final MultiBufferSource buffer = mc.renderBuffers().bufferSource();
//        final Camera camera = mc.gameRenderer.getMainCamera();
//        final Vec3 cameraPos = camera.getPosition();
//
//        // Render block highlight at cursor position
//        if (mc.screen instanceof LaunchPadConfigureScreen configureScreen) {
//            final PoseStack poseStack = new PoseStack();
//            poseStack.mulPose(event.getModelViewMatrix());
//            final Matrix4f projectionViewMatrix = new Matrix4f(event.getProjectionMatrix());
//            projectionViewMatrix.mul(poseStack.last().pose());
//            projectionViewMatrix.invert();
//
//            configureScreen.setProjectionViewMatrix(projectionViewMatrix);
//
//            if (configureScreen.getBlockUnderCursor() != null) {
//                final AABB box = new AABB(configureScreen.getBlockUnderCursor()).move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
//
//                LevelRenderer.renderLineBox(
//                    event.getPoseStack(),
//                    buffer.getBuffer(RenderType.lines()),
//                    box,
//                    0.0F, 1.0F, 0.0F,
//                    1.0F
//                );
//            }
//        }
//
//        // Render area where blocks for the rocket can be placed
//        final Set<BlockPos> rocketPos = new HashSet<>();
//        LAUNCH_PAD_PREVIEW_BLOCKS.forEach((key, previewList) -> {
//            if (previewList != null) {
//                for (final PreviewInfo info : previewList) {
//                    if (info.expectedBlock().isEmpty()) {
//                        rocketPos.add(info.pos());
//                    }
//                }
//            }
//        });
//
//        if (!rocketPos.isEmpty()) {
//            Utils.drawConnectedWireframe(event.getPoseStack(), buffer.getBuffer(RenderType.lines()), rocketPos, cameraPos);
//        }
    }

    @SubscribeEvent
    public static void cancelRenderingGuiLayers(final RenderGuiLayerEvent.Pre event) {
        if (CameraHandler.isCameraOutsideOfPlayer()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void cancelRenderingHand(final RenderHandEvent event) {
        if (CameraHandler.isCameraOutsideOfPlayer()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void registerScreens(final RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.DISTILLATION_COLUMN.get(), DistillationColumnScreen::new);
        event.register(ModMenuTypes.AIR_ABSORBER.get(), AirAbsorberScreen::new);
        event.register(ModMenuTypes.HEAT_EXCHANGER.get(), HeatExchangerScreen::new);
        event.register(ModMenuTypes.ELECTROLYSIS_PLANT.get(), ElectrolysisPlantScreen::new);
        event.register(ModMenuTypes.BIOGAS_PLANT.get(), BiogasPlantScreen::new);
        event.register(ModMenuTypes.ROCKET_CONTROLLER.get(), RocketControllerScreen::new);
        event.register(ModMenuTypes.SELECT_CONFIGURATION.get(), RocketControllerConfigurationScreen::new);
        event.register(ModMenuTypes.ROCKET_STORAGE_VIEWER.get(), RocketStorageViewerScreen::new);
        event.register(ModMenuTypes.LAUNCH_PAD_BUILDER.get(), LaunchPadBuilderScreen::new);
        event.register(ModMenuTypes.SMALL_OBSERVATORY.get(), ObservatoryScreen::new);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        for (final RocketEngineBlock.Type type : RocketEngineBlock.Type.values()) {
            event.registerBlockEntityRenderer(type.getBlockEntity().get(), RocketEngineBlockEntityRenderer::new);
        }
        event.registerEntityRenderer(ModEntityTypes.BLOCK_STRUCTURE_ENTITY.get(), BlockStructureEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(final RegisterClientExtensionsEvent event) {
        event.registerFluidType(ModFluids.STILL_EXTENSION.apply("air"), ModFluids.AIR_TYPE.get());
        event.registerFluidType(ModFluids.STILL_EXTENSION.apply("liquid_air"), ModFluids.LIQUID_AIR_TYPE.get());
        event.registerFluidType(ModFluids.STILL_EXTENSION.apply("liquid_oxygen"), ModFluids.LIQUID_OXYGEN_TYPE.get());
        event.registerFluidType(ModFluids.STILL_EXTENSION.apply("methane"), ModFluids.METHANE_TYPE.get());
        event.registerFluidType(ModFluids.STILL_EXTENSION.apply("liquid_methane"), ModFluids.LIQUID_METHANE_TYPE.get());
        event.registerFluidType(ModFluids.STILL_EXTENSION.apply("hydrogen"), ModFluids.HYDROGEN_TYPE.get());
        event.registerFluidType(ModFluids.STILL_EXTENSION.apply("liquid_hydrogen"), ModFluids.LIQUID_HYDROGEN_TYPE.get());
        event.registerFluidType(ModFluids.LIQUID_EXTENSION.apply("petroleum"), ModFluids.PETROLEUM_TYPE.get());
        event.registerFluidType(ModFluids.LIQUID_EXTENSION.apply("kerosene"), ModFluids.KEROSENE_TYPE.get());
        event.registerBlock(ModBlocks.crack(), ModBlocks.BOUNDING_BOX.get());
    }

    @SubscribeEvent
    public static void registerFluidModels(final RegisterFluidModelsEvent event) { //TODO: test this
        event.register(ModFluids.FLUID_UNBAKED_MODEL.apply("petroleum"), ModFluids.PETROLEUM_SOURCE.get(), ModFluids.PETROLEUM_FLOWING.get());
        event.register(ModFluids.FLUID_UNBAKED_MODEL.apply("kerosene"), ModFluids.KEROSENE_SOURCE.get(), ModFluids.KEROSENE_FLOWING.get());
    }

    @SubscribeEvent
    public static void registerParticleProviders(final RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.BIG_SMOKE_PARTICLE.get(), BigSmokeParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerPipRenderers(final RegisterPictureInPictureRenderersEvent event) {
        event.register(ScenePictureInPictureRenderer.State.class, ScenePictureInPictureRenderer::new);
    }
}
