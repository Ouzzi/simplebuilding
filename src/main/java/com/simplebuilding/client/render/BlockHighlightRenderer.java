package com.simplebuilding.client.render;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.client.ClientState;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.util.SledgehammerUtils;
import com.simplebuilding.util.guiDrawHelper.RenderColors;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;

import java.util.List;

import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.function.Predicate;

import static com.simplebuilding.util.guiDrawHelper.*;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class BlockHighlightRenderer {

    /** Fabric: subtract camera position so world-space boxes render at the correct location. */
    public static void renderInWorld(PoseStack poseStack, Camera camera) {
        renderInWorldWithCamera(poseStack, camera.position());
    }

    /** NeoForge 26: pose stack is camera-relative; subtract camera before drawing world-space boxes. */
    public static void renderInWorldWithCamera(PoseStack poseStack, Vec3 cameraPos) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        poseStack.pushPose();
        try {
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            ItemStack octantStack = findOctantStack();
            if (!octantStack.isEmpty()) {
                drawOctantHighlights(poseStack, octantStack);
            }

            ItemStack sledgeStack = findSledgehammerStack();
            if (!sledgeStack.isEmpty()) {
                drawSledgehammerHighlights(poseStack, sledgeStack);
            }
        } finally {
            poseStack.popPose();
        }
    }

    @Deprecated
    public static void render(Matrix4f positionMatrix, Camera camera) {
        ItemStack stack = findOctantStack();
        if (stack.isEmpty()) {
            return;
        }
        PoseStack matrices = new PoseStack();
        matrices.pushPose();
        matrices.mulPose(positionMatrix);
        matrices.translate(-camera.position().x, -camera.position().y, -camera.position().z);
        try {
            drawOctantHighlights(matrices, stack);
        } finally {
            matrices.popPose();
        }
    }

    private static ItemStack findOctantStack() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = client.player.getMainHandItem();
        if (stack.getItem() instanceof OctantItem) {
            return stack;
        }
        stack = client.player.getOffhandItem();
        return stack.getItem() instanceof OctantItem ? stack : ItemStack.EMPTY;
    }

    private static ItemStack findSledgehammerStack() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = client.player.getMainHandItem();
        return stack.getItem() instanceof SledgehammerItem ? stack : ItemStack.EMPTY;
    }

    private static void drawSledgehammerHighlights(PoseStack matrices, ItemStack stack) {
        Minecraft client = Minecraft.getInstance();
        HitResult hit = client.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos centerPos = blockHit.getBlockPos();
        List<BlockPos> targetPositions = SledgehammerItem.getBlocksToBeDestroyed(1, centerPos, client.player);
        if (targetPositions.isEmpty()) {
            return;
        }

        int opacityPercent = Simplebuilding.getConfig().tools.buildingHighlightOpacity;
        float baseAlpha = Math.max(0, Math.min(100, opacityPercent)) / 100.0f;
        float lineAlpha = 0.3f;
        float fillAlpha = 0.3f * baseAlpha;

        ByteBufferBuilder mainAllocator = new ByteBufferBuilder(2097152);
        SequencedMap<RenderType, ByteBufferBuilder> layerBuffers = new LinkedHashMap<>();
        layerBuffers.put(RenderTypes.lines(), new ByteBufferBuilder(2097152));
        layerBuffers.put(RenderTypes.debugQuads(), new ByteBufferBuilder(2097152));
        MultiBufferSource.BufferSource consumers = MultiBufferSource.immediateWithBuffers(layerBuffers, mainAllocator);
        VertexConsumer lines = consumers.getBuffer(RenderTypes.lines());
        VertexConsumer fill = consumers.getBuffer(RenderTypes.debugQuads());

        for (BlockPos pos : targetPositions) {
            if (!SledgehammerUtils.shouldBreak(client.level, pos, centerPos, stack)) {
                continue;
            }
            if (pos.equals(centerPos)) {
                continue;
            }

            BlockState state = client.level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }

            VoxelShape shape = state.getShape(client.level, pos);
            if (shape.isEmpty()) {
                continue;
            }

            for (AABB box : shape.toAabbs()) {
                AABB outlineBox = box.move(pos).inflate(0.001);
                drawBoxOutline(matrices, lines, outlineBox, 0.0f, 0.0f, 0.0f, lineAlpha);

                AABB fillBox = box.move(pos).inflate(0.003);
                drawBoxFill(matrices, fill, fillBox, 0.5f, 0.5f, 0.5f, fillAlpha);
            }
        }

        consumers.endBatch();
        mainAllocator.close();
        layerBuffers.values().forEach(ByteBufferBuilder::close);
    }

    private static void drawOctantHighlights(PoseStack matrices, ItemStack stack) {
        CustomData nbtData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = nbtData.copyTag();

        BlockPos pos1 = getPos(nbt, "Pos1");
        BlockPos pos2 = getPos(nbt, "Pos2");

        String shapeName = nbt.getStringOr("Shape", "");
        OctantItem.SelectionShape shape = OctantItem.SelectionShape.CUBOID;
        try { if (!shapeName.isEmpty()) shape = OctantItem.SelectionShape.valueOf(shapeName); } catch (Exception ignored) {}

        int orientIdx = nbt.getIntOr("Orientation", 1); // 0=+X, 1=+Y, 2=+Z, 3=-X, 4=-Y, 5=-Z
        Direction orientation = switch (orientIdx) {
            case 0 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            case 4 -> Direction.DOWN;
            case 5 -> Direction.NORTH;
            default -> Direction.UP;
        };

        if (pos1 == null && pos2 == null) return;

        boolean isInverted = Simplebuilding.getConfig().tools.invertOctantSneak;
        int opacityPercent = Simplebuilding.getConfig().tools.buildingHighlightOpacity;
        boolean hasConstructorsTouch = hasEnchantment(stack, Minecraft.getInstance(), ModEnchantments.CONSTRUCTORS_TOUCH);
        boolean showFill = isInverted ^ hasConstructorsTouch;
        float baseAlpha = Math.max(0, Math.min(100, opacityPercent)) / 100.0f;

        OctantItem octant = (OctantItem) stack.getItem();
        RenderColors colors = getRenderColors(octant.getColor());

        ByteBufferBuilder mainAllocator = new ByteBufferBuilder(2097152);
        SequencedMap<RenderType, ByteBufferBuilder> layerBuffers = new LinkedHashMap<>();
        layerBuffers.put(RenderTypes.lines(), new ByteBufferBuilder(2097152));
        layerBuffers.put(RenderTypes.debugQuads(), new ByteBufferBuilder(2097152));
        MultiBufferSource.BufferSource consumers = MultiBufferSource.immediateWithBuffers(layerBuffers, mainAllocator);
        VertexConsumer lines = consumers.getBuffer(RenderTypes.lines());
        VertexConsumer fill = consumers.getBuffer(RenderTypes.debugQuads());

        float lineAlpha = 0.8f;
        float fillAlpha = 0.3f * baseAlpha;

        if (pos1 != null) drawBoxOutline(matrices, lines, new AABB(pos1).inflate(0.001), colors.r1(), colors.g1(), colors.b1(), lineAlpha);
        if (pos2 != null) drawBoxOutline(matrices, lines, new AABB(pos2).inflate(0.002), colors.r2(), colors.g2(), colors.b2(), lineAlpha);

        if (pos1 != null && pos2 != null && showFill && ClientState.showHighlights) {
            AABB bounds = getFullArea(pos1, pos2);

            Predicate<BlockPos> shapeFunc = switch (shape) {
                // Fix: Uses strict < bounds.maxY to exclude the block above the selection
                case CYLINDER, ELLIPSE -> p -> {
                    // Normalize point relative to bounds center
                    AABB tBounds = transformToY(bounds, orientation.getAxis());
                    BlockPos transformed = transformToY(p, orientation, tBounds);
                    // Check Height (Y in transformed space)
                    if (transformed.getY() < tBounds.minY || transformed.getY() >= tBounds.maxY) return false;
                    return isPointInEllipse(transformed.getX() + 0.5, transformed.getZ() + 0.5, tBounds);
                };
                case SPHERE -> p -> isPointInEllipsoid(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, bounds);
                case PYRAMID -> p -> {
                     AABB tBounds = transformToY(bounds, orientation.getAxis());
                     BlockPos transformed = transformToY(p, orientation, tBounds);
                     return isPointInPyramid(transformed.getX() + 0.5, transformed.getY() + 0.5, transformed.getZ() + 0.5, tBounds);
                };
                case TRIANGLE -> p -> { // Prism
                     AABB tBounds = transformToY(bounds, orientation.getAxis());
                     BlockPos transformed = transformToY(p, orientation, tBounds);
                     return isPointInPrism(transformed.getX() + 0.5, transformed.getY() + 0.5, transformed.getZ() + 0.5, tBounds);
                };
                case RECTANGLE, CUBOID -> p -> true;
            };

            if (shape == OctantItem.SelectionShape.CUBOID || shape == OctantItem.SelectionShape.RECTANGLE) {
                drawBoxOutline(matrices, lines, bounds.inflate(0.003), colors.r3(), colors.g3(), colors.b3(), lineAlpha);
                drawBoxFill(matrices, fill, bounds.inflate(0.009), colors.r3(), colors.g3(), colors.b3(), fillAlpha);
            } else {
                renderVoxelShape(matrices, lines, fill, bounds, shapeFunc, colors.r3(), colors.g3(), colors.b3(), lineAlpha, fillAlpha);
            }
        }
        consumers.endBatch();
        mainAllocator.close();
        layerBuffers.values().forEach(ByteBufferBuilder::close);
    }

    // --- TRANSFORM HELPERS (Rotate space to Y-up) ---
    private static BlockPos transformToY(BlockPos p, Direction orientation, AABB transformedBounds) {
        BlockPos transformed = switch (orientation.getAxis()) {
            case Y -> p;
            case X -> new BlockPos(p.getY(), p.getX(), p.getZ());
            case Z -> new BlockPos(p.getX(), p.getZ(), p.getY());
        };

        if (orientation.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            int flippedY = (int) (transformedBounds.minY + transformedBounds.maxY - 1 - transformed.getY());
            return new BlockPos(transformed.getX(), flippedY, transformed.getZ());
        }

        return transformed;
    }
    private static AABB transformToY(AABB b, Direction.Axis orientation) {
        if (orientation == Direction.Axis.Y) return b;
        if (orientation == Direction.Axis.X) return new AABB(b.minY, b.minX, b.minZ, b.maxY, b.maxX, b.maxZ);
        return new AABB(b.minX, b.minZ, b.minY, b.maxX, b.maxZ, b.maxY);
    }

    // =================================================================================
    // VOXEL SHAPE LOGIK
    // =================================================================================

    private static void renderVoxelShape(PoseStack matrices, VertexConsumer lines, VertexConsumer fill, AABB bounds, Predicate<BlockPos> inShape, float r, float g, float b, float la, float fa) {
        int minX = (int) bounds.minX; int minY = (int) bounds.minY; int minZ = (int) bounds.minZ;
        int maxX = (int) bounds.maxX; int maxY = (int) bounds.maxY; int maxZ = (int) bounds.maxZ;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos diagPos = new BlockPos.MutableBlockPos();

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    pos.set(x, y, z);
                    if (inShape.test(pos)) {
                        for (Direction dir : Direction.values()) {
                            neighborPos.set(pos).move(dir);
                            boolean isSurfaceFace = !inShape.test(neighborPos);
                            if (isSurfaceFace) {
                                drawQuadFace(matrices, fill, new AABB(pos).inflate(0.002), dir, r, g, b, fa);
                                for (Direction edgeDir : Direction.values()) {
                                    if (edgeDir == dir || edgeDir == dir.getOpposite()) continue;
                                    BlockPos sideNeighbor = new BlockPos(pos).offset(edgeDir.getUnitVec3i());
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

    private static void drawEdgeLine(PoseStack matrices, VertexConsumer builder, BlockPos pos, Direction face, Direction edgeDir, float r, float g, float b, float a) {
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

        drawLineWithNormal(builder, matrices.last().pose(), x1, y1, z1, x2, y2, z2, r, g, b, a);
    }

    // --- MATH HELPERS ---
    private static boolean isPointInEllipse(double x, double z, AABB b) {
        double width = b.maxX - b.minX; double length = b.maxZ - b.minZ;
        double cx = b.minX + width / 2.0; double cz = b.minZ + length / 2.0;
        double rx = width / 2.0; double rz = length / 2.0;
        if (rx <= 0 || rz <= 0) return false;
        return Math.pow(x - cx, 2) / Math.pow(rx, 2) + Math.pow(z - cz, 2) / Math.pow(rz, 2) <= 1.0;
    }
    private static boolean isPointInEllipsoid(double x, double y, double z, AABB b) {
        double w = b.maxX - b.minX; double h = b.maxY - b.minY; double l = b.maxZ - b.minZ;
        double cx = b.minX + w/2.0; double cy = b.minY + h/2.0; double cz = b.minZ + l/2.0;
        double rx = w/2.0; double ry = h/2.0; double rz = l/2.0;
        if (rx<=0||ry<=0||rz<=0) return false;
        return Math.pow(x-cx,2)/Math.pow(rx,2) + Math.pow(y-cy,2)/Math.pow(ry,2) + Math.pow(z-cz,2)/Math.pow(rz,2) <= 1.0;
    }
    private static boolean isPointInPyramid(double x, double y, double z, AABB b) {
        double w = b.maxX - b.minX; double h = b.maxY - b.minY; double l = b.maxZ - b.minZ;
        double cx = b.minX + w/2.0; double cz = b.minZ + l/2.0;
        double rx = w/2.0; double rz = l/2.0;
        if (y < b.minY || y >= b.maxY) return false;
        double progress = (y - b.minY) / h;
        double crx = rx * (1.0 - progress);
        double crz = rz * (1.0 - progress);
        return Math.abs(x - cx) <= crx && Math.abs(z - cz) <= crz;
    }
    private static boolean isPointInPrism(double x, double y, double z, AABB b) {
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