package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.asteroids.AsteroidConfig;
import com.ultramega.asteroidmining.events.AsteroidReloadListener;
import com.ultramega.asteroidmining.gui.renderer.OrbitRenderState;
import com.ultramega.asteroidmining.gui.widgets.AsteroidSearchBox;
import com.ultramega.asteroidmining.gui.widgets.ImageButton;
import com.ultramega.asteroidmining.gui.widgets.ImagesButton;
import com.ultramega.asteroidmining.utils.ClientUtils;
import com.ultramega.asteroidmining.utils.CommonUtils;
import com.ultramega.asteroidmining.utils.TextColors;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
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

public class SolarSystemViewScreen extends Screen {
    private static final Identifier SELECTED = AsteroidMining.makeId("selected");
    private static final Identifier SEARCH = AsteroidMining.makeId("search");
    private static final Identifier UP = AsteroidMining.makeId("up");
    private static final Identifier DOWN = AsteroidMining.makeId("down");

    private static final int MAX_ZOOM = 20;
    private static final int MIN_MAX_X = 5000;
    private static final int MIN_MAX_Y = 3000;

    private static final int MAX_RENDERED_ASTEROIDS = 900;
    private static final int MAX_RENDER_CANDIDATES = 4500;
    private static final int MAX_ORBIT_QUERY_PER_GROUP = 6000;
    private static final int MAX_RENDERED_ORBITS = 48;
    private static final int CACHE_REBUILD_FRAME_INTERVAL = 20;
    private static final int ORBIT_SEGMENTS_HIGH_DETAIL = 96;
    private static final int ORBIT_SEGMENTS_LOW_DETAIL = 20;
    private static final double VIEW_PADDING = 96.0D;
    private static final double ORBIT_QUERY_WORLD_PADDING = 256.0D;

    private final Consumer<@Nullable Identifier> selectAsteroid;
    private final ObservatoryScreen parent;
    private final List<RenderEntry> renderCache = new ArrayList<>();
    private final List<RenderEntry> renderEntryPool = new ArrayList<>();
    private final List<float[]> orbitPointPool = new ArrayList<>();
    private final List<float[]> activeOrbitPointArrays = new ArrayList<>();
    private final Point2D.Double scratchPosition = new Point2D.Double();
    private final Point2D.Double scratchCenter = new Point2D.Double();

    private int detailWidth;
    private int detailHeight;
    private int centerX;
    private int centerY;
    private boolean isDragging;
    private double dragX;
    private double dragY;
    private float zoom = 1f;
    private boolean followAsteroid;
    private boolean zoomOntoAsteroid;
    private boolean updateSelectedAsteroid = true;

    @Nullable
    private AsteroidConfig selectedAsteroid;
    @Nullable
    private AsteroidConfig hoveredAsteroid = null;

    private boolean displayOrbitDetails;
    private AsteroidSearchBox searchBox;
    private Button selectButton;
    private Button displayOrbitDetailsButton;
    private boolean clickedOrbitDetailsButton;
    private boolean cancelMouseRelease;

    private long frameCounter;
    private long lastCacheRebuildFrame = -1;
    private double cachedDragX = Double.NaN;
    private double cachedDragY = Double.NaN;
    private float cachedZoom = Float.NaN;
    private int cachedWidth = -1;
    private int cachedHeight = -1;
    @Nullable
    private Identifier cachedSelectedAsteroidId;

    public SolarSystemViewScreen(@Nullable final AsteroidConfig selectedAsteroid, final Consumer<@Nullable Identifier> selectAsteroid, final ObservatoryScreen parent) {
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

        this.searchBox = new AsteroidSearchBox(this.font, 7 + 16, 5, 64, 16, 128, true,
                Component.translatable("gui.asteroidmining.asteroid.search"),
                (name) -> {
                    this.selectedAsteroid = AsteroidReloadListener.INSTANCE.findAsteroidByName(name);
                    this.updateSelectedAsteroid = false;
                    this.invalidateRenderCache();
                });
        this.searchBox.setVisible(false);
        this.addRenderableWidget(this.searchBox);

        this.selectButton = Button.builder(Component.translatable("gui.asteroidmining.select"), (button) -> this.onClose())
                .bounds(0, 0, 48, 16).build();
        this.selectButton.visible = false;
        this.addRenderableWidget(this.selectButton);

        this.displayOrbitDetailsButton = new ImageButton(0, 0, 16, 16, DOWN, (button) -> {
            this.displayOrbitDetails = !this.displayOrbitDetails;
            ((ImageButton) button).setImage(this.displayOrbitDetails ? UP : DOWN);
            this.clickedOrbitDetailsButton = true;
        });
        this.displayOrbitDetailsButton.visible = false;
        this.addRenderableWidget(this.displayOrbitDetailsButton);
    }

    // TODO: override extractRenderStateWithTooltipAndSubtitles?
    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        this.renderAsteroids(graphics, mouseX, mouseY);
        this.renderSelectedAsteroidDetails(graphics, mouseX, mouseY);
        for (final Renderable renderable : this.renderables) {
            renderable.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        }
        this.extractTooltip(graphics, mouseX, mouseY);
    }

    private void renderSelectedAsteroidDetails(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        if (this.selectedAsteroid == null) {
            this.selectButton.visible = false;
            this.displayOrbitDetailsButton.visible = false;
            return;
        }

        // <<< Text Content Preparation >>>
        final String asteroidName = this.selectedAsteroid.getName();
        final Component sizeLabel = Component.translatable("gui.asteroidmining.observatory.diameter").withStyle(ChatFormatting.AQUA)
            .append(Component.literal(CommonUtils.formatPlanetSize(this.selectedAsteroid.getDiameter())).withStyle(ChatFormatting.WHITE));

        final boolean hasComposition = !this.selectedAsteroid.getComposition().isEmpty();
        final Component compositionLabel = Component.translatable("gui.asteroidmining.observatory.composition").withStyle(ChatFormatting.AQUA);

        final boolean hasOrbitDetails = !this.selectedAsteroid.getCentralBodyName().isBlank();
        final String orbitDetailsLabel = Component.translatable("gui.asteroidmining.observatory.display_orbit_details").getString();
        final Component centralBodyLabel = Component.translatable("gui.asteroidmining.observatory.central_body").withStyle(ChatFormatting.AQUA)
            .append(Component.literal(this.selectedAsteroid.getCentralBodyName()).withStyle(ChatFormatting.WHITE));
        final Component semiMayorAxisLabel = Component.translatable("gui.asteroidmining.observatory.semi_mayor_axis").withStyle(ChatFormatting.AQUA)
            .append(Component.literal(CommonUtils.formatAstronomicalUnit(this.selectedAsteroid.getSemiMajorAxis())).withStyle(ChatFormatting.WHITE));
        final Component semiMinorAxisLabel = Component.translatable("gui.asteroidmining.observatory.semi_minor_axis").withStyle(ChatFormatting.AQUA)
            .append(Component.literal(CommonUtils.formatAstronomicalUnit(this.selectedAsteroid.getSemiMinorAxis())).withStyle(ChatFormatting.WHITE));

        // TODO: add orbital trustForce

        // <<< Text Width Calculation >>>
        int maxTextWidth = 0;
        maxTextWidth = Math.max(maxTextWidth, this.font.width(asteroidName));
        maxTextWidth = Math.max(maxTextWidth, this.font.width(sizeLabel));
        if (hasComposition) {
            maxTextWidth = Math.max(maxTextWidth, this.font.width(compositionLabel));
        }
        maxTextWidth = Math.max(maxTextWidth, this.font.width(orbitDetailsLabel) + 8);
        if (this.displayOrbitDetails) {
            maxTextWidth = Math.max(maxTextWidth, this.font.width(centralBodyLabel));
            maxTextWidth = Math.max(maxTextWidth, this.font.width(semiMayorAxisLabel));
            maxTextWidth = Math.max(maxTextWidth, this.font.width(semiMinorAxisLabel));
        }
        this.detailWidth = Math.max(100, maxTextWidth + 20);

        // <<< Text Height Calculation >>>
        final int baseTextHeight = 55;
        final int compositionTextHeight = hasComposition ? 25 : 0;
        final int compositionStacksHeight = (this.selectedAsteroid.getComposition().size() / 5) * 18;
        final int orbitDetailsButtonExtraHeight = hasOrbitDetails ? 20 : 0;
        final int orbitDetailsExtraHeight = this.displayOrbitDetails ? 35 : 0;
        this.detailHeight = baseTextHeight + compositionTextHeight + compositionStacksHeight + orbitDetailsButtonExtraHeight + orbitDetailsExtraHeight;
        final int detailX = this.width - this.detailWidth;

        // <<< Update UI elements >>>
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

        // <<< Draw information text >>>
        int y = 5;

        graphics.text(this.font, asteroidName, (this.detailWidth - this.font.width(asteroidName)) / 2, y, TextColors.GOLD.getHexCode());
        y += 15;

        graphics.text(this.font, sizeLabel, 5, y, -1);
        y += 10;

        if (!this.selectedAsteroid.getComposition().isEmpty()) {
            graphics.text(this.font, compositionLabel, 5, y, -1);
            ClientUtils.renderResourcesWithSlot(graphics, this.font, mouseX, mouseY, 5, 41, 5, detailX, 0,
                this.selectedAsteroid.getComposition());
            y += compositionTextHeight + compositionStacksHeight;
        }

        if (this.displayOrbitDetails) {
            y += 27;
            graphics.text(this.font, centralBodyLabel, 5, y, -1);
            y += 10;
            graphics.text(this.font, semiMayorAxisLabel, 5, y, -1);
            y += 10;
            graphics.text(this.font, semiMinorAxisLabel, 5, y, -1);
        }
        poseStack.popMatrix();
    }

    public void renderAsteroids(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        this.frameCounter++;
        this.releaseOrbitPointArrays();

        if (this.shouldRebuildRenderCache()) {
            this.rebuildRenderCache();
        }

        final Matrix3x2fStack poseStack = graphics.pose();
        this.hoveredAsteroid = null;
        int renderedOrbitCount = 0;
        final int orbitBudget = this.currentOrbitBudget();

        for (final RenderEntry entry : this.renderCache) {
            final AsteroidConfig asteroid = entry.asteroid;
            final boolean isSelectedAsteroid = this.selectedAsteroid != null && asteroid.getId().equals(this.selectedAsteroid.getId());

            asteroid.increaseOrbitalAngle();
            this.updateRenderEntry(entry);

            if (this.followAsteroid && isSelectedAsteroid) {
                if (this.zoomOntoAsteroid) {
                    this.zoom = Mth.clamp(MAX_ZOOM - ((float) this.selectedAsteroid.getDiameter() / 2), 0.05F, MAX_ZOOM);
                }
                this.dragX = -entry.worldX;
                this.dragY = -entry.worldY;
                this.invalidateRenderCache();
                this.updateRenderEntry(entry);
            }

            if (!isSelectedAsteroid && !this.isSpriteInView(entry.topLeftX, entry.topLeftY, entry.size)) {
                continue;
            }

            if (isSelectedAsteroid || (this.zoom >= 0.2F && asteroid.isOrbitVisible() && renderedOrbitCount < orbitBudget)) {
                asteroid.writeCenter(this.scratchCenter);
                this.drawOrbit(graphics, this.scratchCenter.x, this.scratchCenter.y, asteroid.getSemiMajorAxis(), asteroid.getSemiMinorAxis(),
                        asteroid.getCentralBody() != null ? asteroid.getCentralBody().getRadius() : 0, isSelectedAsteroid);
                renderedOrbitCount++;
            }

            if (ClientUtils.isMouseOver(entry.topLeftX, entry.topLeftY, entry.size, entry.size, mouseX, mouseY)) {
                this.hoveredAsteroid = asteroid;
            }

            poseStack.pushMatrix();
            poseStack.translate(entry.topLeftX, entry.topLeftY);

            if (asteroid.isRotateAroundItself()) {
                asteroid.increaseRotation();

                final float center = entry.size / 2.0f;
                poseStack.pushMatrix();
                poseStack.rotateAbout(asteroid.getRotation(), center, center);

                graphics.blitSprite(GUI_TEXTURED, asteroid.getTexture(), entry.size, entry.size, 0, 0, 0, 0, entry.size, entry.size);

                poseStack.popMatrix();
            } else {
                graphics.blitSprite(GUI_TEXTURED, asteroid.getTexture(), entry.size, entry.size, 0, 0, 0, 0, entry.size, entry.size);
            }

            if (isSelectedAsteroid) {
                graphics.blitSprite(GUI_TEXTURED, SELECTED, entry.size, entry.size, 0, 0, 0, 0, entry.size, entry.size);
            }
            poseStack.popMatrix();
        }
    }

    private int currentOrbitBudget() {
        if (this.selectedAsteroid == null && this.zoom < 0.2F) {
            return 0;
        }
        if (this.zoom < 0.75F) {
            return 12;
        }
        return MAX_RENDERED_ORBITS;
    }

    private boolean shouldRebuildRenderCache() {
        if (this.lastCacheRebuildFrame < 0) {
            return true;
        }
        if (this.frameCounter - this.lastCacheRebuildFrame >= CACHE_REBUILD_FRAME_INTERVAL) {
            return true;
        }
        if (this.cachedWidth != this.width || this.cachedHeight != this.height) {
            return true;
        }
        if (Math.abs(this.cachedDragX - this.dragX) > 0.01D || Math.abs(this.cachedDragY - this.dragY) > 0.01D) {
            return true;
        }
        if (Math.abs(this.cachedZoom - this.zoom) > 0.001F) {
            return true;
        }

        final Identifier selectedId = this.selectedAsteroid == null ? null : this.selectedAsteroid.getId();
        return selectedId == null ? this.cachedSelectedAsteroidId != null : !selectedId.equals(this.cachedSelectedAsteroidId);
    }

    private void rebuildRenderCache() {
        this.recycleRenderCache();
        final Identifier selectedId = this.selectedAsteroid == null ? null : this.selectedAsteroid.getId();
        final WorldView view = this.createWorldView();
        final AsteroidReloadListener.RenderIndex index = AsteroidReloadListener.INSTANCE.getRenderIndex();

        if (this.selectedAsteroid != null) {
            this.addRenderCandidate(this.selectedAsteroid, true, view, true);
        }

        for (final AsteroidConfig asteroid : index.getRootAsteroids()) {
            if (this.renderCache.size() >= MAX_RENDER_CANDIDATES) {
                break;
            }
            final boolean isSelectedAsteroid = selectedId != null && selectedId.equals(asteroid.getId());
            if (!isSelectedAsteroid) {
                this.addRenderCandidate(asteroid, false, view, false);
            }
        }

        for (final AsteroidReloadListener.OrbitGroup group : index.getOrbitGroups()) {
            if (this.renderCache.size() >= MAX_RENDER_CANDIDATES) {
                break;
            }
            this.addOrbitGroupCandidates(group, selectedId, view);
        }

        this.sortAndTrimRenderCache();

        this.lastCacheRebuildFrame = this.frameCounter;
        this.cachedDragX = this.dragX;
        this.cachedDragY = this.dragY;
        this.cachedZoom = this.zoom;
        this.cachedWidth = this.width;
        this.cachedHeight = this.height;
        this.cachedSelectedAsteroidId = selectedId;
    }

    private void addOrbitGroupCandidates(final AsteroidReloadListener.OrbitGroup group, @Nullable final Identifier selectedId, final WorldView view) {
        group.getCentralBody().writePosition(this.scratchCenter);
        final double minDistance = this.distanceFromPointToRect(this.scratchCenter.x, this.scratchCenter.y, view);
        final double maxDistance = this.maxDistanceFromPointToRect(this.scratchCenter.x, this.scratchCenter.y, view);
        final double padding = view.worldPadding + ORBIT_QUERY_WORLD_PADDING;

        if (group.getMaxOrbitRadius() < minDistance - padding || group.getMinOrbitRadius() > maxDistance + padding) {
            return;
        }

        final int start = group.lowerBound(Math.max(0.0D, minDistance - padding));
        final int end = group.upperBound(maxDistance + padding);
        final int count = end - start;
        if (count <= 0) {
            return;
        }

        final int step = Math.max(1, (count + MAX_ORBIT_QUERY_PER_GROUP - 1) / MAX_ORBIT_QUERY_PER_GROUP);
        for (int i = start; i < end && this.renderCache.size() < MAX_RENDER_CANDIDATES; i += step) {
            final AsteroidReloadListener.OrbitEntry entry = group.getEntries().get(i);
            final AsteroidConfig asteroid = entry.getAsteroid();
            if (selectedId != null && selectedId.equals(asteroid.getId())) {
                continue;
            }
            if (!this.passesZoomSample(entry, step)) {
                continue;
            }
            this.addRenderCandidate(asteroid, false, view, false);
        }
    }

    private boolean passesZoomSample(final AsteroidReloadListener.OrbitEntry entry, final int rangeStep) {
        if (this.zoom >= 1.0F || rangeStep > 1) {
            return true;
        }

        final int sampleMask;
        if (this.zoom < 0.1F) {
            sampleMask = 0x7F;
        } else if (this.zoom < 0.2F) {
            sampleMask = 0x3F;
        } else if (this.zoom < 0.4F) {
            sampleMask = 0x1F;
        } else if (this.zoom < 0.7F) {
            sampleMask = 0x0F;
        } else {
            sampleMask = 0x07;
        }
        return (entry.getStableHash() & sampleMask) == 0L;
    }

    private void addRenderCandidate(final AsteroidConfig asteroid, final boolean selected, final WorldView view, final boolean force) {
        asteroid.writePosition(this.scratchPosition);
        final int size = Math.max(1, Math.round(asteroid.getDiameter() * this.zoom));
        final float topLeftX = this.getTopLeftXFromCentered(this.scratchPosition.x, size);
        final float topLeftY = this.getTopLeftYFromCentered(this.scratchPosition.y, size);

        if (!force && !this.isSpriteInView(topLeftX, topLeftY, size)) {
            return;
        }

        final RenderEntry entry = this.obtainRenderEntry();
        entry.asteroid = asteroid;
        entry.selected = selected;
        entry.worldX = this.scratchPosition.x;
        entry.worldY = this.scratchPosition.y;
        entry.size = size;
        entry.topLeftX = topLeftX;
        entry.topLeftY = topLeftY;
        entry.distanceToViewCenterSq = this.distanceSqToViewCenter(entry.worldX, entry.worldY, view);
        this.renderCache.add(entry);
    }

    private void updateRenderEntry(final RenderEntry entry) {
        entry.asteroid.writePosition(this.scratchPosition);
        entry.worldX = this.scratchPosition.x;
        entry.worldY = this.scratchPosition.y;
        entry.size = Math.max(1, Math.round(entry.asteroid.getDiameter() * this.zoom));
        entry.topLeftX = this.getTopLeftXFromCentered(entry.worldX, entry.size);
        entry.topLeftY = this.getTopLeftYFromCentered(entry.worldY, entry.size);
    }

    private void sortAndTrimRenderCache() {
        this.renderCache.sort((left, right) -> {
            if (left.selected != right.selected) {
                return left.selected ? -1 : 1;
            }
            final int diameterCompare = Integer.compare(right.asteroid.getDiameter(), left.asteroid.getDiameter());
            if (diameterCompare != 0) {
                return diameterCompare;
            }
            final int distanceCompare = Double.compare(left.distanceToViewCenterSq, right.distanceToViewCenterSq);
            return distanceCompare != 0 ? distanceCompare : left.asteroid.getName().compareToIgnoreCase(right.asteroid.getName());
        });

        while (this.renderCache.size() > MAX_RENDERED_ASTEROIDS) {
            this.recycleRenderEntry(this.renderCache.remove(this.renderCache.size() - 1));
        }
    }

    private RenderEntry obtainRenderEntry() {
        if (this.renderEntryPool.isEmpty()) {
            return new RenderEntry();
        }
        return this.renderEntryPool.remove(this.renderEntryPool.size() - 1);
    }

    private void recycleRenderCache() {
        for (final RenderEntry entry : this.renderCache) {
            this.recycleRenderEntry(entry);
        }
        this.renderCache.clear();
    }

    private void recycleRenderEntry(final RenderEntry entry) {
        entry.asteroid = null;
        if (this.renderEntryPool.size() < MAX_RENDER_CANDIDATES) {
            this.renderEntryPool.add(entry);
        }
    }

    private void invalidateRenderCache() {
        this.lastCacheRebuildFrame = -1;
    }

    private WorldView createWorldView() {
        final double minX = ((-VIEW_PADDING - this.centerX) / this.zoom) - this.dragX;
        final double maxX = ((this.width + VIEW_PADDING - this.centerX) / this.zoom) - this.dragX;
        final double minY = ((-VIEW_PADDING - this.centerY) / this.zoom) - this.dragY;
        final double maxY = ((this.height + VIEW_PADDING - this.centerY) / this.zoom) - this.dragY;
        return new WorldView(minX, maxX, minY, maxY, (minX + maxX) * 0.5D, (minY + maxY) * 0.5D, VIEW_PADDING / this.zoom);
    }

    private double distanceSqToViewCenter(final double x, final double y, final WorldView view) {
        final double dx = x - view.centerX;
        final double dy = y - view.centerY;
        return dx * dx + dy * dy;
    }

    private double distanceFromPointToRect(final double x, final double y, final WorldView view) {
        final double dx;
        if (x < view.minX) {
            dx = view.minX - x;
        } else if (x > view.maxX) {
            dx = x - view.maxX;
        } else {
            dx = 0.0D;
        }

        final double dy;
        if (y < view.minY) {
            dy = view.minY - y;
        } else if (y > view.maxY) {
            dy = y - view.maxY;
        } else {
            dy = 0.0D;
        }
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double maxDistanceFromPointToRect(final double x, final double y, final WorldView view) {
        final double d1 = this.distanceSq(x, y, view.minX, view.minY);
        final double d2 = this.distanceSq(x, y, view.minX, view.maxY);
        final double d3 = this.distanceSq(x, y, view.maxX, view.minY);
        final double d4 = this.distanceSq(x, y, view.maxX, view.maxY);
        return Math.sqrt(Math.max(Math.max(d1, d2), Math.max(d3, d4)));
    }

    private double distanceSq(final double x1, final double y1, final double x2, final double y2) {
        final double dx = x1 - x2;
        final double dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    private boolean isSpriteInView(final float topLeftX, final float topLeftY, final int size) {
        return topLeftX + size >= -VIEW_PADDING
                && topLeftX <= this.width + VIEW_PADDING
                && topLeftY + size >= -VIEW_PADDING
                && topLeftY <= this.height + VIEW_PADDING;
    }

    private void drawOrbit(final GuiGraphicsExtractor graphics,
                           final double centerX,
                           final double centerY,
                           final double semiMajorAxis,
                           final double semiMinorAxis,
                           final double centralBodyRadius,
                           final boolean isSelected) {
        final int segments = isSelected || this.zoom >= 1.0F ? ORBIT_SEGMENTS_HIGH_DETAIL : ORBIT_SEGMENTS_LOW_DETAIL;
        final float[] points = this.obtainOrbitPoints((segments + 1) * 2);
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (int i = 0; i <= segments; i++) {
            final double angle = (2.0 * Math.PI * i) / segments;
            final double offsetX = (semiMajorAxis + centralBodyRadius) * Math.cos(angle);
            final double offsetY = (semiMinorAxis + centralBodyRadius) * Math.sin(angle);
            final float x = this.getTopLeftXFromCentered(centerX + offsetX, 0);
            final float y = this.getTopLeftYFromCentered(centerY + offsetY, 0);
            points[i * 2] = x;
            points[i * 2 + 1] = y;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        final int color = isSelected ? 0xFFFF0000 : 0xFFFFFFFF;
        graphics.submitGuiElementRenderState(new OrbitRenderState(
            new Matrix3x2f(graphics.pose()),
            points,
            color,
            new ScreenRectangle(
                    Mth.floor(minX),
                    Mth.floor(minY),
                    Mth.ceil(maxX - minX) + 1,
                    Mth.ceil(maxY - minY) + 1
            ),
            graphics.peekScissorStack()
        ));
    }

    private float[] obtainOrbitPoints(final int length) {
        for (int i = this.orbitPointPool.size() - 1; i >= 0; i--) {
            final float[] points = this.orbitPointPool.get(i);
            if (points.length == length) {
                this.orbitPointPool.remove(i);
                this.activeOrbitPointArrays.add(points);
                return points;
            }
        }

        final float[] points = new float[length];
        this.activeOrbitPointArrays.add(points);
        return points;
    }

    private void releaseOrbitPointArrays() {
        if (this.activeOrbitPointArrays.isEmpty()) {
            return;
        }
        for (final float[] points : this.activeOrbitPointArrays) {
            if (this.orbitPointPool.size() < MAX_RENDERED_ORBITS * 2) {
                this.orbitPointPool.add(points);
            }
        }
        this.activeOrbitPointArrays.clear();
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF000000);
    }

    private void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        if (this.isMouseOnDetailPanel(mouseX, mouseY)) {
            if (this.selectedAsteroid != null) {
                final int detailX = this.width - this.detailWidth;
                ClientUtils.renderTooltipOfResources(graphics, mouseX, mouseY, 5, 41, 5, detailX, 0,
                    this.selectedAsteroid.getComposition());
            }
            return;
        }

        if (this.hoveredAsteroid != null) {
            final List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(this.hoveredAsteroid.getName()));
            final List clientTooltips = ClientHooks.gatherTooltipComponents(ItemStack.EMPTY, tooltip, Optional.empty(), mouseX,
                graphics.guiWidth(), graphics.guiHeight(), this.font);
            ClientUtils.renderResourcesInsideTooltip(graphics, this.font, clientTooltips, mouseX, mouseY,
                DefaultTooltipPositioner.INSTANCE, this.hoveredAsteroid.getComposition());
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
            return false;
        }

        if (event.button() == 0 && !this.isDragging) {
            if (!this.searchBox.isMouseOver(event.x(), event.y())) {
                if (this.updateSelectedAsteroid) {
                    this.selectedAsteroid = this.hoveredAsteroid;
                    this.invalidateRenderCache();
                }
                this.followAsteroid = true;
                this.zoomOntoAsteroid = true;
            }
            this.updateSelectedAsteroid = true;
        }

        this.isDragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        if (this.isMouseOnDetailPanel(event.x(), event.y())) {
            return false;
        }

        if (Math.abs(dragX) > 0.02 || Math.abs(dragY) > 0.02) {
            this.isDragging = true;
        }

        this.dragX += dragX / this.zoom;
        this.dragY += dragY / this.zoom;
        this.dragX = Mth.clamp(this.dragX, -MIN_MAX_X * this.zoom, MIN_MAX_X * this.zoom);
        this.dragY = Mth.clamp(this.dragY, -MIN_MAX_Y * this.zoom, MIN_MAX_Y * this.zoom);
        this.followAsteroid = false;
        this.zoomOntoAsteroid = false;
        this.invalidateRenderCache();
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        if (this.searchBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return false;
        }

        final float scrollAmount = (float) scrollY * 0.1f * this.zoom;
        this.zoom += scrollAmount;
        this.zoom = Mth.clamp(this.zoom, 0.05f, MAX_ZOOM);
        this.zoomOntoAsteroid = false;
        this.invalidateRenderCache();
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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

    private boolean isMouseOnDetailPanel(final double mouseX, final double mouseY) {
        return this.selectedAsteroid != null
            && ClientUtils.isMouseOver(this.width - this.detailWidth, 0, this.detailWidth, this.detailHeight, mouseX, mouseY);
    }

    public float getTopLeftXFromCentered(final double x, final int size) {
        return (float) (this.centerX + ((x + this.dragX) * this.zoom) - (size / 2.0));
    }

    public float getTopLeftYFromCentered(final double y, final int size) {
        return (float) (this.centerY + ((y + this.dragY) * this.zoom) - (size / 2.0));
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

    private static final class RenderEntry {
        private AsteroidConfig asteroid;
        private boolean selected;
        private double worldX;
        private double worldY;
        private float topLeftX;
        private float topLeftY;
        private int size;
        private double distanceToViewCenterSq;
    }

    private record WorldView(double minX, double maxX, double minY, double maxY, double centerX, double centerY, double worldPadding) {
    }
}
