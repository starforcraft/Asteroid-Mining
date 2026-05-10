package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.config.ServerConfig;
import com.ultramega.asteroidmining.container.ElectrolysisPlantContainerMenu;
import com.ultramega.asteroidmining.registry.ModFluids;
import com.ultramega.asteroidmining.utils.ClientUtils;
import com.ultramega.asteroidmining.utils.FluidContainerUtil;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public class ElectrolysisPlantScreen extends AbstractContainerScreen<ElectrolysisPlantContainerMenu> {
    private static final Identifier BACKGROUND = AsteroidMining.makeId("textures/gui/electrolysis_plant.png");
    private static final int ENERGY_BAR_HEIGHT = 52;
    private static final int FLUID_BAR_HEIGHT = 48;

    public ElectrolysisPlantScreen(final ElectrolysisPlantContainerMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        graphics.text(this.font, this.title, 8, 6, -12566464, false);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.getLeftPos(), this.getTopPos(), 0, 0, this.getImageWidth(), this.getImageHeight(), 256, 256);

        // Draw energy bar
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos + 8, this.topPos + 18, 176, 0, 6, ENERGY_BAR_HEIGHT, 256, 256);
        final int energyAmount = this.menu.getEnergyStored();
        final int energyLevel = (int) (energyAmount * (ENERGY_BAR_HEIGHT / (float) this.menu.getMaxEnergyStored()));
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos + 8, this.topPos + 18, 8, 18, 6, ENERGY_BAR_HEIGHT - energyLevel, 256, 256);

        // Draw fluid tanks
        FluidContainerUtil.renderFluidTank(graphics, this, this.menu.blockEntity.fluidTank.getStackInTank(0),
            this.menu.blockEntity.fluidTank.getCapacityAsInt(0), 67, 20, 14, FLUID_BAR_HEIGHT, 0);
        FluidContainerUtil.renderFluidTank(graphics, this, this.menu.blockEntity.fluidTank.getStackInTank(1),
            this.menu.blockEntity.fluidTank.getCapacityAsInt(1), 115, 20, 6, FLUID_BAR_HEIGHT, 0);

        // Draw fluid tank overlays
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos + 67 + 6, this.topPos + 20, 184, 0, 2, FLUID_BAR_HEIGHT, 256, 256);
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos + 115, this.topPos + 20, 182, 0, 2, FLUID_BAR_HEIGHT, 256, 256);

        // Draw progress
        if (this.menu.getRecipeProgress() > 0) {
            final int progress = (int) ((ServerConfig.ELECTROLYSIS_PLANT_RECIPE_DURATION.get() - this.menu.getRecipeProgress())
                * ((5 + 39 + 5) / (float) ServerConfig.ELECTROLYSIS_PLANT_RECIPE_DURATION.get()));

            // Draw gas from first tank into air
            if (progress < 20) {
                FluidContainerUtil.renderTiledFluid(graphics, this, new FluidStack(Fluids.WATER, 1),
                    69, 19 - progress, 1, 1);
            }

            // Draw gas from first tank to second tank
            final FluidStack fluidStack = new FluidStack(ModFluids.HYDROGEN.get(), 1);
            if (progress < 5) {
                FluidContainerUtil.renderTiledFluid(graphics, this, fluidStack,
                    78, 19 - progress, 1, 1);
            } else if (progress < 5 + 39) {
                FluidContainerUtil.renderTiledFluid(graphics, this, fluidStack,
                    78 + progress - 5, 15, 1, 1);
            } else {
                FluidContainerUtil.renderTiledFluid(graphics, this, fluidStack,
                    118, 15 + progress - (5 + 39), 1, 1);
            }
        }
    }

    @Override
    protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);

        if (this.isHovering(7, 18, 8, ENERGY_BAR_HEIGHT + 1, mouseX, mouseY)) {
            graphics.tooltip(this.font, ClientUtils.createTooltip(Component.translatable("gui.asteroidmining.energy",
                this.menu.getEnergyStored())), mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        }

        if (this.isHovering(67, 20, 14, FLUID_BAR_HEIGHT + 1, mouseX, mouseY)) {
            FluidContainerUtil.renderFluidTooltip(graphics, this.font, this.menu.blockEntity.fluidTank.getStackInTank(0), mouseX, mouseY);
        }
        if (this.isHovering(115, 20, 6, FLUID_BAR_HEIGHT + 1, mouseX, mouseY)) {
            FluidContainerUtil.renderFluidTooltip(graphics, this.font, this.menu.blockEntity.fluidTank.getStackInTank(1), mouseX, mouseY);
        }
    }
}
