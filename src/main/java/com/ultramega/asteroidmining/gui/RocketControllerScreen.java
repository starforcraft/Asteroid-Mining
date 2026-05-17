package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.container.RocketControllerContainerMenu;
import com.ultramega.asteroidmining.gui.widgets.ImageButton;
import com.ultramega.asteroidmining.gui.widgets.RocketViewerWidget;
import com.ultramega.asteroidmining.network.c2s.LaunchRocketPayload;
import com.ultramega.asteroidmining.network.c2s.OpenSelectConfigurationScreenPayload;
import com.ultramega.asteroidmining.registry.ModDataComponentTypes;
import com.ultramega.asteroidmining.storage.ClientConfigurationSavedData;
import com.ultramega.asteroidmining.storage.NetworkConfiguration;
import com.ultramega.asteroidmining.utils.TextColors;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public class RocketControllerScreen extends AbstractModuleScreen<RocketControllerContainerMenu> {
    private static final Identifier BACKGROUND = AsteroidMining.makeId("textures/gui/rocket_controller.png");
    private static final Identifier CONFIGURE = AsteroidMining.makeId("configure");
    private static final Identifier ERROR = AsteroidMining.makeId("error");

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

        // TODO: add cancel launch
        // TODO: disable button if errors are present
        this.addRenderableWidget(Button.builder(Component.translatable("gui.asteroidmining.rocket_controller.launch_rocket"), (button) -> {
            this.getMenu().getBlockEntity().setPlayedTMinusSound(false);
            ClientPacketDistributor.sendToServer(new LaunchRocketPayload(this.getMenu().getBlockEntity().getBlockPos()));
        }).bounds(this.leftPos + (this.imageWidth - 80) / 2, this.topPos + 158, 85, 18).build());
        final ImageButton configureButton = new ImageButton(this.leftPos + this.imageWidth - (24 + 5), this.topPos + 5, 24, 24, 6, 6, CONFIGURE, (button) ->
            ClientPacketDistributor.sendToServer(new OpenSelectConfigurationScreenPayload(this.getMenu().getBlockEntity().getBlockPos())));
        configureButton.setActiveTooltip(Component.translatable("gui.asteroidmining.rocket_controller.configuration"));
        this.addRenderableWidget(configureButton);

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
    public void extractModuleBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0, 0, this.getImageWidth(), this.getImageHeight(), 256, 256);
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
                        graphics.text(this.font, Component.translatable("gui.asteroidmining.rocket_controller.selected_configuration",
                                configuration.launchPadConfiguration().name()), this.titleLabelX, this.titleLabelY + 19, TextColors.GREEN.getHexCode(), true);
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

    @Override
    protected void drawTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
    }
}
