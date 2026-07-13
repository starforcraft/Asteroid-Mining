package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.asteroids.AsteroidConfig;
import com.ultramega.asteroidmining.events.AsteroidReloadListener;
import com.ultramega.asteroidmining.gui.renderer.AsteroidBatchRenderState;
import com.ultramega.asteroidmining.gui.renderer.OrbitRenderState;
import com.ultramega.asteroidmining.gui.widgets.AsteroidSearchBox;
import com.ultramega.asteroidmining.gui.widgets.ImageButton;
import com.ultramega.asteroidmining.gui.widgets.ImagesButton;
import com.ultramega.asteroidmining.utils.ClientUtils;
import com.ultramega.asteroidmining.utils.CommonUtils;
import com.ultramega.asteroidmining.utils.TextColors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

/**
 * Displays the asteroid data set without iterating over every asteroid every frame.
 *
 * <p>The render index narrows the data set to orbit-radius ranges intersecting a padded
 * camera region. If that range is still too large, candidates are sampled uniformly and
 * deterministically. The final visible set is reduced with a screen-space grid only when
 * it exceeds the sprite budget. Consequently, zoomed-in views still render every relevant
 * asteroid while overview views have a hard and predictable CPU/GPU cost.</p>
 */
public class SolarSystemViewScreen extends Screen {
    private static final Identifier SELECTED = AsteroidMining.makeId("selected");
    private static final Identifier SEARCH = AsteroidMining.makeId("search");
    private static final Identifier UP = AsteroidMining.makeId("up");
    private static final Identifier DOWN = AsteroidMining.makeId("down");

    private static final float MIN_ZOOM = 0.05F;
    private static final float MAX_ZOOM = 20.0F;
    private static final double MIN_MAX_X = 5000.0D;
    private static final double MIN_MAX_Y = 3000.0D;

    /** Maximum number of asteroid sprites submitted in one frame. */
    private static final int MAX_RENDERED_ASTEROIDS = 2048 * 2;
    /** Maximum number of moving objects whose positions are evaluated in one frame. */
    private static final int MAX_RENDER_CANDIDATES = 12_000;
    private static final int MAX_RENDERED_ORBITS = 32;
    private static final int MAX_ROOT_CANDIDATES = 2048;
    private static final int MAX_POSITION_STATES = MAX_RENDER_CANDIDATES * 6;

    private static final long CACHE_MAX_AGE_TICKS = 20L;
    private static final float CACHE_ZOOM_RATIO = 1.35F;
    private static final double MIN_CACHE_PADDING_PIXELS = 320.0D;
    private static final double VIEW_PADDING_PIXELS = 96.0D;
    private static final double ORBIT_QUERY_WORLD_PADDING = 256.0D;

    private static final int ORBIT_SEGMENTS_HIGH_DETAIL = 96;
    private static final int ORBIT_SEGMENTS_LOW_DETAIL = 24;
    private static final float[] UNIT_ORBIT_HIGH_DETAIL = createUnitOrbit(ORBIT_SEGMENTS_HIGH_DETAIL);
    private static final float[] UNIT_ORBIT_LOW_DETAIL = createUnitOrbit(ORBIT_SEGMENTS_LOW_DETAIL);

    private final Consumer<@Nullable Identifier> selectAsteroid;
    private final ObservatoryScreen parent;

    private final List<RenderEntry> renderCache = new ArrayList<>(MAX_RENDER_CANDIDATES);
    private final List<RenderEntry> renderEntryPool = new ArrayList<>(MAX_RENDER_CANDIDATES);
    private final List<RenderEntry> visibleEntries = new ArrayList<>(MAX_RENDER_CANDIDATES);
    private final List<RenderEntry> frameRenderEntries = new ArrayList<>(MAX_RENDERED_ASTEROIDS);
    private final List<GroupSlice> groupSlices = new ArrayList<>();
    private final List<GroupSlice> groupSlicePool = new ArrayList<>();
    private final IdentityHashMap<AsteroidConfig, PositionState> positionStates = new IdentityHashMap<>();
    private final IdentityHashMap<AsteroidConfig, Boolean> renderCandidateSet = new IdentityHashMap<>();

    @Nullable
    private RenderEntry[] lodCells;
    private long positionFrame;
    private double renderTimeTicks;
    private long elapsedTicks;
    private long simulationEpochTick;

    private int detailWidth;
    private int detailHeight;
    private int centerX;
    private int centerY;
    private boolean isDragging;
    private double dragX;
    private double dragY;
    private float zoom = 1.0F;
    private boolean followAsteroid;
    private boolean zoomOntoAsteroid;
    private boolean updateSelectedAsteroid = true;

    @Nullable
    private AsteroidConfig selectedAsteroid;
    @Nullable
    private AsteroidConfig hoveredAsteroid;

    private boolean displayOrbitDetails;
    private AsteroidSearchBox searchBox;
    private Button selectButton;
    private Button displayOrbitDetailsButton;
    private boolean clickedOrbitDetailsButton;
    private boolean cancelMouseRelease;

    private AsteroidReloadListener.@Nullable RenderIndex observedRenderIndex;
    @Nullable
    private WorldView cachedQueryView;
    @Nullable
    private Identifier cachedSelectedAsteroidId;
    private long lastCacheRebuildTick = Long.MIN_VALUE;
    private float cachedZoom = Float.NaN;
    private int cachedWidth = -1;
    private int cachedHeight = -1;

    /**
     * Cleared at the beginning of each asteroid render pass.
     *
     * This keeps sprite resolution safe across resource reloads while ensuring
     * that each unique asteroid texture is resolved only once per frame.
     */
    private final Map<Identifier, CachedGuiSprite> frameGuiSpriteCache = new HashMap<>();

    public SolarSystemViewScreen(
        @Nullable final AsteroidConfig selectedAsteroid,
        final Consumer<@Nullable Identifier> selectAsteroid,
        final ObservatoryScreen parent
    ) {
        super(GameNarrator.NO_TITLE);
        this.selectedAsteroid = selectedAsteroid;
        this.selectAsteroid = selectAsteroid;
        this.parent = parent;
        this.cancelMouseRelease = true;

        if (selectedAsteroid != null) {
            this.followAsteroid = true;
            this.zoomOntoAsteroid = true;
        }
    }

    @Override
    protected void init() {
        super.init();
        this.centerX = this.width / 2;
        this.centerY = this.height / 2;

        this.addRenderableWidget(new ImagesButton(3, 5, 16, 16, SEARCH, button -> {
            this.searchBox.setVisible(!this.searchBox.isVisible());
            this.updateSelectedAsteroid = false;
        }));

        this.searchBox = new AsteroidSearchBox(
            this.font,
            23,
            5,
            64,
            16,
            128,
            true,
            Component.translatable("gui.asteroidmining.asteroid.search"),
            name -> {
                this.selectedAsteroid = AsteroidReloadListener.INSTANCE.findAsteroidByName(name);
                this.followAsteroid = this.selectedAsteroid != null;
                this.zoomOntoAsteroid = this.selectedAsteroid != null;
                this.updateSelectedAsteroid = false;
                this.invalidateRenderCache();
            }
        );
        this.searchBox.setVisible(false);
        this.addRenderableWidget(this.searchBox);

        this.selectButton = Button.builder(
            Component.translatable("gui.asteroidmining.select"),
            button -> this.onClose()
        ).bounds(0, 0, 48, 16).build();
        this.selectButton.visible = false;
        this.addRenderableWidget(this.selectButton);

        this.displayOrbitDetailsButton = new ImageButton(0, 0, 16, 16, DOWN, button -> {
            this.displayOrbitDetails = !this.displayOrbitDetails;
            ((ImageButton) button).setImage(this.displayOrbitDetails ? UP : DOWN);
            this.clickedOrbitDetailsButton = true;
        });
        this.displayOrbitDetailsButton.visible = false;
        this.addRenderableWidget(this.displayOrbitDetailsButton);

        this.invalidateRenderCache();
    }

    @Override
    public void tick() {
        super.tick();
        this.elapsedTicks++;
    }

    @Override
    public void extractRenderState(
        final GuiGraphicsExtractor graphics,
        final int mouseX,
        final int mouseY,
        final float partialTicks
    ) {
        this.beginRenderFrame(partialTicks);
        this.renderAsteroids(graphics, mouseX, mouseY);
        this.renderSelectedAsteroidDetails(graphics, mouseX, mouseY);

        for (final Renderable renderable : this.renderables) {
            renderable.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        }

        this.extractTooltip(graphics, mouseX, mouseY);
    }

    private void beginRenderFrame(final float partialTicks) {
        this.positionFrame++;
        final AsteroidReloadListener listener = AsteroidReloadListener.INSTANCE;
        final AsteroidReloadListener.RenderIndex index = listener.getRenderIndex();
        if (index != this.observedRenderIndex) {
            this.observedRenderIndex = index;
            this.positionStates.clear();
            this.simulationEpochTick = this.elapsedTicks;
            this.invalidateRenderCache();

            if (this.selectedAsteroid != null) {
                this.selectedAsteroid = listener.getData().get(this.selectedAsteroid.getId());
                if (this.selectedAsteroid == null) {
                    this.followAsteroid = false;
                    this.zoomOntoAsteroid = false;
                }
            }
        }

        this.renderTimeTicks = (this.elapsedTicks - this.simulationEpochTick) + partialTicks;

        if (this.positionStates.size() > MAX_POSITION_STATES) {
            this.positionStates.clear();
            this.positionFrame++;
        }

        this.updateFollowCamera();
    }

    private void updateFollowCamera() {
        if (!this.followAsteroid || this.selectedAsteroid == null) {
            return;
        }

        if (this.zoomOntoAsteroid) {
            this.zoom = Mth.clamp(
                MAX_ZOOM - (this.selectedAsteroid.getDiameter() / 2.0F),
                MIN_ZOOM,
                MAX_ZOOM
            );
            this.zoomOntoAsteroid = false;
        }

        final PositionState position = this.getPosition(this.selectedAsteroid);
        this.dragX = -position.x;
        this.dragY = -position.y;
    }

    private void renderSelectedAsteroidDetails(
        final GuiGraphicsExtractor graphics,
        final int mouseX,
        final int mouseY
    ) {
        if (this.selectedAsteroid == null) {
            this.selectButton.visible = false;
            this.displayOrbitDetailsButton.visible = false;
            return;
        }

        final String asteroidName = this.selectedAsteroid.getName();
        final Component sizeLabel = Component.translatable("gui.asteroidmining.observatory.diameter")
            .withStyle(ChatFormatting.AQUA)
            .append(Component.literal(CommonUtils.formatPlanetSize(this.selectedAsteroid.getDiameter()))
                .withStyle(ChatFormatting.WHITE));
        final boolean hasComposition = !this.selectedAsteroid.getComposition().isEmpty();
        final Component compositionLabel = Component.translatable("gui.asteroidmining.observatory.composition")
            .withStyle(ChatFormatting.AQUA);
        final boolean hasOrbitDetails = !this.selectedAsteroid.getCentralBodyName().isBlank();
        final String orbitDetailsLabel = Component.translatable(
            "gui.asteroidmining.observatory.display_orbit_details"
        ).getString();
        final Component centralBodyLabel = Component.translatable("gui.asteroidmining.observatory.central_body")
            .withStyle(ChatFormatting.AQUA)
            .append(Component.literal(this.selectedAsteroid.getCentralBodyName()).withStyle(ChatFormatting.WHITE));
        final Component semiMajorAxisLabel = Component.translatable("gui.asteroidmining.observatory.semi_mayor_axis")
            .withStyle(ChatFormatting.AQUA)
            .append(Component.literal(CommonUtils.formatAstronomicalUnit(this.selectedAsteroid.getSemiMajorAxis()))
                .withStyle(ChatFormatting.WHITE));
        final Component semiMinorAxisLabel = Component.translatable("gui.asteroidmining.observatory.semi_minor_axis")
            .withStyle(ChatFormatting.AQUA)
            .append(Component.literal(CommonUtils.formatAstronomicalUnit(this.selectedAsteroid.getSemiMinorAxis()))
                .withStyle(ChatFormatting.WHITE));

        int maxTextWidth = Math.max(this.font.width(asteroidName), this.font.width(sizeLabel));
        if (hasComposition) {
            maxTextWidth = Math.max(maxTextWidth, this.font.width(compositionLabel));
        }
        maxTextWidth = Math.max(maxTextWidth, this.font.width(orbitDetailsLabel) + 8);
        if (this.displayOrbitDetails) {
            maxTextWidth = Math.max(maxTextWidth, this.font.width(centralBodyLabel));
            maxTextWidth = Math.max(maxTextWidth, this.font.width(semiMajorAxisLabel));
            maxTextWidth = Math.max(maxTextWidth, this.font.width(semiMinorAxisLabel));
        }

        this.detailWidth = Math.min(this.width, Math.max(100, maxTextWidth + 20));

        final int baseTextHeight = 55;
        final int compositionTextHeight = hasComposition ? 25 : 0;
        final int compositionStacksHeight = hasComposition
            ? ((this.selectedAsteroid.getComposition().size() - 1) / 5) * 18
            : 0;
        final int orbitDetailsButtonExtraHeight = hasOrbitDetails ? 20 : 0;
        final int orbitDetailsExtraHeight = this.displayOrbitDetails ? 35 : 0;
        this.detailHeight = baseTextHeight
            + compositionTextHeight
            + compositionStacksHeight
            + orbitDetailsButtonExtraHeight
            + orbitDetailsExtraHeight;

        final int detailX = this.width - this.detailWidth;
        this.selectButton.setX(detailX + (this.detailWidth - 48) / 2);
        this.selectButton.setY(this.detailHeight - 20);
        this.selectButton.visible = true;

        this.displayOrbitDetailsButton.setX(detailX + 1);
        this.displayOrbitDetailsButton.setY(this.detailHeight - 38 - orbitDetailsExtraHeight);
        this.displayOrbitDetailsButton.visible = hasOrbitDetails;

        final Matrix3x2fStack poseStack = graphics.pose();
        poseStack.pushMatrix();
        poseStack.translate(detailX, 0);
        graphics.fill(0, 0, this.detailWidth, this.detailHeight, 0xFF525151);

        if (hasOrbitDetails) {
            graphics.text(this.font, orbitDetailsLabel, 20, this.detailHeight - 34 - orbitDetailsExtraHeight, -1);
        }

        int y = 5;
        graphics.text(
            this.font,
            asteroidName,
            (this.detailWidth - this.font.width(asteroidName)) / 2,
            y,
            TextColors.GOLD.getHexCode()
        );
        y += 15;
        graphics.text(this.font, sizeLabel, 5, y, -1);
        y += 10;

        if (hasComposition) {
            graphics.text(this.font, compositionLabel, 5, y, -1);
            ClientUtils.renderResourcesWithSlot(
                graphics,
                this.font,
                mouseX,
                mouseY,
                5,
                41,
                5,
                detailX,
                0,
                this.selectedAsteroid.getComposition()
            );
            y += compositionTextHeight + compositionStacksHeight;
        }

        if (this.displayOrbitDetails) {
            y += 27;
            graphics.text(this.font, centralBodyLabel, 5, y, -1);
            y += 10;
            graphics.text(this.font, semiMajorAxisLabel, 5, y, -1);
            y += 10;
            graphics.text(this.font, semiMinorAxisLabel, 5, y, -1);
        }

        poseStack.popMatrix();
    }

    private void renderAsteroids(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        final AsteroidReloadListener.RenderIndex index = this.observedRenderIndex == null
            ? AsteroidReloadListener.RenderIndex.EMPTY
            : this.observedRenderIndex;
        final WorldView visibleView = this.createWorldView(0.0D);

        if (this.shouldRebuildRenderCache(index, visibleView)) {
            this.rebuildRenderCache(index);
        }

        this.collectVisibleEntries();
        this.applyScreenSpaceLod();
        this.renderOrbits(graphics);
        this.renderAsteroidSprites(graphics, mouseX, mouseY);
    }

    private boolean shouldRebuildRenderCache(
        final AsteroidReloadListener.RenderIndex index,
        final WorldView visibleView
    ) {
        if (index != this.observedRenderIndex || this.cachedQueryView == null) {
            return true;
        }
        if (this.cachedWidth != this.width || this.cachedHeight != this.height) {
            return true;
        }
        if (!this.cachedQueryView.contains(visibleView)) {
            return true;
        }
        if (this.elapsedTicks - this.lastCacheRebuildTick >= CACHE_MAX_AGE_TICKS) {
            return true;
        }

        final float zoomRatio = this.zoom / this.cachedZoom;
        if (!Float.isFinite(zoomRatio) || zoomRatio > CACHE_ZOOM_RATIO || zoomRatio < 1.0F / CACHE_ZOOM_RATIO) {
            return true;
        }

        final Identifier selectedId = this.selectedAsteroid == null ? null : this.selectedAsteroid.getId();
        return selectedId == null
            ? this.cachedSelectedAsteroidId != null
            : !selectedId.equals(this.cachedSelectedAsteroidId);
    }

    private void rebuildRenderCache(final AsteroidReloadListener.RenderIndex index) {
        this.recycleRenderCache();
        this.recycleGroupSlices();

        final double cachePaddingPixels = Math.max(
            MIN_CACHE_PADDING_PIXELS,
            Math.max(this.width, this.height) * 0.55D
        );
        final WorldView queryView = this.createWorldView(cachePaddingPixels);
        final Identifier selectedId = this.selectedAsteroid == null ? null : this.selectedAsteroid.getId();

        if (this.selectedAsteroid != null) {
            this.addRenderCandidate(this.selectedAsteroid, true, 0L);
        }

        this.addRootCandidates(index.getRootAsteroids(), selectedId, queryView);
        this.createGroupSlices(index, queryView);
        this.addGroupCandidates(selectedId);

        this.renderCache.sort((left, right) -> {
            if (left.selected != right.selected) {
                return left.selected ? -1 : 1;
            }
            return Long.compareUnsigned(left.stableHash, right.stableHash);
        });

        this.cachedQueryView = queryView;
        this.cachedSelectedAsteroidId = selectedId;
        this.cachedZoom = this.zoom;
        this.cachedWidth = this.width;
        this.cachedHeight = this.height;
        this.lastCacheRebuildTick = this.elapsedTicks;
    }


    private void addRootCandidates(
        final List<AsteroidConfig> roots,
        @Nullable final Identifier selectedId,
        final WorldView queryView
    ) {
        final int remainingBudget = MAX_RENDER_CANDIDATES - this.renderCache.size();
        final int sampleCount = Math.min(roots.size(), Math.min(MAX_ROOT_CANDIDATES, remainingBudget));
        if (sampleCount <= 0) {
            return;
        }

        final double stride = roots.size() / (double) sampleCount;
        int previousIndex = -1;
        for (int sample = 0; sample < sampleCount; sample++) {
            final int index = Math.min(roots.size() - 1, (int) Math.floor((sample + 0.5D) * stride));
            if (index == previousIndex) {
                continue;
            }
            previousIndex = index;

            final AsteroidConfig asteroid = roots.get(index);
            if (selectedId != null && selectedId.equals(asteroid.getId())) {
                continue;
            }

            final PositionState position = this.getPosition(asteroid);
            final double radius = Math.max(1.0D, asteroid.getDiameter() * 0.5D);
            if (queryView.intersectsCircle(position.x, position.y, radius)) {
                this.addRenderCandidate(asteroid, false, stableHash(asteroid.getId()));
            }
        }
    }

    private void createGroupSlices(
        final AsteroidReloadListener.RenderIndex index,
        final WorldView queryView
    ) {
        for (final AsteroidReloadListener.OrbitGroup group : index.getOrbitGroups()) {
            final PositionState center = this.getPosition(group.getCentralBody());
            final double minDistance = distanceFromPointToRect(center.x, center.y, queryView);
            final double maxDistance = maxDistanceFromPointToRect(center.x, center.y, queryView);
            final double padding = ORBIT_QUERY_WORLD_PADDING + (VIEW_PADDING_PIXELS / this.zoom);

            if (group.getMaxOrbitRadius() < minDistance - padding
                || group.getMinOrbitRadius() > maxDistance + padding) {
                continue;
            }

            final int start = group.lowerBound(Math.max(0.0D, minDistance - padding));
            final int end = group.upperBound(maxDistance + padding);
            if (end <= start) {
                continue;
            }

            final AsteroidConfig centralBody = group.getCentralBody();
            final double centralRadius = Math.max(1.0D, centralBody.getDiameter() * 0.5D);
            if (queryView.intersectsCircle(center.x, center.y, centralRadius)) {
                this.addRenderCandidate(centralBody, false, stableHash(centralBody.getId()));
            }

            final GroupSlice slice = this.obtainGroupSlice();
            slice.group = group;
            slice.start = start;
            slice.end = end;
            this.groupSlices.add(slice);
        }
    }

    private void addGroupCandidates(@Nullable final Identifier selectedId) {
        int remainingBudget = MAX_RENDER_CANDIDATES - this.renderCache.size();
        long remainingCount = 0L;
        for (final GroupSlice slice : this.groupSlices) {
            remainingCount += slice.end - slice.start;
        }

        int remainingGroups = this.groupSlices.size();
        for (final GroupSlice slice : this.groupSlices) {
            if (remainingBudget <= 0 || remainingCount <= 0L) {
                break;
            }

            final int count = slice.end - slice.start;
            int sampleCount;
            if (remainingGroups == 1) {
                sampleCount = Math.min(count, remainingBudget);
            } else {
                sampleCount = (int) Math.round(remainingBudget * (count / (double) remainingCount));
                sampleCount = Mth.clamp(sampleCount, 1, Math.min(count, remainingBudget));
            }

            this.addUniformlySampledCandidates(slice, sampleCount, selectedId);
            remainingBudget = MAX_RENDER_CANDIDATES - this.renderCache.size();
            remainingCount -= count;
            remainingGroups--;
        }
    }

    private void addUniformlySampledCandidates(
        final GroupSlice slice,
        final int sampleCount,
        @Nullable final Identifier selectedId
    ) {
        final int count = slice.end - slice.start;
        if (sampleCount <= 0 || count <= 0) {
            return;
        }

        final double stride = count / (double) sampleCount;
        final long groupHash = stableHash(slice.group.getCentralBody().getId());
        final double phase = unsignedUnit(mix64(groupHash));
        int previousIndex = -1;

        for (int sample = 0; sample < sampleCount && this.renderCache.size() < MAX_RENDER_CANDIDATES; sample++) {
            int relativeIndex = (int) Math.floor((sample + phase) * stride);
            relativeIndex = Mth.clamp(relativeIndex, 0, count - 1);
            final int index = slice.start + relativeIndex;
            if (index == previousIndex) {
                continue;
            }
            previousIndex = index;

            final AsteroidReloadListener.OrbitEntry orbitEntry = slice.group.getEntries().get(index);
            final AsteroidConfig asteroid = orbitEntry.getAsteroid();
            if (selectedId != null && selectedId.equals(asteroid.getId())) {
                continue;
            }

            this.addRenderCandidate(asteroid, false, orbitEntry.getStableHash());
        }
    }

    private void addRenderCandidate(
        final AsteroidConfig asteroid,
        final boolean selected,
        final long stableHash
    ) {
        if (this.renderCache.size() >= MAX_RENDER_CANDIDATES
            || this.renderCandidateSet.put(asteroid, Boolean.TRUE) != null) {
            return;
        }

        final RenderEntry entry = this.obtainRenderEntry();
        entry.asteroid = asteroid;
        entry.selected = selected;
        entry.stableHash = stableHash;
        this.renderCache.add(entry);
    }

    private void collectVisibleEntries() {
        this.visibleEntries.clear();

        for (final RenderEntry entry : this.renderCache) {
            this.updateRenderEntry(entry);
            if (entry.selected || this.isSpriteInView(entry.topLeftX, entry.topLeftY, entry.size)) {
                this.visibleEntries.add(entry);
            }
        }
    }

    private void updateRenderEntry(final RenderEntry entry) {
        final PositionState position = this.getPosition(entry.asteroid);
        entry.worldX = position.x;
        entry.worldY = position.y;
        entry.size = Math.max(1, Math.round(entry.asteroid.getDiameter() * this.zoom));
        entry.topLeftX = this.getTopLeftXFromCentered(entry.worldX, entry.size);
        entry.topLeftY = this.getTopLeftYFromCentered(entry.worldY, entry.size);
    }

    private void applyScreenSpaceLod() {
        this.frameRenderEntries.clear();
        if (this.visibleEntries.size() <= MAX_RENDERED_ASTEROIDS) {
            this.frameRenderEntries.addAll(this.visibleEntries);
            this.sortFrameEntries();
            return;
        }

        final int regularBudget = Math.max(1, MAX_RENDERED_ASTEROIDS - 1);
        int cellSize = Math.max(1, (int) Math.ceil(Math.sqrt(
            Math.max(1.0D, (this.width * (double) this.height) / regularBudget)
        )));
        int columns = divideRoundUp(this.width, cellSize);
        int rows = divideRoundUp(this.height, cellSize);
        while ((long) columns * rows > regularBudget) {
            cellSize++;
            columns = divideRoundUp(this.width, cellSize);
            rows = divideRoundUp(this.height, cellSize);
        }

        final int cellCount = Math.max(1, columns * rows);
        if (this.lodCells == null || this.lodCells.length < cellCount) {
            this.lodCells = new RenderEntry[cellCount];
        } else {
            Arrays.fill(this.lodCells, 0, cellCount, null);
        }

        RenderEntry selectedEntry = null;
        for (final RenderEntry entry : this.visibleEntries) {
            if (entry.selected) {
                selectedEntry = entry;
                continue;
            }

            final int centerScreenX = Mth.clamp(Mth.floor(entry.topLeftX + entry.size * 0.5F), 0, Math.max(0, this.width - 1));
            final int centerScreenY = Mth.clamp(Mth.floor(entry.topLeftY + entry.size * 0.5F), 0, Math.max(0, this.height - 1));
            final int column = Math.min(columns - 1, centerScreenX / cellSize);
            final int row = Math.min(rows - 1, centerScreenY / cellSize);
            final int cellIndex = row * columns + column;
            final RenderEntry current = this.lodCells[cellIndex];

            if (current == null || isHigherLodPriority(entry, current)) {
                this.lodCells[cellIndex] = entry;
            }
        }

        for (int i = 0; i < cellCount; i++) {
            if (this.lodCells[i] != null) {
                this.frameRenderEntries.add(this.lodCells[i]);
            }
        }
        if (selectedEntry != null) {
            this.frameRenderEntries.add(selectedEntry);
        }

        this.sortFrameEntries();
    }

    private void sortFrameEntries() {
        this.frameRenderEntries.sort((left, right) -> {
            if (left.selected != right.selected) {
                return left.selected ? 1 : -1;
            }
            final int sizeCompare = Integer.compare(left.size, right.size);
            return sizeCompare != 0
                ? sizeCompare
                : Long.compareUnsigned(left.stableHash, right.stableHash);
        });
    }

    private void renderOrbits(final GuiGraphicsExtractor graphics) {
        int remainingOrbitBudget = this.currentOrbitBudget();

        for (final RenderEntry entry : this.frameRenderEntries) {
            final AsteroidConfig asteroid = entry.asteroid;
            if (entry.selected) {
                this.drawOrbitIfVisible(graphics, asteroid, true);
                continue;
            }
            if (remainingOrbitBudget <= 0 || !asteroid.isOrbitVisible()) {
                continue;
            }
            if (this.drawOrbitIfVisible(graphics, asteroid, false)) {
                remainingOrbitBudget--;
            }
        }
    }

    private int currentOrbitBudget() {
        if (this.selectedAsteroid == null && this.zoom < 0.2F) {
            return 0;
        }
        if (this.zoom < 0.75F) {
            return 8;
        }
        return MAX_RENDERED_ORBITS;
    }

    private boolean drawOrbitIfVisible(
        final GuiGraphicsExtractor graphics,
        final AsteroidConfig asteroid,
        final boolean selected
    ) {
        final AsteroidConfig centralBody = asteroid.getCentralBody();
        if (centralBody == null || asteroid.getSemiMajorAxis() <= 0.0F) {
            return false;
        }

        final PositionState center = this.getPosition(centralBody);
        final double centralBodyRadius = centralBody.getRadius();
        final double radiusX = asteroid.getSemiMajorAxis() + centralBodyRadius;
        final double radiusY = asteroid.getSemiMinorAxis() + centralBodyRadius;
        final float screenCenterX = this.getScreenX(center.x);
        final float screenCenterY = this.getScreenY(center.y);
        final double screenRadiusX = radiusX * this.zoom;
        final double screenRadiusY = radiusY * this.zoom;

        if (!selected
            && (screenCenterX + screenRadiusX < -VIEW_PADDING_PIXELS
                || screenCenterX - screenRadiusX > this.width + VIEW_PADDING_PIXELS
                || screenCenterY + screenRadiusY < -VIEW_PADDING_PIXELS
                || screenCenterY - screenRadiusY > this.height + VIEW_PADDING_PIXELS)) {
            return false;
        }

        this.drawOrbit(graphics, center.x, center.y, radiusX, radiusY, selected);
        return true;
    }

    private void drawOrbit(
        final GuiGraphicsExtractor graphics,
        final double centerX,
        final double centerY,
        final double radiusX,
        final double radiusY,
        final boolean selected
    ) {
        final float[] unitOrbit = selected || this.zoom >= 1.0F
            ? UNIT_ORBIT_HIGH_DETAIL
            : UNIT_ORBIT_LOW_DETAIL;
        // OrbitRenderState keeps this reference until vertex extraction; do not recycle it next frame.
        final float[] points = new float[unitOrbit.length];

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (int i = 0; i < unitOrbit.length; i += 2) {
            final float x = this.getScreenX(centerX + radiusX * unitOrbit[i]);
            final float y = this.getScreenY(centerY + radiusY * unitOrbit[i + 1]);
            points[i] = x;
            points[i + 1] = y;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        graphics.submitGuiElementRenderState(new OrbitRenderState(
            new Matrix3x2f(graphics.pose()),
            points,
            selected ? 0xFFFF0000 : 0xFFFFFFFF,
            new ScreenRectangle(
                Mth.floor(minX),
                Mth.floor(minY),
                Math.max(1, Mth.ceil(maxX - minX) + 1),
                Math.max(1, Mth.ceil(maxY - minY) + 1)
            ),
            graphics.peekScissorStack()
        ));
    }

    private void renderAsteroidSprites(
        final GuiGraphicsExtractor graphics,
        final int mouseX,
        final int mouseY
    ) {
        this.hoveredAsteroid = null;
        this.frameGuiSpriteCache.clear();

        double hoveredDistanceSq = Double.POSITIVE_INFINITY;

        /*
         * Every rendered asteroid needs one quad. The selected asteroid needs
         * one additional overlay quad.
         *
         * Do not reuse or modify this array after submitting the render state:
         * GUI vertex extraction occurs after this method returns.
         */
        final AsteroidBatchRenderState.Quad[] quads =
            new AsteroidBatchRenderState.Quad[
                this.frameRenderEntries.size() + 1
                ];

        int quadCount = 0;

        /*
         * Resolve from the current GUI atlas during the submission phase.
         * This ensures resource-pack reloads are handled correctly.
         */
        final TextureAtlas guiAtlas = this.minecraft
            .getAtlasManager()
            .getAtlasOrThrow(AtlasIds.GUI);

        @Nullable
        CachedGuiSprite firstResolvedSprite = null;

        for (final RenderEntry entry : this.frameRenderEntries) {
            if (!entry.selected
                && !this.isSpriteInView(
                entry.topLeftX,
                entry.topLeftY,
                entry.size
            )) {
                continue;
            }

            final AsteroidConfig asteroid = entry.asteroid;

            /*
             * Resolved only once per unique Identifier during this frame.
             */
            final CachedGuiSprite sprite = this.resolveGuiSprite(
                guiAtlas,
                asteroid.getTexture()
            );

            if (firstResolvedSprite == null) {
                firstResolvedSprite = sprite;
            }

            final float rotationRadians =
                asteroid.isRotateAroundItself()
                    ? (float) Math.toRadians(
                    this.getRotationDegrees(asteroid)
                )
                    : 0.0F;

            quads[quadCount++] = new AsteroidBatchRenderState.Quad(
                entry.topLeftX,
                entry.topLeftY,
                entry.size,
                rotationRadians,
                sprite.u0(),
                sprite.v0(),
                sprite.u1(),
                sprite.v1(),
                0xFFFFFFFF
            );

            if (entry.selected) {
                final CachedGuiSprite selectedSprite =
                    this.resolveGuiSprite(guiAtlas, SELECTED);

                quads[quadCount++] = new AsteroidBatchRenderState.Quad(
                    entry.topLeftX,
                    entry.topLeftY,
                    entry.size,
                    0.0F,
                    selectedSprite.u0(),
                    selectedSprite.v0(),
                    selectedSprite.u1(),
                    selectedSprite.v1(),
                    0xFFFFFFFF
                );
            }

            if (ClientUtils.isMouseOver(
                entry.topLeftX,
                entry.topLeftY,
                entry.size,
                entry.size,
                mouseX,
                mouseY
            )) {
                final double centerX =
                    entry.topLeftX + entry.size * 0.5D;
                final double centerY =
                    entry.topLeftY + entry.size * 0.5D;

                final double dx = mouseX - centerX;
                final double dy = mouseY - centerY;
                final double distanceSq = dx * dx + dy * dy;

                if (distanceSq < hoveredDistanceSq) {
                    hoveredDistanceSq = distanceSq;
                    this.hoveredAsteroid = asteroid;
                }
            }
        }

        if (quadCount == 0 || firstResolvedSprite == null) {
            return;
        }

        /*
         * Every resolved sprite comes from AtlasIds.GUI, so one atlas texture
         * setup is sufficient for the entire batch.
         */
        final AbstractTexture atlasTexture = this.minecraft
            .getTextureManager()
            .getTexture(firstResolvedSprite.atlasLocation());

        final TextureSetup textureSetup = TextureSetup.singleTexture(
            atlasTexture.getTextureView(),
            atlasTexture.getSampler()
        );

        graphics.submitGuiElementRenderState(
            new AsteroidBatchRenderState(
                new Matrix3x2f(graphics.pose()),
                textureSetup,
                quads,
                quadCount,
                graphics.peekScissorStack()
            )
        );
    }

    private CachedGuiSprite resolveGuiSprite(
        final TextureAtlas guiAtlas,
        final Identifier spriteId
    ) {
        final CachedGuiSprite cached =
            this.frameGuiSpriteCache.get(spriteId);

        if (cached != null) {
            return cached;
        }

        /*
         * This is equivalent to the GUI sprite lookup performed internally by
         * GuiGraphicsExtractor.blitSprite, but it happens only once per unique
         * texture in this frame.
         */
        final TextureAtlasSprite sprite = guiAtlas.getSprite(spriteId);

        final CachedGuiSprite resolved = new CachedGuiSprite(
            sprite.atlasLocation(),
            sprite.getU0(),
            sprite.getV0(),
            sprite.getU1(),
            sprite.getV1()
        );

        this.frameGuiSpriteCache.put(spriteId, resolved);
        return resolved;
    }

    private PositionState getPosition(final AsteroidConfig asteroid) {
        PositionState state = this.positionStates.get(asteroid);
        if (state == null) {
            state = new PositionState(asteroid.getCurrentAngleDegrees(), asteroid.getRotation());
            this.positionStates.put(asteroid, state);
        }
        if (state.frame == this.positionFrame) {
            return state;
        }
        if (state.resolvingFrame == this.positionFrame) {
            // Invalid cyclic central-body definitions should not crash the render loop.
            state.x = 0.0D;
            state.y = 0.0D;
            state.frame = this.positionFrame;
            return state;
        }

        state.resolvingFrame = this.positionFrame;
        final AsteroidConfig centralBody = asteroid.getCentralBody();
        final double centerX;
        final double centerY;
        final double centralBodyRadius;
        if (centralBody == null) {
            centerX = 0.0D;
            centerY = 0.0D;
            centralBodyRadius = 0.0D;
        } else {
            final PositionState center = this.getPosition(centralBody);
            centerX = center.x;
            centerY = center.y;
            centralBodyRadius = centralBody.getRadius();
        }

        final double angleRadians = Math.toRadians(this.getAngleDegrees(asteroid, state));
        state.x = centerX + (asteroid.getSemiMajorAxis() + centralBodyRadius) * Math.cos(angleRadians);
        state.y = centerY + (asteroid.getSemiMinorAxis() + centralBodyRadius) * Math.sin(angleRadians);
        state.frame = this.positionFrame;
        state.resolvingFrame = Long.MIN_VALUE;
        return state;
    }

    private double getAngleDegrees(final AsteroidConfig asteroid, final PositionState state) {
        final double semiMajorAxis = asteroid.getSemiMajorAxis();
        if (semiMajorAxis == 0.0D) {
            return state.baseAngleDegrees;
        }

        final double angularSpeed = asteroid.getOrbitalSpeed() / (2.0D * Math.PI * semiMajorAxis);
        final double degreesPerTick = angularSpeed * 360.0D / 20.0D;
        final double direction = asteroid.isClockwise() ? 1.0D : -1.0D;
        return normalizeDegrees(state.baseAngleDegrees + direction * degreesPerTick * this.renderTimeTicks);
    }

    private double getRotationDegrees(final AsteroidConfig asteroid) {
        PositionState state = this.positionStates.get(asteroid);
        if (state == null) {
            state = new PositionState(asteroid.getCurrentAngleDegrees(), asteroid.getRotation());
            this.positionStates.put(asteroid, state);
        }
        return normalizeDegrees(state.baseRotationDegrees + asteroid.getOrbitalSpeed() * this.renderTimeTicks / 8.0D);
    }

    private RenderEntry obtainRenderEntry() {
        if (this.renderEntryPool.isEmpty()) {
            return new RenderEntry();
        }
        return this.renderEntryPool.remove(this.renderEntryPool.size() - 1);
    }

    private void recycleRenderCache() {
        for (final RenderEntry entry : this.renderCache) {
            entry.asteroid = null;
            entry.selected = false;
            if (this.renderEntryPool.size() < MAX_RENDER_CANDIDATES) {
                this.renderEntryPool.add(entry);
            }
        }
        this.renderCache.clear();
        this.renderCandidateSet.clear();
    }

    private GroupSlice obtainGroupSlice() {
        if (this.groupSlicePool.isEmpty()) {
            return new GroupSlice();
        }
        return this.groupSlicePool.remove(this.groupSlicePool.size() - 1);
    }

    private void recycleGroupSlices() {
        for (final GroupSlice slice : this.groupSlices) {
            slice.group = null;
            this.groupSlicePool.add(slice);
        }
        this.groupSlices.clear();
    }

    private void invalidateRenderCache() {
        this.cachedQueryView = null;
        this.lastCacheRebuildTick = Long.MIN_VALUE;
    }

    private WorldView createWorldView(final double screenPadding) {
        final double minX = ((-screenPadding - this.centerX) / this.zoom) - this.dragX;
        final double maxX = ((this.width + screenPadding - this.centerX) / this.zoom) - this.dragX;
        final double minY = ((-screenPadding - this.centerY) / this.zoom) - this.dragY;
        final double maxY = ((this.height + screenPadding - this.centerY) / this.zoom) - this.dragY;
        return new WorldView(minX, maxX, minY, maxY);
    }

    private static double distanceFromPointToRect(
        final double x,
        final double y,
        final WorldView view
    ) {
        final double dx = x < view.minX
            ? view.minX - x
            : x > view.maxX ? x - view.maxX : 0.0D;
        final double dy = y < view.minY
            ? view.minY - y
            : y > view.maxY ? y - view.maxY : 0.0D;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static double maxDistanceFromPointToRect(
        final double x,
        final double y,
        final WorldView view
    ) {
        final double d1 = distanceSq(x, y, view.minX, view.minY);
        final double d2 = distanceSq(x, y, view.minX, view.maxY);
        final double d3 = distanceSq(x, y, view.maxX, view.minY);
        final double d4 = distanceSq(x, y, view.maxX, view.maxY);
        return Math.sqrt(Math.max(Math.max(d1, d2), Math.max(d3, d4)));
    }

    private static double distanceSq(
        final double x1,
        final double y1,
        final double x2,
        final double y2
    ) {
        final double dx = x1 - x2;
        final double dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    private boolean isSpriteInView(final float topLeftX, final float topLeftY, final int size) {
        return topLeftX + size >= -VIEW_PADDING_PIXELS
            && topLeftX <= this.width + VIEW_PADDING_PIXELS
            && topLeftY + size >= -VIEW_PADDING_PIXELS
            && topLeftY <= this.height + VIEW_PADDING_PIXELS;
    }

    @Override
    public void extractBackground(
        final GuiGraphicsExtractor graphics,
        final int mouseX,
        final int mouseY,
        final float partialTick
    ) {
        graphics.fill(0, 0, this.width, this.height, 0xFF000000);
    }

    private void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        if (this.isMouseOnDetailPanel(mouseX, mouseY)) {
            if (this.selectedAsteroid != null) {
                final int detailX = this.width - this.detailWidth;
                ClientUtils.renderTooltipOfResources(
                    graphics,
                    mouseX,
                    mouseY,
                    5,
                    41,
                    5,
                    detailX,
                    0,
                    this.selectedAsteroid.getComposition()
                );
            }
            return;
        }

        if (this.hoveredAsteroid != null) {
            final List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(this.hoveredAsteroid.getName()));
            final var clientTooltips = ClientHooks.gatherTooltipComponents(
                ItemStack.EMPTY,
                tooltip,
                Optional.empty(),
                mouseX,
                graphics.guiWidth(),
                graphics.guiHeight(),
                this.font
            );
            ClientUtils.renderResourcesInsideTooltip(
                graphics,
                this.font,
                clientTooltips,
                mouseX,
                mouseY,
                DefaultTooltipPositioner.INSTANCE,
                this.hoveredAsteroid.getComposition()
            );
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.searchBox.mouseClicked(event, doubleClick)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        if (this.cancelMouseRelease) {
            this.cancelMouseRelease = false;
            return false;
        }
        if (this.isMouseOnDetailPanel(event.x(), event.y()) || this.clickedOrbitDetailsButton) {
            this.clickedOrbitDetailsButton = false;
            this.isDragging = false;
            return false;
        }

        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && !this.isDragging) {
            if (!this.searchBox.isMouseOver(event.x(), event.y())) {
                if (this.updateSelectedAsteroid) {
                    this.selectedAsteroid = this.hoveredAsteroid;
                    this.invalidateRenderCache();
                }
                this.followAsteroid = this.selectedAsteroid != null;
                this.zoomOntoAsteroid = this.selectedAsteroid != null;
            }
            this.updateSelectedAsteroid = true;
        }

        this.isDragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(
        final MouseButtonEvent event,
        final double dragX,
        final double dragY
    ) {
        if (this.isMouseOnDetailPanel(event.x(), event.y())) {
            return false;
        }
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseDragged(event, dragX, dragY);
        }

        if (Math.abs(dragX) > 0.02D || Math.abs(dragY) > 0.02D) {
            this.isDragging = true;
        }

        this.dragX += dragX / this.zoom;
        this.dragY += dragY / this.zoom;
        this.clampCamera();
        this.followAsteroid = false;
        this.zoomOntoAsteroid = false;
        return true;
    }

    @Override
    public boolean mouseScrolled(
        final double mouseX,
        final double mouseY,
        final double scrollX,
        final double scrollY
    ) {
        if (this.searchBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (scrollY == 0.0D) {
            return false;
        }

        final float oldZoom = this.zoom;
        final float zoomFactor = (float) Math.pow(1.12D, scrollY);
        this.zoom = Mth.clamp(oldZoom * zoomFactor, MIN_ZOOM, MAX_ZOOM);

        if (!this.followAsteroid && this.zoom != oldZoom) {
            final double worldXUnderMouse = ((mouseX - this.centerX) / oldZoom) - this.dragX;
            final double worldYUnderMouse = ((mouseY - this.centerY) / oldZoom) - this.dragY;
            this.dragX = ((mouseX - this.centerX) / this.zoom) - worldXUnderMouse;
            this.dragY = ((mouseY - this.centerY) / this.zoom) - worldYUnderMouse;
            this.clampCamera();
        }

        this.zoomOntoAsteroid = false;
        return true;
    }

    @Override
    public void mouseMoved(final double mouseX, final double mouseY) {
        this.searchBox.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (!this.searchBox.isFocused() && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (this.searchBox.keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        if (this.searchBox.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }

    private void clampCamera() {
        this.dragX = Mth.clamp(this.dragX, -MIN_MAX_X, MIN_MAX_X);
        this.dragY = Mth.clamp(this.dragY, -MIN_MAX_Y, MIN_MAX_Y);
    }

    private boolean isMouseOnDetailPanel(final double mouseX, final double mouseY) {
        return this.selectedAsteroid != null
            && ClientUtils.isMouseOver(
                this.width - this.detailWidth,
                0,
                this.detailWidth,
                this.detailHeight,
                mouseX,
                mouseY
            );
    }

    public float getTopLeftXFromCentered(final double x, final int size) {
        return this.getScreenX(x) - size * 0.5F;
    }

    public float getTopLeftYFromCentered(final double y, final int size) {
        return this.getScreenY(y) - size * 0.5F;
    }

    private float getScreenX(final double worldX) {
        return (float) (this.centerX + (worldX + this.dragX) * this.zoom);
    }

    private float getScreenY(final double worldY) {
        return (float) (this.centerY + (worldY + this.dragY) * this.zoom);
    }

    @Override
    public void onClose() {
        this.selectAsteroid.accept(this.selectedAsteroid != null ? this.selectedAsteroid.getId() : null);
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean isHigherLodPriority(final RenderEntry candidate, final RenderEntry current) {
        if (candidate.size != current.size) {
            return candidate.size > current.size;
        }
        return Long.compareUnsigned(candidate.stableHash, current.stableHash) < 0;
    }

    private static int divideRoundUp(final int value, final int divisor) {
        return Math.max(1, (value + divisor - 1) / divisor);
    }

    private static float[] createUnitOrbit(final int segments) {
        final float[] points = new float[(segments + 1) * 2];
        for (int i = 0; i <= segments; i++) {
            final double angle = (Math.PI * 2.0D * i) / segments;
            points[i * 2] = (float) Math.cos(angle);
            points[i * 2 + 1] = (float) Math.sin(angle);
        }
        return points;
    }

    private static double normalizeDegrees(final double degrees) {
        final double normalized = degrees % 360.0D;
        return normalized < 0.0D ? normalized + 360.0D : normalized;
    }

    private static long stableHash(final Identifier id) {
        return Integer.toUnsignedLong(id.toString().hashCode());
    }

    private static long mix64(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static double unsignedUnit(final long value) {
        return (value >>> 11) * 0x1.0p-53;
    }

    private static final class RenderEntry {
        private AsteroidConfig asteroid;
        private boolean selected;
        private long stableHash;
        private double worldX;
        private double worldY;
        private float topLeftX;
        private float topLeftY;
        private int size;
    }

    private record CachedGuiSprite(
        Identifier atlasLocation,
        float u0,
        float v0,
        float u1,
        float v1
    ) {
    }

    private static final class GroupSlice {
        private AsteroidReloadListener.OrbitGroup group;
        private int start;
        private int end;
    }

    private static final class PositionState {
        private final double baseAngleDegrees;
        private final double baseRotationDegrees;
        private long frame = Long.MIN_VALUE;
        private long resolvingFrame = Long.MIN_VALUE;
        private double x;
        private double y;

        private PositionState(final double baseAngleDegrees, final double baseRotationDegrees) {
            this.baseAngleDegrees = baseAngleDegrees;
            this.baseRotationDegrees = baseRotationDegrees;
        }
    }

    private record WorldView(double minX, double maxX, double minY, double maxY) {
        private boolean contains(final WorldView other) {
            return other.minX >= this.minX
                && other.maxX <= this.maxX
                && other.minY >= this.minY
                && other.maxY <= this.maxY;
        }

        private boolean intersectsCircle(final double x, final double y, final double radius) {
            final double closestX = Mth.clamp(x, this.minX, this.maxX);
            final double closestY = Mth.clamp(y, this.minY, this.maxY);
            return distanceSq(x, y, closestX, closestY) <= radius * radius;
        }
    }
}
