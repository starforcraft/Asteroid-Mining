package com.ultramega.asteroidmining.gui.widgets;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.container.RocketControllerContainerMenu;
import com.ultramega.asteroidmining.gui.RocketFullscreenView;
import com.ultramega.asteroidmining.gui.renderer.ScenePictureInPictureRenderer;
import com.ultramega.asteroidmining.registry.ModDataComponentTypes;
import com.ultramega.asteroidmining.storage.ClientConfigurationSavedData;
import com.ultramega.asteroidmining.storage.NetworkConfiguration;
import com.ultramega.asteroidmining.utils.ClientUtils;
import com.ultramega.asteroidmining.utils.FakeForwardingServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.IntSupplier;

import guideme.color.LightDarkMode;
import guideme.document.LytRect;
import guideme.scene.CameraSettings;
import guideme.scene.GuidebookLevelRenderer;
import guideme.scene.GuidebookScene;
import guideme.scene.level.GuidebookLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import static com.ultramega.asteroidmining.utils.ClientUtils.createTooltip;
import static com.ultramega.asteroidmining.utils.CommonUtils.rotateOffset;

public class RocketViewerWidget extends AbstractMovableWidget {
    public static final int WIDTH = 97;
    public static final int HEIGHT = 112;

    private static final Identifier FULLSCREEN_ICON = AsteroidMining.makeId("fullscreen");
    private static final int FULLSCREEN_ICON_SIZE = 16;

    private static final GuidebookLevelRenderer LEVEL_RENDERER = GuidebookLevelRenderer.getInstance();

    private final RocketControllerContainerMenu menu;
    private final List<BlockPos> rocketPos = new ArrayList<>();

    @Nullable
    private GuidebookScene scene;

    public RocketViewerWidget(final RocketControllerContainerMenu menu,
                              final int defaultX,
                              final int defaultY,
                              final IntSupplier screenWidth,
                              final IntSupplier screenHeight) {
        super(MovableWidgetType.ROCKET_VIEWER, defaultX, defaultY, WIDTH, HEIGHT, screenWidth, screenHeight, true);
        this.menu = menu;

        //TODO: these lines are duplicate with RocketControllerBlockEntity
        final int selectedConfiguration = menu.getBlockEntity().getSelectedConfigurationIndex();
        if (selectedConfiguration != -1) {
            final ItemResource resource = menu.getBlockEntity().inventoryHandler.getResource(this.menu.getBlockEntity().getSelectedConfigurationIndex());
            if (resource.isEmpty() || !resource.has(ModDataComponentTypes.CONFIGURATION_PATH_DATA)) {
                return;
            }

            final UUID uuid = resource.get(ModDataComponentTypes.CONFIGURATION_PATH_DATA);
            if (uuid == null) {
                return;
            }

            final NetworkConfiguration configuration = ClientConfigurationSavedData.INSTANCE.get(uuid);
            if (configuration == null) {
                return;
            }

            final BlockPos mainPos = configuration.launchPadConfiguration().mainPos();
            final int width = configuration.launchPadConfiguration().width();
            final int height = configuration.launchPadConfiguration().height();
            final Direction facing = configuration.launchPadConfiguration().facing();

            // TODO: duplicate
            final int minOffset = -(width - 1) / 2;
            final int maxOffset = width / 2;
            for (int dx = minOffset + 4; dx <= maxOffset - 4; dx++) {
                for (int dz = 2; dz <= width - 7; dz++) {
                    for (int dy = 0; dy < height + 1; dy++) { // + 1 because the height starts at the floor
                        final BlockPos rotatedPos = rotateOffset(mainPos.above(dy), facing.getOpposite(), dx, dz);
                        this.rocketPos.add(rotatedPos);
                    }
                }
            }
        }
    }

    @Override
    protected void extractMovableContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FULLSCREEN_ICON, this.getX() + this.getWidth() - FULLSCREEN_ICON_SIZE - 4, this.getY() + 4,
            FULLSCREEN_ICON_SIZE, FULLSCREEN_ICON_SIZE);

        // Render rocket (TODO: mostly duplicate with RocketFullScreenView)
        final Level level = Minecraft.getInstance().level;
        if (level == null || this.menu.getBlockEntity().getSelectedConfigurationIndex() == -1) {
            return;
        }

        // TODO: somehow cache the scene or all rocket pos so that the rocket is still visible in the ui when launched
        if (this.scene == null) {
            this.scene = new GuidebookScene(new GuidebookLevel(), new CameraSettings());
        }
        final FakeForwardingServerLevel wrap = new FakeForwardingServerLevel(this.scene.getLevel());
        StructureTemplate template = new StructureTemplate();
        final StructurePlaceSettings settings = new StructurePlaceSettings();
        final var random = new SingleThreadedRandomSource(0L);
        settings.setIgnoreEntities(true);
        try {
            for (final BlockPos pos : this.rocketPos) {
                template.fillFromWorld(level, pos, new Vec3i(1, 1, 1), false, List.of(Blocks.AIR));
                template.placeInWorld(wrap, pos, BlockPos.ZERO, settings, random, 0);
                template = new StructureTemplate();
            }
        } catch (Throwable ignored) {
            this.scene = new GuidebookScene(new GuidebookLevel(), new CameraSettings());
        }
        this.scene.getCameraSettings().setRotationCenter(this.scene.getWorldCenter());
        this.scene.getCameraSettings().setZoom(1.0F);
        final LytRect bounds = new LytRect(this.getX(), this.getY(), this.getWidth(), this.getHeight());
        this.scene.getCameraSettings().setViewportSize(bounds.size());
        this.scene.centerScene();

        ScreenRectangle screenBounds = bounds.toScreenRectangle().transformMaxBounds(graphics.pose());
        final ScreenRectangle scissorArea = graphics.peekScissorStack();
        // Pre-apply scissor area
        screenBounds = scissorArea != null ? scissorArea.intersection(screenBounds) : screenBounds;
        if (screenBounds != null) {
            graphics.submitPictureInPictureRenderState(new ScenePictureInPictureRenderer.State(
                LightDarkMode.LIGHT_MODE,
                new Matrix3x2f(graphics.pose()),
                bounds.x(),
                bounds.y(),
                bounds.right(),
                bounds.bottom(),
                screenBounds,
                scissorArea,
                (lightDarkMode, _, buffers) ->
                    LEVEL_RENDERER.render(this.scene.getLevel(), this.scene.getCameraSettings(), buffers, List.of(), lightDarkMode)));
        }
    }

    @Override
    protected void extractTooltips(final GuiGraphicsExtractor graphics, final Font font, final int mouseX, final int mouseY) {
        if (ClientUtils.isMouseOver(this.getX() + this.getWidth() - FULLSCREEN_ICON_SIZE - 4, this.getY() + 4, FULLSCREEN_ICON_SIZE, FULLSCREEN_ICON_SIZE, mouseX, mouseY)) {
            final var tooltip = createTooltip(List.of(Component.translatable("gui.asteroidmining.rocket_controller.fullscreen")));
            graphics.tooltip(font, tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        }
    }

    @Override
    protected boolean mouseClickedInside(final MouseButtonEvent event, final boolean doubleClick) {
        if (ClientUtils.isMouseOver(this.getX() + this.getWidth() - FULLSCREEN_ICON_SIZE - 4, this.getY() + 4, FULLSCREEN_ICON_SIZE, FULLSCREEN_ICON_SIZE, event.x(), event.y())) {
            Minecraft.getInstance().pushGuiLayer(new RocketFullscreenView(this.rocketPos));
            return true;
        }
        return false;
    }
}
