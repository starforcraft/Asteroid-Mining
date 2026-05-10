package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.container.AirAbsorberContainerMenu;
import com.ultramega.asteroidmining.utils.ClientUtils;
import com.ultramega.asteroidmining.utils.FluidContainerUtil;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public class AirAbsorberScreen extends AbstractContainerScreen<AirAbsorberContainerMenu> {
    private static final Identifier BACKGROUND = AsteroidMining.makeId("textures/gui/air_absorber.png");
    private static final int ENERGY_BAR_HEIGHT = 52;
    private static final int FLUID_BAR_HEIGHT = 48;

    public AirAbsorberScreen(final AirAbsorberContainerMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
        graphics.text(this.font, this.title, 8, 6, -12566464, false);
    }

    //TODO: abstract this stuff on the machines
    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.getLeftPos(), this.getTopPos(), 0, 0, this.getImageWidth(), this.getImageHeight(), 256, 256);

        // Draw energy bar
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos + 8, this.topPos + 18, 176, 0, 6, ENERGY_BAR_HEIGHT, 256, 256);
        final int energyAmount = this.menu.getEnergyStored();
        final int energyLevel = (int) (energyAmount * (ENERGY_BAR_HEIGHT / (float) this.menu.getMaxEnergyStored()));
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos + 8, this.topPos + 18, 8, 18, 6, ENERGY_BAR_HEIGHT - energyLevel, 256, 256);

        // Draw gas tank
        final FluidResource resource = this.menu.blockEntity.fluidTank.getResource(0);
        FluidContainerUtil.renderFluidTank(graphics, this, resource.toStack(this.menu.blockEntity.fluidTank.getAmountAsInt(0)),
            this.menu.blockEntity.fluidTank.getCapacityAsInt(0, resource), 85, 20, 6, FLUID_BAR_HEIGHT, 0);

        // Draw gas tank overlay
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos + 85, this.topPos + 20, 101, 182, 0, 2, FLUID_BAR_HEIGHT, 256, 256);
    }

    @Override
    protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);

        if (this.isHovering(7, 18, 8, ENERGY_BAR_HEIGHT + 1, mouseX, mouseY)) {
            graphics.tooltip(this.font, ClientUtils.createTooltip(Component.translatable("gui.asteroidmining.energy",
                this.menu.getEnergyStored())), mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        }

        if (this.isHovering(85, 20, 6, FLUID_BAR_HEIGHT + 1, mouseX, mouseY)) {
            final FluidResource resource = this.menu.blockEntity.fluidTank.getResource(0);
            FluidContainerUtil.renderFluidTooltip(graphics, this.font, resource.toStack(this.menu.blockEntity.fluidTank.getAmountAsInt(0)), mouseX, mouseY);
        }
    }
}
