package com.simplemaps.client;

import com.simplemaps.CubeNet;
import com.simplemaps.Waypoint;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/** Draws a waypoint's icon (owner N65): the 3D block model, one side of the block as a flat picture, or the item. */
public final class WaypointArt {
    private WaypointArt() {}

    public static Item item(Identifier id) {
        return BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
    }

    /** Whether the item has a block model that can be shown in 3D or side by side (owner N65: otherwise no side choice). */
    public static boolean hasBlockModel(Item item) {
        return item instanceof BlockItem;
    }

    public static Direction direction(int face) {
        return switch (face) {
            case CubeNet.EAST -> Direction.EAST;
            case CubeNet.SOUTH -> Direction.SOUTH;
            case CubeNet.WEST -> Direction.WEST;
            case CubeNet.UP -> Direction.UP;
            case CubeNet.DOWN -> Direction.DOWN;
            default -> Direction.NORTH;
        };
    }

    /** The texture of one side of the block (untinted); null when the item has no block model. */
    public static @Nullable TextureAtlasSprite faceSprite(Item item, int face) {
        if (!(item instanceof BlockItem blockItem) || face < CubeNet.NORTH) return null;
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(blockItem.getBlock().defaultBlockState());
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(0L), parts);
        Direction dir = direction(face);
        for (BlockStateModelPart part : parts) {
            for (BakedQuad quad : part.getQuads(dir)) return quad.materialInfo().sprite();
        }
        for (BlockStateModelPart part : parts) {
            for (BakedQuad quad : part.getQuads(null)) if (quad.direction() == dir) return quad.materialInfo().sprite();
        }
        return model.particleMaterial().sprite();
    }

    public static void draw(GuiGraphicsExtractor g, Waypoint w, int x, int y, int size) {
        draw(g, w.icon(), w.face(), x, y, size);
    }

    /** {@code size} px square at {@code x, y}, with the 1 px black outline. */
    public static void draw(GuiGraphicsExtractor g, Identifier icon, int face, int x, int y, int size) {
        draw(g, icon, face, x, y, size, true);
    }

    /**
     * {@code size} px square at {@code x, y} (owner N68): the picture fills the square completely, whatever the model - a
     * side texture is stretched over it, an item or 3D model is scaled by its own bounding box - with a 1 px black outline
     * round the silhouette (inside the square) and no background. {@code outline} false = full square, no outline.
     */
    public static void draw(GuiGraphicsExtractor g, Identifier icon, int face, int x, int y, int size, boolean outline) {
        Item item = item(icon);
        if (item == Items.AIR) return;
        TextureAtlasSprite sprite = face == CubeNet.MODEL ? null : faceSprite(item, face);
        if (sprite != null) {
            int pad = outline ? 1 : 0, s = size - 2 * pad;
            if (outline) for (int[] d : DIRS) g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + pad + d[0], y + pad + d[1], s, s, BLACK);
            g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + pad, y + pad, s, s);
            return;
        }
        drawModel(g, new ItemStack(item), x, y, size, outline);
    }

    /** The item or 3D block model of {@code stack}, scaled so its rendered bounding box fills {@code size} px (less the outline). */
    public static void drawModel(GuiGraphicsExtractor g, ItemStack stack, int x, int y, int size, boolean outline) {
        Minecraft mc = Minecraft.getInstance();
        TrackingItemStackRenderState state = new TrackingItemStackRenderState();
        mc.getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.GUI, mc.level, mc.player, 0);
        List<Vector3f> points = new ArrayList<>();
        state.visitExtents(v -> points.add(new Vector3f(v)));
        float[] box = bounds(points);
        float extent = box == null ? 0 : Math.max(box[2] - box[0], box[3] - box[1]) * 16.0F;
        int pad = outline ? 1 : 0;
        g.pose().pushMatrix();
        if (extent <= 0) {
            // No usable geometry: the plain item, 16 px scaled to the square.
            g.pose().translate(x + pad, y + pad);
            g.pose().scale((size - 2 * pad) / 16.0F, (size - 2 * pad) / 16.0F);
            g.item(stack, 0, 0);
            g.pose().popMatrix();
            return;
        }
        float k = (size - 2 * pad) / extent, cx = (box[0] + box[2]) / 2, cy = (box[1] + box[3]) / 2;
        // Item space: the 16 px cell has its middle at (8, 8), +y of the model points up. Put the box's middle on the square's middle.
        g.pose().translate(x + size / 2.0F, y + size / 2.0F);
        g.pose().scale(k, k);
        g.pose().translate(-(8 + cx * 16), -(8 - cy * 16));
        if (outline) {
            boolean flat = box[5] - box[4] < FLAT_DEPTH;
            var material = flat ? state.pickParticleMaterial(RandomSource.create(0L)) : null;
            for (int[] d : DIRS) {
                g.pose().pushMatrix();
                g.pose().translate(d[0] / k, d[1] / k);
                if (material != null) {
                    int lx = Math.round(8 + box[0] * 16), ly = Math.round(8 - box[3] * 16);
                    g.blitSprite(RenderPipelines.GUI_TEXTURED, material.sprite(), lx, ly, Math.round((box[2] - box[0]) * 16), Math.round((box[3] - box[1]) * 16), BLACK);
                } else {
                    silhouette(g, points);
                }
                g.pose().popMatrix();
            }
        }
        g.item(stack, 0, 0);
        g.pose().popMatrix();
    }

    private static final int BLACK = 0xFF000000;
    /** Offsets of the four black passes that make the 1 px outline. */
    private static final int[][] DIRS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    /** A model thinner than this (1/16 = a flat item) counts as a flat picture: its outline follows the sprite's pixels. */
    private static final float FLAT_DEPTH = 0.1F;

    /** minX, minY, maxX, maxY, minZ, maxZ of the points, null when there are none. */
    static float @Nullable [] bounds(List<Vector3f> points) {
        if (points.isEmpty()) return null;
        float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE};
        for (Vector3f p : points) {
            b[0] = Math.min(b[0], p.x);
            b[1] = Math.min(b[1], p.y);
            b[2] = Math.max(b[2], p.x);
            b[3] = Math.max(b[3], p.y);
            b[4] = Math.min(b[4], p.z);
            b[5] = Math.max(b[5], p.z);
        }
        return b;
    }

    /** Black fill of the convex hull of the model's corners (item-space pixels), row by row. */
    private static void silhouette(GuiGraphicsExtractor g, List<Vector3f> points) {
        float[][] hull = hull(points);
        float top = Float.MAX_VALUE, bottom = -Float.MAX_VALUE;
        for (float[] p : hull) {
            top = Math.min(top, p[1]);
            bottom = Math.max(bottom, p[1]);
        }
        for (int row = (int) Math.floor(top); row < Math.ceil(bottom); row++) {
            float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE, yc = row + 0.5F;
            for (int i = 0; i < hull.length; i++) {
                float[] a = hull[i], c = hull[(i + 1) % hull.length];
                if ((a[1] <= yc) == (c[1] <= yc)) continue;
                float x = a[0] + (yc - a[1]) / (c[1] - a[1]) * (c[0] - a[0]);
                lo = Math.min(lo, x);
                hi = Math.max(hi, x);
            }
            if (hi >= lo) g.fill((int) Math.floor(lo), row, (int) Math.ceil(hi), row + 1, BLACK);
        }
    }

    /** Convex hull (monotone chain) of the points projected to item-space pixels, counter-clockwise in y-down screen terms. */
    static float[][] hull(List<Vector3f> points) {
        List<float[]> pts = new ArrayList<>();
        for (Vector3f p : points) pts.add(new float[] {8 + p.x * 16, 8 - p.y * 16});
        pts.sort((a, b) -> a[0] != b[0] ? Float.compare(a[0], b[0]) : Float.compare(a[1], b[1]));
        if (pts.size() < 3) return pts.toArray(new float[0][]);
        float[][] h = new float[2 * pts.size()][];
        int n = 0;
        for (float[] p : pts) {
            while (n >= 2 && cross(h[n - 2], h[n - 1], p) <= 0) n--;
            h[n++] = p;
        }
        for (int i = pts.size() - 2, lower = n + 1; i >= 0; i--) {
            float[] p = pts.get(i);
            while (n >= lower && cross(h[n - 2], h[n - 1], p) <= 0) n--;
            h[n++] = p;
        }
        return java.util.Arrays.copyOf(h, Math.max(1, n - 1));
    }

    private static float cross(float[] o, float[] a, float[] b) {
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
    }
}
