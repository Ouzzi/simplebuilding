package com.simplebuilding.client.render;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.util.SledgehammerUtils;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.List;

import static com.simplebuilding.util.guiDrawHelper.drawBoxFill;
import static com.simplebuilding.util.guiDrawHelper.drawBoxOutline;

public class SledgehammerOutlineRenderer {

    public static void register() {
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register(SledgehammerOutlineRenderer::onBlockOutline);
    }

    private static boolean onBlockOutline(WorldRenderContext context, OutlineRenderState outlineRenderState) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null || client.world == null) return true;

        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof SledgehammerItem)) return true;

        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return true;

        BlockPos centerPos = blockHit.getBlockPos();

        List<BlockPos> targetPositions = SledgehammerItem.getBlocksToBeDestroyed(1, centerPos, player);
        if (targetPositions.isEmpty()) return true;

        // --- Rendering Setup ---
        MatrixStack matrices = context.matrices();
        Camera camera = client.gameRenderer.getCamera();

        // Nutze getCameraPos() oder getPos() je nach Mapping (hier dein Fix)
        Vec3d camPos = camera.getCameraPos();
        double camX = camPos.x;
        double camY = camPos.y;
        double camZ = camPos.z;

        // --- FARBEN & CONFIG ---
        int opacityPercent = Simplebuilding.getConfig().tools.buildingHighlightOpacity;
        opacityPercent = Math.max(0, Math.min(100, opacityPercent));
        float baseAlpha = opacityPercent / 100.0f;

        float r = 0.0f; float g = 0.0f; float b = 0.0f; float a = 0.3f;
        float r1 = 0.5f; float g1 = 0.5f; float b1 = 0.5f; float a1 = 0.3f * baseAlpha;

        // ====================================================================
        // PASS 1: NUR OUTLINES (LINIEN)
        // ====================================================================
        // Buffer holen. Dies ist der einzig aktive Buffer in dieser Schleife.
        VertexConsumer lines = context.consumers().getBuffer(RenderLayers.lines());

        matrices.push();
        for (BlockPos pos : targetPositions) {
            if (!SledgehammerUtils.shouldBreak(client.world, pos, centerPos, stack)) continue;
            if (pos.equals(centerPos)) continue;

            BlockState state = client.world.getBlockState(pos);
            if (state.isAir()) continue;

            VoxelShape shape = state.getOutlineShape(client.world, pos);
            if (shape.isEmpty()) continue;

            for (Box box : shape.getBoundingBoxes()) {
                matrices.push();
                matrices.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);

                // Nur Outline zeichnen
                drawBoxOutline(matrices, lines, box.expand(0.0001), r, g, b, a);

                matrices.pop();
            }
        }
        matrices.pop();
        // Pass 1 Ende. Der 'lines' Buffer kann nun intern geflusht werden.

        // ====================================================================
        // PASS 2: NUR FÜLLUNG (QUADS)
        // ====================================================================
        // Neuen Buffer holen.
        VertexConsumer fill = context.consumers().getBuffer(RenderLayers.debugQuads());

        matrices.push();
        for (BlockPos pos : targetPositions) {
            if (!SledgehammerUtils.shouldBreak(client.world, pos, centerPos, stack)) continue;
            if (pos.equals(centerPos)) continue;

            BlockState state = client.world.getBlockState(pos);
            if (state.isAir()) continue;

            VoxelShape shape = state.getOutlineShape(client.world, pos);
            if (shape.isEmpty()) continue;

            for (Box box : shape.getBoundingBoxes()) {
                matrices.push();
                matrices.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);

                // Nur Füllung zeichnen
                drawBoxFill(matrices, fill, box.expand(0.003), r1, g1, b1, a1);

                matrices.pop();
            }
        }
        matrices.pop();

        return true;
    }
}