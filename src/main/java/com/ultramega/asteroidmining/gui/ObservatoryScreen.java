package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.asteroids.AsteroidConfig;
import com.ultramega.asteroidmining.blockentities.AbstractModuleBlockEntity;
import com.ultramega.asteroidmining.container.ObservatoryContainerMenu;
import com.ultramega.asteroidmining.events.AsteroidReloadListener;
import com.ultramega.asteroidmining.network.c2s.SelectAsteroidPayload;
import com.ultramega.asteroidmining.storage.NetworkConfiguration;
import com.ultramega.asteroidmining.utils.TextColors;

import java.util.Optional;

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

    private final AbstractModuleBlockEntity.SelectedConfigurationResult selectedConfigurationResult;

    @Nullable
    private AsteroidConfig selectedAsteroid;

    public ObservatoryScreen(final ObservatoryContainerMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, 176, 182);
        this.inventoryLabelY = this.imageHeight - 94;
        this.selectedConfigurationResult = this.menu.getBlockEntity().getSelectedConfigurationResult();

        final NetworkConfiguration selectedConfiguration = this.menu.getBlockEntity().getSelectedClientNetworkConfiguration();
        if (selectedConfiguration != null) {
            selectedConfiguration.moduleProperties().selectedAsteroid().ifPresent(this::updateSelectedAsteroid);
        }
    }

    @Override
    protected void init() {
        super.init();

        final Button selectButton = Button.builder(Component.translatable("gui.asteroidmining.select"), (button -> {
            if (!this.selectedConfigurationResult.isValid()) {
                return;
            }

            Minecraft.getInstance().setScreen(new SolarSystemViewScreen(this.selectedAsteroid, (selectedAsteroid -> {
                ClientPacketDistributor.sendToServer(new SelectAsteroidPayload(Optional.ofNullable(selectedAsteroid), this.selectedConfigurationResult.uuid()));
                this.updateSelectedAsteroid(selectedAsteroid);
            }), this));
        })).bounds(this.leftPos + (this.imageWidth - 60) / 2, this.topPos + 70, 60, 18).build();
        selectButton.active = this.selectedConfigurationResult.isValid();
        this.addRenderableWidget(selectButton);
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

        if (this.selectedConfigurationResult.hasError()) {
            final Component message = this.selectedConfigurationResult.errorMessage();
            final int x = this.leftPos + (this.imageWidth - this.font.width(message)) / 2;
            final int y = this.topPos + 40;
            graphics.text(this.font, message, x, y, TextColors.RED.getHexCode(), true);
        }
    }

    @Override
    protected void drawTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
    }

    private void updateSelectedAsteroid(@Nullable final Identifier asteroidId) {
        final Optional<AsteroidConfig> asteroid = AsteroidReloadListener.INSTANCE.getData().values()
            .stream()
            .filter(config -> config.getId().equals(asteroidId))
            .findFirst();
        asteroid.ifPresent(asteroidConfig -> this.selectedAsteroid = asteroidConfig);
    }
}
