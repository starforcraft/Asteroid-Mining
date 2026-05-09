package com.ultramega.asteroidmining.blockentities.renderer;

import com.ultramega.asteroidmining.blockentities.LaunchPadBuilderBlockEntity;
import com.ultramega.asteroidmining.events.ClientEvents;
import com.ultramega.asteroidmining.gui.renderer.LaunchPadPreviewRenderState;
import com.ultramega.asteroidmining.utils.ClientUtils;
import com.ultramega.asteroidmining.utils.PreviewInfo;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.model.data.ModelData;
import org.jspecify.annotations.Nullable;

public class LaunchPadBuilderBlockEntityRenderer implements BlockEntityRenderer<LaunchPadBuilderBlockEntity, LaunchPadBuilderBlockEntityRenderState> {
    public LaunchPadBuilderBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void extractRenderState(final LaunchPadBuilderBlockEntity blockEntity,
                                   final LaunchPadBuilderBlockEntityRenderState state,
                                   final float partialTicks,
                                   final Vec3 cameraPosition,
                                   final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

        state.previewState.previewBlocks.clear();
        state.previewState.rocketPositions.clear();

        final BlockPos launchPadPos = blockEntity.getBlockPos();
        final List<PreviewInfo> previewList = ClientEvents.LAUNCH_PAD_PREVIEW_BLOCKS.get(launchPadPos);
        if (previewList == null || previewList.isEmpty()) {
            return;
        }

        final ClientLevel level = (ClientLevel) blockEntity.getLevel();
        if (level == null) {
            return;
        }

        for (final PreviewInfo info : previewList) {
            if (info.expectedBlock().isPresent()) {
                BlockState previewBlockState = info.expectedBlock().get().defaultBlockState();

                if (ClientEvents.HIDE_PREVIEW_BLOCKS.contains(launchPadPos) && !previewBlockState.isAir()) {
                    continue;
                }

                final BlockState currentState = level.getBlockState(info.pos());
                if (previewBlockState.is(currentState.getBlock())) {
                    continue;
                }

                if (previewBlockState.isAir()) {
                    previewBlockState = Blocks.RED_TERRACOTTA.defaultBlockState();
                }

                state.previewState.previewBlocks.add(new LaunchPadPreviewRenderState.Entry(
                    info.pos(),
                    !currentState.isAir(),
                    buildMovingBlockRenderState(level, info.pos(), previewBlockState)
                ));
            } else {
                state.previewState.rocketPositions.add(info.pos());
            }
        }
    }

    @Override
    public void submit(final LaunchPadBuilderBlockEntityRenderState state,
                       final PoseStack poseStack,
                       final SubmitNodeCollector collector,
                       final CameraRenderState camera) {
        LaunchPadPreviewRenderState.submitPreviewBlocks(
            state.previewState,
            poseStack,
            collector,
            state.blockPos
        );

//        for (final LaunchPadPreviewRenderState.Entry entry : state.previewState.previewBlocks) {
//            if (!entry.occupied()) {
//                continue;
//            }
//
//            collector.submitCustomGeometry(
//                poseStack,
//                RenderTypes.lines(),
//                (pose, consumer) -> {
//                    ShapeRenderer.renderShape(
//                        poseStack,
//                        consumer,
//                        Shapes.block().move(entry.pos().getX(), entry.pos().getY(), entry.pos().getZ()),
//                        -camera.pos.x,
//                        -camera.pos.y,
//                        -camera.pos.z,
//                        0x99FF0000,
//                        2F
//                    );
//                }
//            );
//        }

        // Render area where blocks for the rocket can be placed
        if (!state.previewState.rocketPositions.isEmpty()) {
            collector.submitCustomGeometry(
                poseStack,
                RenderTypes.lines(),
                (_, consumer) -> ClientUtils.drawConnectedWireframe(
                    poseStack,
                    consumer,
                    state.previewState.rocketPositions,
                    camera.pos
                )
            );
        }
    }

    @Override
    public AABB getRenderBoundingBox(final LaunchPadBuilderBlockEntity blockEntity) { //TODO: just pass the max size of the launch pad instead?
        final BlockPos origin = blockEntity.getBlockPos();
        final List<PreviewInfo> previewList = ClientEvents.LAUNCH_PAD_PREVIEW_BLOCKS.get(origin);
        AABB box = new AABB(origin);

        if (previewList == null || previewList.isEmpty()) {
            return box.inflate(1.0D);
        }

        for (final PreviewInfo info : previewList) {
            box = box.minmax(new AABB(info.pos()));
        }

        return box.inflate(1.0D);
    }

    @Override
    public LaunchPadBuilderBlockEntityRenderState createRenderState() {
        return new LaunchPadBuilderBlockEntityRenderState();
    }

    private static MovingBlockRenderState buildMovingBlockRenderState(final ClientLevel level,
                                                                      final BlockPos pos,
                                                                      final BlockState state) {
        final MovingBlockRenderState renderState = new MovingBlockRenderState();
        renderState.blockPos = pos;
        renderState.randomSeedPos = pos;
        renderState.blockState = state;
        renderState.biome = level.getBiome(pos);
        renderState.modelData = ModelData.EMPTY;
        renderState.lightEngine = level.getLightEngine();
        renderState.cardinalLighting = level.cardinalLighting();
        return renderState;
    }
}
