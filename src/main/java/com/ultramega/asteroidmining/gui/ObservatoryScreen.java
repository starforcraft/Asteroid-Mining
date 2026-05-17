package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.asteroids.AsteroidConfig;
import com.ultramega.asteroidmining.container.ObservatoryContainerMenu;
import com.ultramega.asteroidmining.events.AsteroidReloadListener;
import com.ultramega.asteroidmining.network.c2s.SelectAsteroidPayload;
import com.ultramega.asteroidmining.storage.NetworkConfiguration;
import com.ultramega.asteroidmining.utils.TextColors;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public class ObservatoryScreen extends AbstractModuleScreen<ObservatoryContainerMenu> {
    private static final Identifier BACKGROUND = AsteroidMining.makeId("textures/gui/observatory.png");

    @Nullable
    private Button selectButton;

    @Nullable
    private AsteroidConfig selectedAsteroid;
    @Nullable
    private UUID selectedConfigurationUUID;

    public ObservatoryScreen(final ObservatoryContainerMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, 176, 182);
        this.inventoryLabelY = this.imageHeight - 94;

        this.updateSelectedConfigurationUUID();
        final NetworkConfiguration selectedConfiguration = this.menu.getBlockEntity().getSelectedClientNetworkConfiguration();
        if (selectedConfiguration != null) {
            selectedConfiguration.moduleProperties().selectedAsteroid().ifPresent(this::updateSelectedAsteroid);
        }
    }

    @Override
    protected void containerTick() {
        this.updateSelectedConfigurationUUID();
        this.updateSelectButton();
    }

    @Override
    protected void init() {
        super.init();

        this.selectButton = Button.builder(Component.translatable("gui.asteroidmining.select"), (button -> {
            if (this.selectedConfigurationUUID == null) {
                return;
            }

            Minecraft.getInstance().setScreen(new SolarSystemViewScreen(this.selectedAsteroid, (selectedAsteroid -> {
                ClientPacketDistributor.sendToServer(new SelectAsteroidPayload(Optional.of(selectedAsteroid), this.selectedConfigurationUUID));

                this.updateSelectedAsteroid(selectedAsteroid);
            }), this));
        })).bounds(this.leftPos + (this.imageWidth - 60) / 2, this.topPos + 70, 60, 18).build();
        this.updateSelectButton();
        this.addRenderableWidget(this.selectButton);
    }

    @Override
    public void extractModuleBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0, 0, this.getImageWidth(), this.getImageHeight(), 256, 256);

        // TODO: overhaul UI
        if (this.selectedAsteroid != null) {
            final String asteroidName = this.selectedAsteroid.getName();

            final int size = 32;
            graphics.blitSprite(GUI_TEXTURED, this.selectedAsteroid.getTexture(), size, size, 0, 0,
                this.leftPos + (this.imageWidth - size) / 2, this.topPos + 20, size, size);

            graphics.text(this.font, asteroidName,
                this.leftPos + (this.imageWidth - this.font.width(asteroidName)) / 2, this.topPos + 55, TextColors.BLACK.getHexCode(), false);
        }

        final String noConfigurationLabel = Component.translatable("gui.asteroidmining.rocket_controller.no_configuration_selected").getString();

        // TODO: also show all other error messages (invalid configuration)
        if (this.selectedConfigurationUUID == null) {
            graphics.text(this.font, noConfigurationLabel,
                this.leftPos + (this.imageWidth - this.font.width(noConfigurationLabel)) / 2, this.topPos + 20, TextColors.RED.getHexCode(), true);
        }
    }

    @Override
    protected void drawTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
    }

    private void updateSelectedConfigurationUUID() {
        this.selectedConfigurationUUID = this.menu.getBlockEntity().getSelectedConfigurationUUID();
    }

    private void updateSelectedAsteroid(final Identifier asteroidId) {
        final Optional<AsteroidConfig> asteroid = AsteroidReloadListener.INSTANCE.getData().values()
            .stream()
            .filter(config -> config.getId().equals(asteroidId))
            .findFirst();
        asteroid.ifPresent(asteroidConfig -> this.selectedAsteroid = asteroidConfig);
    }

    private void updateSelectButton() {
        if (this.selectButton != null) {
            this.selectButton.active = this.selectedConfigurationUUID != null;
        }
    }
}
