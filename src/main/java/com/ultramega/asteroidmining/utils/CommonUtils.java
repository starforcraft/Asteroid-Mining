package com.ultramega.asteroidmining.utils;

import com.ultramega.asteroidmining.blocks.RocketEngineBlock;
import com.ultramega.asteroidmining.blocks.StorageTankBlock;
import com.ultramega.asteroidmining.registry.ModBlocks;
import com.ultramega.asteroidmining.storage.LaunchPadConfiguration;
import com.ultramega.asteroidmining.storage.NetworkConfiguration;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

    public static boolean isSpacePortValid(final Level level, final NetworkConfiguration configuration) {
        return analyzeSpacePort(level, configuration).valid();
    }

    public static List<LaunchError> getSpacePortErrors(final Level level, final NetworkConfiguration configuration) {
        return analyzeSpacePort(level, configuration).errors();
    }

    public static SpacePortAnalysis analyzeSpacePort(final Level level, final NetworkConfiguration configuration) {
        final List<PreviewInfo> previewInfos = calculateSpacePort(level, configuration.launchPadConfiguration(), true);
        final RocketBlockPositions rocketBlocks = getRocketBlockPositions(level, configuration.launchPadConfiguration());
        final List<BlockPos> rocketPositions = rocketBlocks.positions();
        final ChopstickPositions chopstickPositions = getChopstickPositions(configuration.launchPadConfiguration());

        final Set<LaunchError> errors = new LinkedHashSet<>();

        for (final PreviewInfo previewInfo : previewInfos) {
            if (previewInfo.expectedBlock().isEmpty()) {
                continue;
            }

            final Block expectedBlock = previewInfo.expectedBlock().get();
            final BlockState expectedState = expectedBlock.defaultBlockState();
            final BlockState actualState = level.getBlockState(previewInfo.pos());

            if (actualState.is(expectedBlock) || expectedBlock.defaultBlockState().isAir() && actualState.isAir()) {
                continue;
            }

            errors.add(LaunchError.at(
                expectedState.isAir() ? LaunchError.LaunchErrors.BLOCKS_ABOVE_LAUNCH_PAD : LaunchError.LaunchErrors.WRONG_BLOCK,
                previewInfo.pos(),
                expectedState,
                actualState
            ));
        }

        final List<BlockPos> disconnectedRocketPositions = getDisconnectedPositions(rocketPositions);
        if (!disconnectedRocketPositions.isEmpty()) {
            errors.add(LaunchError.positions(LaunchError.LaunchErrors.ROCKET_HAS_AIR_GAP, disconnectedRocketPositions));
        }

        addUnmovableBlockErrors(level, rocketPositions, errors);
        addUnmovableBlockErrors(level, chopstickPositions.chopstick1Positions(), errors);
        addUnmovableBlockErrors(level, chopstickPositions.chopstick2Positions(), errors);

        if (!rocketBlocks.hasEngine()) {
            errors.add(LaunchError.simple(LaunchError.LaunchErrors.MISSING_ENGINE));
        }

        if (!rocketBlocks.hasItemStorageOrFluidTank()) {
            errors.add(LaunchError.simple(LaunchError.LaunchErrors.MISSING_ITEM_STORAGE_OR_FLUID_TANK));
        }

        if (configuration.moduleProperties().selectedAsteroid().isEmpty()) {
            errors.add(LaunchError.simple(LaunchError.LaunchErrors.NO_DESTINATION_SELECTED));
        }

        return new SpacePortAnalysis(
            previewInfos,
            rocketPositions,
            chopstickPositions.chopstick1Positions(),
            chopstickPositions.chopstick2Positions(),
            chopstickPositions.pivotWorldPos(),
            List.copyOf(errors)
        );
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
            previewBlocks.add(new PreviewInfo(checkPos, Optional.of(Blocks.AIR)));
        }

        return previewBlocks;
    }

    public static RocketBlockPositions getRocketBlockPositions(final Level level, final LaunchPadConfiguration launchPadConfiguration) {
        final List<BlockPos> rocketPositions = new ArrayList<>();
        boolean hasEngine = false;
        boolean hasItemStorageOrFluidTank = false;

        final BlockPos mainPos = launchPadConfiguration.mainPos();
        final int width = launchPadConfiguration.width();
        final int height = launchPadConfiguration.height();
        final Direction facing = launchPadConfiguration.facing();

        final int minOffset = -(width - 1) / 2;
        final int maxOffset = width / 2;

        for (int dx = minOffset + 4; dx <= maxOffset - 4; dx++) {
            for (int dz = 2; dz <= width - 7; dz++) {
                for (int dy = 0; dy < height + 1; dy++) {
                    final BlockPos rotatedPos = rotateOffset(mainPos.above(dy), facing.getOpposite(), dx, dz);
                    final BlockState state = level.getBlockState(rotatedPos);
                    if (!state.isAir()) {
                        rocketPositions.add(rotatedPos);

                        if (state.getBlock() instanceof RocketEngineBlock) {
                            hasEngine = true;
                        } else if (state.getBlock() instanceof StorageTankBlock) {
                            hasItemStorageOrFluidTank = true;
                        }
                    }
                }
            }
        }

        return new RocketBlockPositions(rocketPositions, hasEngine, hasItemStorageOrFluidTank);
    }

    public static ChopstickPositions getChopstickPositions(final LaunchPadConfiguration launchPadConfiguration) {
        final BlockPos mainPos = launchPadConfiguration.mainPos();
        final int width = launchPadConfiguration.width();
        final int height = launchPadConfiguration.height();
        final Direction facing = launchPadConfiguration.facing();

        final int towerWidth = width - 8;
        final int minOffset = -(towerWidth - 1) / 2;
        final int chopstickWidth = width - 5;

        final List<BlockPos> chopstick1Positions = new ArrayList<>();
        final List<BlockPos> chopstick2Positions = new ArrayList<>();

        for (int dz = 0; dz < chopstickWidth; dz++) {
            final BlockPos targetPos1 = rotateOffset(mainPos.above(height - 2), facing.getOpposite(), -minOffset + 1, dz);
            final BlockPos targetPos2 = rotateOffset(mainPos.above(height - 2), facing.getOpposite(), minOffset - 1, dz);

            chopstick1Positions.add(targetPos1);
            chopstick2Positions.add(targetPos2);

            if (dz != 0) {
                final BlockPos targetPos3 = rotateOffset(mainPos.above(height - 2), facing.getOpposite(), -minOffset + 2, dz);
                final BlockPos targetPos4 = rotateOffset(mainPos.above(height - 2), facing.getOpposite(), minOffset - 2, dz);

                chopstick1Positions.add(targetPos3);
                chopstick2Positions.add(targetPos4);
            }
        }

        return new ChopstickPositions(chopstick1Positions, chopstick2Positions, mainPos.above(height - 2));
    }

    private static List<BlockPos> getDisconnectedPositions(final List<BlockPos> positions) {
        if (positions.isEmpty()) {
            return List.of();
        }

        final Set<BlockPos> allPositions = new HashSet<>(positions);
        final Set<BlockPos> visited = new HashSet<>();
        final Queue<BlockPos> queue = new LinkedList<>();

        final BlockPos start = allPositions.iterator().next();
        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            final BlockPos current = queue.poll();

            for (final Direction direction : Direction.values()) {
                final BlockPos neighbor = current.relative(direction);

                if (allPositions.contains(neighbor) && visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        if (visited.size() == allPositions.size()) {
            return List.of();
        }

        final List<BlockPos> disconnectedPositions = new ArrayList<>();
        for (final BlockPos pos : positions) {
            if (!visited.contains(pos)) {
                disconnectedPositions.add(pos);
            }
        }

        return disconnectedPositions;
    }

    private static void addUnmovableBlockErrors(final Level level, final List<BlockPos> positions, final Set<LaunchError> errors) {
        for (final BlockPos pos : positions) {
            final BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.getDestroySpeed(level, pos) <= 0) {
                errors.add(LaunchError.at(
                    LaunchError.LaunchErrors.UNMOVABLE_BLOCK,
                    pos,
                    null,
                    state
                ));
                return;
            }
        }
    }

    public record SpacePortAnalysis(List<PreviewInfo> previewInfos,
                                    List<BlockPos> rocketPositions,
                                    List<BlockPos> chopstick1Positions,
                                    List<BlockPos> chopstick2Positions,
                                    BlockPos chopstickPivotWorldPos,
                                    List<LaunchError> errors) {
        public boolean valid() {
            return this.errors.isEmpty();
        }

        public List<LaunchError.LaunchErrors> errorTypes() {
            return this.errors.stream()
                .map(LaunchError::type)
                .distinct()
                .toList();
        }
    }

    public record RocketBlockPositions(List<BlockPos> positions,
                                       boolean hasEngine,
                                       boolean hasItemStorageOrFluidTank) {
    }

    public record ChopstickPositions(List<BlockPos> chopstick1Positions,
                                     List<BlockPos> chopstick2Positions,
                                     BlockPos pivotWorldPos) {
    }
}
