package com.ultramega.asteroidmining.entities.renderer;

import com.ultramega.asteroidmining.entities.BlockStructureEntity;

import java.util.Comparator;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class BlockStructureEntityRenderer extends EntityRenderer<BlockStructureEntity, BlockStructureEntityRenderState> { //TODO: test this class
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
        state.origin = infos.stream()
            .map(StructureTemplate.StructureBlockInfo::pos)
            .min(Comparator.comparingInt(Vec3i::getY))
            .orElse(BlockPos.ZERO);

        state.movingBlocks.clear();
        state.blockEntities.clear();

        for (final StructureTemplate.StructureBlockInfo info : infos) {
            final BlockPos pos = info.pos();
            final BlockState blockState = info.state();

            final MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
            movingBlockRenderState.blockPos = pos;
            movingBlockRenderState.blockState = blockState;
            state.movingBlocks.add(new BlockStructureEntityRenderState.MovingBlockEntry(pos, movingBlockRenderState));

            if (blockState.hasBlockEntity()) {
                final BlockEntity blockEntity = entity.blockEntityCache.get(pos);
                if (blockEntity != null) {
                    state.blockEntities.add(new BlockStructureEntityRenderState.BlockEntityEntry(pos, blockEntity));
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
        poseStack.translate(pivot.getX(), pivot.getY(), pivot.getZ());
        poseStack.mulPose(Axis.YP.rotationDegrees(-renderState.interpolatedYRot));
        poseStack.translate(-pivot.getX(), -pivot.getY(), -pivot.getZ());

        // TODO: Are blocks hidden by others rendered?
        for (final BlockStructureEntityRenderState.MovingBlockEntry entry : renderState.movingBlocks) {
            final BlockPos relativePos = entry.pos().subtract(renderState.origin);

            poseStack.pushPose();
            poseStack.translate(relativePos.getX(), relativePos.getY(), relativePos.getZ());

            collector.submitMovingBlock(poseStack, entry.renderState());

            poseStack.popPose();
        }

        for (final BlockStructureEntityRenderState.BlockEntityEntry entry : renderState.blockEntities) {
            final BlockPos relativePos = entry.pos().subtract(renderState.origin);

            poseStack.pushPose();
            poseStack.translate(relativePos.getX(), relativePos.getY(), relativePos.getZ());

            final BlockEntityRenderState beState = this.blockEntityRenderDispatcher.tryExtractRenderState(entry.blockEntity(), renderState.partialTicks, null, null);
            if (beState != null) {
                this.blockEntityRenderDispatcher.submit(beState, poseStack, collector, camera);
            }

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(final BlockStructureEntity entity, final Frustum camera, final double camX, final double camY, final double camZ) {
        //TODO or fix bounding box
        return super.shouldRender(entity, camera, camX, camY, camZ);
    }

    @Override
    public BlockStructureEntityRenderState createRenderState() {
        return new BlockStructureEntityRenderState();
    }
}
