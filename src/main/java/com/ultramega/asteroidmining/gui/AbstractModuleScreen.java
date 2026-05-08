package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.container.AbstractModuleContainerMenu;
import com.ultramega.asteroidmining.network.c2s.OpenTabModuleMessage;
import com.ultramega.asteroidmining.utils.TextColors;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public abstract class AbstractModuleScreen<T extends AbstractModuleContainerMenu> extends AbstractContainerScreen<T> {
    private static final Identifier SELECTED_TAB_TOP = AsteroidMining.makeId("selected_tab_top");
    private static final Identifier SELECTED_TAB_BOTTOM = AsteroidMining.makeId("selected_tab_bottom");
    private static final Identifier SELECTED_TAB = AsteroidMining.makeId("selected_tab");
    private static final Identifier UNSELECTED_TAB = AsteroidMining.makeId("unselected_tab");

    private static final int MAX_SHOWN_TABS = 7;

    private int hoveredTab;
    private int selectedTab;
    private int totalPages;
    private int currentTabPage;

    private Button upButton;
    private Button downButton;

    public AbstractModuleScreen(final T menu,
                                final Inventory inventory,
                                final Component title,
                                final int imageWidth,
                                final int imageHeight) {
        super(menu, inventory, title, imageWidth, imageHeight);

        final List<BlockPos> connectedModules = new ArrayList<>(this.getMenu().getConnectedModules());
        this.selectedTab = connectedModules.indexOf(this.getMenu().getBlockEntity().getBlockPos());
        if (this.selectedTab >= MAX_SHOWN_TABS) {
            this.currentTabPage = this.selectedTab / MAX_SHOWN_TABS;
        }
    }

    @Override
    protected void init() {
        super.init();

        this.upButton = Button.builder(Component.literal("∧"), (button) ->
            AbstractModuleScreen.this.currentTabPage--
        ).bounds(this.leftPos - 21, this.topPos - 22, 20, 20).build();
        this.addRenderableWidget(this.upButton);
        this.downButton = Button.builder(Component.literal("∨"), (button) ->
            AbstractModuleScreen.this.currentTabPage++
        ).bounds(this.leftPos - 21, this.topPos + this.imageHeight + 2, 20, 20).build();
        this.addRenderableWidget(this.downButton);

        this.updateTabButtons();
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks); //TODO?

        // Render left side tabs
        final Level level = this.getMenu().getBlockEntity().getLevel();
        if (level != null) {
            final List<BlockPos> modules = new ArrayList<>(this.getMenu().getConnectedModules());
            this.totalPages = (int) Math.ceil((double) modules.size() / MAX_SHOWN_TABS);

            this.currentTabPage = Math.clamp(this.currentTabPage, 0, this.totalPages - 1);
            this.hoveredTab = -1;
            this.updateTabButtons();

            final int startIndex = this.currentTabPage * MAX_SHOWN_TABS;
            final int endIndex = Math.min(startIndex + MAX_SHOWN_TABS, modules.size());
            for (int i = startIndex; i < endIndex; i++) {
                final int tabIndexOnPage = i - startIndex;

                final BlockPos pos = modules.get(i);
                final ItemStack stack = new ItemStack(level.getBlockState(pos).getBlock()); //TODO: maybe cache this?
                final boolean selected = i == this.selectedTab;

                final int x = this.leftPos - 28;
                final int y = this.topPos + (26 * tabIndexOnPage);

                final Matrix3x2fStack poseStack = graphics.pose();
                poseStack.pushMatrix();

                graphics.blitSprite(GUI_TEXTURED, selected
                    ? (tabIndexOnPage == 0 ? SELECTED_TAB_TOP : (tabIndexOnPage == MAX_SHOWN_TABS - 1 ? SELECTED_TAB_BOTTOM : SELECTED_TAB))
                    : UNSELECTED_TAB, x, y, 32, 26);
                graphics.item(stack, x + 9, y + 5);

                poseStack.popMatrix();

                if (this.isHovering(x - this.leftPos + 3, y - this.topPos + 2, 24, 22, mouseX, mouseY)) {
                    graphics.setTooltipForNextFrame(this.font, this.getTooltipFromContainerItem(stack), stack.getTooltipImage(),
                        stack, mouseX, mouseY, stack.get(DataComponents.TOOLTIP_STYLE));
                    this.hoveredTab = i;
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.hoveredTab != -1) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.selectedTab = this.hoveredTab;

            // Open module menu
            final double[] cursorX = new double[1];
            final double[] cursorY = new double[1];

            GLFW.glfwGetCursorPos(Minecraft.getInstance().getWindow().handle(), cursorX, cursorY);

            this.getMenu().getConnectedModules().stream()
                .skip(this.selectedTab)
                .findFirst()
                .ifPresent(blockPos ->
                    ClientPacketDistributor.sendToServer(new OpenTabModuleMessage(blockPos, (int) Math.round(cursorX[0]), (int) Math.round(cursorY[0]))));
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
        if (this.inventoryLabelX >= 0 && this.inventoryLabelY >= 0) {
            graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
        }

        if (this.shouldShowSideBar()) {
            graphics.text(this.font, Component.translatable("%s / %s", this.currentTabPage + 1, this.totalPages),
                -60, (this.imageHeight - this.font.lineHeight) / 2 + 2, TextColors.WHITE.getHexCode(), true);
        }
    }

    private void updateTabButtons() {
        this.upButton.visible = this.shouldShowSideBar();
        this.downButton.visible = this.shouldShowSideBar();
    }

    private boolean shouldShowSideBar() {
        return this.totalPages != 1 && this.totalPages != 0;
    }
}
