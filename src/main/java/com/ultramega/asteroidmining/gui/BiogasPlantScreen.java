package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.blockentities.BiogasPlantBlockEntity;
import com.ultramega.asteroidmining.container.BiogasPlantContainerMenu;
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

public class BiogasPlantScreen extends AbstractContainerScreen<BiogasPlantContainerMenu> {
    private static final Identifier BACKGROUND = AsteroidMining.makeId("textures/gui/biogas_plant.png");
    private static final Identifier BURN_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final int ENERGY_BAR_HEIGHT = 52;
    private static final int FLUID_BAR_HEIGHT = 48;

    public BiogasPlantScreen(final BiogasPlantContainerMenu container, final Inventory inventory, final Component title) {
        super(container, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
        graphics.text(this.font, this.title, 8, 6, -12566464, false);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.getLeftPos(), this.getTopPos(), 0, 0, this.getImageWidth(), this.getImageHeight(), 256, 256);

        // Draw energy bar
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos + 8, this.topPos + 18, 176, 0, 6, ENERGY_BAR_HEIGHT, 256, 256);
        final int energyAmount = this.menu.getEnergyStored();
        final int energyLevel = (int) (energyAmount * (ENERGY_BAR_HEIGHT / (float) this.menu.getMaxEnergyStored()));
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos + 8, this.topPos + 18, 8, 18, 6, ENERGY_BAR_HEIGHT - energyLevel, 256, 256);

        // Draw fluid tank
        final FluidResource resource = this.menu.blockEntity.fluidTank.getResource(0);
        FluidContainerUtil.renderFluidTank(graphics, this, resource.toStack(this.menu.blockEntity.fluidTank.getAmountAsInt(0)),
            this.menu.blockEntity.fluidTank.getCapacityAsInt(0, resource), 121, 20, 6, FLUID_BAR_HEIGHT, 0);

        // Draw gas tank overlay
        // TODO: something is wrong here:
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos + 121, this.topPos + 20, 101, 182, 0, 2, FLUID_BAR_HEIGHT, 256, 256);

        // Draw progress
        if (this.menu.getRecipeProgress() > 0) {
            final int progress = (int) ((BiogasPlantBlockEntity.RECIPE_DURATION - this.menu.getRecipeProgress())
                * (24 / (float) BiogasPlantBlockEntity.RECIPE_DURATION));

            graphics.blitSprite(GUI_TEXTURED, BURN_PROGRESS_SPRITE, 24, 16, 0, 0, this.getLeftPos() + 89, this.getTopPos() + 36, progress + 1, 16);
        }
    }

    @Override
    protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);

        if (this.isHovering(7, 18, 8, ENERGY_BAR_HEIGHT + 1, mouseX, mouseY)) {
            graphics.tooltip(this.font, ClientUtils.createTooltip(Component.translatable("gui.asteroidmining.energy",
                this.menu.getEnergyStored())), mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        }

        if (this.isHovering(121, 20, 6, FLUID_BAR_HEIGHT + 1, mouseX, mouseY)) {
            final FluidResource resource = this.menu.blockEntity.fluidTank.getResource(0);
            FluidContainerUtil.renderFluidTooltip(graphics, this.font, resource.toStack(this.menu.blockEntity.fluidTank.getAmountAsInt(0)), mouseX, mouseY);
        }
    }
}
