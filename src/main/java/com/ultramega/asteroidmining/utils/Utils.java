package com.ultramega.asteroidmining.utils;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.storage.LaunchPadConfiguration;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Vector2ic;
import org.jspecify.annotations.Nullable;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public final class Utils { //TODO: split this class into Client and Common/Server
    public static final StreamCodec<ByteBuf, StructureTemplate.StructureBlockInfo> STRUCTURE_BLOCK_INFO_STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, StructureTemplate.StructureBlockInfo::pos,
        ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), StructureTemplate.StructureBlockInfo::state,
        ByteBufCodecs.COMPOUND_TAG, StructureTemplate.StructureBlockInfo::nbt,
        StructureTemplate.StructureBlockInfo::new
    );
    public static final StreamCodec<ByteBuf, List<StructureTemplate.StructureBlockInfo>> STRUCTURE_BLOCK_INFO_STREAM_CODEC_LIST =
        Utils.STRUCTURE_BLOCK_INFO_STREAM_CODEC.apply(ByteBufCodecs.list());

    public static final Codec<StructureTemplate.StructureBlockInfo> STRUCTURE_BLOCK_INFO_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BlockPos.CODEC.fieldOf("blockPos").forGetter(StructureTemplate.StructureBlockInfo::pos),
        BlockState.CODEC.fieldOf("state").forGetter(StructureTemplate.StructureBlockInfo::state),
        CompoundTag.CODEC.optionalFieldOf("nbt").forGetter(info -> Optional.ofNullable(info.nbt()))
    ).apply(instance, (pos, state, nbt) ->
        new StructureTemplate.StructureBlockInfo(pos, state, nbt.orElse(null))));

    public static final Codec<List<StructureTemplate.StructureBlockInfo>> STRUCTURE_BLOCK_INFO_LIST_CODEC = STRUCTURE_BLOCK_INFO_CODEC.listOf();

    public static final Codec<ItemStack> BIG_ITEM_STACK_CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create((instance) -> instance.group(
        Item.CODEC_WITH_BOUND_COMPONENTS.fieldOf("id").forGetter(ItemStack::typeHolder),
        ExtraCodecs.POSITIVE_INT.fieldOf("count").orElse(1).forGetter(ItemStack::getCount),
        DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(ItemStack::getComponentsPatch)
    ).apply(instance, ItemStack::new)));

    public static final Codec<List<BlockPos>> BLOCK_POS_LIST = Codec.list(BlockPos.CODEC);

    public static final long BUCKET_AMOUNT = 1000;

    private static final DecimalFormat FORMATTER_WITH_UNITS = new DecimalFormat(
        "####0.#",
        DecimalFormatSymbols.getInstance(Locale.US)
    );

    static {
        FORMATTER_WITH_UNITS.setRoundingMode(RoundingMode.FLOOR);
    }

    private Utils() {
    }

    public static void renderStacksTooltip(final GuiGraphicsExtractor graphics,
                                           final Font font,
                                           final List<ClientTooltipComponent> components,
                                           final int mouseX,
                                           final int mouseY,
                                           final ClientTooltipPositioner tooltipPositioner,
                                           final List<ItemFluidStack> stacks) {
        if (components.isEmpty()) {
            return;
        }

        final RenderTooltipEvent.Pre preEvent = ClientHooks.onRenderTooltipPre(ItemStack.EMPTY, graphics, mouseX, mouseY, graphics.guiWidth(), graphics.guiHeight(),
            components, font, tooltipPositioner);
        if (preEvent.isCanceled()) {
            return;
        }

        final int textWidth = components.stream()
            .mapToInt(component -> component.getWidth(preEvent.getFont()))
            .max().orElse(0);
        final int textHeight = components.stream()
            .mapToInt(c -> c.getHeight(font))
            .sum() + (components.size() > 1 ? 0 : -2);

        final int itemRows = (int) Math.ceil(stacks.size() / 4.0);
        final int itemWidth = Math.min(stacks.size(), 4) * 18;
        final int itemHeight = itemRows * 18;

        final int totalWidth = Math.max(textWidth, itemWidth);
        final int totalHeight = textHeight + itemHeight;

        final Vector2ic position = tooltipPositioner.positionTooltip(
            graphics.guiWidth(), graphics.guiHeight(), preEvent.getX(), preEvent.getY(), totalWidth, totalHeight);
        final int posX = position.x();
        final int posY = position.y();

        graphics.pose().pushMatrix();
        final RenderTooltipEvent.Texture event = ClientHooks.onRenderTooltipTexture(ItemStack.EMPTY, graphics, posX, posY, preEvent.getFont(), components, null);
        TooltipRenderUtil.extractTooltipBackground(graphics, posX, posY, textWidth, totalHeight, event.getTexture());

        int textY = posY;
        for (final ClientTooltipComponent component : components) {
            component.extractText(graphics, font, posX, textY);
            textY += component.getHeight(font) + (textY == posY ? 2 : 0);
        }

        textY = posY;
        for (final ClientTooltipComponent component : components) {
            component.extractImage(font, posX, textY, totalWidth, totalHeight, graphics);
            textY += component.getHeight(font) + (textY == posY ? 2 : 0);
        }

        if (!stacks.isEmpty()) {
            renderStacks(graphics, font, posX, textY, 4, stacks);
        }

        graphics.pose().popMatrix();
    }

    public static void renderStacksWithTooltip(final GuiGraphicsExtractor graphics,
                                               final Font font,
                                               final int mouseX,
                                               final int mouseY,
                                               final int posX,
                                               final int posY,
                                               final int lineBreak,
                                               final int leftPos,
                                               final int topPos,
                                               final List<ItemFluidStack> stacks) {
        int stackX = posX;
        int stackY = posY;
        for (int i = 0; i < stacks.size(); i++) {
            final ItemFluidStack stack = stacks.get(i);

            graphics.blitSprite(GUI_TEXTURED, AsteroidMining.makeId("slot"), stackX - 1, stackY - 1, 18, 18);
            if (stack.getItemStack() != null) {
                graphics.item(stack.getItemStack(), stackX, stackY);
            } else if (stack.getFluidStack() != null) {
                FluidContainerUtil.renderTiledFluid(graphics, stack.getFluidStack(), 0, 0, stackX, stackY, 16, 16);
            }
            renderAmount(graphics, font, stackX, stackY, Utils.formatWithUnits(stack.getCount()), 16777215);

            if (isMouseOver(leftPos + stackX, topPos + stackY, 18, 18, mouseX, mouseY)) {
                drawSlotHighlight(graphics, stackX, stackY);

                final Matrix3x2fStack poseStack = graphics.pose();
                poseStack.pushMatrix();
                poseStack.translate(-leftPos, 0);

                renderResourceTooltip(graphics, stack, mouseX, mouseY);

                poseStack.popMatrix();
            }

            if ((i + 1) % lineBreak == 0) {
                stackX = posX;
                stackY += 18;
            } else {
                stackX += 18;
            }
        }
    }

    public static void renderStacks(final GuiGraphicsExtractor graphics,
                                    final Font font,
                                    final int posX,
                                    final int posY,
                                    final int lineBreak,
                                    final List<ItemFluidStack> stacks) {
        int stackX = posX;
        int stackY = posY;
        for (int i = 0; i < stacks.size(); i++) {
            final ItemFluidStack stack = stacks.get(i);
            if (stack.getItemStack() != null) {
                graphics.item(stack.getItemStack(), stackX, stackY);
            } else if (stack.getFluidStack() != null) {
                FluidContainerUtil.renderTiledFluid(graphics, stack.getFluidStack(), 0, 0, stackX, stackY, 16, 16);
            }
            renderAmount(graphics, font, stackX, stackY, Utils.formatWithUnits(stack.getCount()), 0xFFFFFF);

            if ((i + 1) % lineBreak == 0) {
                stackX = posX;
                stackY += 18;
            } else {
                stackX += 18;
            }
        }
    }

    public static void renderAmount(final GuiGraphicsExtractor graphics,
                                    final Font font,
                                    final int x,
                                    final int y,
                                    final String text,
                                    final int color) {
        final Matrix3x2fStack poseStack = graphics.pose();
        poseStack.pushMatrix();
        poseStack.translate(x, y);
        poseStack.scale(0.5F, 0.5F);
        graphics.text(font, text, 32 - font.width(text), 23, color, true);
        poseStack.popMatrix();
    }

    public static void renderResourceTooltip(final GuiGraphicsExtractor graphics,
                                             @Nullable final ItemFluidStack stack,
                                             final int mouseX,
                                             final int mouseY) {
        if (stack == null) {
            return;
        }

        final List<Component> tooltip = new ArrayList<>();
        if (stack.getItemStack() != null) {
            tooltip.addAll(Screen.getTooltipFromItem(Minecraft.getInstance(), stack.getItemStack()));
            tooltip.add(Component.translatable("gui.asteroidmining.rocket_storage_viewer.total", stack.getItemStack().getCount())
                .withStyle(ChatFormatting.GRAY));

            renderResourceTooltip(graphics, stack.getItemStack(), tooltip, stack.getItemStack().getTooltipImage(), mouseX, mouseY);
        } else if (stack.getFluidStack() != null) {
            tooltip.add(stack.getFluidStack().getHoverName());
            tooltip.add(Component.translatable("gui.asteroidmining.rocket_storage_viewer.total", stack.getFluidStack().getAmount() + "mB")
                .withStyle(ChatFormatting.GRAY));

            renderResourceTooltip(graphics, ItemStack.EMPTY, tooltip, Optional.empty(), mouseX, mouseY);
        }
    }

    private static void renderResourceTooltip(final GuiGraphicsExtractor graphics,
                                              final ItemStack stack,
                                              final List<Component> tooltip,
                                              final Optional<TooltipComponent> tooltipImage,
                                              final int mouseX,
                                              final int mouseY) {
        final List<ClientTooltipComponent> components = ClientHooks.gatherTooltipComponents(
            stack, tooltip, tooltipImage, mouseX, graphics.guiWidth(), graphics.guiHeight(), Minecraft.getInstance().font);

        graphics.tooltip(
            Minecraft.getInstance().font,
            components,
            mouseX,
            mouseY,
            DefaultTooltipPositioner.INSTANCE,
            null
        );
    }

    public static List<ClientTooltipComponent> createTooltip(final List<Component> component) {
        return component.stream().map(comp -> ClientTooltipComponent.create(comp.getVisualOrderText())).toList();
    }

    public static List<ClientTooltipComponent> createTooltip(final Component component) {
        return List.of(ClientTooltipComponent.create(component.getVisualOrderText()));
    }

    public static void drawSlotHighlight(final GuiGraphicsExtractor graphics, final int x, final int y) {
        graphics.fillGradient(x, y, x + 16, y + 16, -2130706433, -2130706433);
    }

    /**
     * Copied from <a href="https://github.com/refinedmods/refinedstorage2/blob/develop/refinedstorage-common/src/main/java/com/refinedmods/refinedstorage/common/util/IdentifierUtil.java#L91">Refined Storage</a>
     */
    public static String formatWithUnits(final double qty) {
        if (qty < 0.001) {
            return "0";
        }

        return switch ((int) Math.floor(Math.log10(qty) / 3)) {
            case -1 -> FORMATTER_WITH_UNITS.format(qty * 10e2) + "m";
            case 0 -> FORMATTER_WITH_UNITS.format(qty >= 100 ? Math.floor(qty) : qty);
            case 1 -> FORMATTER_WITH_UNITS.format(qty >= 10e4 ? Math.floor(qty / 10e2) : qty / 10e2) + "k";
            case 2 -> FORMATTER_WITH_UNITS.format(qty >= 10e7 ? Math.floor(qty / 10e5) : qty / 10e5) + "M";
            case 3 -> FORMATTER_WITH_UNITS.format(qty >= 10e10 ? Math.floor(qty / 10e8) : qty / 10e8) + "G";
            case 4 -> FORMATTER_WITH_UNITS.format(qty >= 10e13 ? Math.floor(qty / 10e11) : qty / 10e11) + "T";
            case 5 -> FORMATTER_WITH_UNITS.format(qty >= 10e16 ? Math.floor(qty / 10e14) : qty / 10e14) + "P";
            case 6 -> FORMATTER_WITH_UNITS.format(qty >= 10e19 ? Math.floor(qty / 10e17) : qty / 10e17) + "E";
            default -> "∞";
        };
    }

    public static String formatWithUnitsFluid(final double qty) {
        return formatWithUnits(qty / Utils.BUCKET_AMOUNT) + "B";
    }

    public static String formatPlanetSize(final int size) {
        return "≈" + size * 5000 + " km";
    }

    public static String formatAstronomicalUnit(final float au) {
        return String.format("%.3f", au * 0.01) + " AU";
    }

    //TODO reimplement?
//    /**
//     * Copied and modified from {@link ModelBlockRenderer#tesselateWithoutAO(
//     *BlockAndTintGetter, BakedModel, BlockState, BlockPos, PoseStack, VertexConsumer, boolean, RandomSource, long, int, ModelData, RenderType)}
//     */
//    public static void tesselateBlock(final ModelBlockRenderer modelBlockRenderer,
//                                      final BlockAndTintGetter level,
//                                      final BakedModel model,
//                                      final BlockState state,
//                                      final BlockPos pos,
//                                      final PoseStack poseStack,
//                                      final VertexConsumer consumer,
//                                      final boolean checkSides,
//                                      final RandomSource random,
//                                      final long seed,
//                                      final int packedOverlay,
//                                      final int packedLight,
//                                      final ModelData modelData,
//                                      final RenderType renderType) {
//        final BitSet bitset = new BitSet(3);
//        final BlockPos.MutableBlockPos blockpos$mutableblockpos = pos.mutable();
//
//        for (final Direction direction : Direction.values()) {
//            random.setSeed(seed);
//            final List<BakedQuad> list = model.getQuads(state, direction, random, modelData, renderType);
//            if (!list.isEmpty()) {
//                blockpos$mutableblockpos.setWithOffset(pos, direction);
//                if (!checkSides || Block.shouldRenderFace(state, level, pos, direction, blockpos$mutableblockpos)) {
//                    modelBlockRenderer.renderModelFaceFlat(level, state, pos, packedLight, packedOverlay, false, poseStack, consumer, list, bitset);
//                }
//            }
//        }
//
//        random.setSeed(seed);
//        final List<BakedQuad> list = model.getQuads(state, null, random, modelData, renderType);
//        if (!list.isEmpty()) {
//            modelBlockRenderer.renderModelFaceFlat(level, state, pos, packedLight, packedOverlay, false, poseStack, consumer, list, bitset);
//        }
//    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(final BlockEntityType<A> serverType,
                                                                                                         final BlockEntityType<E> clientType,
                                                                                                         final BlockEntityTicker<? super E> ticker) {
        return clientType == serverType ? (BlockEntityTicker<A>) ticker : null;
    }

    public static float getRed(final int color) {
        return (color >> 16 & 255) / 255.0F;
    }

    public static float getGreen(final int color) {
        return (color >> 8 & 255) / 255.0F;
    }

    public static float getBlue(final int color) {
        return (color & 255) / 255.0F;
    }

    public static float getAlpha(final int color) {
        return (color >> 24 & 255) / 255.0F;
    }

    public static boolean isMouseOver(final int x, final int y, final int width, final int height, final int mouseX, final int mouseY) {
        return isMouseOver(x, y, width, height, (double) mouseX, mouseY);
    }

    public static boolean isMouseOver(final int x, final int y, final int width, final int height, final double mouseX, final double mouseY) {
        return isMouseOver((double) x, y, width, height, mouseX, mouseY);
    }

    public static boolean isMouseOver(final double x, final double y, final int width, final int height, final double mouseX, final double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static float randomOffset(final RandomSource random, final float range) {
        return Mth.nextFloat(random, -range, range);
    }

    public static int[] interpolateGradient(final float t, final int[] colorA, final int[] colorB, final int[] colorC) {
        if (t < 0.5f) {
            return lerpColor(t / 0.5f, colorA, colorB);
        } else {
            return lerpColor((t - 0.5f) / 0.5f, colorB, colorC);
        }
    }

    private static int[] lerpColor(final float t, final int[] from, final int[] to) {
        return new int[] {
            (int) (from[0] + (to[0] - from[0]) * t),
            (int) (from[1] + (to[1] - from[1]) * t),
            (int) (from[2] + (to[2] - from[2]) * t)
        };
    }

    public static BlockPos rotateCounterClockwise(final BlockPos center, final BlockPos pos) {
        final int dx = pos.getX() - center.getX();
        final int dz = pos.getZ() - center.getZ();

        final int rotatedX = center.getX() - dz;
        final int rotatedZ = center.getZ() + dx;

        return new BlockPos(rotatedX, pos.getY(), rotatedZ);
    }

    public static BlockPos rotateOffset(final BlockPos center, final Direction facing, final int dx, final int dz) {
        return switch (facing) {
            case NORTH -> center.offset(dx, 0, -dz);
            case SOUTH -> center.offset(-dx, 0, dz);
            case WEST -> center.offset(-dz, 0, -dx);
            case EAST -> center.offset(dz, 0, dx);
            default -> center.offset(dx, 0, dz);
        };
    }

    public static int getWidth(final List<StructureTemplate.StructureBlockInfo> positions) {
        final int minX = positions.stream().map(StructureTemplate.StructureBlockInfo::pos).mapToInt(BlockPos::getX).min().orElse(0);
        final int maxX = positions.stream().map(StructureTemplate.StructureBlockInfo::pos).mapToInt(BlockPos::getX).max().orElse(0);
        return Math.abs(maxX - minX + 1);
    }

    public static int getHeight(final List<StructureTemplate.StructureBlockInfo> positions) {
        final int minY = positions.stream().map(StructureTemplate.StructureBlockInfo::pos).mapToInt(BlockPos::getY).min().orElse(0);
        final int maxY = positions.stream().map(StructureTemplate.StructureBlockInfo::pos).mapToInt(BlockPos::getY).max().orElse(0);
        return Math.abs(maxY - minY + 1);
    }

    public static Vec3 getBottomCenter(final List<BlockPos> positions) {
        final int minX = positions.stream().mapToInt(BlockPos::getX).min().orElse(0);
        final int maxX = positions.stream().mapToInt(BlockPos::getX).max().orElse(0);
        final int minY = positions.stream().mapToInt(BlockPos::getY).min().orElse(0);
        final int minZ = positions.stream().mapToInt(BlockPos::getZ).min().orElse(0);
        final int maxZ = positions.stream().mapToInt(BlockPos::getZ).max().orElse(0);

        final double centerX = (minX + maxX) / 2.0 + 0.5;
        final double centerZ = (minZ + maxZ) / 2.0 + 0.5;

        return new Vec3(centerX, minY, centerZ);
    }

    public static PreviewBlockHitResult raytraceGivenBlocks(final Vec3 start, final Vec3 end, final List<PreviewInfo> targets, final Level level) {
        PreviewBlockHitResult closestHit = null;
        double closestDistance = Double.MAX_VALUE;
        final Vec3 rayDir = end.subtract(start);

        for (final PreviewInfo info : targets) {
            if (info.expectedBlock().isEmpty()) {
                continue;
            }

            final BlockPos pos = info.pos();
            final BlockState state = level.getBlockState(pos);
            if (info.expectedBlock().get().defaultBlockState().isAir() && state.isAir()) {
                continue;
            }
            final VoxelShape shape = state.getShape(level, pos);
            final AABB aabb = shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos);

            final Optional<Vec3> intersection = aabb.clip(start, end);
            if (intersection.isPresent()) {
                final double distance = start.distanceToSqr(intersection.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    final Direction hitDirection = Direction.getNearest((int) rayDir.x, (int) rayDir.y, (int) rayDir.z, null);
                    closestHit = new PreviewBlockHitResult(intersection.get(), hitDirection, pos, false, false, info);
                }
            }
        }

        if (closestHit != null) {
            return closestHit;
        }

        final Direction missDirection = Direction.getNearest((int) rayDir.x, (int) rayDir.y, (int) rayDir.z, null);
        return new PreviewBlockHitResult(true, end, missDirection, BlockPos.containing(end), false, false, null);
    }

    public static void drawConnectedWireframe(final PoseStack poseStack,
                                              final VertexConsumer consumer,
                                              final Set<BlockPos> blocks,
                                              final Vec3 cameraPos) {
        final int[][] offsets = {
            {0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1},
            {0, 1, 0}, {1, 1, 0}, {1, 1, 1}, {0, 1, 1}
        };

        record Face(Direction dir, int[] idx) { }

        final Face[] faces = {
            new Face(Direction.DOWN, new int[]{0, 1, 2, 3}),
            new Face(Direction.UP, new int[]{4, 5, 6, 7}),
            new Face(Direction.NORTH, new int[]{3, 2, 6, 7}),
            new Face(Direction.SOUTH, new int[]{1, 0, 4, 5}),
            new Face(Direction.WEST, new int[]{0, 3, 7, 4}),
            new Face(Direction.EAST, new int[]{2, 1, 5, 6})
        };

        record Edge(Vec3 a, Vec3 b) {
            static Edge of(final Vec3 p, final Vec3 q) {
                if (p.x < q.x || (p.x == q.x && (p.y < q.y || (p.y == q.y && p.z < q.z)))) {
                    return new Edge(p, q);
                } else {
                    return new Edge(q, p);
                }
            }
        }

        final Set<Edge> edges = new HashSet<>();

        for (final BlockPos pos : blocks) {
            final Vec3 base = Vec3.atLowerCornerOf(pos).subtract(cameraPos);
            final Vec3[] corners = new Vec3[8];
            for (int i = 0; i < 8; i++) {
                corners[i] = new Vec3(
                    base.x + offsets[i][0],
                    base.y + offsets[i][1],
                    base.z + offsets[i][2]
                );
            }

            for (final Face face : faces) {
                if (blocks.contains(pos.relative(face.dir))) {
                    continue;
                }

                final int[] idxs = face.idx;
                for (int j = 0; j < 4; j++) {
                    final int aIdx = idxs[j];
                    final int bIdx = idxs[(j + 1) & 3];

                    final int[] ao = offsets[aIdx];
                    final int[] bo = offsets[bIdx];

                    if (ao[1] == bo[1] && ao[1] == 0) {
                        if (blocks.contains(pos.below())) {
                            continue;
                        }
                    }
                    if (ao[1] == bo[1] && ao[1] == 1) {
                        if (blocks.contains(pos.above())) {
                            continue;
                        }
                    }

                    if (ao[1] != bo[1]) {
                        if (ao[0] == 0 && blocks.contains(pos.relative(Direction.WEST))) {
                            continue;
                        }
                        if (ao[0] == 1 && blocks.contains(pos.relative(Direction.EAST))) {
                            continue;
                        }
                        if (ao[2] == 0 && blocks.contains(pos.relative(Direction.NORTH))) {
                            continue;
                        }
                        if (ao[2] == 1 && blocks.contains(pos.relative(Direction.SOUTH))) {
                            continue;
                        }
                    } else {
                        if (ao[2] == 1 && bo[2] == 0 && blocks.contains(pos.relative(Direction.WEST))) {
                            continue;
                        }
                        if (ao[2] == 0 && bo[2] == 1 && blocks.contains(pos.relative(Direction.EAST))) {
                            continue;
                        }

                        if (ao[0] == 1 && bo[0] == 0 && blocks.contains(pos.relative(Direction.SOUTH))) {
                            continue;
                        }
                        if (ao[0] == 0 && bo[0] == 1 && blocks.contains(pos.relative(Direction.NORTH))) {
                            continue;
                        }

                        if (ao[0] == 1 && ao[1] == 0 && bo[2] == 0 && blocks.contains(pos.relative(Direction.NORTH))) {
                            continue;
                        }
                        if (ao[2] == 1 && ao[1] == 0 && bo[2] == 1 && blocks.contains(pos.relative(Direction.SOUTH))) {
                            continue;
                        }
                    }

                    final Vec3 p = corners[aIdx];
                    final Vec3 q = corners[bIdx];
                    edges.add(Edge.of(p, q));
                }
            }
        }

        final Matrix4f matrix = poseStack.last().pose();
        for (final Edge edge : edges) {
            //TODO: for some reason some lines are smaller than others (either find a better way to symbolize the area where the rocket can be build or fix this shit)
            consumer.addVertex(matrix, (float) edge.a.x(), (float) edge.a.y(), (float) edge.a.z())
                .setColor(0.0F, 1.0F, 0.0F, 1.0F)
                .setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
            consumer.addVertex(matrix, (float) edge.b.x(), (float) edge.b.y(), (float) edge.b.z())
                .setColor(0.0F, 1.0F, 0.0F, 1.0F)
                .setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
        }
    }

    public static boolean isSpacePortValid(final Level level, final LaunchPadConfiguration launchPadConfiguration) {
        //TODO: send error messages to client
        final Set<BlockPos> rocketPositions = new HashSet<>();

        // Check if all correct blocks are placed
        final List<PreviewInfo> previewInfos = calculateSpacePort(level, launchPadConfiguration, true);
        for (final PreviewInfo previewInfo : previewInfos) {
            if (previewInfo.expectedBlock().isPresent()) {
                if (!level.getBlockState(previewInfo.pos()).is(previewInfo.expectedBlock().get())) {
                    return false;
                }
            } else {
                rocketPositions.add(previewInfo.pos());
            }
        }

        // Check if all rocket blocks are connected
        if (!rocketPositions.isEmpty()) {
            final Set<BlockPos> visited = new HashSet<>();
            final Queue<BlockPos> queue = new LinkedList<>();
            final BlockPos start = rocketPositions.iterator().next();
            queue.add(start);
            visited.add(start);

            while (!queue.isEmpty()) {
                final BlockPos current = queue.poll();
                for (final Direction direction : Direction.values()) {
                    final BlockPos neighbor = current.relative(direction);
                    if (rocketPositions.contains(neighbor) && visited.add(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }

            return visited.size() == rocketPositions.size();
        }

        return true;
    }

    public static List<PreviewInfo> calculateSpacePort(final Level level, final LaunchPadConfiguration launchPadConfiguration, final boolean includeRocketArea) {
        final List<PreviewInfo> previewBlocks = new ArrayList<>();

        final BlockPos mainPos = launchPadConfiguration.mainPos();
        final int width = launchPadConfiguration.width();
        final int height = launchPadConfiguration.height();
        final Direction facing = launchPadConfiguration.facing();

        int minOffset = -(width - 1) / 2;
        int maxOffset = width / 2;
        final Set<BlockPos> allPos = new HashSet<>();
        for (int dx = minOffset; dx <= maxOffset; dx++) {
            for (int dz = 0; dz < width; dz++) {
                for (int dy = 0; dy < level.getMaxY(); dy++) {
                    final BlockPos rotatedPos = rotateOffset(mainPos.above(dy), facing.getOpposite(), dx, dz);

                    // Rocket area
                    if (dx >= minOffset + 4 && dx <= maxOffset - 4
                        && dz >= 2 && dz <= width - 7
                        && dy <= height - 1) {
                        if (includeRocketArea) {
                            previewBlocks.add(new PreviewInfo(rotatedPos, Optional.empty()));
                        }
                        continue;
                    } else if (dx >= minOffset + 3 && dx <= maxOffset - 3
                        && dz >= 1 && dz <= width - 6
                        && dy == height) {
                        if (includeRocketArea) {
                            previewBlocks.add(new PreviewInfo(rotatedPos, Optional.empty()));
                        }
                        continue;
                    }

                    allPos.add(rotatedPos);
                }
            }
        }

        // Ground
        final Block rocketBase = ModBlocks.ROCKET_BASE.get();
        for (int dx = minOffset; dx <= maxOffset; dx++) {
            for (int dz = 0; dz < width; dz++) {
                final BlockPos targetPos = rotateOffset(mainPos.below(1), facing.getOpposite(), dx, dz);
                allPos.remove(targetPos);
                previewBlocks.add(new PreviewInfo(targetPos, Optional.of(rocketBase)));
            }
        }

        ///TODO: there are many duplicate for loops/functions with {@link RocketControllerBlockEntity#serverTick(Level, BlockPos, BlockState, RocketControllerBlockEntity)}

        // Tower rod
        final Block scaffolding = ModBlocks.METAL_SCAFFOLDING.get();
        final int towerWidth = width - 8;
        minOffset = -(towerWidth - 1) / 2;
        maxOffset = towerWidth / 2;
        for (int dx = minOffset; dx <= maxOffset; dx++) {
            for (int dy = 0; dy < height; dy++) {
                final BlockPos targetPos = rotateOffset(mainPos.above(dy), facing.getOpposite(), dx, 0);
                allPos.remove(targetPos);
                previewBlocks.add(new PreviewInfo(targetPos, Optional.of(scaffolding)));
            }
        }

        // Tower chopsticks
        final int chopstickWidth = width - 5;
        for (int dz = 0; dz < chopstickWidth; dz++) {
            final BlockPos targetPos1 = rotateOffset(mainPos.above(height - 2), facing.getOpposite(), -minOffset + 1, dz);
            final BlockPos targetPos2 = rotateOffset(mainPos.above(height - 2), facing.getOpposite(), minOffset - 1, dz);

            allPos.remove(targetPos1);
            allPos.remove(targetPos2);
            previewBlocks.add(new PreviewInfo(targetPos1, Optional.of(scaffolding)));
            previewBlocks.add(new PreviewInfo(targetPos2, Optional.of(scaffolding)));

            if (dz != 0) {
                final BlockPos targetPos3 = rotateOffset(mainPos.above(height - 2), facing.getOpposite(), -minOffset + 2, dz);
                final BlockPos targetPos4 = rotateOffset(mainPos.above(height - 2), facing.getOpposite(), minOffset - 2, dz);

                allPos.remove(targetPos3);
                allPos.remove(targetPos4);

                previewBlocks.add(new PreviewInfo(targetPos3, Optional.of(scaffolding)));
                previewBlocks.add(new PreviewInfo(targetPos4, Optional.of(scaffolding)));
            }
        }

        for (final BlockPos checkPos : allPos) {
            // TODO: because I removed this check previewList can now be immensely big (because the pos stretch to the sky) so definitely improve the performance somehow
            //if (!level.getBlockState(checkPos).isAir()) {
            previewBlocks.add(new PreviewInfo(checkPos, Optional.of(Blocks.AIR)));
            //}
        }

        return previewBlocks;
    }
}
