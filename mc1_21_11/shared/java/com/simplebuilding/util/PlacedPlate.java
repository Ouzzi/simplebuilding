package com.simplebuilding.util;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Lage und Form einer abgelegten Platte (Schmiedevorlage, Blaupause - {@code PlacedTemplateBlock}):
 * dieselbe Rechnung fuer den Renderer ({@code PlacedTemplateRenderer}) und die Trefferform, damit
 * man genau die gezeichneten Pixel trifft und sonst nichts (Besitzer 2026-09-28).
 *
 * <p>Das Item-Modell ({@code item/generated}) ist die ausgestanzte Item-Textur: Pixel (u, v) liegt im
 * Modellraum bei x in [u, u+1]/16, y in [15-v, 16-v]/16, z in [7.5, 8.5]/16. {@link #transform} bildet
 * das in den Block ab - Kantenlaenge {@link #SCALE}, Dicke um {@link #THICKNESS_SCALE} gestreckt,
 * Rueckseite an Boden, Wand oder Decke. Die Trefferform ist die Vereinigung der deckenden Pixel,
 * zeilenweise zu Streifen zusammengefasst.
 *
 * <p>Welche Pixel decken: Mod-Items (dieser Mod und anderer Mods) lesen ihre Item-Textur
 * {@code assets/<ns>/textures/item/<pfad>.png} vom Klassenpfad - die liegt auch auf dem dedizierten
 * Server im Mod-Jar; Vanilla-Texturen hat der Server nicht, fuer sie steht die Maske in
 * {@link #VANILLA}. Geht beides nicht, deckt die ganze Flaeche.
 */
public final class PlacedPlate {
    /** Kantenlaenge der Platte in Blockbreiten (14 von 16 Pixeln). */
    public static final float SCALE = 14.0F / 16.0F;
    /** Streckung der Dicke: das Item-Modell ist 1/16 dick, gestreckt gut 1,4 Pixel. */
    public static final float THICKNESS_SCALE = 1.6F;
    /** Abstand zur Auflageflaeche gegen Z-Fighting. */
    public static final float GAP = 0.002F;
    /** Ab dieser Deckkraft zeichnet der Cutout-Renderer ein Pixel (0,1 * 255). */
    public static final int SOLID_ALPHA = 26;

    /** Eine Zeile je Texturzeile (oben zuerst), Bit 15 = linkes Pixel. */
    public static final short[] FULL = full();

    /**
     * Deckende Pixel der Vanilla-Vorlagen (26.2, 1.21.11 und 26.3 gleich), je 16 Zeilen als 64
     * Hex-Ziffern - erzeugt aus den Texturen des Client-Jars. Besatzvorlagen, die hier fehlen
     * (neue Vanilla-Versionen), nehmen {@link #TRIM}.
     */
    private static final String TRIM = "038007e007f80ffe0fff1fff1fff3ffe3ffe7ffc7ffc7ff87ff83ff00ff003e0";
    private static final Map<String, String> VANILLA = Map.of(
            "coast_armor_trim_smithing_template", "038c07fe07ff0fff0fff1fff1fff3fff3fff7ffe7ffcfff8fff8fff06ff003e0",
            "wild_armor_trim_smithing_template", "038007e007f80ffe0fff1fff1fff3fff3fff7ffe7ffc7ff87ff87ff02ff003e0",
            "netherite_upgrade_smithing_template", "00000ff81ffc1ffc1ffc1ffc1ffc1ffc1ffc1ffc1ffc1ffc1ffc0ff807c00000");

    private static final Map<Item, short[]> MASKS = new ConcurrentHashMap<>();
    private static final Map<Item, VoxelShape[]> SHAPES = new ConcurrentHashMap<>();
    private static final VoxelShape[] FULL_SHAPES = new VoxelShape[AttachFace.values().length * 4];

    private PlacedPlate() {
    }

    // =====================================================================================
    // Lage
    // =====================================================================================

    /**
     * Blockraum (0..1) aus dem Modellraum des Item-Modells (0..1): genau die Transformation des
     * Renderers. An der Wand zeigt die Schauseite (+Z des Modells) in {@code facing}, am Boden und
     * an der Decke zeigt die Oberkante (+Y) in die Blickrichtung beim Ablegen.
     */
    public static Matrix4f transform(AttachFace face, Direction facing) {
        Matrix4f m = new Matrix4f().translate(0.5F, 0.5F, 0.5F);
        float yRot = facing.toYRot();
        switch (face) {
            case WALL -> m.rotateY(rad(-yRot));
            case FLOOR -> m.rotateY(rad(180.0F - yRot)).rotateX(rad(-90.0F));
            case CEILING -> m.rotateY(rad(-yRot)).rotateX(rad(90.0F));
        }
        float thickness = SCALE * THICKNESS_SCALE / 16.0F;
        m.translate(0.0F, 0.0F, -(0.5F - thickness / 2.0F - GAP));
        m.scale(SCALE, SCALE, SCALE * THICKNESS_SCALE);
        m.translate(-0.5F, -0.5F, -0.5F);
        return m;
    }

    private static float rad(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    // =====================================================================================
    // Form
    // =====================================================================================

    /** Trefferform der Platte mit diesem Item in dieser Lage (zwischengespeichert). */
    public static VoxelShape shape(Item item, AttachFace face, Direction facing) {
        int index = face.ordinal() * 4 + facing.get2DDataValue();
        if (item == null) {
            VoxelShape cached = FULL_SHAPES[index];
            if (cached == null) {
                cached = build(FULL, face, facing);
                FULL_SHAPES[index] = cached;
            }
            return cached;
        }
        VoxelShape[] shapes = SHAPES.computeIfAbsent(item, unused -> new VoxelShape[AttachFace.values().length * 4]);
        VoxelShape cached = shapes[index];
        if (cached == null) {
            cached = build(mask(item), face, facing);
            shapes[index] = cached;
        }
        return cached;
    }

    /** Die Form aus einer Maske: je Zeile die Streifen deckender Pixel, gleiche Nachbarzeilen zusammengefasst. */
    public static VoxelShape build(short[] mask, AttachFace face, Direction facing) {
        Matrix4f m = transform(face, facing);
        VoxelShape shape = Shapes.empty();
        int v = 0;
        while (v < 16) {
            int row = mask[v] & 0xFFFF;
            int end = v + 1;
            while (end < 16 && (mask[end] & 0xFFFF) == row) {
                end++;
            }
            int u = 0;
            while (u < 16) {
                if ((row & (1 << (15 - u))) == 0) {
                    u++;
                    continue;
                }
                int u1 = u;
                while (u1 < 16 && (row & (1 << (15 - u1))) != 0) {
                    u1++;
                }
                shape = Shapes.or(shape, box(m, u / 16.0F, (16 - end) / 16.0F, 7.5F / 16.0F, u1 / 16.0F, (16 - v) / 16.0F, 8.5F / 16.0F));
                u = u1;
            }
            v = end;
        }
        return shape.optimize();
    }

    private static VoxelShape box(Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        Vector3f corner = new Vector3f();
        for (int i = 0; i < 8; i++) {
            m.transformPosition((i & 1) == 0 ? x0 : x1, (i & 2) == 0 ? y0 : y1, (i & 4) == 0 ? z0 : z1, corner);
            minX = Math.min(minX, corner.x);
            minY = Math.min(minY, corner.y);
            minZ = Math.min(minZ, corner.z);
            maxX = Math.max(maxX, corner.x);
            maxY = Math.max(maxY, corner.y);
            maxZ = Math.max(maxZ, corner.z);
        }
        return Shapes.box(round(minX), round(minY), round(minZ), round(maxX), round(maxY), round(maxZ));
    }

    /** Auf 1/10000 gerundet und in den Block geklemmt: Drehungen um 90 Grad hinterlassen Float-Reste. */
    private static double round(float value) {
        return Math.max(0.0, Math.min(1.0, Math.round(value * 10000.0) / 10000.0));
    }

    // =====================================================================================
    // Maske
    // =====================================================================================

    /** Die deckenden Pixel der Item-Textur ({@link #FULL}, wenn sie sich nicht bestimmen lassen). */
    public static short[] mask(Item item) {
        return MASKS.computeIfAbsent(item, PlacedPlate::computeMask);
    }

    private static short[] computeMask(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if ("minecraft".equals(id.getNamespace())) {
            String hex = VANILLA.get(id.getPath());
            if (hex == null && id.getPath().endsWith("_armor_trim_smithing_template")) {
                hex = TRIM;
            }
            return hex == null ? FULL : parse(hex);
        }
        short[] read = read("/assets/" + id.getNamespace() + "/textures/item/" + id.getPath() + ".png");
        return read == null ? FULL : read;
    }

    /** Liest eine Textur vom Klassenpfad und rastert sie auf 16 x 16; null, wenn das nicht geht. */
    public static short[] read(String path) {
        try (InputStream in = PlacedPlate.class.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                return null;
            }
            int w = image.getWidth();
            // Animierte Texturen stehen untereinander: nur das erste (quadratische) Bild.
            int h = Math.min(image.getHeight(), w);
            short[] rows = new short[16];
            boolean any = false;
            for (int v = 0; v < 16; v++) {
                int row = 0;
                for (int u = 0; u < 16; u++) {
                    if (solid(image, u * w / 16, (u + 1) * w / 16, v * h / 16, (v + 1) * h / 16)) {
                        row |= 1 << (15 - u);
                        any = true;
                    }
                }
                rows[v] = (short) row;
            }
            return any ? rows : null;
        } catch (Throwable unreadable) {
            // Kein java.desktop, kaputtes PNG: dann eben die ganze Flaeche.
            return null;
        }
    }

    private static boolean solid(BufferedImage image, int x0, int x1, int y0, int y1) {
        for (int y = y0; y < Math.max(y1, y0 + 1); y++) {
            for (int x = x0; x < Math.max(x1, x0 + 1); x++) {
                if ((image.getRGB(x, y) >>> 24) >= SOLID_ALPHA) {
                    return true;
                }
            }
        }
        return false;
    }

    private static short[] parse(String hex) {
        short[] rows = new short[16];
        for (int v = 0; v < 16; v++) {
            rows[v] = (short) Integer.parseInt(hex.substring(v * 4, v * 4 + 4), 16);
        }
        return rows;
    }

    private static short[] full() {
        short[] rows = new short[16];
        java.util.Arrays.fill(rows, (short) 0xFFFF);
        return rows;
    }
}
