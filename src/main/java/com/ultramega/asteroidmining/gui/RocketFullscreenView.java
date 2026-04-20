package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.blocks.RocketEngineBlock;
import com.ultramega.asteroidmining.gui.renderer.ScenePictureInPictureRenderer;
import com.ultramega.asteroidmining.gui.widgets.ImageButton;
import com.ultramega.asteroidmining.utils.FakeForwardingServerLevel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import guideme.color.ConstantColor;
import guideme.color.LightDarkMode;
import guideme.document.LytPoint;
import guideme.document.LytRect;
import guideme.document.interaction.GuideTooltip;
import guideme.document.interaction.TextTooltip;
import guideme.scene.CameraSettings;
import guideme.scene.GuidebookLevelRenderer;
import guideme.scene.GuidebookScene;
import guideme.scene.annotation.InWorldAnnotation;
import guideme.scene.annotation.InWorldBoxAnnotation;
import guideme.scene.annotation.SceneAnnotation;
import guideme.scene.level.GuidebookLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public class RocketFullscreenView extends Screen {
    private static final Identifier CLOSE = AsteroidMining.makeId("close");

    private final List<BlockPos> rocketPos;

    @Nullable
    private GuidebookScene scene;
    @Nullable
    private SceneAnnotation hoveredAnnotation;
    private LytRect bounds;
    private float zoom = 2.0f;
    private boolean initialized;

    //TODO: show error info (and rocket details?)
    public RocketFullscreenView(final List<BlockPos> rocketPos) {
        super(Component.empty());
        this.rocketPos = rocketPos;
    }

    @Override
    protected void init() {
        super.init();

        final ImageButton closeButton = new ImageButton(2, 2, 24, 24, 0, 0, CLOSE, (button) ->
            this.onClose());
        closeButton.setActiveTooltip(Component.translatable("gui.asteroidmining.rocket_controller.view.close"));
        closeButton.setRenderBackground(false);
        this.addRenderableWidget(closeButton);
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
        if (!this.initialized) {
            this.scene = new GuidebookScene(new GuidebookLevel(), new CameraSettings());
            this.bounds = new LytRect(0, 0, this.width, this.height);

            try {
                final Level level = Minecraft.getInstance().level;
                if (level == null) {
                    return;
                }

                final FakeForwardingServerLevel wrap = new FakeForwardingServerLevel(this.scene.getLevel());
                final StructureTemplate tmp = new StructureTemplate();
                final StructurePlaceSettings settings = new StructurePlaceSettings();
                final var random = new SingleThreadedRandomSource(0L);
                settings.setIgnoreEntities(true);

                for (final BlockPos pos : this.rocketPos) {
                    tmp.fillFromWorld(level, pos, new Vec3i(1, 1, 1), false, List.of(Blocks.AIR));
                    tmp.placeInWorld(wrap, pos, BlockPos.ZERO, settings, random, Block.UPDATE_CLIENTS);

                    final BlockState state = level.getBlockState(pos);
                    if (!state.isAir()) {
                        // TODO: also support multiblocks/bounding box (use new InWorldBoxAnnotation(minCorner, maxCorner, color) for this instead)!
                        final var annotation = InWorldBoxAnnotation.forBlock(pos, ConstantColor.WHITE);
                        annotation.setTooltip(new TextTooltip(state.getBlock().getName(), this.getAdditionalTooltips(state.getBlock()).toArray(new Component[0])));
                        this.scene.addAnnotation(annotation);
                    }
                }

                this.scene.getCameraSettings().setRotationCenter(this.scene.getWorldCenter());
                this.scene.getCameraSettings().setZoom(this.zoom);
                this.scene.getCameraSettings().setViewportSize(this.bounds.size());
                this.scene.centerScene();

                this.initialized = true;
            } catch (Exception e) {
                AsteroidMining.LOGGER.error(e.getMessage());
            }
        }

        if (this.initialized) {
            ScreenRectangle screenBounds = this.bounds.toScreenRectangle().transformMaxBounds(graphics.pose());
            final ScreenRectangle scissorArea = graphics.peekScissorStack();
            // Pre-apply scissor area
            screenBounds = scissorArea != null ? scissorArea.intersection(screenBounds) : screenBounds;
            if (screenBounds != null) {
                graphics.submitPictureInPictureRenderState(new ScenePictureInPictureRenderer.State(
                    LightDarkMode.LIGHT_MODE,
                    new Matrix3x2f(graphics.pose()),
                    this.bounds.x(),
                    this.bounds.y(),
                    this.bounds.right(),
                    this.bounds.bottom(),
                    screenBounds,
                    scissorArea,
                    (lightDarkMode, _, buffers) -> this.renderViewport(lightDarkMode, buffers)));
            }
        } else {
            final String loadingLabel = Component.translatable("gui.asteroidmining.rocket_controller.view.loading").getString();
            graphics.text(this.font, loadingLabel, (this.width - this.font.width(loadingLabel)) / 2, this.height / 2 + this.font.lineHeight, -1);
        }

        for (final Renderable renderable : this.renderables) {
            renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }

        this.renderAnnotationTooltip(graphics, mouseX, mouseY);
    }

    private void renderViewport(final LightDarkMode lightDarkMode,
                                final MultiBufferSource.BufferSource buffers) {
        final var renderer = GuidebookLevelRenderer.getInstance();

        final Collection<InWorldAnnotation> inWorldAnnotations;
        if (this.hoveredAnnotation instanceof InWorldAnnotation hoveredInWorldAnnotation) {
            inWorldAnnotations = Collections.singletonList(hoveredInWorldAnnotation);
        } else {
            inWorldAnnotations = Collections.emptyList();
        }
        renderer.render(this.scene.getLevel(), this.scene.getCameraSettings(), buffers, inWorldAnnotations, lightDarkMode);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, this.width, this.height, 0xAF000000);
    }

    @Override
    public void mouseMoved(final double mouseX, final double mouseY) {
        if (this.scene == null) {
            return;
        }

        final LytPoint point = new LytPoint((float) mouseX, (float) mouseY);
        this.hoveredAnnotation = this.scene.pickAnnotation(point, this.bounds, SceneAnnotation::hasTooltip);
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        if (this.initialized && this.scene != null) {
            final float dx = (float) dragX;
            final float dy = (float) dragY;
            final CameraSettings camera = this.scene.getCameraSettings();
            if (event.button() == 0) {
                camera.setRotationY(camera.getRotationY() + dx);
                camera.setRotationX(camera.getRotationX() + dy);
            } /*else if (button == 1) { //TODO: remove this?
                camera.setOffsetX(camera.getOffsetX() + dx);
                camera.setOffsetY(camera.getOffsetY() - dy);
            }*/
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        if (this.initialized) {
            this.zoom = (float) Mth.clamp(this.zoom + scrollY / 5, 0.5, 10);
            this.scene.getCameraSettings().setZoom(this.zoom);
            this.scene.centerScene();
            this.mouseMoved(mouseX, mouseY);
            return true;
        }
        return false;
    }

    private List<Component> getAdditionalTooltips(final Block block) {
        final List<Component> tooltips = new ArrayList<>();
        if (block instanceof RocketEngineBlock rocketEngine) {
            tooltips.add(Component.translatable("gui.asteroidmining.rocket_controller.weight", rocketEngine.getType().getWeight())
                .withStyle(ChatFormatting.GOLD));
            tooltips.add(Component.translatable("gui.asteroidmining.rocket_controller.thrust_force", rocketEngine.getType().getThrustForce())
                .withStyle(ChatFormatting.GOLD));
        } else {
            tooltips.add(Component.translatable("gui.asteroidmining.rocket_controller.weight", 1)
                .withStyle(ChatFormatting.GOLD));
        }

        return tooltips;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderAnnotationTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        if (this.hoveredAnnotation != null && this.hoveredAnnotation.getTooltip() != null) {
            this.renderAnnotationTooltip(graphics, this.hoveredAnnotation.getTooltip(), mouseX, mouseY, null);
        }
    }

    /**
     * Copied from {@link guideme.internal.screen.DocumentScreen#renderTooltip(GuiGraphicsExtractor, GuideTooltip, int, int, Identifier)}
     */
    private void renderAnnotationTooltip(final GuiGraphicsExtractor guiGraphics, final GuideTooltip tooltip, final int mouseX, final int mouseY,
                                         final @Nullable Identifier sprite) {
        final var minecraft = Minecraft.getInstance();
        final var clientLines = tooltip.getLines();

        if (clientLines.isEmpty()) {
            return;
        }

        int frameWidth = 0;
        int frameHeight = clientLines.size() == 1 ? -2 : 0;

        for (final var clientTooltipComponent : clientLines) {
            frameWidth = Math.max(frameWidth, clientTooltipComponent.getWidth(minecraft.font));
            frameHeight += clientTooltipComponent.getHeight(font);
        }

        final var icon = tooltip.getIcon();

        if (!icon.isEmpty()) {
            frameWidth += 18;
            frameHeight = Math.max(frameHeight, 18);
        }

        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + frameWidth > this.width) {
            x -= 28 + frameWidth;
        }

        if (y + frameHeight + 6 > this.height) {
            y = this.height - frameHeight - 6;
        }

        TooltipRenderUtil.extractTooltipBackground(guiGraphics, x, y, frameWidth, frameHeight, sprite);

        if (!icon.isEmpty()) {
            x += 18;
        }

        final var bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        int currentY = y;

        // Batch-render tooltip text first
        for (int i = 0; i < clientLines.size(); ++i) {
            final var line = clientLines.get(i);
            line.extractText(guiGraphics, minecraft.font, x, currentY);
            currentY += line.getHeight(this.font) + (i == 0 ? 2 : 0);
        }

        bufferSource.endBatch();

        // Then render tooltip decorations, items, etc.
        currentY = y;
        if (!icon.isEmpty()) {
            guiGraphics.item(icon, x - 18, y);
        }

        for (int i = 0; i < clientLines.size(); ++i) {
            final var line = clientLines.get(i);
            line.extractImage(minecraft.font, x, currentY, frameWidth, frameHeight, guiGraphics);
            currentY += line.getHeight(this.font) + (i == 0 ? 2 : 0);
        }
    }
}
