package com.ultramega.asteroidmining.events;

import com.ultramega.asteroidmining.gui.LaunchPadConfigureScreen;
import com.ultramega.asteroidmining.utils.PreviewInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.pipeline.VertexConsumerWrapper;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

@EventBusSubscriber(value = Dist.CLIENT)
public final class PreviewClientEvents {
    public static final Map<BlockPos, UUID> LAUNCH_PAD_BUILDER_POS = new Object2ObjectOpenHashMap<>(); //TODO: delete?
    public static final Map<BlockPos, List<PreviewInfo>> LAUNCH_PAD_PREVIEW_BLOCKS = new Object2ObjectOpenHashMap<>();
    // TODO: Investigate if this is a good way to go about this (Hides preview blocks on launch so that the chopsticks aren't visible)
    public static final Set<BlockPos> HIDE_PREVIEW_BLOCKS = new HashSet<>();

    private static final float PREVIEW_BLOCK_SCALE = 1.005F;

    private PreviewClientEvents() {
    }

    @SubscribeEvent
    public static void onRenderLevelStageAfterTranslucent(final RenderLevelStageEvent.AfterTranslucentBlocks event) {
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
            for (final PreviewInfo info : previewList) {
                if (!SectionPos.of(info.pos()).equals(section)) {
                    continue;
                }

                if (info.expectedBlock().isEmpty()) {
                    continue;
                }

                final BlockState state = info.expectedBlock().get().defaultBlockState();

                if (HIDE_PREVIEW_BLOCKS.contains(key) && !state.isAir()) {
                    continue;
                }

                previewBlocks.put(info.pos(), state);
            }
        });

        if (previewBlocks.isEmpty()) {
            return;
        }

        event.addRenderer(context -> {
            final BlockAndTintGetter level = context.getRegion();
            final Minecraft mc = Minecraft.getInstance();

            final BlockStateModelSet modelSet = mc.getModelManager().getBlockStateModelSet();
            final ModelBlockRenderer blockRenderer = context.getBlockRenderer();

            for (final Map.Entry<BlockPos, BlockState> entry : previewBlocks.entrySet()) {
                final BlockPos pos = entry.getKey();
                BlockState previewState = entry.getValue();

                final BlockState currentState = level.getBlockState(pos);
                final boolean isCorrectBlock = previewState.is(currentState.getBlock());

                if (isCorrectBlock) {
                    continue;
                } else if (previewState.isAir()) {
                    previewState = Blocks.RED_TERRACOTTA.defaultBlockState();
                }

                final BlockStateModel model = modelSet.get(previewState);

                final float baseX = SectionPos.sectionRelative(pos.getX());
                final float baseY = SectionPos.sectionRelative(pos.getY());
                final float baseZ = SectionPos.sectionRelative(pos.getZ());

                final VertexConsumer buffer = new AlphaColorWrapper(context.getOrCreateChunkBuffer(ChunkSectionLayer.TRANSLUCENT), !currentState.isAir());
                final BlockQuadOutput output = scaledPreviewOutput(buffer, PREVIEW_BLOCK_SCALE);

                blockRenderer.tesselateBlock(output, baseX, baseY, baseZ, level, pos, previewState, model, previewState.getSeed(pos));
            }
        });
    }

    @SubscribeEvent
    public static void onRenderLevelStageAfterOpaque(final RenderLevelStageEvent.AfterOpaqueBlocks event) {
        // Cursor highlight
        if (Minecraft.getInstance().screen instanceof LaunchPadConfigureScreen configureScreen) {
            final Matrix4f projectionViewMatrix = new Matrix4f(event.getModelViewMatrix());
            projectionViewMatrix.invert();

            configureScreen.setInverseProjectionViewMatrix(projectionViewMatrix);

            final BlockPos hovered = configureScreen.getBlockUnderCursor();
            if (hovered != null) {
                final AABB box = new AABB(hovered);

                Gizmos.cuboid(box, GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 0.0F, 1.0F, 0.0F)), true);
            }
        }
    }

    @SubscribeEvent
    public static void extractBlockOutline(final ExtractBlockOutlineRenderStateEvent event) {
        final BlockPos pos = event.getBlockPos();

        final boolean isPreviewBlock = LAUNCH_PAD_PREVIEW_BLOCKS.values().stream()
            .anyMatch(infos -> infos.stream()
                .anyMatch(info -> info.pos().equals(pos) && info.expectedBlock().isPresent()));
        if (!isPreviewBlock) {
            return;
        }

        event.addCustomRenderer((renderState, bufferSource, poseStack, translucentPass, levelRenderState) -> {
            if (renderState.isTranslucent() != translucentPass) {
                return false;
            }

            final Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
            final VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.lines());

            ShapeRenderer.renderShape(
                poseStack,
                buffer,
                renderState.shape(),
                renderState.pos().getX() - cameraPos.x,
                renderState.pos().getY() - cameraPos.y,
                renderState.pos().getZ() - cameraPos.z,
                0xFF000000,
                1.0F
            );

            event.setCanceled(true);
            return true;
        });
    }

    private static BlockQuadOutput scaledPreviewOutput(final VertexConsumer buffer, final float scale) {
        return (x, y, z, quad, instance) -> emitScaledQuad(buffer, x, y, z, quad, instance, scale);
    }

    private static void emitScaledQuad(final VertexConsumer buffer,
                                       final float x,
                                       final float y,
                                       final float z,
                                       final BakedQuad quad,
                                       final QuadInstance instance,
                                       final float scale) {
        final Vector3fc normal = quad.direction().getUnitVec3f();
        final int lightEmission = quad.materialInfo().lightEmission();

        for (int vertex = 0; vertex < 4; vertex++) {
            final Vector3fc pos = quad.position(vertex);

            final float vx = (pos.x() - 0.5F) * scale + 0.5F;
            final float vy = (pos.y() - 0.5F) * scale + 0.5F;
            final float vz = (pos.z() - 0.5F) * scale + 0.5F;

            final long packedUv = quad.packedUV(vertex);

            buffer.addVertex(
                vx + x,
                vy + y,
                vz + z,
                instance.getColor(vertex),
                UVPair.unpackU(packedUv),
                UVPair.unpackV(packedUv),
                instance.overlayCoords(),
                instance.getLightCoordsWithEmission(vertex, lightEmission),
                normal.x(),
                normal.y(),
                normal.z()
            );
        }
    }

    public static class AlphaColorWrapper extends VertexConsumerWrapper {
        private final boolean redTint;

        public AlphaColorWrapper(final VertexConsumer consumer, final boolean redTint) {
            super(consumer);
            this.redTint = redTint;
        }

        @Override
        public VertexConsumer setColor(final int color) {
            final int rgb = this.redTint ? 0x00FF0000 : color & 0x00FFFFFF;
            super.setColor(rgb | 0x99000000);
            return this;
        }
    }
}
