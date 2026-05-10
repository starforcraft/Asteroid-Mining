package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.container.SelectConfigurationContainerMenu;
import com.ultramega.asteroidmining.gui.widgets.ImageButton;
import com.ultramega.asteroidmining.network.c2s.OpenSaveRocketControllerPayload;
import com.ultramega.asteroidmining.network.c2s.SetSelectConfigurationPayload;
import com.ultramega.asteroidmining.registry.ModDataComponentTypes;
import com.ultramega.asteroidmining.storage.ClientConfigurationSavedData;
import com.ultramega.asteroidmining.storage.NetworkConfiguration;

import java.util.UUID;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.gui.widget.ExtendedSlider;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public class RocketControllerConfigurationScreen extends AbstractContainerScreen<SelectConfigurationContainerMenu> {
    private static final Identifier BACKGROUND = AsteroidMining.makeId("textures/gui/rocket_controller_configuration.png");
    private static final Identifier SIDE_PANEL = AsteroidMining.makeId("textures/gui/white_side_panel.png");
    private static final Identifier CLOSE = AsteroidMining.makeId("close");
    private static final Identifier RETURN = AsteroidMining.makeId("return");

    private final ImageButton[] selectButtons;

    private ExtendedSlider cooldownSlider;
    private Checkbox overlayCheckbox;
    private Checkbox commentatorCheckbox;

    public RocketControllerConfigurationScreen(final SelectConfigurationContainerMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, 176, 166);
        this.selectButtons = new ImageButton[this.getMenu().getBlockEntity().inventoryHandler.size()];
    }

    @Override
    protected void init() {
        super.init();

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

        this.cooldownSlider = new ExtendedSlider(this.leftPos + this.imageWidth + 10, this.topPos + 50, 82, 16,
            Component.empty(), Component.literal("s"), 0, 120, (int) (this.menu.getLaunchCooldown() / 20), 1, 0, true);
        this.addRenderableWidget(this.cooldownSlider);

        //TODO: find better name
        this.overlayCheckbox = Checkbox.builder(Component.translatable("gui.asteroidmining.rocket_controller.configuration.overlay"), this.font)
            .pos(this.leftPos + this.imageWidth + 10, this.topPos + 70)
            .selected(this.menu.isLaunchCooldownOverlay())
            .build();
        this.overlayCheckbox.textWidget.setFGColor(-1);
        this.addRenderableWidget(this.overlayCheckbox);

        //TODO: find better name (Sound/Announcement?)
        this.commentatorCheckbox = Checkbox.builder(Component.translatable("gui.asteroidmining.rocket_controller.configuration.commentator"), this.font)
            .pos(this.leftPos + this.imageWidth + 10, this.topPos + 70 + 20)
            .selected(this.menu.isLaunchCooldownCommentator())
            .build();
        this.commentatorCheckbox.textWidget.setFGColor(-1);
        this.addRenderableWidget(this.commentatorCheckbox);
    }

    private void updateSelectButtons(final int index) {
        for (int i = 0; i < this.selectButtons.length; i++) {
            this.selectButtons[i].active = i != index;
        }
    }

    @Override
    public void onClose() {
        ClientPacketDistributor.sendToServer(new OpenSaveRocketControllerPayload(
            this.getMenu().getBlockEntity().getBlockPos(),
            (int) this.cooldownSlider.getValue() * 20,
            this.overlayCheckbox.selected(),
            this.commentatorCheckbox.selected()));
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0, 0, this.getImageWidth(), this.getImageHeight(), 256, 256);
        graphics.blit(GUI_TEXTURED, SIDE_PANEL, this.leftPos + this.imageWidth + 3, this.topPos + 25, 0, 0, 97, 112, 256, 256); //TODO center this
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
        // TODO: rename to only "Cooldown"?
        graphics.text(this.font, Component.translatable("gui.asteroidmining.rocket_controller.configuration.cooldown_settings"),
            this.imageWidth + 7, 31, -12566464, false);

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
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        if (this.cooldownSlider.isMouseOver(event.x(), event.y())) {
            this.cooldownSlider.mouseDragged(event, dragX, dragY);
        }
        return super.mouseDragged(event, dragX, dragY);
    }
}
