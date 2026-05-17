package com.ultramega.asteroidmining.entities.renderer;

import com.ultramega.asteroidmining.entities.BlockStructureEntity;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BlockStructureEntityRenderer extends EntityRenderer<BlockStructureEntity, BlockStructureEntityRenderState> {
    private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;

    public BlockStructureEntityRenderer(final EntityRendererProvider.Context context) {
        super(context);
        this.blockEntityRenderDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
    }

    @Override
    public void extractRenderState(final BlockStructureEntity entity, final BlockStructureEntityRenderState state, final float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.partialTicks = partialTicks;
        state.pivotPoint = entity.getPivotPoint();
        state.interpolatedYRot = entity.getPreciseBodyRotation(partialTicks);

        final List<StructureTemplate.StructureBlockInfo> infos = List.copyOf(entity.getStructureBlockInfos());
        state.structureBlockInfos = infos;

        state.movingBlocks.clear();
        state.blockEntities.clear();

        final Vec3 entityPos = entity.getPosition(partialTicks);

        for (final StructureTemplate.StructureBlockInfo info : infos) {
            final BlockPos localPos = info.pos();
            final BlockState blockState = info.state();
            final BlockPos worldPos = BlockPos.containing(entityPos.x + localPos.getX(), entityPos.y + localPos.getY(), entityPos.z + localPos.getZ());

            final MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
            movingBlockRenderState.blockPos = worldPos;
            movingBlockRenderState.blockState = blockState;
            if (entity.level() instanceof ClientLevel clientLevel) {
                movingBlockRenderState.biome = clientLevel.getBiome(worldPos);
                movingBlockRenderState.cardinalLighting = clientLevel.cardinalLighting();
                movingBlockRenderState.lightEngine = clientLevel.getLightEngine();
            }
            state.movingBlocks.add(new BlockStructureEntityRenderState.MovingBlockEntry(localPos, movingBlockRenderState));

            if (blockState.hasBlockEntity()) {
                final BlockEntity blockEntity = entity.blockEntityCache.get(localPos);
                if (blockEntity != null) {
                    state.blockEntities.add(new BlockStructureEntityRenderState.BlockEntityEntry(localPos, blockEntity));
                }
            }
        }
    }

    @Override
    public void submit(final BlockStructureEntityRenderState renderState,
                       final PoseStack poseStack,
                       final SubmitNodeCollector collector,
                       final CameraRenderState camera) {
        if (renderState.structureBlockInfos.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(-0.5, 0.0, -0.5);

        final BlockPos pivot = renderState.pivotPoint;
        poseStack.translate(pivot.getX() + 0.5, pivot.getY(), pivot.getZ() + 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-renderState.interpolatedYRot));
        poseStack.translate(-(pivot.getX() + 0.5), -pivot.getY(), -(pivot.getZ() + 0.5));

        // TODO: Are blocks hidden by others rendered?
        for (final BlockStructureEntityRenderState.MovingBlockEntry entry : renderState.movingBlocks) {
            final BlockPos pos = entry.pos();

            poseStack.pushPose();
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());

            collector.submitMovingBlock(poseStack, entry.renderState());

            poseStack.popPose();
        }

        for (final BlockStructureEntityRenderState.BlockEntityEntry entry : renderState.blockEntities) {
            final BlockPos pos = entry.pos();

            poseStack.pushPose();
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());

            final BlockEntityRenderState beState = this.blockEntityRenderDispatcher.tryExtractRenderState(entry.blockEntity(), renderState.partialTicks, null, null);
            if (beState != null) {
                this.blockEntityRenderDispatcher.submit(beState, poseStack, collector, camera);
            }

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    @Override
    protected AABB getBoundingBoxForCulling(final BlockStructureEntity entity) {
        return super.getBoundingBoxForCulling(entity);
    }

    @Override
    public BlockStructureEntityRenderState createRenderState() {
        return new BlockStructureEntityRenderState();
    }
}
