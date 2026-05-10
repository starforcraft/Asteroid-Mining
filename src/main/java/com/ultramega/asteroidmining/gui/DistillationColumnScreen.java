package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.blockentities.DistillationColumnBlockEntity;
import com.ultramega.asteroidmining.container.DistillationColumnContainerMenu;
import com.ultramega.asteroidmining.utils.ClientUtils;
import com.ultramega.asteroidmining.utils.CoolantData;
import com.ultramega.asteroidmining.utils.FluidContainerUtil;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public class DistillationColumnScreen extends AbstractSideConfigScreen<DistillationColumnContainerMenu> {
    private static final Identifier BACKGROUND = AsteroidMining.makeId("textures/gui/distillation_column.png");
    private static final Identifier BURN_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final Identifier LIT_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final Identifier COOLING_PROGRESS_SPRITE = AsteroidMining.makeId("cooling_progress"); //TODO: update texture
    private static final int ENERGY_BAR_HEIGHT = 52;
    private static final int FLUID_BAR_HEIGHT = 22;

    public DistillationColumnScreen(final DistillationColumnContainerMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        graphics.text(this.font, this.title, 8, 6, -12566464, false);

        final FormattedCharSequence text = Component.literal(this.menu.getTemperature() + "°C").getVisualOrderText();
        graphics.text(this.font, text, 35 - this.font.width(text) / 2, 62, -12566464, false);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);

        graphics.blit(GUI_TEXTURED, BACKGROUND, this.getLeftPos(), this.getTopPos(), 0, 0, this.getImageWidth(), this.getImageHeight(), 256, 256);

        // Draw energy bar
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos + 8, this.topPos + 18, 176, 0, 6, ENERGY_BAR_HEIGHT, 256, 256);
        final int energyAmount = this.menu.getEnergyStored();
        final int energyLevel = (int) (energyAmount * (ENERGY_BAR_HEIGHT / (float) this.menu.getMaxEnergyStored()));
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos + 8, this.topPos + 18, 8, 18, 6, ENERGY_BAR_HEIGHT - energyLevel, 256, 256);

        // Draw gas tank
        FluidContainerUtil.renderFluidTank(graphics, this, this.menu.blockEntity.fluidTank.getStackInTank(0),
            this.menu.blockEntity.fluidTank.getCapacityAsInt(0), 34, 16, 6, FLUID_BAR_HEIGHT, 0);
        // Draw fluid tanks
        FluidContainerUtil.renderFluidTank(graphics, this, this.menu.blockEntity.fluidTank.getStackInTank(1),
            this.menu.blockEntity.fluidTank.getCapacityAsInt(1), 58, 16, 12, FLUID_BAR_HEIGHT, 0);
        FluidContainerUtil.renderFluidTank(graphics, this, this.menu.blockEntity.fluidTank.getStackInTank(2),
            this.menu.blockEntity.fluidTank.getCapacityAsInt(2), 121, 18, 6, ENERGY_BAR_HEIGHT, 0);

        // Draw gas tank overlay
        graphics.blit(GUI_TEXTURED, BACKGROUND, this.leftPos + 34, this.topPos + 16, 182, 0, 2, FLUID_BAR_HEIGHT, 256, 256);

        // Draw progress
        if (this.menu.getRecipeProgress() > 0) {
            final int progress = (int) ((DistillationColumnBlockEntity.RECIPE_DURATION - this.menu.getRecipeProgress())
                * (24 / (float) DistillationColumnBlockEntity.RECIPE_DURATION));

            graphics.blitSprite(GUI_TEXTURED, BURN_PROGRESS_SPRITE, 24, 16, 0, 0, this.getLeftPos() + 84, this.getTopPos() + 36, progress + 1, 16);
        }
        if (this.menu.getLitTime() > 0) {
            final int progress = Mth.ceil(this.menu.getLitDuration() * 13.0F) + 1;
            graphics.blitSprite(GUI_TEXTURED, LIT_PROGRESS_SPRITE, 14, 14, 0, 14 - progress, this.leftPos + 57, this.topPos + 42 + 14 - progress, 14, progress);
        } else if (this.menu.getCoolingTime() > 0) {
            final int progress = Mth.ceil(this.menu.getCoolingDuration() * 13.0F) + 1;
            graphics.blitSprite(GUI_TEXTURED, COOLING_PROGRESS_SPRITE, 14, 14, 0, 14 - progress, this.leftPos + 57, this.topPos + 42 + 14 - progress, 14, progress);
        }
    }

    @Override
    protected void drawTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            final ItemStack stack = this.hoveredSlot.getItem();
            final CoolantData data = stack.typeHolder().getData(CoolantData.COOLANT_DATA);

            final List<Component> tooltip = this.getTooltipFromContainerItem(stack);
            if (data != null) {
                tooltip.add(1, Component.literal(data.temperature() + "°C").withStyle(ChatFormatting.AQUA));
            }
            graphics.setTooltipForNextFrame(this.font, tooltip, stack.getTooltipImage(),
                stack, mouseX, mouseY, stack.get(DataComponents.TOOLTIP_STYLE));
        }

        if (this.isHovering(7, 18, 8, ENERGY_BAR_HEIGHT + 1, mouseX, mouseY)) {
            graphics.tooltip(this.font, ClientUtils.createTooltip(Component.translatable("gui.asteroidmining.energy",
                this.menu.getEnergyStored())), mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        }

        if (this.isHovering(34, 16, 6, FLUID_BAR_HEIGHT + 1, mouseX, mouseY)) {
            FluidContainerUtil.renderFluidTooltip(graphics, this.font, this.menu.blockEntity.fluidTank.getStackInTank(0), mouseX, mouseY);
        }
        if (this.isHovering(58, 16, 12, FLUID_BAR_HEIGHT + 1, mouseX, mouseY)) {
            FluidContainerUtil.renderFluidTooltip(graphics, this.font, this.menu.blockEntity.fluidTank.getStackInTank(1), mouseX, mouseY);
        }
        if (this.isHovering(121, 18, 6, ENERGY_BAR_HEIGHT + 1, mouseX, mouseY)) {
            FluidContainerUtil.renderFluidTooltip(graphics, this.font, this.menu.blockEntity.fluidTank.getStackInTank(2), mouseX, mouseY);
        }
    }
}
