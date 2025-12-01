package com.simplebuilding.client.render;

import com.simplebuilding.items.custom.BuildingWandItem;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
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
import org.joml.Matrix4f;

import java.util.List;

public class BuildingWandOutlineRenderer {

    // Wie stark die Box geschrumpft werden soll (0.05 = 5% Abstand zum Rand)
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
        List<BlockPos> targetPositions = BuildingWandItem.getBuildingPositions(client.world, player, stack, centerPos, face, diameter);

        if (targetPositions.isEmpty()) return true;

        // 2. Form des Originalblocks holen (damit Treppen/Zäune richtig aussehen)
        BlockState stateToCopy = client.world.getBlockState(centerPos);
        if (stateToCopy.isAir()) return true;

        VoxelShape baseShape = stateToCopy.getOutlineShape(client.world, centerPos);
        if (baseShape.isEmpty()) return true;

        // --- Rendering Setup ---
        MatrixStack matrices = context.matrices();
        Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        double camX = camPos.x;
        double camY = camPos.y;
        double camZ = camPos.z;

        VertexConsumer lines = context.consumers().getBuffer(RenderLayer.getLines());

        matrices.push();

        // Farbe: Cyan/Türkis (R=0, G=1, B=1)
        float r = 0.1f;
        float g = 0.1f;
        float b = 0.1f;
        float a = 0.3f;

        // 3. Über alle Ziel-Positionen iterieren und EINZELN zeichnen
        for (BlockPos pos : targetPositions) {
            // Prüfung: Ist der Platz frei?
            BlockState targetState = client.world.getBlockState(pos);
            if (!targetState.isReplaceable()) continue;

            // Wir holen alle Boxen der Form (falls es eine komplexe Form wie eine Treppe ist)
            for (Box box : baseShape.getBoundingBoxes()) {
                // A. Box verkleinern (Aesthetic Effect)
                Box shrunkBox = box.contract(SHRINK_AMOUNT);

                matrices.push();

                // B. Zu der spezifischen Welt-Position verschieben (Minus Kamera)
                matrices.translate(
                        pos.getX() - camX,
                        pos.getY() - camY,
                        pos.getZ() - camZ
                );

                // C. Zeichnen
                drawBoxOutline(matrices, lines, shrunkBox, r, g, b, a);

                matrices.pop();
            }
        }

        matrices.pop();

        // false = Wir haben gezeichnet, aber Vanilla soll den Hauptblock trotzdem normal highlighten
        // (Da wir hier nur die *neuen* Positionen zeichnen und nicht den Ursprungsblock)
        return false;
    }

    // --- Hilfsmethoden (Unverändert) ---

    private static void drawBoxOutline(MatrixStack matrices, VertexConsumer builder, Box box, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        double x1 = box.minX; double y1 = box.minY; double z1 = box.minZ;
        double x2 = box.maxX; double y2 = box.maxY; double z2 = box.maxZ;

        // Unten
        drawLineWithNormal(builder, matrix, x1, y1, z1, x2, y1, z1, r, g, b, a);
        drawLineWithNormal(builder, matrix, x2, y1, z1, x2, y1, z2, r, g, b, a);
        drawLineWithNormal(builder, matrix, x2, y1, z2, x1, y1, z2, r, g, b, a);
        drawLineWithNormal(builder, matrix, x1, y1, z2, x1, y1, z1, r, g, b, a);

        // Oben
        drawLineWithNormal(builder, matrix, x1, y2, z1, x2, y2, z1, r, g, b, a);
        drawLineWithNormal(builder, matrix, x2, y2, z1, x2, y2, z2, r, g, b, a);
        drawLineWithNormal(builder, matrix, x2, y2, z2, x1, y2, z2, r, g, b, a);
        drawLineWithNormal(builder, matrix, x1, y2, z2, x1, y2, z1, r, g, b, a);

        // Vertikal
        drawLineWithNormal(builder, matrix, x1, y1, z1, x1, y2, z1, r, g, b, a);
        drawLineWithNormal(builder, matrix, x2, y1, z1, x2, y2, z1, r, g, b, a);
        drawLineWithNormal(builder, matrix, x2, y1, z2, x2, y2, z2, r, g, b, a);
        drawLineWithNormal(builder, matrix, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    private static void drawLineWithNormal(VertexConsumer builder, Matrix4f matrix, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        float nx = (float)(x2 - x1); float ny = (float)(y2 - y1); float nz = (float)(z2 - z1);
        float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0) { nx /= len; ny /= len; nz /= len; }

        builder.vertex(matrix, (float)x1, (float)y1, (float)z1)
                .color(r, g, b, a)
                .normal(nx, ny, nz);

        builder.vertex(matrix, (float)x2, (float)y2, (float)z2)
                .color(r, g, b, a)
                .normal(nx, ny, nz);
    }
}