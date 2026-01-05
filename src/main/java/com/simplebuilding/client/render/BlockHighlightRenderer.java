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
import net.minecraft.util.math.Direction;
import org.joml.Matrix4f;

import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.function.Predicate;

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
        if (isRangefinder) renderHighlights(positionMatrix, camera, stack);
    }

    private static void renderHighlights(Matrix4f positionMatrix, Camera camera, ItemStack stack) {
        NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtData.copyNbt();

        BlockPos pos1 = getPos(nbt, "Pos1");
        BlockPos pos2 = getPos(nbt, "Pos2");

        String shapeName = nbt.getString("Shape", "");
        OctantItem.SelectionShape shape = OctantItem.SelectionShape.CUBOID;
        try { if (!shapeName.isEmpty()) shape = OctantItem.SelectionShape.valueOf(shapeName); } catch (Exception ignored) {}

        int orientIdx = nbt.getInt("Orientation", 1); // 0=X, 1=Y, 2=Z
        Direction.Axis orientation = orientIdx == 0 ? Direction.Axis.X : (orientIdx == 2 ? Direction.Axis.Z : Direction.Axis.Y);

        if (pos1 == null && pos2 == null) return;

        boolean isInverted = Simplebuilding.getConfig().tools.invertOctantSneak;
        int opacityPercent = Simplebuilding.getConfig().tools.buildingHighlightOpacity;
        boolean hasConstructorsTouch = hasEnchantment(stack, MinecraftClient.getInstance(), ModEnchantments.CONSTRUCTORS_TOUCH);
        boolean showFill = isInverted ^ hasConstructorsTouch;
        float baseAlpha = Math.max(0, Math.min(100, opacityPercent)) / 100.0f;

        OctantItem octant = (OctantItem) stack.getItem();
        RenderColors colors = getRenderColors(octant.getColor());

        double camX = camera.getCameraPos().x;
        double camY = camera.getCameraPos().y;
        double camZ = camera.getCameraPos().z;

        MatrixStack matrices = new MatrixStack();
        matrices.push();
        matrices.multiplyPositionMatrix(positionMatrix);
        matrices.translate(-camX, -camY, -camZ);

        BufferAllocator mainAllocator = new BufferAllocator(2097152);
        SequencedMap<RenderLayer, BufferAllocator> layerBuffers = new LinkedHashMap<>();
        layerBuffers.put(RenderLayers.lines(), new BufferAllocator(2097152));
        layerBuffers.put(RenderLayers.debugQuads(), new BufferAllocator(2097152));
        VertexConsumerProvider.Immediate consumers = VertexConsumerProvider.immediate(layerBuffers, mainAllocator);
        VertexConsumer lines = consumers.getBuffer(RenderLayers.lines());
        VertexConsumer fill = consumers.getBuffer(RenderLayers.debugQuads());

        float lineAlpha = 0.8f;
        float fillAlpha = 0.3f * baseAlpha;

        if (pos1 != null) drawBoxOutline(matrices, lines, new Box(pos1).expand(0.001), colors.r1(), colors.g1(), colors.b1(), lineAlpha);
        if (pos2 != null) drawBoxOutline(matrices, lines, new Box(pos2).expand(0.002), colors.r2(), colors.g2(), colors.b2(), lineAlpha);

        if (pos1 != null && pos2 != null && showFill && SimplebuildingClient.showHighlights) {
            Box bounds = getFullArea(pos1, pos2);

            // Adjust bounds based on 2D Shapes to make them flat 1 block thick if requested by shape type?
            // Actually, Octant positions define the box. 2D shapes are usually just subsets of that box.
            // If "RECTANGLE" or "ELLIPSE" is selected, we enforce flat selection on the orientation axis in logic below or just treat it as such.

            Predicate<BlockPos> shapeFunc = switch (shape) {
                // Fix: Uses strict < bounds.maxY to exclude the block above the selection
                case CYLINDER, ELLIPSE -> p -> {
                    // Normalize point relative to bounds center
                    BlockPos transformed = transformToY(p, orientation);
                    Box tBounds = transformToY(bounds, orientation);
                    // Check Height (Y in transformed space)
                    // If shape is 2D (Ellipse), we treat it as 1-block thick in transformed Y? Or just normal check?
                    // "Closed from below" fix: Use < maxY
                    if (transformed.getY() < tBounds.minY || transformed.getY() >= tBounds.maxY) return false;
                    return isPointInEllipse(transformed.getX() + 0.5, transformed.getZ() + 0.5, tBounds);
                };
                case SPHERE -> p -> isPointInEllipsoid(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, bounds);
                case PYRAMID -> p -> {
                     BlockPos transformed = transformToY(p, orientation);
                     Box tBounds = transformToY(bounds, orientation);
                     return isPointInPyramid(transformed.getX() + 0.5, transformed.getY() + 0.5, transformed.getZ() + 0.5, tBounds);
                };
                case TRIANGLE -> p -> { // Prism
                     BlockPos transformed = transformToY(p, orientation);
                     Box tBounds = transformToY(bounds, orientation);
                     return isPointInPrism(transformed.getX() + 0.5, transformed.getY() + 0.5, transformed.getZ() + 0.5, tBounds);
                };
                case RECTANGLE -> p -> {
                    // Check if on plane?
                    // For now Rectangle behaves like Cuboid but implies flat intention.
                    // If orientation is Y, we might want to check only one Y layer?
                    // But usually user defines pos1/pos2 flatly for rectangle.
                    // We just treat as cuboid.
                    return true;
                };
                case CUBOID -> p -> true;
            };

            if (shape == OctantItem.SelectionShape.CUBOID || shape == OctantItem.SelectionShape.RECTANGLE) {
                drawBoxOutline(matrices, lines, bounds.expand(0.003), colors.r3(), colors.g3(), colors.b3(), lineAlpha);
                drawBoxFill(matrices, fill, bounds.expand(0.009), colors.r3(), colors.g3(), colors.b3(), fillAlpha);
            } else {
                renderVoxelShape(matrices, lines, fill, bounds, shapeFunc, colors.r3(), colors.g3(), colors.b3(), lineAlpha, fillAlpha);
            }
        }
        consumers.draw();
        matrices.pop();
        mainAllocator.close();
        layerBuffers.values().forEach(BufferAllocator::close);
    }

    // --- TRANSFORM HELPERS (Rotate space to Y-up) ---
    private static BlockPos transformToY(BlockPos p, Direction.Axis orientation) {
        if (orientation == Direction.Axis.Y) return p;
        if (orientation == Direction.Axis.X) return new BlockPos(p.getY(), p.getX(), p.getZ()); // Swap X/Y
        return new BlockPos(p.getX(), p.getZ(), p.getY()); // Swap Z/Y
    }
    private static Box transformToY(Box b, Direction.Axis orientation) {
        if (orientation == Direction.Axis.Y) return b;
        if (orientation == Direction.Axis.X) return new Box(b.minY, b.minX, b.minZ, b.maxY, b.maxX, b.maxZ);
        return new Box(b.minX, b.minZ, b.minY, b.maxX, b.maxZ, b.maxY);
    }

    // =================================================================================
    // VOXEL SHAPE LOGIK
    // =================================================================================

    private static void renderVoxelShape(MatrixStack matrices, VertexConsumer lines, VertexConsumer fill, Box bounds, Predicate<BlockPos> inShape, float r, float g, float b, float la, float fa) {
        int minX = (int) bounds.minX; int minY = (int) bounds.minY; int minZ = (int) bounds.minZ;
        int maxX = (int) bounds.maxX; int maxY = (int) bounds.maxY; int maxZ = (int) bounds.maxZ;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        BlockPos.Mutable neighborPos = new BlockPos.Mutable();
        BlockPos.Mutable diagPos = new BlockPos.Mutable();

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    pos.set(x, y, z);
                    if (inShape.test(pos)) {
                        for (Direction dir : Direction.values()) {
                            neighborPos.set(pos).move(dir);
                            boolean isSurfaceFace = !inShape.test(neighborPos);
                            if (isSurfaceFace) {
                                drawQuadFace(matrices, fill, new Box(pos).expand(0.002), dir, r, g, b, fa);
                                for (Direction edgeDir : Direction.values()) {
                                    if (edgeDir == dir || edgeDir == dir.getOpposite()) continue;
                                    BlockPos sideNeighbor = new BlockPos(pos).add(edgeDir.getVector());
                                    diagPos.set(neighborPos).move(edgeDir);
                                    boolean sideIsShape = inShape.test(sideNeighbor);
                                    boolean diagIsShape = inShape.test(diagPos);
                                    if (!sideIsShape || diagIsShape) {
                                        drawEdgeLine(matrices, lines, pos, dir, edgeDir, r, g, b, la);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void drawEdgeLine(MatrixStack matrices, VertexConsumer builder, BlockPos pos, Direction face, Direction edgeDir, float r, float g, float b, float a) {
        // Koordinaten berechnen
        double x1 = pos.getX(); double y1 = pos.getY(); double z1 = pos.getZ();
        double x2 = x1 + 1; double y2 = y1 + 1; double z2 = z1 + 1;

        // Flache Seite anpassen
        if (face == Direction.DOWN) y2 = y1;
        if (face == Direction.UP)   y1 = y2;
        if (face == Direction.NORTH) z2 = z1;
        if (face == Direction.SOUTH) z1 = z2;
        if (face == Direction.WEST) x2 = x1;
        if (face == Direction.EAST) x1 = x2;

        // Kante einschränken
        if (edgeDir == Direction.DOWN)  y2 = y1;
        if (edgeDir == Direction.UP)    y1 = y2;
        if (edgeDir == Direction.NORTH) z2 = z1;
        if (edgeDir == Direction.SOUTH) z1 = z2;
        if (edgeDir == Direction.WEST)  x2 = x1;
        if (edgeDir == Direction.EAST)  x1 = x2;

        // Reset der Achse, die die Linie bildet (damit sie Länge 1 hat)
        // Die Linie verläuft entlang der Achse, die weder face noch edgeDir ist.
        if (face.getAxis() != Direction.Axis.X && edgeDir.getAxis() != Direction.Axis.X) { x1 = pos.getX(); x2 = pos.getX() + 1; }
        if (face.getAxis() != Direction.Axis.Y && edgeDir.getAxis() != Direction.Axis.Y) { y1 = pos.getY(); y2 = pos.getY() + 1; }
        if (face.getAxis() != Direction.Axis.Z && edgeDir.getAxis() != Direction.Axis.Z) { z1 = pos.getZ(); z2 = pos.getZ() + 1; }

        drawLineWithNormal(builder, matrices.peek().getPositionMatrix(), x1, y1, z1, x2, y2, z2, r, g, b, a);
    }

    // --- MATH HELPERS ---
    private static boolean isPointInEllipse(double x, double z, Box b) {
        double width = b.maxX - b.minX; double length = b.maxZ - b.minZ;
        double cx = b.minX + width / 2.0; double cz = b.minZ + length / 2.0;
        double rx = width / 2.0; double rz = length / 2.0;
        if (rx <= 0 || rz <= 0) return false;
        return Math.pow(x - cx, 2) / Math.pow(rx, 2) + Math.pow(z - cz, 2) / Math.pow(rz, 2) <= 1.0;
    }

    private static boolean isPointInEllipsoid(double x, double y, double z, Box b) {
        double w = b.maxX - b.minX; double h = b.maxY - b.minY; double l = b.maxZ - b.minZ;
        double cx = b.minX + w/2.0; double cy = b.minY + h/2.0; double cz = b.minZ + l/2.0;
        double rx = w/2.0; double ry = h/2.0; double rz = l/2.0;
        if (rx<=0||ry<=0||rz<=0) return false;
        return Math.pow(x-cx,2)/Math.pow(rx,2) + Math.pow(y-cy,2)/Math.pow(ry,2) + Math.pow(z-cz,2)/Math.pow(rz,2) <= 1.0;
    }

    private static boolean isPointInPyramid(double x, double y, double z, Box b) {
        double w = b.maxX - b.minX; double h = b.maxY - b.minY; double l = b.maxZ - b.minZ;
        double cx = b.minX + w/2.0; double cz = b.minZ + l/2.0;
        double rx = w/2.0; double rz = l/2.0;
        // Fix: Bounds strict check
        if (y < b.minY || y >= b.maxY) return false;
        double progress = (y - b.minY) / h;
        double crx = rx * (1.0 - progress);
        double crz = rz * (1.0 - progress);
        return Math.abs(x - cx) <= crx && Math.abs(z - cz) <= crz;
    }

    private static boolean isPointInPrism(double x, double y, double z, Box b) {
        double wX = b.maxX - b.minX; double h = b.maxY - b.minY; double wZ = b.maxZ - b.minZ;
        if (y < b.minY || y >= b.maxY) return false;
        boolean alongZ = wZ > wX;
        double progress = (y - b.minY) / h;
        if (alongZ) {
            double cx = b.minX + wX / 2.0;
            return Math.abs(x - cx) <= (wX / 2.0) * (1.0 - progress);
        } else {
            double cz = b.minZ + wZ / 2.0;
            return Math.abs(z - cz) <= (wZ / 2.0) * (1.0 - progress);
        }
    }
}