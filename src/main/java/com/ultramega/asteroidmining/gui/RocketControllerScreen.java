package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.container.RocketControllerContainerMenu;
import com.ultramega.asteroidmining.gui.renderer.ScenePictureInPictureRenderer;
import com.ultramega.asteroidmining.gui.widgets.ImageButton;
import com.ultramega.asteroidmining.network.c2s.LaunchRocketMessage;
import com.ultramega.asteroidmining.network.c2s.OpenSelectConfigurationScreenMessage;
import com.ultramega.asteroidmining.registry.ModDataComponentTypes;
import com.ultramega.asteroidmining.storage.ClientConfigurationSavedData;
import com.ultramega.asteroidmining.storage.NetworkConfiguration;
import com.ultramega.asteroidmining.utils.FakeForwardingServerLevel;
import com.ultramega.asteroidmining.utils.TextColors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import guideme.color.LightDarkMode;
import guideme.document.LytRect;
import guideme.scene.CameraSettings;
import guideme.scene.GuidebookLevelRenderer;
import guideme.scene.GuidebookScene;
import guideme.scene.level.GuidebookLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import static com.ultramega.asteroidmining.utils.Utils.rotateOffset;
import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public class RocketControllerScreen extends AbstractModuleScreen<RocketControllerContainerMenu> {
    private static final Identifier BACKGROUND = AsteroidMining.makeId("textures/gui/rocket_controller.png");
    private static final Identifier SIDE_PANEL = AsteroidMining.makeId("textures/gui/gray_side_panel.png");
    private static final Identifier CONFIGURE = AsteroidMining.makeId("configure");
    private static final Identifier FULLSCREEN = AsteroidMining.makeId("fullscreen");
    private static final Identifier ERROR = AsteroidMining.makeId("error");
    private static final int SIDE_PANEL_WIDTH = 97;
    private static final int SIDE_PANEL_HEIGHT = 112;

    private static final GuidebookLevelRenderer LEVEL_RENDERER = GuidebookLevelRenderer.getInstance();

    private final List<BlockPos> rocketPos = new ArrayList<>();

    @Nullable
    private GuidebookScene scene;

    public RocketControllerScreen(final RocketControllerContainerMenu container, final Inventory inventory, final Component title) {
        super(container, inventory, title, 223, 182);
        this.inventoryLabelX = -1;
        this.inventoryLabelY = -1;

        //TODO: these lines are duplicate with RocketControllerBlockEntity
        final int selectedConfiguration = this.menu.getBlockEntity().getSelectedConfigurationIndex();
        if (selectedConfiguration != -1) {
            final ItemResource resource = this.menu.getBlockEntity().inventoryHandler.getResource(this.menu.getBlockEntity().getSelectedConfigurationIndex());
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
    protected void init() {
        super.init();

        // TODO: add cancel launch
        // TODO: disable button if errors are present
        this.addRenderableWidget(Button.builder(Component.translatable("gui.asteroidmining.rocket_controller.launch_rocket"), (button) -> {
            this.getMenu().getBlockEntity().playedTMinusSound = false;
            ClientPacketDistributor.sendToServer(new LaunchRocketMessage(this.getMenu().getBlockEntity().getBlockPos()));
        }).bounds(this.leftPos + (this.imageWidth - 80) / 2, this.topPos + 158, 85, 18).build());
        final ImageButton configureButton = new ImageButton(this.leftPos + this.imageWidth - (24 + 5), this.topPos + 5, 24, 24, 6, 6, CONFIGURE, (button) ->
            ClientPacketDistributor.sendToServer(new OpenSelectConfigurationScreenMessage(this.getMenu().getBlockEntity().getBlockPos())));
        configureButton.setActiveTooltip(Component.translatable("gui.asteroidmining.rocket_controller.configuration"));
        this.addRenderableWidget(configureButton);

        final ImageButton fullscreenButton = new ImageButton(this.leftPos + this.imageWidth + SIDE_PANEL_WIDTH - 23, this.topPos + 40 + 2, 24, 24, 6, 6, FULLSCREEN, (button) ->
            Minecraft.getInstance().pushGuiLayer(new RocketFullscreenView(this.rocketPos))
        );
        fullscreenButton.setActiveTooltip(Component.translatable("gui.asteroidmining.rocket_controller.fullscreen"));
        fullscreenButton.setRenderBackground(false);
        this.addRenderableWidget(fullscreenButton);

        // TODO: implement this
        if (false) {
            final ImageButton errorButton = new ImageButton(this.leftPos + 5, this.topPos - 19, 24, 24, 0, 0, ERROR, (button) ->
                Minecraft.getInstance().pushGuiLayer(new RocketControllerErrorScreen())
            );
            errorButton.setActiveTooltip(Component.translatable("gui.asteroidmining.rocket_controller.show_errors"));
            errorButton.setRenderBackground(false);
            this.addRenderableWidget(errorButton);
        }
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0, 0, this.getImageWidth(), this.getImageHeight(), 256, 256);

        final int sidePanelX = this.leftPos + this.imageWidth + 3;
        final int sidePanelY = this.topPos + 40; //TODO center this vertically
        graphics.blit(GUI_TEXTURED, SIDE_PANEL, sidePanelX, sidePanelY, 0, 0, SIDE_PANEL_WIDTH, SIDE_PANEL_HEIGHT, 256, 256);

        // Render rocket (TODO: mostly duplicate with RocketFullScreenView)
        final Level level = Minecraft.getInstance().level;
        if (level == null || this.getMenu().getBlockEntity().getSelectedConfigurationIndex() == -1) {
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
        final LytRect bounds = new LytRect(sidePanelX, sidePanelY, SIDE_PANEL_WIDTH, SIDE_PANEL_HEIGHT);
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
                    GuidebookLevelRenderer.getInstance().render(this.scene.getLevel(), this.scene.getCameraSettings(), buffers, List.of(), lightDarkMode)));
        }

        //TODO: above causes Rocket Storage Viewer tooltip being black with no text
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);

        //TODO: refactor this
        final int selectedConfiguration = this.getMenu().getBlockEntity().getSelectedConfigurationIndex();
        if (selectedConfiguration == -1) {
            graphics.text(this.font, Component.translatable("gui.asteroidmining.rocket_controller.no_configuration_selected"),
                this.titleLabelX, this.titleLabelY + 19, TextColors.RED.getHexCode(), true);
        } else {
            final ItemResource resource = this.getMenu().getBlockEntity().inventoryHandler.getResource(selectedConfiguration);
            if (resource.isEmpty() || !resource.has(ModDataComponentTypes.CONFIGURATION_PATH_DATA.get())) {
                graphics.text(this.font, Component.translatable("gui.asteroidmining.rocket_controller.invalid_configuration"),
                    this.titleLabelX, this.titleLabelY + 19, TextColors.RED.getHexCode(), true);
            } else {
                final UUID uuid = resource.get(ModDataComponentTypes.CONFIGURATION_PATH_DATA.get());
                if (uuid == null) {
                    graphics.text(this.font, Component.translatable("gui.asteroidmining.rocket_controller.invalid_configuration"),
                        this.titleLabelX, this.titleLabelY + 19, TextColors.RED.getHexCode(), true);
                } else {
                    final NetworkConfiguration configuration = ClientConfigurationSavedData.INSTANCE.get(uuid);
                    if (configuration == null) {
                        graphics.text(this.font, Component.translatable("gui.asteroidmining.rocket_controller.invalid_configuration"),
                            this.titleLabelX, this.titleLabelY + 19, TextColors.RED.getHexCode(), true);
                    } else {
                        graphics.text(this.font, Component.translatable("gui.asteroidmining.rocket_controller.selected_configuration", configuration.launchPadConfiguration().name()),
                            this.titleLabelX, this.titleLabelY + 19, TextColors.GREEN.getHexCode(), true);
                    }
                }
            }
        }

        final NetworkConfiguration configuration = this.getMenu().getBlockEntity().getSelectedClientNetworkConfiguration();

        String weight = "x";
        String thrustForce = "x";

        // TODO: add consumption rates

        if (configuration != null && configuration.rocketProperties().isPresent()) {
            weight = String.valueOf(configuration.rocketProperties().get().weight());
            thrustForce = String.valueOf(configuration.rocketProperties().get().trustForce());
        }

        graphics.text(this.font, Component.translatable("gui.asteroidmining.rocket_controller.weight", weight),
            this.titleLabelX, this.titleLabelY + 19 + 16 + 2, -1, false);
        graphics.text(this.font, Component.translatable("gui.asteroidmining.rocket_controller.thrust_force", thrustForce),
            this.titleLabelX, this.titleLabelY + 19 + 16 * 2, -1, false);
    }
}
