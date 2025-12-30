package com.simplebuilding.client.render;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.SimplebuildingClient;
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
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import static com.simplebuilding.util.guiDrawHelper.*;

public class BlockHighlightRenderer {

    public static void render(Matrix4f positionMatrix, Camera camera) {
        // Prüfen, ob Highlights per Keybind deaktiviert wurden
        if (!SimplebuildingClient.showHighlights) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        ItemStack stack = client.player.getMainHandStack();
        boolean isRangefinder = stack.getItem() instanceof OctantItem;

        if (!isRangefinder) {
            stack = client.player.getOffHandStack();
            isRangefinder = stack.getItem() instanceof OctantItem;
        }

        if (isRangefinder) {
            renderHighlights(positionMatrix, camera, stack);
        }
    }

    private static void renderHighlights(Matrix4f positionMatrix, Camera camera, ItemStack stack) {
        NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtData.copyNbt();

        BlockPos pos1 = getPos(nbt, "Pos1");
        BlockPos pos2 = getPos(nbt, "Pos2");

        // --- Form auslesen ---
        String shapeName = nbt.getString("Shape").orElse("");
        OctantItem.SelectionShape shape = OctantItem.SelectionShape.CUBOID; // Default
        try {
            if (!shapeName.isEmpty()) shape = OctantItem.SelectionShape.valueOf(shapeName);
        } catch (Exception ignored) {}

        if (pos1 == null && pos2 == null) return;

        boolean hasConstructorsTouch = hasEnchantment(stack, MinecraftClient.getInstance(), ModEnchantments.CONSTRUCTORS_TOUCH);
        boolean showFill = hasConstructorsTouch;
        // Invertierte Logik aus Config
        boolean isInverted = Simplebuilding.getConfig().tools.invertOctantSneak;
        int opacityPercent = Simplebuilding.getConfig().tools.buildingHighlightOpacity;

        // Deckkraft Berechnung
        float baseAlpha = Math.max(0, Math.min(100, opacityPercent)) / 100.0f;

        // Farben
        OctantItem octant = (OctantItem) stack.getItem();
        DyeColor dyeColor = octant.getColor();

        // Holt jetzt 3 Farben: Pos1, Pos2, Area
        RenderColors colors = getRenderColors(dyeColor);

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

        // --- 1. Kleine Boxen für Pos1/Pos2 (immer sichtbar zur Orientierung) ---
        if (pos1 != null) drawBoxOutline(matrices, lines, new Box(pos1).expand(0.001), colors.r1(), colors.g1(), colors.b1(), lineAlpha);
        if (pos2 != null) drawBoxOutline(matrices, lines, new Box(pos2).expand(0.002), colors.r2(), colors.g2(), colors.b2(), lineAlpha);

        // --- Area Highlight je nach Form ---
        if (pos1 != null && pos2 != null && showFill) {

            // Berechnung der Bounding Box (der gesamte Bereich)
            Box bounds = getFullArea(pos1, pos2);

            switch (shape) {
                case CYLINDER:
                    renderVoxelizedCylinder(matrices, lines, bounds, colors.r3(), colors.g3(), colors.b3(), lineAlpha);
                    break;
                case SPHERE:
                    renderVoxelizedSphere(matrices, lines, bounds, colors.r3(), colors.g3(), colors.b3(), lineAlpha);
                    break;
                case PYRAMID:
                case TRIANGLE:
                    renderVoxelizedPyramid(matrices, lines, bounds, colors.r3(), colors.g3(), colors.b3(), lineAlpha);
                    break;
                case CUBOID:
                default:
                    // Beim Cuboid ist die "Block-Perfekte" Darstellung einfach nur der äußere Rahmen
                    // Wenn du hier auch jeden einzelnen Block am Rand sehen willst, müsstest du eine Loop machen.
                    // Aber "Outer Walls" beim Cuboid ist identisch mit dem Rahmen.
                    drawBoxOutline(matrices, lines, bounds.expand(0.003), colors.r3(), colors.g3(), colors.b3(), lineAlpha);
                    break;
            }
        }

        consumers.draw(RenderLayers.lines());

        matrices.pop();
        allocator.close();
    }

    // =================================================================================
    // VOXEL SHAPE LOGIK
    // =================================================================================

    private static void renderVoxelizedCylinder(MatrixStack matrices, VertexConsumer builder, Box bounds, float r, float g, float b, float a) {
        int minX = (int) bounds.minX; int minY = (int) bounds.minY; int minZ = (int) bounds.minZ;
        int maxX = (int) bounds.maxX; int maxY = (int) bounds.maxY; int maxZ = (int) bounds.maxZ;

        // Zentrum & Radius auf X/Z Ebene berechnen
        double width = maxX - minX;
        double length = maxZ - minZ;

        double centerX = minX + width / 2.0;
        double centerZ = minZ + length / 2.0;
        double radiusX = width / 2.0;
        double radiusZ = length / 2.0;

        // Durch alle Blöcke iterieren
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                // Check: Ist dieser Block im Kreis? (Ellipsen-Formel für Oval-Support)
                if (isPointInEllipse(x + 0.5, z + 0.5, centerX, centerZ, radiusX, radiusZ)) {

                    // Check: Ist es ein Rand-Block? (Hat er einen Nachbarn, der NICHT drin ist?)
                    boolean edgeX = !isPointInEllipse(x + 1.5, z + 0.5, centerX, centerZ, radiusX, radiusZ) || !isPointInEllipse(x - 0.5, z + 0.5, centerX, centerZ, radiusX, radiusZ);
                    boolean edgeZ = !isPointInEllipse(x + 0.5, z + 1.5, centerX, centerZ, radiusX, radiusZ) || !isPointInEllipse(x + 0.5, z - 0.5, centerX, centerZ, radiusX, radiusZ);

                    // Wir zeichnen die Säule nur, wenn sie am Rand des Kreises ist
                    if (edgeX || edgeZ) {
                        // Zeichne eine Box von ganz unten bis ganz oben (Wand)
                        drawBoxOutline(matrices, builder, new Box(x, minY, z, x + 1, maxY, z + 1).expand(0.002), r, g, b, a);
                    } else {
                        // Es ist im Inneren des Zylinders.
                        // Zeichne nur Deckel und Boden für innere Blöcke
                        // (Optional: Wenn du wirklich NUR Außenwände willst, lass das weg)
                        drawBoxOutline(matrices, builder, new Box(x, minY, z, x + 1, minY + 1, z + 1).expand(0.002), r, g, b, a); // Boden
                        drawBoxOutline(matrices, builder, new Box(x, maxY - 1, z, x + 1, maxY, z + 1).expand(0.002), r, g, b, a); // Deckel
                    }
                }
            }
        }
    }

    private static void renderVoxelizedSphere(MatrixStack matrices, VertexConsumer builder, Box bounds, float r, float g, float b, float a) {
        int minX = (int) bounds.minX; int minY = (int) bounds.minY; int minZ = (int) bounds.minZ;
        int maxX = (int) bounds.maxX; int maxY = (int) bounds.maxY; int maxZ = (int) bounds.maxZ;

        double centerX = minX + (maxX - minX) / 2.0;
        double centerY = minY + (maxY - minY) / 2.0;
        double centerZ = minZ + (maxZ - minZ) / 2.0;

        double rx = (maxX - minX) / 2.0;
        double ry = (maxY - minY) / 2.0;
        double rz = (maxZ - minZ) / 2.0;

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {

                    // Ist Block in der Ellipsoid/Kugel?
                    if (isPointInEllipsoid(x + 0.5, y + 0.5, z + 0.5, centerX, centerY, centerZ, rx, ry, rz)) {

                        // Ist er am Rand? (Check 6 Nachbarn)
                        boolean visible =
                                !isPointInEllipsoid(x+1.5, y+0.5, z+0.5, centerX, centerY, centerZ, rx, ry, rz) ||
                                        !isPointInEllipsoid(x-0.5, y+0.5, z+0.5, centerX, centerY, centerZ, rx, ry, rz) ||
                                        !isPointInEllipsoid(x+0.5, y+1.5, z+0.5, centerX, centerY, centerZ, rx, ry, rz) ||
                                        !isPointInEllipsoid(x+0.5, y-0.5, z+0.5, centerX, centerY, centerZ, rx, ry, rz) ||
                                        !isPointInEllipsoid(x+0.5, y+0.5, z+1.5, centerX, centerY, centerZ, rx, ry, rz) ||
                                        !isPointInEllipsoid(x+0.5, y+0.5, z-0.5, centerX, centerY, centerZ, rx, ry, rz);

                        if (visible) {
                            drawBoxOutline(matrices, builder, new Box(x, y, z, x + 1, y + 1, z + 1).expand(0.002), r, g, b, a);
                        }
                    }
                }
            }
        }
    }

    private static void renderVoxelizedPyramid(MatrixStack matrices, VertexConsumer builder, Box bounds, float r, float g, float b, float a) {
        int minX = (int) bounds.minX; int minY = (int) bounds.minY; int minZ = (int) bounds.minZ;
        int maxX = (int) bounds.maxX; int maxY = (int) bounds.maxY; int maxZ = (int) bounds.maxZ;

        double centerX = minX + (maxX - minX) / 2.0;
        double centerZ = minZ + (maxZ - minZ) / 2.0;
        double height = maxY - minY;
        double radiusX = (maxX - minX) / 2.0;
        double radiusZ = (maxZ - minZ) / 2.0;

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {

                    if (isPointInPyramid(x + 0.5, y + 0.5, z + 0.5, centerX, minY, centerZ, radiusX, radiusZ, height)) {

                        boolean visible =
                                !isPointInPyramid(x+1.5, y+0.5, z+0.5, centerX, minY, centerZ, radiusX, radiusZ, height) ||
                                        !isPointInPyramid(x-0.5, y+0.5, z+0.5, centerX, minY, centerZ, radiusX, radiusZ, height) ||
                                        !isPointInPyramid(x+0.5, y+1.5, z+0.5, centerX, minY, centerZ, radiusX, radiusZ, height) ||
                                        !isPointInPyramid(x+0.5, y-0.5, z+0.5, centerX, minY, centerZ, radiusX, radiusZ, height) ||
                                        !isPointInPyramid(x+0.5, y+0.5, z+1.5, centerX, minY, centerZ, radiusX, radiusZ, height) ||
                                        !isPointInPyramid(x+0.5, y+0.5, z-0.5, centerX, minY, centerZ, radiusX, radiusZ, height);

                        if (visible) {
                            drawBoxOutline(matrices, builder, new Box(x, y, z, x + 1, y + 1, z + 1).expand(0.002), r, g, b, a);
                        }
                    }
                }
            }
        }
    }

    // --- Mathe Helper ---

    private static boolean isPointInEllipse(double x, double z, double cx, double cz, double rx, double rz) {
        if (rx <= 0 || rz <= 0) return false;
        double dx = x - cx;
        double dz = z - cz;
        return (dx * dx) / (rx * rx) + (dz * dz) / (rz * rz) <= 1.0;
    }

    private static boolean isPointInEllipsoid(double x, double y, double z, double cx, double cy, double cz, double rx, double ry, double rz) {
        if (rx <= 0 || ry <= 0 || rz <= 0) return false;
        return ((x - cx) * (x - cx)) / (rx * rx) +
                ((y - cy) * (y - cy)) / (ry * ry) +
                ((z - cz) * (z - cz)) / (rz * rz) <= 1.0;
    }

    private static boolean isPointInPyramid(double x, double y, double z, double cx, double baseY, double cz, double rx, double rz, double height) {
        if (y < baseY || y > baseY + height) return false;

        // Fortschritt von unten nach oben (0.0 = Basis, 1.0 = Spitze)
        double progress = (y - baseY) / height;

        // Verfügbarer Radius auf dieser Höhe (wird kleiner nach oben)
        double currentRx = rx * (1.0 - progress);
        double currentRz = rz * (1.0 - progress);

        // Pyramide ist im Prinzip ein shrinking Cuboid
        return Math.abs(x - cx) <= currentRx && Math.abs(z - cz) <= currentRz;
    }
}