//package com.ultramega.asteroidmining.gui;
//
//import com.ultramega.asteroidmining.AsteroidMining;
//import com.ultramega.asteroidmining.events.AsteroidReloadListener;
//import com.ultramega.asteroidmining.gui.widgets.PlaceholderEditBox;
//import com.ultramega.asteroidmining.gui.widgets.ScrollbarWidget;
//import com.ultramega.asteroidmining.utils.AsteroidConfig;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//import com.mojang.blaze3d.vertex.PoseStack;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.gui.GuiGraphics;
//import net.minecraft.client.gui.screens.Screen;
//import net.minecraft.network.chat.Component;
//import net.minecraft.resources.ResourceLocation;
//
//public class AsteroidListScreen extends Screen {
//    private static final ResourceLocation BACKGROUND = AsteroidMining.makeId("textures/gui/asteroid_list.png");
//    private static final ResourceLocation EDIT_TEXTURE = AsteroidMining.makeId("edit");
//    private static final ResourceLocation ADD_TEXTURE = AsteroidMining.makeId("add");
//
//    private static final int AREA_HEIGHT = 134;
//
//    private static final int ROW_START_Y = 42;
//    private static final int ROW_HEIGHT = 17;
//    private static final int ROWS_DISPLAYED = 8;
//
//    private final List<AsteroidConfig> filteredData = new ArrayList<>();
//
//    private final int imageWidth = 223;
//    private final int imageHeight = 182;
//
//    private int leftPos;
//    private int topPos;
//
//    private ScrollbarWidget scrollbar;
//
//    public AsteroidListScreen() {
//        super(Component.translatable("gui.asteroidmining.asteroid.edit_asteroids"));
//
//        for (final Map.Entry<ResourceLocation, AsteroidConfig> entry : AsteroidReloadListener.INSTANCE.getData().entrySet()) {
//            filteredData.add(entry.getValue());
//        }
//    }
//
//    @Override
//    protected void init() {
//        this.leftPos = (this.width - this.imageWidth) / 2;
//        this.topPos = (this.height - this.imageHeight) / 2;
//
//        scrollbar = new ScrollbarWidget(
//            204,
//            38,
//            AREA_HEIGHT
//        );
//        final int overflowingRows = AsteroidReloadListener.INSTANCE.getData().size() - ROWS_DISPLAYED;
//        final int maxOffset = overflowingRows * ROWS_DISPLAYED;
//        scrollbar.setMaxOffset(maxOffset);
//        scrollbar.setEnabled(maxOffset > 0);
//        scrollbar.setListener(value -> updateWidgets());
//        this.addWidget(scrollbar);
//
//        final PlaceholderEditBox searchBox = new PlaceholderEditBox(
//            this.font,
//            this.leftPos + 7,
//            this.topPos + 17,
//            190,
//            18,
//            64,
//            Component.translatable("gui.asteroidmining.asteroid.search"),
//            null
//        );
//        searchBox.setResponder(this::onSearchChanged);
//        this.addRenderableWidget(searchBox);
//    }
//
//    @Override
//    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
//        final PoseStack poseStack = graphics.pose();
//
//        super.render(graphics, mouseX, mouseY, partialTick);
//
//        poseStack.pushPose();
//        poseStack.translate((float) leftPos, (float) topPos, 0.0F);
//
//        graphics.blitSprite(ADD_TEXTURE, 201, 18, 16, 16);
//
//        this.scrollbar.render(graphics, mouseX, mouseY, partialTick);
//        this.renderContent(graphics, mouseX, mouseY);
//        this.renderLabels(graphics);
//
//        poseStack.popPose();
//
//        this.renderTooltip(graphics, mouseX, mouseY);
//    }
//
//    private void renderContent(final GuiGraphics graphics, final int mouseX, final int mouseY) {
//        final int scrollbarOffset = (int) scrollbar.getOffset();
//        int y = ROW_START_Y - (scrollbarOffset * ROW_HEIGHT);
//        for (final AsteroidConfig asteroid : filteredData) {
//            if (y >= ROW_START_Y && y < ROW_START_Y + ROW_HEIGHT * ROWS_DISPLAYED) {
//                graphics.drawString(this.font, asteroid.getName(), 10, y, -12566464, false);
//                graphics.blitSprite(EDIT_TEXTURE, 178, y - 4, 16, 16);
//
//                if (isHovering(178, y - 4, 16, 15, mouseX, mouseY)) {
//                    graphics.renderTooltip(font, Component.translatable("gui.asteroidmining.asteroid.edit_asteroid", asteroid.getName()), mouseX - leftPos, mouseY - topPos);
//                }
//            }
//
//            y += ROW_HEIGHT;
//        }
//    }
//
//    private void renderTooltip(final GuiGraphics graphics, final int mouseX, final int mouseY) {
//        if (this.isHovering(201, 18, 16, 16, mouseX, mouseY)) {
//            graphics.renderTooltip(this.font, Component.translatable("gui.asteroidmining.asteroid.add_asteroid"), mouseX, mouseY);
//        }
//    }
//
//    @Override
//    public void renderBackground(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
//        super.renderBackground(graphics, mouseX, mouseY, partialTick);
//
//        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
//    }
//
//    private void renderLabels(final GuiGraphics graphics) {
//        graphics.drawString(this.font, this.title, 8, 6, -12566464, false);
//    }
//
//    private void updateWidgets() {
//        final int totalRows = filteredData.size();
//        final double maxOffset = totalRows - ROWS_DISPLAYED;
//        this.scrollbar.setMaxOffset(maxOffset);
//        this.scrollbar.setEnabled(maxOffset > 0);
//    }
//
//    private void onSearchChanged(final String query) {
//        filteredData.clear();
//
//        for (final Map.Entry<ResourceLocation, AsteroidConfig> entry : AsteroidReloadListener.INSTANCE.getData().entrySet()) {
//            if (matchesSearch(entry.getValue().getName(), query)) { //TODO: autocomplete
//                filteredData.add(entry.getValue());
//            }
//        }
//
//        this.updateWidgets();
//    }
//
//    private boolean matchesSearch(final String name, final String query) {
//        if (query.isEmpty()) {
//            return true;
//        }
//
//        return name.toLowerCase().contains(query.toLowerCase());
//    }
//
//    @Override
//    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
//        // Add Button
//        if (isHovering(201, 18, 16, 16, mouseX, mouseY)) {
//            Minecraft.getInstance().pushGuiLayer(new AsteroidAddEditScreen(null));
//            return true;
//        }
//
//        // Edit Buttons
//        final int scrollbarOffset = (int) scrollbar.getOffset();
//        int y = ROW_START_Y - (scrollbarOffset * ROW_HEIGHT); //TODO: this is uselessly complicated
//        for (final AsteroidConfig clickedAsteroid : filteredData) {
//            if (y >= ROW_START_Y && y < ROW_START_Y + ROW_HEIGHT * ROWS_DISPLAYED) {
//                if (isHovering(178, y - 4, 16, 15, mouseX, mouseY)) {
//                    Minecraft.getInstance().pushGuiLayer(new AsteroidAddEditScreen(clickedAsteroid));
//                    return true;
//                }
//            }
//
//            y += ROW_HEIGHT;
//        }
//
//        if (scrollbar.mouseClicked(mouseX - leftPos, mouseY - topPos, button)) {
//            return true;
//        }
//
//        return super.mouseClicked(mouseX, mouseY, button);
//    }
//
//    @Override
//    public void mouseMoved(final double mouseX, final double mouseY) {
//        this.scrollbar.mouseMoved(mouseX - leftPos, mouseY - topPos);
//        super.mouseMoved(mouseX, mouseY);
//    }
//
//    @Override
//    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
//        if (this.scrollbar.mouseReleased(mouseX - leftPos, mouseY - topPos, button)) {
//            return true;
//        }
//        return super.mouseReleased(mouseX, mouseY, button);
//    }
//
//    @Override
//    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
//        return this.scrollbar.mouseScrolled(mouseX - leftPos, mouseY - topPos, scrollX, scrollY);
//    }
//
//    private boolean isHovering(final int x, final int y, final int width, final int height, final double mouseX, final double mouseY) {
//        final double correctMouseX = mouseX - this.leftPos;
//        final double correctMouseY = mouseY - this.topPos;
//        return correctMouseX >= (double) (x - 1) && correctMouseX < (double) (x + width + 1)
//            && correctMouseY >= (double) (y - 1) && correctMouseY < (double) (y + height + 1);
//    }
//
//    @Override
//    public boolean isPauseScreen() {
//        return false;
//    }
//}
