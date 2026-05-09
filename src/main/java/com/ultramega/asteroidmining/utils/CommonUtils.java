package com.ultramega.asteroidmining.utils;

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

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

public final class CommonUtils {
    public static final StreamCodec<ByteBuf, StructureTemplate.StructureBlockInfo> STRUCTURE_BLOCK_INFO_STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, StructureTemplate.StructureBlockInfo::pos,
        ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), StructureTemplate.StructureBlockInfo::state,
        ByteBufCodecs.COMPOUND_TAG, StructureTemplate.StructureBlockInfo::nbt,
        StructureTemplate.StructureBlockInfo::new
    );
    public static final StreamCodec<ByteBuf, List<StructureTemplate.StructureBlockInfo>> STRUCTURE_BLOCK_INFO_STREAM_CODEC_LIST =
        CommonUtils.STRUCTURE_BLOCK_INFO_STREAM_CODEC.apply(ByteBufCodecs.list());

    public static final Codec<StructureTemplate.StructureBlockInfo> STRUCTURE_BLOCK_INFO_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BlockPos.CODEC.fieldOf("blockPos").forGetter(StructureTemplate.StructureBlockInfo::pos),
        BlockState.CODEC.fieldOf("state").forGetter(StructureTemplate.StructureBlockInfo::state),
        CompoundTag.CODEC.optionalFieldOf("nbt").forGetter(info -> Optional.ofNullable(info.nbt()))
    ).apply(instance, (pos, state, nbt) ->
        new StructureTemplate.StructureBlockInfo(pos, state, nbt.orElse(null))));

    public static final Codec<List<StructureTemplate.StructureBlockInfo>> STRUCTURE_BLOCK_INFO_LIST_CODEC = STRUCTURE_BLOCK_INFO_CODEC.listOf();

    public static final Codec<List<BlockPos>> BLOCK_POS_LIST = Codec.list(BlockPos.CODEC);

    public static final long BUCKET_AMOUNT = 1000;

    private static final DecimalFormat FORMATTER_WITH_UNITS = new DecimalFormat(
        "####0.#",
        DecimalFormatSymbols.getInstance(Locale.US)
    );

    static {
        FORMATTER_WITH_UNITS.setRoundingMode(RoundingMode.FLOOR);
    }

    private CommonUtils() {
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
        return formatWithUnits(qty / CommonUtils.BUCKET_AMOUNT) + "B";
    }

    public static String formatPlanetSize(final int size) {
        return "≈" + size * 5000 + " km";
    }

    public static String formatAstronomicalUnit(final float au) {
        return String.format("%.3f", au * 0.01) + " AU";
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(final BlockEntityType<A> serverType,
                                                                                                         final BlockEntityType<E> clientType,
                                                                                                         final BlockEntityTicker<? super E> ticker) {
        return clientType == serverType ? (BlockEntityTicker<A>) ticker : null;
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

    public static boolean isSpacePortValid(final Level level, final LaunchPadConfiguration launchPadConfiguration) {
        //TODO: send error messages to client
        final Set<BlockPos> rocketPositions = new HashSet<>();

        // Check if all correct blocks are placed
        final List<PreviewInfo> previewInfos = calculateSpacePort(level, launchPadConfiguration, true);
        for (final PreviewInfo previewInfo : previewInfos) {
            if (previewInfo.expectedBlock().isPresent()) {
                if (!level.getBlockState(previewInfo.pos()).is(previewInfo.expectedBlock().get())) {
                    // tmp fix
                    if (previewInfo.expectedBlock().get().defaultBlockState().isAir() && level.getBlockState(previewInfo.pos()).isAir()) {
                        continue;
                    }
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

    public static BlockPos getMinCorner(final List<BlockPos> positions) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;

        for (final BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
        }

        return new BlockPos(minX, minY, minZ);
    }

    public static List<BlockPos> toLocalPositions(final List<BlockPos> positions, final BlockPos origin) {
        final List<BlockPos> result = new ArrayList<>(positions.size());
        for (final BlockPos pos : positions) {
            result.add(pos.subtract(origin));
        }
        return result;
    }
}
