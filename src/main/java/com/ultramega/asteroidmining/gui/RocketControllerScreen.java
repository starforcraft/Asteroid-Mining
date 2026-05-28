package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.blockentities.AbstractModuleBlockEntity;
import com.ultramega.asteroidmining.container.RocketControllerContainerMenu;
import com.ultramega.asteroidmining.gui.widgets.ImageButton;
import com.ultramega.asteroidmining.gui.widgets.RocketViewerWidget;
import com.ultramega.asteroidmining.gui.widgets.SpacePortErrorsWidget;
import com.ultramega.asteroidmining.network.c2s.LaunchRocketPayload;
import com.ultramega.asteroidmining.network.c2s.OpenSelectConfigurationScreenPayload;
import com.ultramega.asteroidmining.registry.ModDataComponentTypes;
import com.ultramega.asteroidmining.storage.ClientConfigurationSavedData;
import com.ultramega.asteroidmining.storage.NetworkConfiguration;
import com.ultramega.asteroidmining.utils.CommonUtils;
import com.ultramega.asteroidmining.utils.LaunchError;
import com.ultramega.asteroidmining.utils.TextColors;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public class RocketControllerScreen extends AbstractModuleScreen<RocketControllerContainerMenu> {
    private static final Identifier BACKGROUND = AsteroidMining.makeId("textures/gui/rocket_controller.png");
    private static final Identifier CONFIGURE = AsteroidMining.makeId("configure");
    private static final Identifier ERROR = AsteroidMining.makeId("error");

    private static final int LAUNCH_ROCKET_BUTTON_WIDTH = 85;
    private static final int LAUNCH_ROCKET_BUTTON_HEIGHT = 18;

    @Nullable
    private SpacePortErrorsWidget spacePortErrorsWidget;

    public RocketControllerScreen(final RocketControllerContainerMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, 223, 182);
        this.inventoryLabelX = -1;
        this.inventoryLabelY = -1;
    }

    @Override
    protected void init() {
        super.init();

        this.addTopLayerWidget(new RocketViewerWidget(
            this.menu,
            this.leftPos + this.imageWidth,
            this.topPos + (this.imageHeight - RocketViewerWidget.HEIGHT) / 2,
            () -> this.width,
            () -> this.height));

        final List<LaunchError> spacePortErrors = this.getSpacePortErrors();
        this.spacePortErrorsWidget = new SpacePortErrorsWidget(
            this.leftPos - this.getTabs().getTabWidth() + (this.imageWidth - RocketViewerWidget.WIDTH) / 2,
            this.topPos + (this.imageHeight - RocketViewerWidget.HEIGHT) / 2,
            () -> this.width,
            () -> this.height,
            spacePortErrors);
        this.addTopLayerWidget(this.spacePortErrorsWidget);

        // TODO: add cancel launch
        final int launchRocketX = this.leftPos + (this.imageWidth - 80) / 2;
        final int launchRocketY = this.topPos + 158;
        final Button launchRocket = Button.builder(Component.translatable("gui.asteroidmining.rocket_controller.launch_rocket"), (button) -> {
            this.getMenu().getBlockEntity().setPlayedTMinusSound(false);
            ClientPacketDistributor.sendToServer(new LaunchRocketPayload(this.getMenu().getBlockEntity().getBlockPos()));
        }).bounds(launchRocketX, launchRocketY, LAUNCH_ROCKET_BUTTON_WIDTH, LAUNCH_ROCKET_BUTTON_HEIGHT).build();
        launchRocket.active = this.canLaunchRocket();
        this.addRenderableWidget(launchRocket);

        if (!spacePortErrors.isEmpty()) {
            final ImageButton errorButton = new ImageButton(launchRocketX + LAUNCH_ROCKET_BUTTON_WIDTH - 8, launchRocketY - LAUNCH_ROCKET_BUTTON_HEIGHT / 2,
                16, 16, 0, 0, ERROR, (button) -> this.spacePortErrorsWidget.openOrClose());
            errorButton.setActiveTooltip(Component.translatable("gui.asteroidmining.rocket_controller.show_errors"));
            errorButton.setRenderBackground(false);
            this.addRenderableWidget(errorButton);
        }

        final ImageButton configureButton = new ImageButton(this.leftPos + this.imageWidth - (24 + 5), this.topPos + 5, 24, 24, 6, 6, CONFIGURE, (button) ->
            ClientPacketDistributor.sendToServer(new OpenSelectConfigurationScreenPayload(this.getMenu().getBlockEntity().getBlockPos())));
        configureButton.setActiveTooltip(Component.translatable("gui.asteroidmining.rocket_controller.configuration"));
        this.addRenderableWidget(configureButton);
    }

    @Override
    public void extractModuleBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0, 0, this.getImageWidth(), this.getImageHeight(), 256, 256);
    }

    @Override
    protected void drawTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);

        final AbstractModuleBlockEntity.SelectedConfigurationResult result = this.menu.getBlockEntity().getSelectedConfigurationResult();
        if (result.hasError()) {
            graphics.text(this.font, result.errorMessage(), this.titleLabelX, this.titleLabelY + 19, TextColors.RED.getHexCode(), true);
        } else if (result.isValid()) {
            final NetworkConfiguration configuration = ClientConfigurationSavedData.INSTANCE.get(result.uuid());
            if (configuration == null) {
                graphics.text(this.font, Component.translatable("gui.asteroidmining.rocket_controller.invalid_configuration"),
                    this.titleLabelX, this.titleLabelY + 19, TextColors.RED.getHexCode(), true);
                return;
            }

            graphics.text(this.font, Component.translatable("gui.asteroidmining.rocket_controller.selected_configuration", configuration.launchPadConfiguration().name()),
                this.titleLabelX, this.titleLabelY + 19, TextColors.GREEN.getHexCode(), true);
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

    private boolean canLaunchRocket() {
        if (!this.getSpacePortErrors().isEmpty()) {
            return false;
        }

        final int selectedConfiguration = this.getMenu().getBlockEntity().getSelectedConfigurationIndex();
        if (selectedConfiguration == -1) {
            return false;
        }

        final ItemResource resource = this.getMenu().getBlockEntity().inventoryHandler.getResource(selectedConfiguration);
        if (resource.isEmpty() || !resource.has(ModDataComponentTypes.CONFIGURATION_PATH_DATA.get())) {
            return false;
        }

        return true;
    }

    public List<LaunchError> getSpacePortErrors() {
        final ClientLevel level = Minecraft.getInstance().level;
        final NetworkConfiguration configuration = this.getMenu().getBlockEntity().getSelectedClientNetworkConfiguration();
        if (configuration == null || level == null) {
            return List.of();
        }

        return CommonUtils.getSpacePortErrors(level, configuration);
    }
}
