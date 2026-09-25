package com.simplebuilding.client.blueprint;

import com.simplebuilding.version.McClientVersion;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simplebuilding.blueprint.BlueprintModel;
import com.simplebuilding.mixin.client.GuiGraphicsExtractorAccessor;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix3x2f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Zeichnet ein Blaupausen-Bauwerk als 3D-Modell in die GUI - fuer den Editor und den Tooltip.
 *
 * <p>Ohne eigenen Picture-in-Picture-Renderer (der je Loader anders registriert wird): die
 * Blockmodelle werden einmal zu einem Netz aus Vierecken zusammengesetzt (innere Flaechen
 * zwischen vollen Bloecken fallen weg), jedes Bild auf der CPU gedreht, orthografisch
 * projiziert, von hinten nach vorn sortiert (Malerverfahren) und als ein einziges
 * {@link GuiElementRenderState} mit dem Block-Atlas eingereicht. Orthografisch bleibt ein
 * Viereck ein Parallelogramm, die Texturkoordinaten sind also exakt.
 */
public final class BlueprintView {
    /** Obergrenze der Vierecke eines Netzes; darueber zeigt die Ansicht nur einen Teil. */
    public static final int MAX_QUADS = 60000;
    /** Mehr Bloecke werden nicht mehr zu einem Netz gebaut (das liefe spuerbar lange auf dem Render-Thread). */
    public static final int MAX_MESH_BLOCKS = 300_000;
    private static final Direction[] DIRECTIONS = Direction.values();

    private static final Map<BlueprintModel.Identity, Mesh> CACHE = new LinkedHashMap<>(8, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<BlueprintModel.Identity, Mesh> eldest) {
            return size() > 6;
        }
    };

    private BlueprintView() {
    }

    /** Das fertige Netz: je Viereck 4 Ecken (xyz), 4 UV-Paare und eine Farbe (Toenung x Schattierung). */
    public static final class Mesh {
        final float[] pos;
        final float[] uv;
        final int[] color;
        final float[] normal;
        final int quads;
        final float radius;
        final boolean truncated;

        Mesh(float[] pos, float[] uv, int[] color, float[] normal, int quads, float radius, boolean truncated) {
            this.pos = pos;
            this.uv = uv;
            this.color = color;
            this.normal = normal;
            this.quads = quads;
            this.radius = radius;
            this.truncated = truncated;
        }

        public boolean truncated() {
            return truncated;
        }

        public int quads() {
            return quads;
        }
    }

    public static Mesh mesh(BlueprintModel model) {
        synchronized (CACHE) {
            Mesh mesh = CACHE.get(new BlueprintModel.Identity(model));
            if (mesh == null) {
                mesh = build(model);
                CACHE.put(new BlueprintModel.Identity(model), mesh);
            }
            return mesh;
        }
    }

    private static Mesh build(BlueprintModel model) {
        Minecraft mc = Minecraft.getInstance();
        BlockColors blockColors = mc.getBlockColors();
        RandomSource random = RandomSource.create();
        FloatArrayList pos = new FloatArrayList();
        FloatArrayList uv = new FloatArrayList();
        FloatArrayList normals = new FloatArrayList();
        IntArrayList colors = new IntArrayList();
        float cx = (model.minX() + model.maxX() + 1) / 2f;
        float cy = (model.minY() + model.maxY() + 1) / 2f;
        float cz = (model.minZ() + model.maxZ() + 1) / 2f;
        int quads = 0;
        boolean truncated = false;
        List<BlockStateModelPart> parts = new ArrayList<>();
        if (model.size() > MAX_MESH_BLOCKS) {
            float sx0 = model.sizeX(), sy0 = model.sizeY(), sz0 = model.sizeZ();
            return new Mesh(new float[0], new float[0], new int[0], new float[0], 0,
                    Math.max(1f, 0.5f * (float) Math.sqrt(sx0 * sx0 + sy0 * sy0 + sz0 * sz0)), true);
        }
        outer:
        for (Int2ObjectMap.Entry<BlockState> e : model.blocks().int2ObjectEntrySet()) {
            int k = e.getIntKey();
            int bx = BlueprintModel.keyX(k), by = BlueprintModel.keyY(k), bz = BlueprintModel.keyZ(k);
            BlockState state = e.getValue();
            BlockStateModel blockModel = mc.getModelManager().getBlockStateModelSet().get(state);
            parts.clear();
            random.setSeed(42L);
            blockModel.collectParts(random, parts);
            int before = quads;
            for (BlockStateModelPart part : parts) {
                for (Direction dir : DIRECTIONS) {
                    BlockState neighbour = model.get(bx + dir.getStepX(), by + dir.getStepY(), bz + dir.getStepZ());
                    if (neighbour != null && neighbour.isSolidRender()) {
                        continue;
                    }
                    for (BakedQuad quad : part.getQuads(dir)) {
                        if (quads >= MAX_QUADS) {
                            truncated = true;
                            break outer;
                        }
                        addQuad(quad, state, blockColors, bx - cx, by - cy, bz - cz, pos, uv, normals, colors);
                        quads++;
                    }
                }
                for (BakedQuad quad : part.getQuads(null)) {
                    if (quads >= MAX_QUADS) {
                        truncated = true;
                        break outer;
                    }
                    addQuad(quad, state, blockColors, bx - cx, by - cy, bz - cz, pos, uv, normals, colors);
                    quads++;
                }
            }
            if (quads == before && hasNoGeometry(parts)) {
                // Block-Entity-Bloecke (Truhe, Schild ...) und Fluessigkeiten haben kein Blockmodell:
                // ein Wuerfel mit ihrem Partikelbild zeigt wenigstens, dass dort etwas steht.
                quads += addParticleCube(blockModel, state, model, bx, by, bz, bx - cx, by - cy, bz - cz, pos, uv, normals, colors);
            }
        }
        float sx = model.sizeX(), sy = model.sizeY(), sz = model.sizeZ();
        float radius = Math.max(1f, 0.5f * (float) Math.sqrt(sx * sx + sy * sy + sz * sz));
        return new Mesh(pos.toFloatArray(), uv.toFloatArray(), colors.toIntArray(), normals.toFloatArray(), quads, radius, truncated);
    }

    private static boolean hasNoGeometry(List<BlockStateModelPart> parts) {
        for (BlockStateModelPart part : parts) {
            for (Direction dir : DIRECTIONS) {
                if (!part.getQuads(dir).isEmpty()) return false;
            }
            if (!part.getQuads(null).isEmpty()) return false;
        }
        return true;
    }

    private static void addQuad(BakedQuad quad, BlockState state, BlockColors blockColors, float ox, float oy, float oz,
                                FloatArrayList pos, FloatArrayList uv, FloatArrayList normals, IntArrayList colors) {
        for (int i = 0; i < 4; i++) {
            Vector3fc p = quad.position(i);
            pos.add(p.x() + ox);
            pos.add(p.y() + oy);
            pos.add(p.z() + oz);
            long packed = quad.packedUV(i);
            uv.add(UVPair.unpackU(packed));
            uv.add(UVPair.unpackV(packed));
        }
        Vector3f a = new Vector3f(quad.position(1)).sub(quad.position(0));
        Vector3f b = new Vector3f(quad.position(2)).sub(quad.position(0));
        Vector3f n = a.cross(b);
        if (n.lengthSquared() < 1.0e-8f) {
            n.set(quad.direction().getStepX(), quad.direction().getStepY(), quad.direction().getStepZ());
        }
        n.normalize();
        normals.add(n.x);
        normals.add(n.y);
        normals.add(n.z);
        int rgb = 0xFFFFFF;
        int tintIndex = quad.materialInfo().tintIndex();
        if (tintIndex != -1) {
            BlockTintSource tint = blockColors.getTintSource(state, tintIndex);
            if (tint != null) {
                rgb = tint.color(state) & 0xFFFFFF;
            }
        }
        float shade = McClientVersion.quadShaded(quad) ? shade(n) : 1f;
        colors.add(ARGB.color(255, (int) (ARGB.red(rgb) * shade), (int) (ARGB.green(rgb) * shade), (int) (ARGB.blue(rgb) * shade)));
    }

    /** Vanillas Richtungsschattierung (oben hell, unten dunkel, Nord/Sued heller als Ost/West). */
    private static float shade(Vector3f n) {
        float up = n.y > 0 ? 1.0f : 0.5f;
        return Math.min(1f, n.x * n.x * 0.6f + n.z * n.z * 0.8f + n.y * n.y * up);
    }

    private static int addParticleCube(BlockStateModel blockModel, BlockState state, BlueprintModel model, int bx, int by, int bz,
                                       float ox, float oy, float oz,
                                       FloatArrayList pos, FloatArrayList uv, FloatArrayList normals, IntArrayList colors) {
        TextureAtlasSprite sprite = blockModel.particleMaterial().sprite();
        int added = 0;
        for (Direction dir : DIRECTIONS) {
            BlockState neighbour = model.get(bx + dir.getStepX(), by + dir.getStepY(), bz + dir.getStepZ());
            if (neighbour != null && neighbour.isSolidRender()) {
                continue;
            }
            float[][] corners = cubeFace(dir);
            for (int i = 0; i < 4; i++) {
                pos.add(corners[i][0] + ox);
                pos.add(corners[i][1] + oy);
                pos.add(corners[i][2] + oz);
            }
            uv.add(sprite.getU0()); uv.add(sprite.getV0());
            uv.add(sprite.getU0()); uv.add(sprite.getV1());
            uv.add(sprite.getU1()); uv.add(sprite.getV1());
            uv.add(sprite.getU1()); uv.add(sprite.getV0());
            Vector3f n = new Vector3f(dir.getStepX(), dir.getStepY(), dir.getStepZ());
            normals.add(n.x);
            normals.add(n.y);
            normals.add(n.z);
            float s = shade(n);
            colors.add(ARGB.color(255, (int) (255 * s), (int) (255 * s), (int) (255 * s)));
            added++;
        }
        return added;
    }

    /** Die vier Ecken einer Wuerfelseite, gegen den Uhrzeigersinn von aussen gesehen. */
    private static float[][] cubeFace(Direction dir) {
        return switch (dir) {
            case UP -> new float[][]{{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}};
            case DOWN -> new float[][]{{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}};
            case NORTH -> new float[][]{{1, 1, 0}, {1, 0, 0}, {0, 0, 0}, {0, 1, 0}};
            case SOUTH -> new float[][]{{0, 1, 1}, {0, 0, 1}, {1, 0, 1}, {1, 1, 1}};
            case WEST -> new float[][]{{0, 1, 0}, {0, 0, 0}, {0, 0, 1}, {0, 1, 1}};
            case EAST -> new float[][]{{1, 1, 1}, {1, 0, 1}, {1, 0, 0}, {1, 1, 0}};
        };
    }

    // =====================================================================================
    // ZEICHNEN
    // =====================================================================================

    /** Standard-Blick: leicht von oben, von vorn rechts auf die Suedseite. */
    public static Quaternionf defaultRotation() {
        return new Quaternionf().rotateX((float) Math.toRadians(28)).rotateY((float) Math.toRadians(-35));
    }

    /**
     * Zeichnet das Netz in das Rechteck {@code x,y,w,h}, gedreht um {@code rotation}; {@code zoom} 1
     * laesst das ganze Bauwerk hineinpassen. Alles ausserhalb des Rechtecks wird abgeschnitten.
     */
    public static void render(GuiGraphicsExtractor graphics, Mesh mesh, int x, int y, int w, int h, Quaternionf rotation, float zoom) {
        if (mesh.quads == 0 || w <= 0 || h <= 0) {
            return;
        }
        Matrix3f rot = new Matrix3f().rotation(rotation);
        float scale = zoom * Math.min(w, h) * 0.5f / mesh.radius;
        float cx = x + w / 2f;
        float cy = y + h / 2f;
        int n = mesh.quads;
        float[] screen = new float[n * 8];
        long[] order = new long[n];
        int visible = 0;
        Vector3f v = new Vector3f();
        for (int q = 0; q < n; q++) {
            v.set(mesh.normal[q * 3], mesh.normal[q * 3 + 1], mesh.normal[q * 3 + 2]);
            rot.transform(v);
            if (v.z < -1.0e-4f) {
                continue; // Rueckseite
            }
            float depth = 0;
            for (int i = 0; i < 4; i++) {
                int p = (q * 4 + i) * 3;
                v.set(mesh.pos[p], mesh.pos[p + 1], mesh.pos[p + 2]);
                rot.transform(v);
                screen[q * 8 + i * 2] = cx + v.x * scale;
                screen[q * 8 + i * 2 + 1] = cy - v.y * scale;
                depth += v.z;
            }
            order[visible++] = ((long) sortableFloat(depth) << 32) | q;
        }
        Arrays.sort(order, 0, visible);
        int[] sorted = new int[visible];
        for (int i = 0; i < visible; i++) {
            sorted[i] = (int) order[i];
        }
        Minecraft mc = Minecraft.getInstance();
        AbstractTexture atlas = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        TextureSetup texture = TextureSetup.singleTexture(atlas.getTextureView(), atlas.getSampler());
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle area = new ScreenRectangle(x, y, w, h).transformMaxBounds(pose);
        ((GuiGraphicsExtractorAccessor) graphics).simplebuilding$guiRenderState()
                .addGuiElement(new MeshState(screen, mesh.uv, mesh.color, sorted, pose, texture, area));
    }

    /** Float-Bits so gedreht, dass die Ganzzahl-Ordnung der Zahlenordnung folgt. */
    private static int sortableFloat(float f) {
        int bits = Float.floatToIntBits(f);
        return bits ^ ((bits >> 31) & 0x7FFFFFFF);
    }

    private record MeshState(float[] screen, float[] uv, int[] color, int[] order, Matrix3x2f pose,
                             TextureSetup textureSetup, ScreenRectangle area) implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer consumer) {
            for (int q : order) {
                int c = color[q];
                for (int i = 0; i < 4; i++) {
                    consumer.addVertexWith2DPose(pose, screen[q * 8 + i * 2], screen[q * 8 + i * 2 + 1])
                            .setUv(uv[q * 8 + i * 2], uv[q * 8 + i * 2 + 1])
                            .setColor(c);
                }
            }
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI_TEXTURED;
        }

        @Override
        public ScreenRectangle scissorArea() {
            return area;
        }

        @Override
        public ScreenRectangle bounds() {
            return area;
        }
    }
}
