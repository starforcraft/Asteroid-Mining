package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.container.AbstractModuleContainerMenu;
import com.ultramega.asteroidmining.network.c2s.OpenTabModulePayload;
import com.ultramega.asteroidmining.utils.TextColors;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public abstract class AbstractModuleScreen<T extends AbstractModuleContainerMenu> extends AbstractMovableWidgetContainerScreen<T> {
    private final PagedSideTabs<BlockPos> tabs = new PagedSideTabs<>(this.imageHeight, false, 16);

    private Button upButton;
    private Button downButton;

    public AbstractModuleScreen(final T menu,
                                final Inventory inventory,
                                final Component title,
                                final int imageWidth,
                                final int imageHeight) {
        super(menu, inventory, title, imageWidth, imageHeight);

        final List<BlockPos> connectedModules = new ArrayList<>(this.getMenu().getConnectedModules());
        this.tabs.setSelectedIndex(connectedModules.indexOf(this.getMenu().getBlockEntity().getBlockPos()));
    }

    @Override
    protected void init() {
        super.init();

        this.upButton = Button.builder(Component.literal("∧"), (button) -> {
            AbstractModuleScreen.this.tabs.previousPage();
            AbstractModuleScreen.this.updateTabButtons();
        }).bounds(this.leftPos - 21, this.topPos - 22, 20, 20).build();
        this.addRenderableWidget(this.upButton);
        this.downButton = Button.builder(Component.literal("∨"), (button) -> {
            AbstractModuleScreen.this.tabs.nextPage();
            AbstractModuleScreen.this.updateTabButtons();
        }).bounds(this.leftPos - 21, this.topPos + this.imageHeight + 2, 20, 20).build();
        this.addRenderableWidget(this.downButton);

        this.updateTabButtons();
    }

    @Override
    public final void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);

        final Level level = this.getMenu().getBlockEntity().getLevel();
        if (level == null) {
            return;
        }

        final List<BlockPos> modules = new ArrayList<>(this.getMenu().getConnectedModules());

        this.tabs.renderUnselectedTabs(graphics, modules, this.leftPos - this.tabs.getTabWidth(), this.topPos, mouseX, mouseY,
            (g, pos, iconX, iconY, mx, my, hovered) ->
                this.renderModuleTab(level, g, pos, iconX, iconY, mx, my, hovered));

        this.extractModuleBackground(graphics, mouseX, mouseY, partialTicks);

        this.tabs.renderSelectedTab(graphics, modules, this.leftPos - this.tabs.getTabWidth(), this.topPos, mouseX, mouseY,
            () -> this.isMouseOverMovableWidget(mouseX, mouseY),
            (g, pos, iconX, iconY, mx, my, hovered) ->
                this.renderModuleTab(level, g, pos, iconX, iconY, mx, my, hovered));
        this.updateTabButtons();
    }

    private void renderModuleTab(final Level level,
                                 final GuiGraphicsExtractor graphics,
                                 final BlockPos pos,
                                 final int iconX,
                                 final int iconY,
                                 final int mouseX,
                                 final int mouseY,
                                 final boolean hovered) {
        final ItemStack stack = new ItemStack(level.getBlockState(pos).getBlock());
        graphics.item(stack, iconX, iconY);
        if (hovered) {
            graphics.setTooltipForNextFrame(this.font, this.getTooltipFromContainerItem(stack), stack.getTooltipImage(), stack,
                mouseX, mouseY, stack.get(DataComponents.TOOLTIP_STYLE));
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        final List<BlockPos> modules = new ArrayList<>(this.getMenu().getConnectedModules());
        if (!this.isMouseOverMovableWidget(event.x(), event.y())
            && this.tabs.mouseClickedTab(event, modules, this.leftPos - this.tabs.getTabWidth(), this.topPos, this::openModuleTab)) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void openModuleTab(final int index, final BlockPos blockPos) {
        final double[] cursorX = new double[1];
        final double[] cursorY = new double[1];

        GLFW.glfwGetCursorPos(Minecraft.getInstance().getWindow().handle(), cursorX, cursorY);

        ClientPacketDistributor.sendToServer(new OpenTabModulePayload(blockPos, (int) Math.round(cursorX[0]), (int) Math.round(cursorY[0])));
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
        if (this.inventoryLabelX >= 0 && this.inventoryLabelY >= 0) {
            graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
        }

        if (this.tabs.shouldShowPageControls()) {
            graphics.text(this.font, this.tabs.pageLabel(), -60, (this.imageHeight - this.font.lineHeight) / 2 + 2, TextColors.WHITE.getHexCode(), true);
        }
    }

    private void updateTabButtons() { //TODO: add this to AbstractTabbedMovableWidget?
        this.tabs.update(this.getMenu().getConnectedModules().size());

        this.upButton.visible = this.tabs.shouldShowPageControls();
        this.downButton.visible = this.tabs.shouldShowPageControls();

        this.upButton.active = this.tabs.canPageUp();
        this.downButton.active = this.tabs.canPageDown();
    }

    public PagedSideTabs<BlockPos> getTabs() {
        return this.tabs;
    }

    protected abstract void extractModuleBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks);
}
