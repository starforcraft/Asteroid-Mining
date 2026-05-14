package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.container.RocketControllerConfigurationContainerMenu;
import com.ultramega.asteroidmining.gui.widgets.ImageButton;
import com.ultramega.asteroidmining.gui.widgets.RocketConfigurationWidget;
import com.ultramega.asteroidmining.gui.widgets.RocketViewerWidget;
import com.ultramega.asteroidmining.network.c2s.OpenSaveRocketControllerPayload;
import com.ultramega.asteroidmining.network.c2s.SetSelectConfigurationPayload;
import com.ultramega.asteroidmining.registry.ModDataComponentTypes;
import com.ultramega.asteroidmining.storage.ClientConfigurationSavedData;
import com.ultramega.asteroidmining.storage.NetworkConfiguration;

import java.util.UUID;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public class RocketControllerConfigurationScreen extends AbstractMovableWidgetContainerScreen<RocketControllerConfigurationContainerMenu> {
    private static final Identifier BACKGROUND = AsteroidMining.makeId("textures/gui/rocket_controller_configuration.png");
    private static final Identifier CLOSE = AsteroidMining.makeId("close");
    private static final Identifier RETURN = AsteroidMining.makeId("return");

    private final ImageButton[] selectButtons;

    @Nullable
    private RocketConfigurationWidget rocketConfigurationWidget;

    public RocketControllerConfigurationScreen(final RocketControllerConfigurationContainerMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, 176, 166);
        this.selectButtons = new ImageButton[this.getMenu().getBlockEntity().inventoryHandler.size()];
    }

    @Override
    protected void init() {
        super.init();

        this.rocketConfigurationWidget = this.addTopLayerWidget(new RocketConfigurationWidget(
            this.menu,
            this.leftPos + this.imageWidth,
            this.topPos + (this.imageHeight - RocketViewerWidget.HEIGHT) / 2,
            () -> this.width,
            () -> this.height));

        final ImageButton returnButton = new ImageButton(this.leftPos + this.imageWidth - 26, this.topPos - 12, 20, 20, 4, 4,
            RETURN, (button) -> this.onClose());
        returnButton.setActiveTooltip(Component.translatable("gui.asteroidmining.return"));
        this.addRenderableWidget(returnButton);

        for (int i = 0; i < this.selectButtons.length; i++) {
            final int index = i;
            this.selectButtons[i] = new ImageButton(this.leftPos + this.imageWidth - 25, this.topPos + 17 + (18 * i), 18, 18,
                2, 2, CLOSE, (button) -> {
                ClientPacketDistributor.sendToServer(new SetSelectConfigurationPayload(this.getMenu().getBlockEntity().getBlockPos(), index));
                RocketControllerConfigurationScreen.this.updateSelectButtons(index);
            });
            this.selectButtons[i].setActiveTooltip(Component.translatable("gui.asteroidmining.select"));
            this.selectButtons[i].setDeactiveTooltip(Component.translatable("gui.asteroidmining.selected"));

            this.addRenderableWidget(this.selectButtons[i]);
        }

        this.updateSelectButtons(this.getMenu().getBlockEntity().getSelectedConfigurationIndex());
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0, 0, this.getImageWidth(), this.getImageHeight(), 256, 256);
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);

        final ItemStacksResourceHandler inventory = this.getMenu().getBlockEntity().inventoryHandler;
        for (int i = 0; i < inventory.size(); i++) {
            final ItemResource resource = inventory.getResource(i);
            if (resource.isEmpty()) {
                continue;
            }
            if (!resource.has(ModDataComponentTypes.CONFIGURATION_PATH_DATA.get())) {
                continue;
            }

            final UUID uuid = resource.get(ModDataComponentTypes.CONFIGURATION_PATH_DATA.get());
            if (uuid == null) {
                continue;
            }

            final NetworkConfiguration configuration = ClientConfigurationSavedData.INSTANCE.get(uuid);
            if (configuration == null) {
                continue;
            }

            graphics.text(this.font, configuration.launchPadConfiguration().name(), 27, 21 + (18 * i), -1, true);
        }
    }

    @Override
    protected void drawTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
    }

    private void updateSelectButtons(final int index) {
        for (int i = 0; i < this.selectButtons.length; i++) {
            this.selectButtons[i].active = i != index;
        }
    }

    @Override
    public void onClose() {
        if (this.rocketConfigurationWidget == null) {
            super.onClose();
            return;
        }

        ClientPacketDistributor.sendToServer(new OpenSaveRocketControllerPayload(
            this.getMenu().getBlockEntity().getBlockPos(),
            this.rocketConfigurationWidget.getLaunchCooldownTicks(),
            this.rocketConfigurationWidget.isLaunchOverlaySelected(),
            this.rocketConfigurationWidget.isLaunchCommentatorSelected()));
    }
}
