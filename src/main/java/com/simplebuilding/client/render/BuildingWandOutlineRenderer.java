package com.simplebuilding.client.render;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.custom.BuildingWandItem;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.state.OutlineRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.List;

import static com.simplebuilding.util.guiDrawHelper.drawBoxFill;
import static com.simplebuilding.util.guiDrawHelper.drawBoxOutline;

public class BuildingWandOutlineRenderer {

    private static final double SHRINK_AMOUNT = 0.25;

    public static void register() {
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register(BuildingWandOutlineRenderer::onBlockOutline);
    }

    private static boolean onBlockOutline(WorldRenderContext context, OutlineRenderState outlineRenderState) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null || client.world == null) return true;

        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof BuildingWandItem wandItem)) return true;

        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return true;

        BlockPos centerPos = blockHit.getBlockPos();
        Direction face = blockHit.getSide();

        // 1. Positionen berechnen
        int diameter = wandItem.getWandSquareDiameter();
        List<BlockPos> targetPositions = BuildingWandItem.getBuildingPositions(client.world, player, stack, centerPos, face, diameter, blockHit);

        if (targetPositions.isEmpty()) return true;

        BlockState stateToCopy = client.world.getBlockState(centerPos);
        if (stateToCopy.isAir()) return true;

        VoxelShape baseShape = stateToCopy.getOutlineShape(client.world, centerPos);
        if (baseShape.isEmpty()) return true;

        // --- Rendering Setup ---
        MatrixStack matrices = context.matrices();
        Camera camera = client.gameRenderer.getCamera();

        // KORREKTUR: Nutze getCameraPos() passend zu deiner Camera.class
        Vec3d camPos = camera.getCameraPos();

        double camX = camPos.x;
        double camY = camPos.y;
        double camZ = camPos.z;

        // --- Config & Farben ---
        int opacityPercent = Simplebuilding.getConfig().tools.buildingHighlightOpacity;
        opacityPercent = Math.max(0, Math.min(100, opacityPercent));
        float baseAlpha = opacityPercent / 100.0f;

        float r = 0.1f; float g = 0.1f; float b = 0.1f; float a = 0.3f;
        float r1 = 1.0f; float g1 = 0.5f; float b1 = 0.3f; float a1 = 0.2f * baseAlpha;

        // ====================================================================
        // PASS 1: Nur die LINIE zeichnen (Outlines)
        // ====================================================================
        // Nutze RenderLayers.lines() passend zu deiner RenderLayers.class
        VertexConsumer lines = context.consumers().getBuffer(RenderLayers.lines());

        matrices.push();
        for (BlockPos pos : targetPositions) {
            BlockState targetState = client.world.getBlockState(pos);
            if (!targetState.isReplaceable()) continue;

            for (Box box : baseShape.getBoundingBoxes()) {
                Box shrunkBox = box.contract(SHRINK_AMOUNT);
                matrices.push();
                matrices.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);

                drawBoxOutline(matrices, lines, shrunkBox, r, g, b, a);

                matrices.pop();
            }
        }
        matrices.pop();

        // ====================================================================
        // PASS 2: Nur die FÜLLUNG zeichnen (Quads)
        // ====================================================================
        // Nutze RenderLayers.debugQuads()
        VertexConsumer fill = context.consumers().getBuffer(RenderLayers.debugQuads());

        matrices.push();
        for (BlockPos pos : targetPositions) {
            BlockState targetState = client.world.getBlockState(pos);
            if (!targetState.isReplaceable()) continue;

            for (Box box : baseShape.getBoundingBoxes()) {
                Box shrunkBox = box.contract(SHRINK_AMOUNT);
                matrices.push();
                matrices.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);

                drawBoxFill(matrices, fill, shrunkBox.expand(-0.003), r1, g1, b1, a1);

                matrices.pop();
            }
        }
        matrices.pop();

        return false;
    }

}