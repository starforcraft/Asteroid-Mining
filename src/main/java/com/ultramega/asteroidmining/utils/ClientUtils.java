package com.ultramega.asteroidmining.utils;

import com.ultramega.asteroidmining.AsteroidMining;
import com.ultramega.asteroidmining.asteroids.AsteroidResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2ic;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

public final class ClientUtils {
    private ClientUtils() {
    }

    public static void renderResourcesInsideTooltip(final GuiGraphicsExtractor graphics,
                                                    final Font font,
                                                    final List<ClientTooltipComponent> components,
                                                    final int mouseX,
                                                    final int mouseY,
                                                    final ClientTooltipPositioner tooltipPositioner,
                                                    final List<AsteroidResource> resources) {
        if (components.isEmpty()) {
            return;
        }

        final RenderTooltipEvent.Pre preEvent = ClientHooks.onRenderTooltipPre(ItemStack.EMPTY, graphics, mouseX, mouseY, graphics.guiWidth(), graphics.guiHeight(),
            components, font, tooltipPositioner);
        if (preEvent.isCanceled()) {
            return;
        }

        final int textWidth = components.stream()
            .mapToInt(component -> component.getWidth(preEvent.getFont()))
            .max().orElse(0);
        final int textHeight = components.stream()
            .mapToInt(c -> c.getHeight(font))
            .sum() + (components.size() > 1 ? 0 : -2);

        final int itemRows = (int) Math.ceil(resources.size() / 4.0);
        final int itemWidth = Math.min(resources.size(), 4) * 18;
        final int itemHeight = itemRows * 18;

        final int totalWidth = Math.max(textWidth, itemWidth);
        final int totalHeight = textHeight + itemHeight;

        final Vector2ic position = tooltipPositioner.positionTooltip(
            graphics.guiWidth(), graphics.guiHeight(), preEvent.getX(), preEvent.getY(), totalWidth, totalHeight);
        final int posX = position.x();
        final int posY = position.y();

        graphics.pose().pushMatrix();
        final RenderTooltipEvent.Texture event = ClientHooks.onRenderTooltipTexture(ItemStack.EMPTY, graphics, posX, posY, preEvent.getFont(), components, null);
        TooltipRenderUtil.extractTooltipBackground(graphics, posX, posY, totalWidth, totalHeight, event.getTexture());

        int textY = posY;
        for (final ClientTooltipComponent component : components) {
            component.extractText(graphics, font, posX, textY);
            textY += component.getHeight(font) + (textY == posY ? 2 : 0);
        }

        textY = posY;
        for (final ClientTooltipComponent component : components) {
            component.extractImage(font, posX, textY, totalWidth, totalHeight, graphics);
            textY += component.getHeight(font) + (textY == posY ? 2 : 0);
        }

        if (!resources.isEmpty()) {
            renderResources(graphics, font, posX, textY, 4, resources);
        }

        graphics.pose().popMatrix();
    }

    public static void renderResource(final GuiGraphicsExtractor graphics,
                                      final Font font,
                                      final int posX,
                                      final int posY,
                                      final AsteroidResource resource) {
        renderResourcesWithSlot(graphics, font, 0, 0, posX, posY, 1, 0, 0, List.of(resource), false);
    }

    public static void renderResources(final GuiGraphicsExtractor graphics,
                                       final Font font,
                                       final int posX,
                                       final int posY,
                                       final int lineBreak,
                                       final List<AsteroidResource> resources) {
        renderResourcesWithSlot(graphics, font, 0, 0, posX, posY, lineBreak, 0, 0, resources, false);
    }

    public static void renderResourcesWithSlot(final GuiGraphicsExtractor graphics,
                                               final Font font,
                                               final int mouseX,
                                               final int mouseY,
                                               final int posX,
                                               final int posY,
                                               final int lineBreak,
                                               final int leftPos,
                                               final int topPos,
                                               final List<AsteroidResource> resources) {
        renderResourcesWithSlot(graphics, font, mouseX, mouseY, posX, posY, lineBreak, leftPos, topPos, resources, true);
    }

    private static void renderResourcesWithSlot(final GuiGraphicsExtractor graphics,
                                                final Font font,
                                                final int mouseX,
                                                final int mouseY,
                                                final int posX,
                                                final int posY,
                                                final int lineBreak,
                                                final int leftPos,
                                                final int topPos,
                                                final List<AsteroidResource> resources,
                                                final boolean drawSlot) {
        int stackX = posX;
        int stackY = posY;
        for (int i = 0; i < resources.size(); i++) {
            final AsteroidResource resource = resources.get(i);

            if (drawSlot) {
                graphics.blitSprite(GUI_TEXTURED, AsteroidMining.makeId("slot"), stackX - 1, stackY - 1, 18, 18);
            }
            resource.drawResourceWithAmount(graphics, font, stackX, stackY);

            if (drawSlot && isMouseOver(leftPos + stackX, topPos + stackY, 18, 18, mouseX, mouseY)) {
                drawSlotHighlight(graphics, stackX, stackY);
            }

            if ((i + 1) % lineBreak == 0) {
                stackX = posX;
                stackY += 18;
            } else {
                stackX += 18;
            }
        }
    }

    public static void renderTooltipOfResources(final GuiGraphicsExtractor graphics,
                                                final int mouseX,
                                                final int mouseY,
                                                final int posX,
                                                final int posY,
                                                final int lineBreak,
                                                final int leftPos,
                                                final int topPos,
                                                final List<AsteroidResource> resources) {
        int stackX = posX;
        int stackY = posY;
        for (int i = 0; i < resources.size(); i++) {
            final AsteroidResource resource = resources.get(i);
            if (isMouseOver(leftPos + stackX, topPos + stackY, 18, 18, mouseX, mouseY)) {
                resource.drawTooltip(graphics, mouseX, mouseY);
            }

            if ((i + 1) % lineBreak == 0) {
                stackX = posX;
                stackY += 18;
            } else {
                stackX += 18;
            }
        }
    }

    public static void renderAmount(final GuiGraphicsExtractor graphics,
                                    final Font font,
                                    final int x,
                                    final int y,
                                    final String text,
                                    final int color) {
        final Matrix3x2fStack poseStack = graphics.pose();
        poseStack.pushMatrix();
        poseStack.translate(x, y);
        poseStack.scale(0.5F, 0.5F);
        graphics.text(font, text, 32 - font.width(text), 23, color, true);
        poseStack.popMatrix();
    }

    public static void renderItemStackTooltip(final GuiGraphicsExtractor graphics,
                                              final ItemStack stack,
                                              final long amount,
                                              final int mouseX,
                                              final int mouseY) {
        final List<Component> tooltip = new ArrayList<>();
        tooltip.addAll(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
        tooltip.add(Component.translatable("gui.asteroidmining.rocket_storage_viewer.total", amount)
            .withStyle(ChatFormatting.GRAY));

        renderResourceTooltip(graphics, stack, tooltip, stack.getTooltipImage(), mouseX, mouseY);
    }

    public static void renderFluidStackTooltip(final GuiGraphicsExtractor graphics,
                                               final FluidStack stack,
                                               final long amount,
                                               final int mouseX,
                                               final int mouseY) {
        final List<Component> tooltip = new ArrayList<>();
        tooltip.add(stack.getHoverName());
        tooltip.add(Component.translatable("gui.asteroidmining.rocket_storage_viewer.total", amount + "mB")
            .withStyle(ChatFormatting.GRAY));

        renderResourceTooltip(graphics, ItemStack.EMPTY, tooltip, Optional.empty(), mouseX, mouseY);
    }

    private static void renderResourceTooltip(final GuiGraphicsExtractor graphics,
                                              final ItemStack stack,
                                              final List<Component> tooltip,
                                              final Optional<TooltipComponent> tooltipImage,
                                              final int mouseX,
                                              final int mouseY) {
        final List<ClientTooltipComponent> components = ClientHooks.gatherTooltipComponents(
            stack, tooltip, tooltipImage, mouseX, graphics.guiWidth(), graphics.guiHeight(), Minecraft.getInstance().font);

        graphics.tooltip(
            Minecraft.getInstance().font,
            components,
            mouseX,
            mouseY,
            DefaultTooltipPositioner.INSTANCE,
            null
        );
    }

    public static List<ClientTooltipComponent> createTooltip(final List<Component> component) {
        return component.stream().map(comp -> ClientTooltipComponent.create(comp.getVisualOrderText())).toList();
    }

    public static List<ClientTooltipComponent> createTooltip(final Component component) {
        return List.of(ClientTooltipComponent.create(component.getVisualOrderText()));
    }

    public static void drawSlotHighlight(final GuiGraphicsExtractor graphics, final int x, final int y) {
        graphics.fillGradient(x, y, x + 16, y + 16, -2130706433, -2130706433);
    }

    public static boolean isMouseOver(final int x, final int y, final int width, final int height, final int mouseX, final int mouseY) {
        return isMouseOver(x, y, width, height, (double) mouseX, mouseY);
    }

    public static boolean isMouseOver(final int x, final int y, final int width, final int height, final double mouseX, final double mouseY) {
        return isMouseOver((double) x, y, width, height, mouseX, mouseY);
    }

    public static boolean isMouseOver(final double x, final double y, final int width, final int height, final double mouseX, final double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static PreviewBlockHitResult raytraceGivenBlocks(final Vec3 start, final Vec3 end, final List<PreviewInfo> targets, final Level level) {
        PreviewBlockHitResult closestHit = null;
        double closestDistance = Double.MAX_VALUE;
        final Vec3 rayDir = end.subtract(start);

        for (final PreviewInfo info : targets) {
            if (info.expectedBlock().isEmpty()) {
                continue;
            }

            final BlockPos pos = info.pos();
            final BlockState expectedState = info.expectedBlock().get().defaultBlockState();
            final BlockState currentState = level.getBlockState(pos);

            if (expectedState.isAir() && currentState.isAir()) {
                continue;
            }

            final BlockState shapeState = expectedState.isAir() ? currentState : expectedState;
            final VoxelShape shape = shapeState.getShape(level, pos);

            final BlockHitResult hitResult;
            if (shape.isEmpty()) {
                final Optional<Vec3> intersection = new AABB(pos).clip(start, end);
                if (intersection.isEmpty()) {
                    continue;
                }

                hitResult = new BlockHitResult(
                    intersection.get(),
                    Direction.getApproximateNearest((float) rayDir.x, (float) rayDir.y, (float) rayDir.z),
                    pos,
                    false
                );
            } else {
                hitResult = shape.clip(start, end, pos);
                if (hitResult == null) {
                    continue;
                }
            }

            final double distance = start.distanceToSqr(hitResult.getLocation());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestHit = new PreviewBlockHitResult(
                    hitResult.getLocation(),
                    hitResult.getDirection(),
                    hitResult.getBlockPos(),
                    false,
                    false,
                    info
                );
            }
        }

        if (closestHit != null) {
            return closestHit;
        }

        return new PreviewBlockHitResult(
            true,
            end,
            Direction.getApproximateNearest((float) rayDir.x, (float) rayDir.y, (float) rayDir.z),
            BlockPos.containing(end),
            false,
            false,
            null
        );
    }

    public static void drawConnectedWireframe(final PoseStack poseStack,
                                              final VertexConsumer consumer,
                                              final Set<BlockPos> blocks,
                                              final Vec3 cameraPos) {
        VoxelShape shape = Shapes.empty();

        for (final BlockPos pos : blocks) {
            shape = Shapes.or(shape, Shapes.block().move(pos.getX(), pos.getY(), pos.getZ()));
        }

        ShapeRenderer.renderShape(
            poseStack,
            consumer,
            shape,
            -cameraPos.x,
            -cameraPos.y,
            -cameraPos.z,
            ARGB.colorFromFloat(1.0F, 0.0F, 1.0F, 0.0F),
            4.0F
        );
    }
}
