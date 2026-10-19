package com.simplebuilding.blueprint;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Bild-Bauplan (Plan docs/ai/PLAN-B-PICTURE-2026-10-19.md): ein Bild wird zu Bau-Code mit
 * {@code pixels}-Zeilen ({@link BlueprintCode#PIXELS}). Der Client rechnet um ({@link #convert}) und setzt nur
 * den fertigen Code in den Editor; der Server prueft ihn wie jeden Code und dazu {@link #refusal}
 * (Schalter, Rechtestufe, Groesse). Umwandlung (Lab, Dithering, Lauflaenge) ist SimpleLib
 * ({@code com.simplelib.api.image}), ueber {@code LibServices}; auf 26.2 gibt es den Bild-Bauplan nicht.
 *
 * <p>Palette: 16 Wolle, 16 Beton, 16 Terrakotta und Terrakotta - Vollbloecke mit eindeutiger Farbe. Die
 * Farbe ist das sRGB-Mittel der deckenden Pixel der Blocktextur im Standard-Paket (so sieht der Spieler
 * es an der Wand), erzeugt mit {@code tools/picture/block_colors.py}. {@code MapColor} taugt nicht:
 * Wolle und Beton teilen sich je Farbstoff eine Kartenfarbe.
 */
public final class BlueprintPicture {
    /** Kleinste und groesste Bildseite (Bloecke); 128 x 128 passt mit Dithering sicher in 32000 Zeichen. */
    public static final int MIN_SIDE = 16, MAX_SIDE = 128;
    /** Groessen-Stufen im Bild-Fenster. */
    public static final int[] SIDES = {16, 32, 48, 64, 96, 128};
    /** So viele Pixel stehen hoechstens in einer {@code pixels}-Zeile (lesbare Zeilen im Editor). */
    public static final int SEGMENT = 64;

    /**
     * Ein Palettenblock (Vanilla-ID ohne Namensraum) und seine mittlere Texturfarbe {@code 0xRRGGBB}. Ueber die ID,
     * weil 26.3 die Farbbloecke als {@code ColorCollection} fuehrt, 26.2 als einzelne Felder.
     */
    public record Entry(String id, int rgb) {
        public Block block() {
            return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(id));
        }

        public Item item() {
            return block().asItem();
        }
    }

    public static final List<Entry> PALETTE = List.of(
            new Entry("white_wool", 0xEAECED),
            new Entry("light_gray_wool", 0x8E8E87),
            new Entry("gray_wool", 0x3F4448),
            new Entry("black_wool", 0x15151A),
            new Entry("brown_wool", 0x724829),
            new Entry("red_wool", 0xA12723),
            new Entry("orange_wool", 0xF17614),
            new Entry("yellow_wool", 0xF9C628),
            new Entry("lime_wool", 0x70B91A),
            new Entry("green_wool", 0x556E1C),
            new Entry("cyan_wool", 0x158A91),
            new Entry("light_blue_wool", 0x3AAFD9),
            new Entry("blue_wool", 0x35399D),
            new Entry("purple_wool", 0x7A2AAD),
            new Entry("magenta_wool", 0xBE45B4),
            new Entry("pink_wool", 0xEE8DAC),
            new Entry("white_concrete", 0xCFD5D6),
            new Entry("light_gray_concrete", 0x7D7D73),
            new Entry("gray_concrete", 0x373A3E),
            new Entry("black_concrete", 0x080A0F),
            new Entry("brown_concrete", 0x603C20),
            new Entry("red_concrete", 0x8E2121),
            new Entry("orange_concrete", 0xE06101),
            new Entry("yellow_concrete", 0xF1AF15),
            new Entry("lime_concrete", 0x5EA918),
            new Entry("green_concrete", 0x495B24),
            new Entry("cyan_concrete", 0x157788),
            new Entry("light_blue_concrete", 0x2489C7),
            new Entry("blue_concrete", 0x2D2F8F),
            new Entry("purple_concrete", 0x64209C),
            new Entry("magenta_concrete", 0xA9309F),
            new Entry("pink_concrete", 0xD6658F),
            new Entry("white_terracotta", 0xD2B2A1),
            new Entry("light_gray_terracotta", 0x876B62),
            new Entry("gray_terracotta", 0x3A2A24),
            new Entry("black_terracotta", 0x251710),
            new Entry("brown_terracotta", 0x4D3324),
            new Entry("red_terracotta", 0x8F3D2F),
            new Entry("orange_terracotta", 0xA25426),
            new Entry("yellow_terracotta", 0xBA8523),
            new Entry("lime_terracotta", 0x687635),
            new Entry("green_terracotta", 0x4C532A),
            new Entry("cyan_terracotta", 0x575B5B),
            new Entry("light_blue_terracotta", 0x716D8A),
            new Entry("blue_terracotta", 0x4A3C5B),
            new Entry("purple_terracotta", 0x764656),
            new Entry("magenta_terracotta", 0x96586D),
            new Entry("pink_terracotta", 0xA24E4F),
            new Entry("terracotta", 0x985E44));

    public enum Dither {
        FLOYD_STEINBERG(1), BAYER(2), NONE(0);

        /** Nummer fuer SimpleLib ({@code PixelArt.Dither}: 0 aus, 1 Floyd-Steinberg, 2 Bayer). */
        public final int lib;

        Dither(int lib) {
            this.lib = lib;
        }

        public String langKey() {
            return "simplebuilding.blueprint.picture.dither." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Wand: Bild steht in der x-y-Ebene (oben = oben); Boden: in der x-z-Ebene (oben = Norden, z = 0). */
    public enum Facing {
        WALL, FLOOR;

        public String langKey() {
            return "simplebuilding.blueprint.picture.facing." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /**
     * Ergebnis der Umwandlung. {@code indices}: Palettenindex je Pixel (-1 frei), Zeile fuer Zeile von oben;
     * {@code code} leer, wenn {@code error} gesetzt ist (Sprachschluessel).
     */
    public record Result(String code, int width, int height, int[] indices, List<Entry> palette, int blocks, int colors,
                         @Nullable String error) {
        public boolean ok() {
            return error == null;
        }
    }

    private BlueprintPicture() {
    }

    /** Ob diese Linie Bilder umwandelt (26.3; 26.2 hat kein SimpleLib). */
    public static boolean available() {
        return com.simplebuilding.util.LibServices.pictures();
    }

    /** Die Palette, begrenzt auf Bloecke, deren Item {@code has} bejaht (z. B. "im Inventar"); null = alle. */
    public static List<Entry> palette(@Nullable Predicate<Item> has) {
        if (has == null) {
            return PALETTE;
        }
        List<Entry> out = new ArrayList<>();
        for (Entry e : PALETTE) {
            if (has.test(e.item())) {
                out.add(e);
            }
        }
        return out;
    }

    /**
     * Bild {@code argb} ({@code width x height}, ARGB, Zeile fuer Zeile von oben links) als Bau-Code: auf
     * {@code maxSide} verkleinert (Seitenverhaeltnis bleibt), mit {@code dither} auf {@code palette}
     * gebracht. Fehler: {@code no_blocks} (leere Palette), {@code empty} (alles durchsichtig), {@code too_long}.
     */
    public static Result convert(int[] argb, int width, int height, int maxSide, List<Entry> palette, Dither dither, Facing facing,
                                 String name) {
        if (!available()) {
            return failed("unsupported");
        }
        if (palette.isEmpty()) {
            return failed("no_blocks");
        }
        int side = Math.max(1, Math.min(MAX_SIDE, maxSide));
        int[] rgb = new int[palette.size()];
        for (int i = 0; i < rgb.length; i++) {
            rgb[i] = palette.get(i).rgb();
        }
        int[] converted = com.simplebuilding.util.LibServices.convertPicture(argb, width, height, side, rgb, dither.lib);
        if (converted == null) {
            return failed("unsupported");
        }
        int w = converted[0], h = converted[1];
        int[] indices = java.util.Arrays.copyOfRange(converted, 2, converted.length);
        return toCode(indices, w, h, palette, dither, facing, name);
    }

    /** Bau-Code fuer fertige Palettenindizes (fuer Tests und {@link #convert}). */
    public static Result toCode(int[] indices, int w, int h, List<Entry> palette, Dither dither, Facing facing, String name) {
        // Buchstaben nur fuer benutzte Bloecke, in Palettenreihenfolge: a, b, c ...
        int[] letter = new int[palette.size()];
        java.util.Arrays.fill(letter, -1);
        int blocks = 0;
        for (int index : indices) {
            if (index >= 0) {
                letter[index] = 0;
                blocks++;
            }
        }
        if (blocks == 0) {
            return new Result("", w, h, indices, palette, 0, 0, "empty");
        }
        StringBuilder code = new StringBuilder();
        String title = name.replaceAll("[\\r\\n#]", " ").strip();
        code.append("# picture ").append(title.isEmpty() ? "" : title + " ").append(w).append('x').append(h).append(", ")
                .append(dither.name().toLowerCase(java.util.Locale.ROOT)).append(", ").append(facing.name().toLowerCase(java.util.Locale.ROOT)).append('\n');
        int colors = 0;
        for (int i = 0; i < palette.size(); i++) {
            if (letter[i] == 0) {
                letter[i] = colors++;
                char c = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt(letter[i]);
                code.append('$').append(c).append(" = ").append(BlueprintCode.spec(palette.get(i).block().defaultBlockState())).append('\n');
            }
        }
        int[] row = new int[w];
        for (int r = 0; r < h; r++) {
            for (int x = 0; x < w; x++) {
                int index = indices[r * w + x];
                row[x] = index < 0 ? -1 : letter[index];
            }
            for (int from = 0; from < w; from += SEGMENT) {
                int to = Math.min(w, from + SEGMENT);
                int start = from, end = to;
                while (start < end && row[start] < 0) {
                    start++;
                }
                while (end > start && row[end - 1] < 0) {
                    end--;
                }
                if (start == end) {
                    continue;
                }
                int y = facing == Facing.WALL ? h - 1 - r : 0;
                int z = facing == Facing.WALL ? 0 : r;
                code.append(BlueprintCode.PIXELS).append(' ').append(start).append(',').append(y).append(',').append(z).append(' ')
                        .append(com.simplebuilding.util.LibServices.encodePixelRow(row, start, end)).append('\n');
            }
        }
        String text = code.toString().stripTrailing();
        if (text.length() > BlueprintCode.MAX_CODE_LENGTH) {
            return new Result("", w, h, indices, palette, blocks, colors, "too_long");
        }
        return new Result(text, w, h, indices, palette, blocks, colors, null);
    }

    private static Result failed(String key) {
        return new Result("", 0, 0, new int[0], List.of(), 0, 0, key);
    }

    // =====================================================================================
    // SERVER
    // =====================================================================================

    /**
     * Warum der Server {@code code} von {@code player} nicht annimmt (Sprachschluessel), oder null: nur Code mit
     * {@code pixels}-Zeilen wird geprueft - Schalter {@code server.blueprint.pictureImport}, Rechtestufe
     * {@code picturePermission} und laengste Kante hoechstens {@code pictureMaxSide}. Dasselbe Bild als
     * gewoehnliche Quader-Zeilen ist ein normaler Bau (wie von Hand geschrieben).
     */
    public static @Nullable String refusal(ServerPlayer player, String code) {
        if (!code.contains(BlueprintCode.PIXELS)) {
            return null;
        }
        BlueprintCode.ParseResult parsed = BlueprintCode.parseCached(code);
        if (parsed.pixels() == 0) {
            return null;
        }
        if (!com.simplebuilding.config.ServerTuning.pictureImport()) {
            return "simplebuilding.blueprint.picture.refused.off";
        }
        if (!hasLevel(player, com.simplebuilding.config.ServerTuning.picturePermission())) {
            return "simplebuilding.blueprint.picture.refused.permission";
        }
        BlueprintModel m = parsed.model();
        int side = com.simplebuilding.config.ServerTuning.pictureMaxSide();
        if (m.maxEdge() > side) {
            return "simplebuilding.blueprint.picture.refused.size";
        }
        return null;
    }

    /** Vanilla-Rechtestufe 0..4 (0 = alle), frisch aus der Operatorliste wie bei Befehlen. */
    public static boolean hasLevel(ServerPlayer player, int level) {
        if (level <= 0) {
            return true;
        }
        var check = switch (level) {
            case 1 -> Commands.LEVEL_MODERATORS;
            case 2 -> Commands.LEVEL_GAMEMASTERS;
            case 3 -> Commands.LEVEL_ADMINS;
            default -> Commands.LEVEL_OWNERS;
        };
        return check.check(player.createCommandSourceStack().permissions());
    }
}
