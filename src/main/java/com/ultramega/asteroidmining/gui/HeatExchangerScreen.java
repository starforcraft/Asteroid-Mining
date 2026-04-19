package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.container.HeatExchangerContainerMenu;
import com.ultramega.asteroidmining.utils.FluidContainerUtil;
import com.ultramega.asteroidmining.utils.Utils;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public class HeatExchangerScreen extends AbstractContainerScreen<HeatExchangerContainerMenu> {
    private static final Identifier BACKGROUND = AsteroidMining.makeId("textures/gui/heat_exchanger.png");
    private static final int ENERGY_BAR_HEIGHT = 52;
    private static final int FLUID_BAR_HEIGHT = 48;

    public HeatExchangerScreen(final HeatExchangerContainerMenu container, final Inventory inventory, final Component title) {
        super(container, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        graphics.text(this.font, this.title, 8, 6, -12566464, false);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        graphics.blit(GUI_TEXTURED, BACKGROUND, getLeftPos(), getTopPos(), 0, 0, getImageWidth(), getImageHeight(), 256, 256);

        // Draw energy bar
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos + 8, this.topPos + 18, 176, 0, 6, ENERGY_BAR_HEIGHT, 256, 256);
        final int energyAmount = this.menu.getEnergyStored();
        final int energyLevel = (int) (energyAmount * (ENERGY_BAR_HEIGHT / (float) this.menu.getMaxEnergyStored()));
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos + 8, this.topPos + 18, 8, 18, 6, ENERGY_BAR_HEIGHT - energyLevel, 256, 256);

        // Draw fluid tanks
        FluidContainerUtil.renderFluidTank(graphics, this, this.menu.blockEntity.fluidTank.getStackInTank(0),
            this.menu.blockEntity.fluidTank.getAmountAsInt(0), 67, 20, 6, FLUID_BAR_HEIGHT, 0);
        FluidContainerUtil.renderFluidTank(graphics, this, this.menu.blockEntity.fluidTank.getStackInTank(1),
            this.menu.blockEntity.fluidTank.getAmountAsInt(1), 115, 20, 6, FLUID_BAR_HEIGHT, 0);

        // Draw gas tank overlay
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos + 67, this.topPos + 20, 101, 182, 0, 2, FLUID_BAR_HEIGHT, 256, 256);

        // Draw progress
        if (this.menu.getRecipeProgress() > 0) {
            final int progress = (int) ((this.menu.getRecipeDuration() - this.menu.getRecipeProgress())
                * (24 / (float) this.menu.getRecipeDuration()));

            graphics.blit(GUI_TEXTURED, BACKGROUND, this.getLeftPos() + 82, this.getTopPos() + 35, 184, 0, progress + 1, 16, 256, 256);
        }
    }

    @Override
    protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);

        if (this.isHovering(7, 18, 8, ENERGY_BAR_HEIGHT + 1, mouseX, mouseY)) {
            graphics.tooltip(this.font, Utils.createTooltip(Component.translatable("gui.asteroidmining.energy",
                this.menu.getEnergyStored())), mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        }

        if (this.isHovering(67, 20, 6, FLUID_BAR_HEIGHT + 1, mouseX, mouseY)) {
            FluidContainerUtil.renderFluidTooltip(graphics, font, this.menu.blockEntity.fluidTank.getStackInTank(0), mouseX, mouseY);
        }
        if (this.isHovering(115, 20, 6, FLUID_BAR_HEIGHT + 1, mouseX, mouseY)) {
            FluidContainerUtil.renderFluidTooltip(graphics, font, this.menu.blockEntity.fluidTank.getStackInTank(1), mouseX, mouseY);
        }
    }
}
