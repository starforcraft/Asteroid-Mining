package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.events.AsteroidReloadListener;
import com.ultramega.asteroidmining.gui.renderer.OrbitRenderState;
import com.ultramega.asteroidmining.gui.widgets.AsteroidSearchBox;
import com.ultramega.asteroidmining.gui.widgets.ImageButton;
import com.ultramega.asteroidmining.gui.widgets.ImagesButton;
import com.ultramega.asteroidmining.utils.AsteroidConfig;
import com.ultramega.asteroidmining.utils.TextColors;
import com.ultramega.asteroidmining.utils.Utils;

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
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public class SolarSystemViewScreen extends Screen {
    private static final Identifier SELECTED = AsteroidMining.makeId("selected");
    private static final Identifier SEARCH = AsteroidMining.makeId("search");
    private static final Identifier UP = AsteroidMining.makeId("up");
    private static final Identifier DOWN = AsteroidMining.makeId("down");

    private static final int MAX_ZOOM = 20;
    private static final int MIN_MAX_X = 5000;
    private static final int MIN_MAX_Y = 3000;

    private final Consumer<Identifier> selectAsteroid;
    private final ObservatoryScreen parent;

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

    public SolarSystemViewScreen(@Nullable final AsteroidConfig selectedAsteroid, final Consumer<Identifier> selectAsteroid, final ObservatoryScreen parent) {
        super(GameNarrator.NO_TITLE);
        this.selectedAsteroid = selectedAsteroid;
        this.selectAsteroid = selectAsteroid;
        this.parent = parent;
        this.cancelMouseRelease = true;

        if (this.selectAsteroid != null) {
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
            Component.translatable("gui.asteroidmining.asteroid.search"), (name) -> {
            this.selectedAsteroid = AsteroidReloadListener.INSTANCE.getData().values()
                .stream()
                .filter((asteroidConfig) -> asteroidConfig.getName().equals(name))
                .findFirst()
                .orElse(null);
            this.updateSelectedAsteroid = false;
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

    //TODO: override extractRenderStateWithTooltipAndSubtitles?
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
            .append(Component.literal(Utils.formatPlanetSize(this.selectedAsteroid.getDiameter())).withStyle(ChatFormatting.WHITE));

        final boolean hasComposition = !this.selectedAsteroid.getCompositionStacks().isEmpty();
        final Component compositionLabel = Component.translatable("gui.asteroidmining.observatory.composition").withStyle(ChatFormatting.AQUA);

        final boolean hasOrbitDetails = !this.selectedAsteroid.getCentralBodyName().isBlank();
        final String orbitDetailsLabel = Component.translatable("gui.asteroidmining.observatory.display_orbit_details").getString();
        final Component centralBodyLabel = Component.translatable("gui.asteroidmining.observatory.central_body").withStyle(ChatFormatting.AQUA)
            .append(Component.literal(this.selectedAsteroid.getCentralBodyName()).withStyle(ChatFormatting.WHITE));
        final Component semiMayorAxisLabel = Component.translatable("gui.asteroidmining.observatory.semi_mayor_axis").withStyle(ChatFormatting.AQUA)
            .append(Component.literal(Utils.formatAstronomicalUnit(this.selectedAsteroid.getSemiMajorAxis())).withStyle(ChatFormatting.WHITE));
        final Component semiMinorAxisLabel = Component.translatable("gui.asteroidmining.observatory.semi_minor_axis").withStyle(ChatFormatting.AQUA)
            .append(Component.literal(Utils.formatAstronomicalUnit(this.selectedAsteroid.getSemiMinorAxis())).withStyle(ChatFormatting.WHITE));

        //TODO: add orbital trustForce

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
        final int compositionStacksHeight = (this.selectedAsteroid.getCompositionStacks().size() / 5) * 18;
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

        if (!this.selectedAsteroid.getCompositionStacks().isEmpty()) {
            graphics.text(this.font, compositionLabel, 5, y, -1);
            Utils.renderStacksWithSlot(graphics, this.font, mouseX, mouseY, 5, 41, 5,
                detailX, 0, this.selectedAsteroid.getCompositionStacks());
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
        final Matrix3x2fStack poseStack = graphics.pose();

        this.hoveredAsteroid = null;

        for (final AsteroidConfig asteroid : AsteroidReloadListener.INSTANCE.getData().values()) { //TODO: dont render asteroids out of view
            final boolean isSelectedAsteroid = this.selectedAsteroid != null && asteroid.getId().equals(this.selectedAsteroid.getId());

            if (asteroid.isOrbitVisible() || isSelectedAsteroid) {
                this.drawOrbit(graphics,
                    asteroid.getCenter(),
                    asteroid.getSemiMajorAxis(),
                    asteroid.getSemiMinorAxis(),
                    asteroid.getCentralBody() != null ? asteroid.getCentralBody().getRadius() : 0,
                    isSelectedAsteroid);
            }

            asteroid.increaseOrbitalAngle();

            final Point2D position = asteroid.getPosition();

            if (this.followAsteroid && isSelectedAsteroid) {
                if (this.zoomOntoAsteroid) {
                    this.zoom = MAX_ZOOM - ((float) this.selectedAsteroid.getDiameter() / 2);
                }
                this.dragX = -position.getX();
                this.dragY = -position.getY();
            }

            final int size = (int) (asteroid.getDiameter() * this.zoom);
            final float topLeftX = this.getTopLeftXFromCentered(position.getX(), size);
            final float topLeftY = this.getTopLeftYFromCentered(position.getY(), size);

            if (Utils.isMouseOver(topLeftX, topLeftY, size, size, mouseX, mouseY)) {
                this.hoveredAsteroid = asteroid;
            }

            poseStack.pushMatrix();
            poseStack.translate(topLeftX, topLeftY);
            if (asteroid.isShouldRotate()) {
                asteroid.increaseRotation();
                //TODO
                poseStack.rotateAbout(asteroid.getRotation(), size / 2.0f, size / 2.0f);
            }

            graphics.blitSprite(GUI_TEXTURED, asteroid.getTexture(), size, size, 0, 0, 0, 0, size, size);
            if (isSelectedAsteroid) {
                graphics.blitSprite(GUI_TEXTURED, SELECTED, size, size, 0, 0, 0, 0, size, size);
            }

            poseStack.popMatrix();
        }
    }

    private void drawOrbit(final GuiGraphicsExtractor graphics,
                           final Point2D.Double center,
                           final double semiMajorAxis,
                           final double semiMinorAxis,
                           final double centralBodyRadius,
                           final boolean isSelected) {
        final int segments = 100;
        final float[] points = new float[(segments + 1) * 2];

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MIN_VALUE;
        float maxY = -Float.MIN_VALUE;

        for (int i = 0; i <= segments; i++) {
            final double angle = (2.0 * Math.PI * i) / segments;

            final double offsetX = (semiMajorAxis + centralBodyRadius) * Math.cos(angle);
            final double offsetY = (semiMinorAxis + centralBodyRadius) * Math.sin(angle);

            final float x = this.getTopLeftXFromCentered(center.x + offsetX, 0);
            final float y = this.getTopLeftYFromCentered(center.y + offsetY, 0);

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

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF000000);
    }

    private void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        if (this.isMouseOnDetailPanel(mouseX, mouseY)) {
            if (this.selectedAsteroid != null) {
                final int detailX = this.width - this.detailWidth;
                Utils.renderTooltipOfStacks(graphics, mouseX, mouseY, 5, 41, 5,
                    detailX, 0, this.selectedAsteroid.getCompositionStacks());
            }

            return;
        }

        if (this.hoveredAsteroid != null) {
            final List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(this.hoveredAsteroid.getName()));

            final List<ClientTooltipComponent> clientTooltips = ClientHooks.gatherTooltipComponents(ItemStack.EMPTY, tooltip,
                Optional.empty(), mouseX, graphics.guiWidth(), graphics.guiHeight(), this.font);
            Utils.renderStacksInsideTooltip(graphics, this.font, clientTooltips, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE,
                this.hoveredAsteroid.getCompositionStacks());
        }
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
        this.dragX = Mth.clamp(this.dragX, -MIN_MAX_X * this.zoom, MIN_MAX_X * this.zoom); //TODO: improve these calculation, as they are currently not zoom independent
        this.dragY = Mth.clamp(this.dragY, -MIN_MAX_Y * this.zoom, MIN_MAX_Y * this.zoom);

        this.followAsteroid = false;
        this.zoomOntoAsteroid = false;

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

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
    public void mouseMoved(final double mouseX, final double mouseY) {
        this.searchBox.mouseMoved(mouseX, mouseY);
    }

    private boolean isMouseOnDetailPanel(final double mouseX, final double mouseY) {
        return this.selectedAsteroid != null
            && Utils.isMouseOver(this.width - this.detailWidth, 0, this.detailWidth, this.detailHeight, mouseX, mouseY);
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
}
