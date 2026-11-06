package com.simplebuildingtools.client.render;

import com.simplebuildingtools.util.OctantShape;
import com.simplebuildingtools.util.OctantSurface;

import com.simplebuildingtools.client.ClientState;
import com.simplebuildingtools.enchantment.BtEnchantments;
import com.simplebuildingtools.items.custom.OctantItem;
import com.simplebuildingtools.util.guiDrawHelper.RenderColors;
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

import static com.simplebuildingtools.util.guiDrawHelper.*;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * The held octant's selection in the world (corners, box, figure; also the octant lying in an open cartography table): the octant
 * part of SimpleBuilding's BlockHighlightRenderer until 07.11.2026. Placed octants stay SimpleBuilding's.
 */
public class OctantHighlightRenderer {

    /** Fabric: subtract camera position so world-space boxes render at the correct location. */
    public static void renderInWorld(SubmitNodeCollector collector, PoseStack poseStack, Camera camera) {
        renderInWorldWithCamera(collector, poseStack, camera.position());
    }

    /** NeoForge 26: pose stack is camera-relative; subtract camera before drawing world-space boxes. */
    public static void renderInWorldWithCamera(SubmitNodeCollector collector, PoseStack poseStack, Vec3 cameraPos) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        poseStack.pushPose();
        try {
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            if (!ClientState.showHighlights) {
                return;
            }

            ItemStack octantStack = findOctantStack();
            if (!octantStack.isEmpty()) {
                drawOctantHighlights(collector, poseStack, octantStack, octantFromTable);
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

    private static void drawOctantHighlights(SubmitNodeCollector collector, PoseStack matrices, ItemStack stack, boolean fullFigure) {
        CustomData nbtData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = nbtData.copyTag();

        BlockPos pos1 = getPos(nbt, "Pos1");
        BlockPos pos2 = getPos(nbt, "Pos2");

        // Form und Ausrichtung aus derselben Quelle wie der Blaupausen-Scan (OctantShape).
        OctantItem.SelectionShape shape = OctantShape.shape(nbt);
        Direction orientation = OctantShape.orientation(nbt);

        if (pos1 == null && pos2 == null) return;

        boolean isInverted = ClientState.invertOctantSneak;
        int opacityPercent = ClientState.highlightOpacity;
        boolean hasConstructorsTouch = hasEnchantment(stack, Minecraft.getInstance(), BtEnchantments.CONSTRUCTORS_TOUCH);
        boolean showFill = (isInverted ^ hasConstructorsTouch) || fullFigure;
        float baseAlpha = Math.max(0, Math.min(100, opacityPercent)) / 100.0f;

        OctantItem octant = (OctantItem) stack.getItem();
        RenderColors colors = getRenderColors(octant.getColor());

        RecordedGeometry lines = new RecordedGeometry(true);
        RecordedGeometry fill = new RecordedGeometry(false);

        float lineAlpha = 0.8f;
        float fillAlpha = 0.3f * baseAlpha;

        if (pos1 != null) drawBoxOutline(matrices, lines, new AABB(pos1).inflate(0.001), colors.r1(), colors.g1(), colors.b1(), lineAlpha);
        if (pos2 != null) drawBoxOutline(matrices, lines, new AABB(pos2).inflate(0.002), colors.r2(), colors.g2(), colors.b2(), lineAlpha);

        if (pos1 != null && pos2 != null && showFill && (ClientState.showOctantFigure || fullFigure)) {
            AABB bounds = getFullArea(pos1, pos2);

            Predicate<BlockPos> shapeFunc = OctantShape.predicate(shape, orientation, bounds);

            if (shape == OctantItem.SelectionShape.CUBOID || shape == OctantItem.SelectionShape.RECTANGLE) {
                drawBoxOutline(matrices, lines, bounds.inflate(0.003), colors.r3(), colors.g3(), colors.b3(), lineAlpha);
                drawBoxFill(matrices, fill, bounds.inflate(0.009), colors.r3(), colors.g3(), colors.b3(), fillAlpha);
            } else {
                renderVoxelShape(matrices, lines, fill, List.of(shape, orientation, pos1, pos2), bounds, shapeFunc,
                        colors.r3(), colors.g3(), colors.b3(), lineAlpha, fillAlpha);
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

    /**
     * Die zuletzt gezeichneten Huellen je Schluessel (Form, Ausrichtung, Ecken), siehe
     * {@link OctantSurface}. Mehrere Eintraege, weil neben dem gehaltenen Oktanten auch eingeblendete
     * abgelegte Oktanten je Bild gezeichnet werden - ein einzelner Platz wuerde jedes Bild neu rechnen.
     */
    private static final int SURFACE_CACHE_SIZE = 8;
    private static final java.util.Map<Object, OctantSurface> SURFACES = new java.util.LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<Object, OctantSurface> eldest) {
            return size() > SURFACE_CACHE_SIZE;
        }
    };

    /** Die Huelle zum Schluessel; neu gesucht nur, wenn sich Form, Ausrichtung oder eine Ecke geaendert hat. */
    static OctantSurface surfaceFor(Object key, AABB bounds, Predicate<BlockPos> inShape) {
        OctantSurface surface = SURFACES.get(key);
        if (surface == null) {
            surface = OctantSurface.compute(bounds, inShape);
            SURFACES.put(key, surface);
        }
        return surface;
    }

    private static void renderVoxelShape(PoseStack matrices, VertexConsumer lines, VertexConsumer fill, Object key, AABB bounds, Predicate<BlockPos> inShape, float r, float g, float b, float la, float fa) {
        OctantSurface surface = surfaceFor(key, bounds, inShape);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Direction[] directions = Direction.values();
        for (int face = 0; face < surface.faces(); face++) {
            pos.set(surface.x(face), surface.y(face), surface.z(face));
            Direction dir = surface.side(face);
            drawQuadFace(matrices, fill, new AABB(pos).inflate(0.002), dir, r, g, b, fa);
            for (Direction edgeDir : directions) {
                if (surface.hasEdge(face, edgeDir)) {
                    drawEdgeLine(matrices, lines, pos, dir, edgeDir, r, g, b, la);
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

        // Abstract in VertexConsumer since MC 26.3; a plain extra method on 26.2 (hence no @Override).
        public VertexConsumer setUv3(float u, float v) {
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