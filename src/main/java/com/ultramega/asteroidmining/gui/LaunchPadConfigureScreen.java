package com.ultramega.asteroidmining.gui;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.events.ClientEvents;
import com.ultramega.asteroidmining.gui.widgets.ImageButton;
import com.ultramega.asteroidmining.utils.CameraHandler;
import com.ultramega.asteroidmining.utils.PreviewBlockHitResult;
import com.ultramega.asteroidmining.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import static com.ultramega.asteroidmining.utils.Utils.raytraceGivenBlocks;

public class LaunchPadConfigureScreen extends Screen {
    private static final Identifier ROTATE_TEXTURE = AsteroidMining.makeId("rotate");

    private final LaunchPadBuilderScreen parent;

    private Matrix4f projectionViewMatrix;
    @Nullable
    private BlockPos blockUnderCursor;

    public LaunchPadConfigureScreen(final LaunchPadBuilderScreen parent) {
        super(Component.empty());
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        final Button rightButton =
            Button.builder(Component.literal("→"), (button) -> this.parent.move(Direction.EAST)).bounds(this.width - 20, (this.height - 20) / 2, 20, 20).build();

        final Button leftButton = Button.builder(Component.literal("←"), (button) -> this.parent.move(Direction.WEST)).bounds(0, (this.height - 20) / 2, 20, 20).build();

        final Button topMinusButton =
            Button.builder(Component.literal("-"), (button) -> this.parent.resize(false, -1)).bounds((this.width - 20) / 2 - 20, 0, 20, 20).build();
        final Button topButton = Button.builder(Component.literal("↑"), (button) -> this.parent.move(Direction.NORTH)).bounds((this.width - 20) / 2, 0, 20, 20).build();
        final Button topPlusButton =
            Button.builder(Component.literal("+"), (button) -> this.parent.resize(false, 1)).bounds((this.width - 20) / 2 + 20, 0, 20, 20).build();

        final Button bottomMinusButton =
            Button.builder(Component.literal("-"), (button) -> this.parent.resize(true, -2)).bounds((this.width - 20) / 2 - 20, this.height - 20, 20, 20).build();
        final Button bottomButton =
            Button.builder(Component.literal("↓"), (button) -> this.parent.move(Direction.SOUTH)).bounds((this.width - 20) / 2, this.height - 20, 20, 20).build();
        final Button bottomPlusButton =
            Button.builder(Component.literal("+"), (button) -> this.parent.resize(true, 2)).bounds((this.width - 20) / 2 + 20, this.height - 20, 20, 20).build();

        final Button rotateButton = new ImageButton((this.width - 20) / 2 + 50, this.height - 20, 20, 20, ROTATE_TEXTURE, (button) -> this.parent.rotate());

        final Button confirmButton = Button.builder(Component.translatable("gui.asteroidmining.launch_pad_configure.confirm"), (button) -> {
            this.parent.confirm();
            this.onClose();
        }).bounds((this.width - 50) / 2 - 65, this.height - 20, 50, 20).build();

        //TODO: show rocket building area button

        this.addRenderableWidget(rightButton);

        this.addRenderableWidget(leftButton);

        this.addRenderableWidget(topMinusButton);
        this.addRenderableWidget(topButton);
        this.addRenderableWidget(topPlusButton);

        this.addRenderableWidget(bottomMinusButton);
        this.addRenderableWidget(bottomButton);
        this.addRenderableWidget(bottomPlusButton);

        this.addRenderableWidget(rotateButton);

        this.addRenderableWidget(confirmButton);
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);

        this.calculateBlockUnderCursor(graphics, mouseX, mouseY);
    }

    private void calculateBlockUnderCursor(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        final Minecraft mc = Minecraft.getInstance();
        double mx = mc.mouseHandler.xpos();
        double my = mc.mouseHandler.ypos();

        my = mc.getWindow().getScreenHeight() - my;
        my *= mc.getWindow().getHeight() / (double) mc.getWindow().getScreenHeight();
        mx *= mc.getWindow().getWidth() / (double) mc.getWindow().getScreenWidth();
        final Vec3 near = this.toWorld(mx, my, 0);
        final Vec3 far = this.toWorld(mx, my, 1);

        if (near == null || far == null) {
            return;
        }

        final Vec3 cameraPos = mc.getEntityRenderDispatcher().camera.position();

        final PreviewBlockHitResult hitResult = raytraceGivenBlocks(near.add(cameraPos), far.add(cameraPos),
            ClientEvents.LAUNCH_PAD_PREVIEW_BLOCKS.getOrDefault(this.parent.getMenu().getBlockEntity().getBlockPos(), new ArrayList<>()), mc.level);

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            this.blockUnderCursor = hitResult.getBlockPos();

            // Render Tooltip
            final BlockState currentState = mc.level.getBlockState(this.blockUnderCursor);
            final Optional<Block> expectedBlock = hitResult.getPreviewInfo().expectedBlock();
            final String expectedName = expectedBlock.map(block -> block.getName().getString()).orElse("");
            final String currentName = currentState.getBlock().getName().getString();

            final List<Component> text;
            if (currentState.isAir()) {
                text = List.of(Component.translatable("gui.asteroidmining.launch_pad_configure.missing").withStyle(ChatFormatting.RED)
                    .append(Component.literal(expectedName).withStyle(ChatFormatting.WHITE)));
            } else if (expectedBlock.isEmpty()) {
                text = List.of(Component.literal(currentName).withStyle(ChatFormatting.WHITE));
            } else if (currentState.is(expectedBlock.get())) {
                text = List.of(Component.translatable("gui.asteroidmining.launch_pad_configure.correct").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(expectedName).withStyle(ChatFormatting.WHITE)));
            } else { //TODO: what about indestructible blocks? (state.getDestroySpeed(level(), pos) <= 0)
                text = List.of(Component.translatable("gui.asteroidmining.launch_pad_configure.expected").withStyle(ChatFormatting.RED)
                        .append(Component.literal(expectedName).withStyle(ChatFormatting.WHITE)),
                    Component.translatable("gui.asteroidmining.launch_pad_configure.currently").withStyle(ChatFormatting.DARK_RED)
                        .append(Component.literal(currentName).withStyle(ChatFormatting.WHITE)));
            }
            graphics.tooltip(this.font, Utils.createTooltip(text), mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        } else {
            this.blockUnderCursor = null;
        }
    }

    private Vec3 toWorld(final double x, final double y, final double z) {
        if (this.projectionViewMatrix == null) {
            return null;
        }

        final Window window = Minecraft.getInstance().getWindow();

        final double normalizedX = x / window.getWidth() * 2.0 - 1.0;
        final double normalizedY = y / window.getHeight() * 2.0 - 1.0;

        final Vector4f normalizedPos = new Vector4f((float) normalizedX, (float) normalizedY, (float) z, 1.0F);
        this.projectionViewMatrix.transform(normalizedPos);

        if (normalizedPos.w() == 0) {
            return null;
        }

        normalizedPos.mul(1.0F / normalizedPos.w());
        return new Vec3(normalizedPos.x(), normalizedPos.y(), normalizedPos.z());
    }

    @Override
    public void onClose() {
        this.parent.confirm();
        CameraHandler.resetPosition();
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    protected void extractBlurredBackground(final GuiGraphicsExtractor graphics) {
    }

    @Override
    protected void extractMenuBackground(final GuiGraphicsExtractor graphics) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void setProjectionViewMatrix(final Matrix4f projectionViewMatrix) {
        this.projectionViewMatrix = projectionViewMatrix;
    }

    @Nullable
    public BlockPos getBlockUnderCursor() {
        return this.blockUnderCursor;
    }
}
