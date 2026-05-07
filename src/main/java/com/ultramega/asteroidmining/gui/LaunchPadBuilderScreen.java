package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.blocks.AbstractFacingBlock;
import com.ultramega.asteroidmining.camera.CameraHandler;
import com.ultramega.asteroidmining.container.LaunchPadBuilderContainerMenu;
import com.ultramega.asteroidmining.gui.widgets.PlaceholderEditBox;
import com.ultramega.asteroidmining.network.c2s.SetConfigurationStackMessage;
import com.ultramega.asteroidmining.registry.ModDataComponentTypes;
import com.ultramega.asteroidmining.storage.ClientConfigurationSavedData;
import com.ultramega.asteroidmining.storage.LaunchPadConfiguration;
import com.ultramega.asteroidmining.storage.NetworkConfiguration;
import com.ultramega.asteroidmining.utils.Utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public class LaunchPadBuilderScreen extends AbstractContainerScreen<LaunchPadBuilderContainerMenu> {
    private static final Identifier BACKGROUND = AsteroidMining.makeId("textures/gui/launch_pad_builder.png");

    private Button configureButton;
    private PlaceholderEditBox nameEditBox;

    public LaunchPadBuilderScreen(final LaunchPadBuilderContainerMenu container, final Inventory inventory, final Component title) {
        super(container, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        this.configureButton = Button.builder(Component.translatable("gui.asteroidmining.launch_pad_builder.configure"), (button) -> {
            this.confirm();
            this.updateCamera(this.getRocketInfo());
            Minecraft.getInstance().setScreen(new LaunchPadConfigureScreen(this));
        }).bounds(this.leftPos + (this.imageWidth - 60) / 2, this.topPos + 57, 60, 16).build();
        this.addRenderableWidget(this.configureButton);

        this.nameEditBox = new PlaceholderEditBox(
            this.font,
            this.leftPos + (this.imageWidth - 130) / 2,
            this.topPos + 38,
            130,
            16,
            22,
            Component.translatable("gui.asteroidmining.launch_pad_configure.configuration_name"),
            Component.literal(this.getRocketInfo().name())) {
            @Override
            public void onValueChange(final String newText) {
                super.onValueChange(newText);

                if (!newText.isEmpty()) {
                    LaunchPadBuilderScreen.this.rename(newText);
                }
            }
        };
        this.addRenderableWidget(this.nameEditBox);

        this.updateWidgets();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.updateWidgets();
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        graphics.text(this.font, this.title, 8, 6, -12566464, false);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.getLeftPos(), this.getTopPos(), 0, 0, this.getImageWidth(), this.getImageHeight(), 256, 256);
    }

    @Override
    protected void slotClicked(final Slot slot, final int slotId, final int buttonNum, final ContainerInput containerInput) {
        super.slotClicked(slot, slotId, buttonNum, containerInput);

        this.updateNameEditBox();
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (this.nameEditBox.isFocused()) {
            this.nameEditBox.keyPressed(event);
            return false;
        }
        return super.keyPressed(event);
    }

    private void updateWidgets() {
        this.configureButton.active = !this.menu.getSlot(0).getItem().isEmpty() && !this.nameEditBox.getValue().isEmpty();
    }

    public void confirm() {
        this.sendUpdate(this.getRocketInfo(), false);
    }

    public void move(final Direction direction) {
        final LaunchPadConfiguration launchPadConfiguration = this.getRocketInfo();
        final Direction facing = launchPadConfiguration.facing();

        final Direction relativeDirection = switch (direction) {
            case EAST -> facing.getClockWise();
            case WEST -> facing.getCounterClockWise();
            case NORTH -> facing;
            case SOUTH -> facing.getOpposite();
            default -> throw new IllegalArgumentException("Unsupported direction: " + direction);
        };

        this.sendUpdate(new LaunchPadConfiguration(
            launchPadConfiguration.name(),
            launchPadConfiguration.mainPos().relative(relativeDirection),
            launchPadConfiguration.width(),
            launchPadConfiguration.height(),
            launchPadConfiguration.facing()
        ), true);
    }

    public void resize(final boolean isWidth, final int amount) {
        final LaunchPadConfiguration launchPadConfiguration = this.getRocketInfo();

        final int newWidth = Math.clamp(launchPadConfiguration.width() + (isWidth ? amount : 0), 9, 21);
        final int newHeight = Math.clamp(launchPadConfiguration.height() + (!isWidth ? amount : 0), 9, 21);

        this.sendUpdate(new LaunchPadConfiguration(launchPadConfiguration.name(), launchPadConfiguration.mainPos(),
            newWidth, newHeight, launchPadConfiguration.facing()), true);
    }

    public void rotate() {
        final LaunchPadConfiguration launchPadConfiguration = this.getRocketInfo();
        this.sendUpdate(new LaunchPadConfiguration(
            launchPadConfiguration.name(), Utils.rotateCounterClockwise(this.menu.getBlockEntity().getBlockPos(), launchPadConfiguration.mainPos()),
            launchPadConfiguration.width(), launchPadConfiguration.height(), launchPadConfiguration.facing().getClockWise()), true);
    }

    public void rename(final String name) {
        final LaunchPadConfiguration launchPadConfiguration = this.getRocketInfo();
        this.sendUpdate(new LaunchPadConfiguration(name, launchPadConfiguration.mainPos(),
            launchPadConfiguration.width(), launchPadConfiguration.height(), launchPadConfiguration.facing()), false);
    }

    private void sendUpdate(final LaunchPadConfiguration launchPadConfiguration, final boolean updateCamera) {
        ClientPacketDistributor.sendToServer(new SetConfigurationStackMessage(this.menu.getBlockEntity().getBlockPos(), launchPadConfiguration));
        if (updateCamera) {
            this.updateCamera(launchPadConfiguration);
        }
    }

    private void updateCamera(final LaunchPadConfiguration launchPadConfiguration) {
        CameraHandler.setPosition(this.getCameraPos(launchPadConfiguration), launchPadConfiguration.facing());
    }

    private BlockPos getCameraPos(@Nullable final LaunchPadConfiguration launchPadConfiguration) {
        final LaunchPadConfiguration pos = launchPadConfiguration != null ? launchPadConfiguration : this.getRocketInfo();

        final double diagonalOffset = (pos.width() + pos.height()) * Math.sin(Math.toRadians(45));
        final double distanceBack = diagonalOffset * 1.1;
        final double distanceUp = diagonalOffset * 1.1;

        return pos.mainPos()
            .relative(pos.facing().getOpposite(), (int) Math.ceil(distanceBack))
            .above((int) Math.ceil(distanceUp));
    }

    private LaunchPadConfiguration getRocketInfo() {
        final ItemStack stack = this.menu.getSlot(0).getItem();
        if (stack.has(ModDataComponentTypes.CONFIGURATION_PATH_DATA.get())) {
            final NetworkConfiguration configuration = ClientConfigurationSavedData.INSTANCE.get(stack.get(ModDataComponentTypes.CONFIGURATION_PATH_DATA.get()));
            if (configuration != null) {
                return configuration.launchPadConfiguration();
            }
        }

        final BlockEntity blockEntity = this.menu.getBlockEntity();
        final Direction facing = blockEntity.getBlockState().getValue(AbstractFacingBlock.FACING);
        final BlockPos targetPos = blockEntity.getBlockPos().relative(facing.getOpposite());
        return new LaunchPadConfiguration("", targetPos, 9, 9, facing);
    }

    public void updateNameEditBox() {
        this.nameEditBox.setValue(this.menu.getSlot(0).getItem().isEmpty() ? "" : this.getRocketInfo().name());
    }

    @Override
    public void onClose() {
        super.onClose();
    }
}
