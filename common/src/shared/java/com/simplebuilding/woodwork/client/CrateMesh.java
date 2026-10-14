package com.simplebuilding.woodwork.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.simplebuilding.woodwork.CrateBlock;
import com.simplebuilding.woodwork.CrateBlockEntity;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * The picture of a crate's contents (Queue N55), built once per content change and kept in
 * {@link CrateBlockEntity#clientCache}: every kind is one bulk layer whose height follows its share of the crate
 * ({@code count / (slots * stack size)}, at least one pixel), stacked bottom to top in the order of
 * {@link CrateBlockEntity#layers()}. A layer looks like the item: its sprite (the item's particle sprite, a block item
 * shows its block texture), tiled {@link #TILE} pixels wide over the four sides - they show through the slat gaps - and,
 * for the top layer, over the top. Behind the tiles lies a darkened plate in the sprite's average colour, so the
 * transparent pixels of a flat item read as shadow between the pieces instead of showing through. On the middle
 * of every outer side wall the first {@link CrateBlockEntity#MAX_KINDS} kinds also show as small icons, three per slat,
 * bottom slat = lowest layers.
 *
 * <p>All quads go into plain float arrays per atlas (item and block sprites live in different atlases), so a frame only
 * copies vertices; nothing is resolved per frame except a check that the atlas was not reloaded.
 */
final class CrateMesh {
    /** Tile edge in pixels: three tiles across the 12 px inside, so the pattern fits without clipping. */
    private static final float TILE = 4.0F;
    private static final float ICON = 3.0F;
    /** The plate lies this far behind the tiles (px); icons stand this far in front of the wall. */
    private static final float PLATE_DEPTH = 0.1F;
    private static final float ICON_LIFT = 0.08F;
    private static final float PLATE_SHADE = 0.5F;
    private static final float INSIDE_SHADE = 0.85F;
    private static final int STRIDE = 9;
    private static final Map<TextureAtlasSprite, float[]> TEXELS = new WeakHashMap<>();

    /** Quads of one atlas: plates, tiles, icons (each as x y z u v nx ny nz shade per vertex, block units). */
    static final class Batch {
        final Identifier atlas;
        final TextureAtlasSprite probe;
        final Buf plates = new Buf();
        final Buf tiles = new Buf();
        final Buf icons = new Buf();

        Batch(TextureAtlasSprite probe) {
            this.atlas = probe.atlasLocation();
            this.probe = probe;
        }
    }

    static final class Buf {
        float[] data = new float[STRIDE * 4 * 16];
        int size;

        void vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz, float shade) {
            if (this.size + STRIDE > this.data.length) {
                this.data = Arrays.copyOf(this.data, this.data.length * 2);
            }
            float[] d = this.data;
            int i = this.size;
            d[i] = x;
            d[i + 1] = y;
            d[i + 2] = z;
            d[i + 3] = u;
            d[i + 4] = v;
            d[i + 5] = nx;
            d[i + 6] = ny;
            d[i + 7] = nz;
            d[i + 8] = shade;
            this.size += STRIDE;
        }
    }

    final int version;
    final Direction facing;
    final List<Batch> batches;

    private CrateMesh(int version, Direction facing, List<Batch> batches) {
        this.version = version;
        this.facing = facing;
        this.batches = batches;
    }

    /** The mesh for the crate's current layers, from its cache when nothing changed (and no resource reload happened). */
    static CrateMesh of(CrateBlockEntity crate, Direction facing, ItemModelResolver resolver) {
        if (crate.clientCache instanceof CrateMesh cached && cached.version == crate.version() && cached.facing == facing
                && cached.stillValid()) {
            return cached;
        }
        CrateMesh built = build(crate, facing, resolver);
        crate.clientCache = built;
        return built;
    }

    private boolean stillValid() {
        for (Batch batch : this.batches) {
            AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(batch.atlas);
            if (!(texture instanceof TextureAtlas atlas) || atlas.getSprite(batch.probe.contents().name()) != batch.probe) {
                return false;
            }
        }
        return true;
    }

    private static CrateMesh build(CrateBlockEntity crate, Direction facing, ItemModelResolver resolver) {
        List<CrateBlockEntity.Layer> layers = crate.layers();
        double[] in = CrateBlock.interior(facing);
        float x0 = (float) in[0];
        float x1 = (float) in[3];
        float z0 = (float) in[2];
        float z1 = (float) in[5];
        float floor = (float) in[1];
        // The surface of a full crate lies two pixels below the rim of an upright one.
        float avail = Math.min((float) in[4], 14.0F) - floor;
        float[] height = heights(layers, crate, avail);
        Map<Identifier, Batch> batches = new LinkedHashMap<>();
        ItemStackRenderState scratch = new ItemStackRenderState();
        float y = floor;
        for (int i = 0; i < layers.size(); i++) {
            ItemStack kind = layers.get(i).kind();
            resolver.updateForTopItem(scratch, kind, ItemDisplayContext.NONE, crate.getLevel(), null, i);
            Material.Baked material = scratch.pickParticleMaterial(RandomSource.create(i));
            scratch.clear();
            if (material != null) {
                TextureAtlasSprite sprite = material.sprite();
                Batch batch = batches.computeIfAbsent(sprite.atlasLocation(), id -> new Batch(sprite));
                layer(batch, sprite, x0, x1, z0, z1, y, y + height[i], i == layers.size() - 1, i);
                if (i < CrateBlockEntity.MAX_KINDS) {
                    icons(batch, sprite, i, facing);
                }
            }
            y += height[i];
        }
        return new CrateMesh(crate.version(), facing, new ArrayList<>(batches.values()));
    }

    /** Layer heights in pixels: the share of the crate, at least one pixel, squeezed to fit if the minimums add up. */
    private static float[] heights(List<CrateBlockEntity.Layer> layers, CrateBlockEntity crate, float avail) {
        float[] height = new float[layers.size()];
        float sum = 0;
        for (int i = 0; i < height.length; i++) {
            CrateBlockEntity.Layer layer = layers.get(i);
            float share = (float) layer.count() / (CrateBlockEntity.SLOTS * crate.getMaxStackSize(layer.kind()));
            height[i] = Math.max(1.0F, avail * share);
            sum += height[i];
        }
        if (sum > avail) {
            for (int i = 0; i < height.length; i++) {
                height[i] *= avail / sum;
            }
        }
        return height;
    }

    // ------------------------------------------------------------------ one layer

    /** Outward normals of the four sides, and for each the face's right-hand direction (up x normal) and its start corner. */
    private static final int[][] SIDES = {
            // nx, nz, rx, rz
            {-1, 0, 0, 1},
            {1, 0, 0, -1},
            {0, -1, -1, 0},
            {0, 1, 1, 0},
    };

    private static void layer(Batch batch, TextureAtlasSprite sprite, float x0, float x1, float z0, float z1,
                              float ya, float yb, boolean top, int index) {
        float[] texel = plateTexel(sprite);
        float h = yb - ya;
        for (int[] side : SIDES) {
            int nx = side[0];
            int nz = side[1];
            int rx = side[2];
            int rz = side[3];
            // corner at the start of the face's right-hand direction, on the layer's bottom
            float px = nx < 0 ? x0 : nx > 0 ? x1 : (rx > 0 ? x0 : x1);
            float pz = nz < 0 ? z0 : nz > 0 ? z1 : (rz > 0 ? z0 : z1);
            float width = rx != 0 ? x1 - x0 : z1 - z0;
            quad(batch.plates, sprite, texel, px - nx * PLATE_DEPTH, ya, pz - nz * PLATE_DEPTH, rx, 0, rz, 0, 1, 0, width, h,
                    nx, 0, nz, PLATE_SHADE);
            tiles(batch.tiles, sprite, px, ya, pz, rx, 0, rz, 0, 1, 0, width, h, nx, 0, nz, index);
        }
        if (top) {
            // top: right = +x, up = -z (so right x up = +y); starts at the south-west corner
            quad(batch.plates, sprite, texel, x0, yb - PLATE_DEPTH, z1, 1, 0, 0, 0, 0, -1, x1 - x0, z1 - z0, 0, 1, 0, PLATE_SHADE);
            tiles(batch.tiles, sprite, x0, yb, z1, 1, 0, 0, 0, 0, -1, x1 - x0, z1 - z0, 0, 1, 0, index);
        }
    }

    /** A face of {@code w} x {@code h} px tiled with the whole sprite, tiles mirrored in a checker pattern, partial tiles cut to the middle of the sprite. */
    private static void tiles(Buf buf, TextureAtlasSprite sprite, float px, float py, float pz, float rx, float ry, float rz,
                              float ux, float uy, float uz, float w, float h, float nx, float ny, float nz, int index) {
        int ai = 0;
        for (float a = 0; a < w - 0.001F; a += TILE, ai++) {
            float tw = Math.min(TILE, w - a);
            int bi = 0;
            for (float b = 0; b < h - 0.001F; b += TILE, bi++) {
                float th = Math.min(TILE, h - b);
                boolean flip = ((ai + bi + index) & 1) == 1;
                // A partial tile shows the middle of the sprite (the edges of a flat item are mostly empty).
                float fu = tw / TILE;
                float fv = th / TILE;
                float ua = (1.0F - fu) / 2.0F;
                float ub = (1.0F + fu) / 2.0F;
                tile(buf, sprite, px + rx * a + ux * b, py + ry * a + uy * b, pz + rz * a + uz * b, rx, ry, rz, ux, uy, uz,
                        tw, th, nx, ny, nz, flip ? ub : ua, flip ? ua : ub, (1.0F - fv) / 2.0F, (1.0F + fv) / 2.0F, INSIDE_SHADE);
            }
        }
    }

    private static void tile(Buf buf, TextureAtlasSprite sprite, float px, float py, float pz, float rx, float ry, float rz,
                             float ux, float uy, float uz, float w, float h, float nx, float ny, float nz,
                             float ua, float ub, float va, float vb, float shade) {
        float u0 = sprite.getU(ua);
        float u1 = sprite.getU(ub);
        float v0 = sprite.getV(va);
        float v1 = sprite.getV(vb);
        corners(buf, px, py, pz, rx, ry, rz, ux, uy, uz, w, h, nx, ny, nz, u0, u1, v0, v1, shade);
    }

    /** A solid quad: all its corners on one texel of the sprite. */
    private static void quad(Buf buf, TextureAtlasSprite sprite, float[] texel, float px, float py, float pz, float rx, float ry,
                             float rz, float ux, float uy, float uz, float w, float h, float nx, float ny, float nz, float shade) {
        float u = sprite.getU(texel[0]);
        float v = sprite.getV(texel[1]);
        corners(buf, px, py, pz, rx, ry, rz, ux, uy, uz, w, h, nx, ny, nz, u, u, v, v, shade);
    }

    /** Four corners counter-clockwise from outside (right x up = normal), bottom left first; pixels in, block units stored. */
    private static void corners(Buf buf, float px, float py, float pz, float rx, float ry, float rz, float ux, float uy,
                                float uz, float w, float h, float nx, float ny, float nz, float u0, float u1, float v0,
                                float v1, float shade) {
        float k = 1.0F / 16.0F;
        buf.vertex(px * k, py * k, pz * k, u0, v1, nx, ny, nz, shade);
        buf.vertex((px + rx * w) * k, (py + ry * w) * k, (pz + rz * w) * k, u1, v1, nx, ny, nz, shade);
        buf.vertex((px + rx * w + ux * h) * k, (py + ry * w + uy * h) * k, (pz + rz * w + uz * h) * k, u1, v0, nx, ny, nz, shade);
        buf.vertex((px + ux * h) * k, (py + uy * h) * k, (pz + uz * h) * k, u0, v0, nx, ny, nz, shade);
    }

    // ------------------------------------------------------------------ icons on the outer walls

    /**
     * Kind number {@code kind} (bottom layer first) as a small icon on every outer side wall, three per slat: slat
     * {@code kind / 3}, place {@code kind % 3}. The slats of an upright crate lie one above the other (icons run
     * across), those of one laid on its side run up the wall and follow each other along its axis.
     */
    private static void icons(Batch batch, TextureAtlasSprite sprite, int kind, Direction facing) {
        boolean laid = facing.getAxis().isHorizontal();
        float slat = 2.0F + 6.0F * (kind / 3);
        float place = 4.0F + 4.0F * (kind % 3);
        float across = laid ? slat : place;
        float up = laid ? place : slat;
        for (int[] side : SIDES) {
            int nx = side[0];
            int nz = side[1];
            if ((nx != 0 ? Direction.Axis.X : Direction.Axis.Z) == facing.getAxis()) {
                continue;
            }
            int rx = side[2];
            int rz = side[3];
            // wall corner at the start of the right-hand direction, on the outer plane
            float cx = nx > 0 || (nx == 0 && rx < 0) ? 16.0F : 0.0F;
            float cz = nz > 0 || (nz == 0 && rz < 0) ? 16.0F : 0.0F;
            float half = ICON / 2.0F;
            float px = cx + rx * (across - half) + nx * ICON_LIFT;
            float pz = cz + rz * (across - half) + nz * ICON_LIFT;
            corners(batch.icons, px, up - half, pz, rx, 0, rz, 0, 1, 0, ICON, ICON, nx, 0, nz,
                    sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), 1.0F);
        }
    }

    // ------------------------------------------------------------------ plate colour

    /**
     * The texel (as sprite fractions) whose colour is closest to the sprite's average over its opaque pixels: the
     * plates are drawn from this single texel, darkened, so they need no texture of their own.
     */
    private static float[] plateTexel(TextureAtlasSprite sprite) {
        return TEXELS.computeIfAbsent(sprite, s -> {
            // The loaded sprite keeps no readable pixels outside the atlas; read the texture file again (once per sprite).
            Identifier name = s.contents().name();
            Identifier file = name.withPath("textures/" + name.getPath() + ".png");
            int w = s.contents().width();
            int h = s.contents().height();
            try (InputStream in = Minecraft.getInstance().getResourceManager().open(file); NativeImage image = NativeImage.read(in)) {
                return averageTexel(image, w, h);
            } catch (IOException | RuntimeException e) {
                return new float[] {0.5F, 0.5F};
            }
        });
    }

    private static float[] averageTexel(NativeImage image, int w, int h) {
        w = Math.min(w, image.getWidth());
        h = Math.min(h, image.getHeight());
        long r = 0;
        long g = 0;
        long b = 0;
        int n = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = image.getPixel(x, y);
                if ((argb >>> 24) >= 128) {
                    r += (argb >> 16) & 255;
                    g += (argb >> 8) & 255;
                    b += argb & 255;
                    n++;
                }
            }
        }
        if (n == 0) {
            return new float[] {0.5F, 0.5F};
        }
        long best = Long.MAX_VALUE;
        int bx = 0;
        int by = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = image.getPixel(x, y);
                if ((argb >>> 24) >= 128) {
                    long dr = ((argb >> 16) & 255) - r / n;
                    long dg = ((argb >> 8) & 255) - g / n;
                    long db = (argb & 255) - b / n;
                    long d = dr * dr + dg * dg + db * db;
                    if (d < best) {
                        best = d;
                        bx = x;
                        by = y;
                    }
                }
            }
        }
        return new float[] {(bx + 0.5F) / w, (by + 0.5F) / h};
    }
}
