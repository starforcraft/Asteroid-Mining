package com.ultramega.asteroidmining.entities.renderer;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class BlockStructureEntityRenderState extends EntityRenderState {
    public float partialTicks;
    public BlockPos pivotPoint = BlockPos.ZERO;
    public float interpolatedYRot;
    public float interpolatedXRot;

    public List<StructureTemplate.StructureBlockInfo> structureBlockInfos = List.of();
    public final List<MovingBlockEntry> movingBlocks = new ArrayList<>();
    public final List<BlockEntityEntry> blockEntities = new ArrayList<>();

    public record MovingBlockEntry(BlockPos pos, MovingBlockRenderState renderState) {
    }

    public record BlockEntityEntry(BlockPos pos, BlockEntity blockEntity) {
    }
}
