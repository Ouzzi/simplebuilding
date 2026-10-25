package com.simplebuilding.modules.simpleearth.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.simplebuilding.modules.simpleearth.SimpleEarth;
import com.simplebuilding.modules.simpleearth.config.EarthClientConfig;
import com.simplebuilding.modules.simpleearth.terrain.TileMath;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.MapColor;

/**
 * Face images of the earth (concept §4.4): one image per terrain face (= tile of the edge length, relative to A), painted
 * pixel by pixel from {@link ClientTiles} in Vanilla map colours and brightness (pixels stay sharp). Missing samples fall back
 * to coarser grids already here (Chunkbase-like progress: coarse first, sharper as tiles arrive); unexplored stays dark.
 * Blended faces (V4/V5 side faces) and the cone-folded faces (V2) are their own images. LRU of {@code textureCache} images.
 * Painting is spread over frames (WD25-16): at most {@code paintBudget} kilopixels a frame, row by row, the renderer asks
 * for the face under the player first; an image still waiting for tiles is repainted at most every {@link #REPAINT_MS}.
 */
public final class FaceTextures {
    public static final int MAX_RES = TileMath.MAX_RES;
    public static final long REPAINT_MS = 250;
    public static final int UNKNOWN = 0xFF1E2333, LOADING = 0xFF2B3044;

    /** What an image shows; tile indices are absolute (relative to A), {@code mixQ} = blend weight in 1/16. */
    public record Key(int tx, int tz, boolean mirror, int mtx, int mtz, boolean mmirror, int mixQ, int res, int foldCx, int foldCz,
                      int foldQ, int reliefQ) {}

    /** Painting context of the current frame. */
    public record Context(double ax, double az, double edge, int e, int seaLevel, double relief, double foldRadius, int res, double px, double pz) {}

    private static final class Entry {
        Identifier id;
        DynamicTexture texture;
        /** Tiles version the finished image shows, -1 = never finished. */
        int version = -1;
        boolean complete;
        /** Next row of the pass in progress ({@code res} = no pass running) and what that pass has seen so far. */
        int row;
        int passVersion;
        boolean passComplete = true;
        long lastPass;
    }

    private static final Map<Key, Entry> CACHE = new LinkedHashMap<>(64, 0.75f, true);
    private static int counter;
    private static long budget;

    private FaceTextures() {}

    public static void newFrame() {
        budget = (long) EarthClientConfig.render("paintBudget") * 1024;
    }

    /** The image for a key, painted (or repainted when new tiles arrived) within the frame budget. Render thread only. */
    public static Identifier get(Key key, Context ctx) {
        Entry e = CACHE.get(key);
        if (e == null) {
            e = new Entry();
            e.id = SimpleEarth.id("earth_face/" + (counter++));
            String label = e.id.toString();
            e.texture = new DynamicTexture(() -> label, key.res(), key.res(), true);
            NativeImage img = e.texture.getPixels();
            if (img != null) img.fillRect(0, 0, key.res(), key.res(), LOADING);
            Minecraft.getInstance().getTextureManager().register(e.id, e.texture);
            CACHE.put(key, e);
            evict();
            e.row = 0;
            e.passVersion = ClientTiles.version;
        } else if (e.row >= key.res() && !e.complete && e.version != ClientTiles.version
                && System.currentTimeMillis() - e.lastPass >= REPAINT_MS) {
            e.row = 0;
            e.passVersion = ClientTiles.version;
            e.passComplete = true;
        }
        if (e.row < key.res() && budget > 0) paint(e, key, ctx);
        return e.id;
    }

    private static void evict() {
        int max = (int) EarthClientConfig.render("textureCache");
        while (CACHE.size() > max) {
            var it = CACHE.entrySet().iterator();
            Entry old = it.next().getValue();
            it.remove();
            Minecraft.getInstance().getTextureManager().release(old.id);
        }
    }

    public static void clear() {
        for (Entry e : CACHE.values()) Minecraft.getInstance().getTextureManager().release(e.id);
        CACHE.clear();
    }

    /** Requests the tiles a face needs at its grid (and the next coarser one, the quick preview), nearest the player first. */
    public static void request(int tx, int tz, Context ctx) {
        double minX = ctx.ax() + tx * ctx.edge() - ctx.edge() / 2, minZ = ctx.az() + tz * ctx.edge() - ctx.edge() / 2;
        for (int e : new int[]{Math.min(TileMath.MAX_E, ctx.e() + 2), ctx.e()}) {
            int t0x = TileMath.tileOf(TileMath.sampleOf(minX, e)), t1x = TileMath.tileOf(TileMath.sampleOf(minX + ctx.edge(), e));
            int t0z = TileMath.tileOf(TileMath.sampleOf(minZ, e)), t1z = TileMath.tileOf(TileMath.sampleOf(minZ + ctx.edge(), e));
            double size = (double) TileMath.TILE * (1L << e);
            java.util.List<long[]> tiles = new java.util.ArrayList<>();
            for (int z = t0z; z <= t1z; z++) {
                for (int x = t0x; x <= t1x; x++) {
                    double dx = (x + 0.5) * size - ctx.px(), dz = (z + 0.5) * size - ctx.pz();
                    tiles.add(new long[]{x, z, (long) (dx * dx / 1e6 + dz * dz / 1e6)});
                }
            }
            tiles.sort(java.util.Comparator.comparingLong(t -> t[2]));
            for (long[] t : tiles) ClientTiles.want(e, (int) t[0], (int) t[1]);
        }
    }

    /** Paints rows of the running pass until the frame budget is used up; uploads what changed. */
    private static void paint(Entry entry, Key key, Context ctx) {
        int n = key.res();
        NativeImage img = entry.texture.getPixels();
        if (img == null) return;
        Sampler a = new Sampler(ctx), b = key.mixQ() > 0 ? new Sampler(ctx) : null;
        double w = key.mixQ() / 16.0, edgeShade = 1 - EarthClientConfig.render("faceEdge");
        int start = entry.row;
        while (entry.row < n && budget > 0) {
            int j = entry.row++;
            for (int i = 0; i < n; i++) {
                int c = a.pixel(key.tx(), key.tz(), key.mirror(), i, j, n, key);
                if (b != null) c = blend(c, b.pixel(key.mtx(), key.mtz(), key.mmirror(), i, j, n, key), w);
                if (i == 0 || j == 0 || i == n - 1 || j == n - 1) c = scale(c, edgeShade); // 1 px edge in map shadow colour
                img.setPixel(i, j, c);
            }
            budget -= n;
        }
        entry.passComplete &= a.complete && (b == null || b.complete);
        if (entry.row > start) entry.texture.upload();
        if (entry.row >= n) {
            entry.version = entry.passVersion;
            entry.complete = entry.passComplete;
            entry.lastPass = System.currentTimeMillis();
        }
    }

    /** Pixels the frame may still paint (renderer debug and tests). */
    public static long budgetLeft() {
        return budget;
    }

    /** Looks samples up on the global grid; remembers the last tile (pixels run along a row). */
    private static final class Sampler {
        final Context ctx;
        boolean complete = true;
        long lastKey = Long.MIN_VALUE;
        ClientTiles.Tile last;

        Sampler(Context ctx) {
            this.ctx = ctx;
        }

        int pixel(int tx, int tz, boolean mirror, int i, int j, int n, Key key) {
            double a = -1 + (i + 0.5) * 2 / n, b = -1 + (j + 0.5) * 2 / n;
            if (mirror) {
                a = -a;
                b = -b;
            }
            double half = ctx.edge() / 2;
            double x = ctx.ax() + tx * ctx.edge() + a * half, z = ctx.az() + tz * ctx.edge() + b * half;
            if (key.foldQ() > 0) {
                double[] f = fold(x, z, ctx.ax() + key.foldCx() * ctx.edge(), ctx.az() + key.foldCz() * ctx.edge(), half, key.foldQ() / 100.0);
                x = f[0];
                z = f[1];
            }
            for (int e = ctx.e(); e <= Math.min(TileMath.MAX_E, ctx.e() + 3); e++) {
                long sx = TileMath.sampleOf(x, e), sz = TileMath.sampleOf(z, e);
                ClientTiles.Tile t = tile(e, sx, sz);
                if (t == null) {
                    complete = false;
                    continue;
                }
                int k = TileMath.inTile(sz) * TileMath.TILE + TileMath.inTile(sx);
                int id = t.colors()[k] & 0xFF;
                if (id == 0) return UNKNOWN;
                int height = t.heights()[k];
                ClientTiles.Tile north = tile(e, sx, sz - 1);
                int northHeight = north == null ? height : north.heights()[TileMath.inTile(sz - 1) * TileMath.TILE + TileMath.inTile(sx)];
                boolean water = id == MapColor.WATER.id;
                int bright = TileMath.brightness(water, height, northHeight, ctx.seaLevel(), e, ctx.relief(), (int) sx, (int) sz);
                return MapColor.getColorFromPackedId(id * 4 + bright);
            }
            return LOADING;
        }

        ClientTiles.Tile tile(int e, long sx, long sz) {
            int tx = TileMath.tileOf(sx), tz = TileMath.tileOf(sz);
            long key = TileMath.key(e, tx, tz);
            if (key != lastKey) {
                lastKey = key;
                last = ClientTiles.get(e, tx, tz);
            }
            return last;
        }
    }

    /**
     * V2 "Kegel-Faltung" (earth-konzept.html fold): within radius {@code r} H of each corner of the centre tile the 360 degrees of
     * the plane are squeezed onto the 270 degrees of the cube corner, so the diagonal tile shows as a wedge.
     */
    public static double[] fold(double wx, double wz, double cx, double cz, double half, double r) {
        double radius = r * half;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                double vx = cx + sx * half, vz = cz + sz * half, dx = wx - vx, dz = wz - vz, d = Math.hypot(dx, dz);
                if (d >= radius || d < 1e-6) continue;
                double g = Math.atan2(-sz, -sx), dl = Math.atan2(dz, dx) - g;
                while (dl > Math.PI) dl -= 2 * Math.PI;
                while (dl < -Math.PI) dl += 2 * Math.PI;
                double q = d / radius, w = 1 - q * q * (3 - 2 * q), angle = g + dl * (1 + w / 3);
                return new double[]{vx + d * Math.cos(angle), vz + d * Math.sin(angle)};
            }
        }
        return new double[]{wx, wz};
    }

    static int blend(int a, int b, double w) {
        int r = (int) Math.round(((a >> 16) & 0xFF) * (1 - w) + ((b >> 16) & 0xFF) * w);
        int g = (int) Math.round(((a >> 8) & 0xFF) * (1 - w) + ((b >> 8) & 0xFF) * w);
        int bl = (int) Math.round((a & 0xFF) * (1 - w) + (b & 0xFF) * w);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    static int scale(int c, double s) {
        return (c & 0xFF000000) | (int) (((c >> 16) & 0xFF) * s) << 16 | (int) (((c >> 8) & 0xFF) * s) << 8 | (int) ((c & 0xFF) * s);
    }
}
