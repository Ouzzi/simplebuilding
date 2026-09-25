package com.simplebuilding.client.render;

import com.simplebuilding.util.OctantShape;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.client.ClientState;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.util.SledgehammerUtils;
import com.simplebuilding.util.guiDrawHelper.RenderColors;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
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

import java.util.Arrays;
import java.util.List;

import java.util.function.Predicate;

import static com.simplebuilding.util.guiDrawHelper.*;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class BlockHighlightRenderer {

    /** Fabric: subtract camera position so world-space boxes render at the correct location. */
    public static void renderInWorld(SubmitNodeCollector collector, PoseStack poseStack, Camera camera) {
        renderInWorldWithCamera(collector, poseStack, camera.position());
    }

    /** NeoForge 26: pose stack is camera-relative; subtract camera before drawing world-space boxes. */
    public static void renderInWorldWithCamera(SubmitNodeCollector collector, PoseStack poseStack, Vec3 cameraPos) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || !ClientState.showHighlights) {
            return;
        }

        poseStack.pushPose();
        try {
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            ItemStack octantStack = findOctantStack();
            if (!octantStack.isEmpty()) {
                drawOctantHighlights(collector, poseStack, octantStack);
            }

            ItemStack sledgeStack = findSledgehammerStack();
            if (!sledgeStack.isEmpty()) {
                drawSledgehammerHighlights(collector, poseStack, sledgeStack);
            }
        } finally {
            poseStack.popPose();
        }
    }

    /** Der Oktant liegt im Kartentisch: seine Auswahl wird voll gezeigt, damit man sieht, was gescannt wird. */
    private static boolean octantFromTable;

    private static ItemStack findOctantStack() {
        Minecraft client = Minecraft.getInstance();
        octantFromTable = false;
        if (client.player == null) {
            return ItemStack.EMPTY;
        }
        if (client.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.CartographyTableScreen table) {
            ItemStack inTable = table.getMenu().getSlot(0).getItem();
            if (inTable.getItem() instanceof OctantItem) {
                octantFromTable = true;
                return inTable;
            }
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

    private static void drawSledgehammerHighlights(SubmitNodeCollector collector, PoseStack matrices, ItemStack stack) {
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

        RecordedGeometry lines = new RecordedGeometry(true);
        RecordedGeometry fill = new RecordedGeometry(false);

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

        submit(collector, matrices, lines, fill);
    }

    private static void drawOctantHighlights(SubmitNodeCollector collector, PoseStack matrices, ItemStack stack) {
        CustomData nbtData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = nbtData.copyTag();

        BlockPos pos1 = getPos(nbt, "Pos1");
        BlockPos pos2 = getPos(nbt, "Pos2");

        // Form und Ausrichtung aus derselben Quelle wie der Blaupausen-Scan (OctantShape).
        OctantItem.SelectionShape shape = OctantShape.shape(nbt);
        Direction orientation = OctantShape.orientation(nbt);

        if (pos1 == null && pos2 == null) return;

        boolean isInverted = Simplebuilding.getConfig().tools.invertOctantSneak;
        int opacityPercent = Simplebuilding.getConfig().tools.buildingHighlightOpacity;
        boolean hasConstructorsTouch = hasEnchantment(stack, Minecraft.getInstance(), ModEnchantments.CONSTRUCTORS_TOUCH);
        boolean showFill = (isInverted ^ hasConstructorsTouch) || octantFromTable;
        float baseAlpha = Math.max(0, Math.min(100, opacityPercent)) / 100.0f;

        OctantItem octant = (OctantItem) stack.getItem();
        RenderColors colors = getRenderColors(octant.getColor());

        RecordedGeometry lines = new RecordedGeometry(true);
        RecordedGeometry fill = new RecordedGeometry(false);

        float lineAlpha = 0.8f;
        float fillAlpha = 0.3f * baseAlpha;

        if (pos1 != null) drawBoxOutline(matrices, lines, new AABB(pos1).inflate(0.001), colors.r1(), colors.g1(), colors.b1(), lineAlpha);
        if (pos2 != null) drawBoxOutline(matrices, lines, new AABB(pos2).inflate(0.002), colors.r2(), colors.g2(), colors.b2(), lineAlpha);

        if (pos1 != null && pos2 != null && showFill && (ClientState.showOctantFigure || octantFromTable)) {
            AABB bounds = getFullArea(pos1, pos2);

            Predicate<BlockPos> shapeFunc = OctantShape.predicate(shape, orientation, bounds);

            if (shape == OctantItem.SelectionShape.CUBOID || shape == OctantItem.SelectionShape.RECTANGLE) {
                drawBoxOutline(matrices, lines, bounds.inflate(0.003), colors.r3(), colors.g3(), colors.b3(), lineAlpha);
                drawBoxFill(matrices, fill, bounds.inflate(0.009), colors.r3(), colors.g3(), colors.b3(), fillAlpha);
            } else {
                renderVoxelShape(matrices, lines, fill, bounds, shapeFunc, colors.r3(), colors.g3(), colors.b3(), lineAlpha, fillAlpha);
            }
        }
        submit(collector, matrices, lines, fill);
    }

    /**
     * Reicht die gesammelte Geometrie an das Submit-Node-System (26.2) weiter.
     * Ersetzt {@code MultiBufferSource.immediateWithBuffers(...) + endBatch()}: die Reihenfolge
     * bleibt "erst Linien, dann Füllflächen", genau wie beim alten SequencedMap-Puffer.
     */
    private static void submit(SubmitNodeCollector collector, PoseStack matrices, RecordedGeometry lines, RecordedGeometry fill) {
        if (!lines.isEmpty()) {
            collector.submitCustomGeometry(matrices, RenderTypes.lines(), (pose, buffer) -> lines.replay(buffer));
        }
        if (!fill.isEmpty()) {
            collector.submitCustomGeometry(matrices, RenderTypes.debugQuads(), (pose, buffer) -> fill.replay(buffer));
        }
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

    /**
     * Puffert Vertices, bis das Submit-Node-System den echten {@link VertexConsumer} bereitstellt.
     * <p>
     * Ab 26.2 gibt es keinen Immediate-Puffer mehr: {@code SubmitNodeCollector.submitCustomGeometry}
     * ruft den Zeichen-Callback erst später in der Render-Phase auf und liefert dann erst den
     * VertexConsumer der jeweiligen RenderType-Gruppe. Da die Geometrie für Linien und Füllflächen
     * in einem gemeinsamen Durchlauf entsteht (siehe {@link #renderVoxelShape}), wird sie hier
     * einmalig aufgezeichnet und in den beiden Callbacks nur noch abgespielt.
     * <p>
     * Aufgezeichnet werden die bereits mit {@code matrices.last().pose()} transformierten
     * Positionen (die Default-Methode {@code addVertex(Matrix4fc, ...)} rechnet vor dem Aufruf von
     * {@link #addVertex(float, float, float)} um) — das Ergebnis ist damit vertex-identisch zum
     * bisherigen Immediate-Puffer.
     */
    private static final class RecordedGeometry implements VertexConsumer {

        private static final int INITIAL_CAPACITY = 256;

        /** true für RenderTypes.lines() (Position/Farbe/Normale/Linienbreite), false für debugQuads() (Position/Farbe). */
        private final boolean withNormals;

        private float[] positions = new float[3 * INITIAL_CAPACITY];
        private int[] colors = new int[INITIAL_CAPACITY];
        private float[] normals;
        private float[] lineWidths;
        private int vertexCount;

        RecordedGeometry(boolean withNormals) {
            this.withNormals = withNormals;
            if (withNormals) {
                this.normals = new float[3 * INITIAL_CAPACITY];
                this.lineWidths = new float[INITIAL_CAPACITY];
            }
        }

        boolean isEmpty() {
            return vertexCount == 0;
        }

        private void grow() {
            int capacity = colors.length * 2;
            positions = Arrays.copyOf(positions, capacity * 3);
            colors = Arrays.copyOf(colors, capacity);
            if (withNormals) {
                normals = Arrays.copyOf(normals, capacity * 3);
                lineWidths = Arrays.copyOf(lineWidths, capacity);
            }
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if (vertexCount == colors.length) {
                grow();
            }
            int base = vertexCount * 3;
            positions[base] = x;
            positions[base + 1] = y;
            positions[base + 2] = z;
            colors[vertexCount] = 0xFFFFFFFF;
            if (withNormals) {
                normals[base] = 0.0f;
                normals[base + 1] = 0.0f;
                normals[base + 2] = 0.0f;
                lineWidths[vertexCount] = 1.0f;
            }
            vertexCount++;
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            if (vertexCount > 0) {
                colors[vertexCount - 1] = (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
            }
            return this;
        }

        @Override
        public VertexConsumer setColor(int argb) {
            if (vertexCount > 0) {
                colors[vertexCount - 1] = argb;
            }
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float nx, float ny, float nz) {
            if (withNormals && vertexCount > 0) {
                int base = (vertexCount - 1) * 3;
                normals[base] = nx;
                normals[base + 1] = ny;
                normals[base + 2] = nz;
            }
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            if (withNormals && vertexCount > 0) {
                lineWidths[vertexCount - 1] = width;
            }
            return this;
        }

        void replay(VertexConsumer out) {
            for (int vertex = 0; vertex < vertexCount; vertex++) {
                int base = vertex * 3;
                int argb = colors[vertex];
                VertexConsumer written = out.addVertex(positions[base], positions[base + 1], positions[base + 2])
                        .setColor((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24) & 255);
                if (withNormals) {
                    written.setNormal(normals[base], normals[base + 1], normals[base + 2])
                            .setLineWidth(lineWidths[vertex]);
                }
            }
        }
    }
}