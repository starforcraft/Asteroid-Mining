package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.container.AbstractSideConfigContainerMenu;
import com.ultramega.asteroidmining.gui.widgets.SideConfigWidget;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public abstract class AbstractSideConfigScreen<M extends AbstractSideConfigContainerMenu<?>> extends AbstractMovableWidgetContainerScreen<M> {
    protected AbstractSideConfigScreen(final M menu,
                                       final Inventory inventory,
                                       final Component title,
                                       final int imageWidth,
                                       final int imageHeight) {
        super(menu, inventory, title, imageWidth, imageHeight);
    }

    @Override
    protected void init() {
        super.init();

        this.addTopLayerWidget(new SideConfigWidget(
            this.menu,
            this.leftPos - SideConfigWidget.WIDTH - 6,
            this.topPos + 8,
            () -> this.width,
            () -> this.height
        ));
    }
}
