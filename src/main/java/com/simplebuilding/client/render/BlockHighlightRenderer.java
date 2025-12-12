package com.simplebuilding.client.render;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.OctantItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;

import static com.simplebuilding.util.guiDrawHelper.*;

public class BlockHighlightRenderer {

    public static void render(Matrix4f positionMatrix, Camera camera) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        ItemStack stack = client.player.getMainHandStack();
        boolean isRangefinder = stack.getItem() instanceof OctantItem;

        if (!isRangefinder) {
            stack = client.player.getOffHandStack();
            isRangefinder = stack.getItem() instanceof OctantItem;
        }

        if (isRangefinder) {
            renderHighlights(positionMatrix, camera, stack, client.player.isSneaking());
        }
    }

    private static void renderHighlights(Matrix4f positionMatrix, Camera camera, ItemStack stack, boolean isSneaking) {
        NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtData.copyNbt();

        BlockPos pos1 = getPos(nbt, "Pos1");
        BlockPos pos2 = getPos(nbt, "Pos2");

        if (pos1 == null && pos2 == null) return;

        boolean hasConstructorsTouch = hasEnchantment(stack, MinecraftClient.getInstance(), ModEnchantments.CONSTRUCTORS_TOUCH);
        boolean showFill;
        boolean isInverted = Simplebuilding.getConfig().tools.invertOctantSneak;
        int opacityPercent = Simplebuilding.getConfig().tools.buildingHighlightOpacity;
        opacityPercent = Math.max(0, Math.min(100, opacityPercent));
        float baseAlpha = opacityPercent / 100.0f;

        if (hasConstructorsTouch) {
            showFill = isInverted ? isSneaking : !isSneaking;
        } else {
            showFill = false;
        }

        // --- Farben bestimmen ---
        OctantItem octant = (OctantItem) stack.getItem();
        DyeColor dyeColor = octant.getColor();

        // Holt jetzt 3 Farben: Pos1, Pos2, Area
        RenderColors colors = getRenderColors(dyeColor);

        float r1 = colors.r1(); float g1 = colors.g1(); float b1 = colors.b1(); // Pos 1 (Hell)
        float r2 = colors.r2(); float g2 = colors.g2(); float b2 = colors.b2(); // Pos 2 (Dunkel)
        float r3 = colors.r3(); float g3 = colors.g3(); float b3 = colors.b3(); // Area (Mittel)

        // --- Rendering Setup ---
        double camX = camera.getCameraPos().x;
        double camY = camera.getCameraPos().y;
        double camZ = camera.getCameraPos().z;

        MatrixStack matrices = new MatrixStack();
        matrices.push();
        matrices.multiplyPositionMatrix(positionMatrix);
        matrices.translate(-camX, -camY, -camZ);

        BufferAllocator allocator = new BufferAllocator(1536);
        VertexConsumerProvider.Immediate consumers = VertexConsumerProvider.immediate(allocator);

        // 1. OUTLINES (Immer sichtbar)
        VertexConsumer lines = consumers.getBuffer(RenderLayers.lines());
        float lineAlpha = 0.8f;

        // Pos 1 Box -> Nutzt Farbe 1
        if (pos1 != null) drawBoxOutline(matrices, lines, new Box(pos1).expand(0.001), r1, g1, b1, lineAlpha);

        // Pos 2 Box -> Nutzt Farbe 2
        if (pos2 != null) drawBoxOutline(matrices, lines, new Box(pos2).expand(0.002), r2, g2, b2, lineAlpha);

        // Area Box -> Nutzt Farbe 3
        if (pos1 != null && pos2 != null && showFill) drawBoxOutline(matrices, lines, getFullArea(pos1, pos2).expand(0.003), r3, g3, b3, lineAlpha);

        consumers.draw(RenderLayers.lines());

        // 2. FÜLLUNGEN (Bedingt sichtbar)
        if (showFill) {
            VertexConsumer fill = consumers.getBuffer(RenderLayers.debugQuads());
            float alpha = 0.3f * baseAlpha;

            if (pos1 != null) drawBoxFill(matrices, fill, new Box(pos1).expand(0.003), r1, g1, b1, alpha);
            if (pos2 != null) drawBoxFill(matrices, fill, new Box(pos2).expand(0.006), r2, g2, b2, alpha);
            if (pos1 != null && pos2 != null) drawBoxFill(matrices, fill, getFullArea(pos1, pos2).expand(0.009), r3, g3, b3, alpha);

            consumers.draw(RenderLayers.debugQuads());
        }

        matrices.pop();
        allocator.close();
    }
}