package com.simplebuilding.modules.simplecontainers.style;

import com.simplelib.api.client.ui.UiMotif;
import com.simplelib.api.client.ui.UiPalette;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.DyeColor;

/**
 * Which faint marks (owner image 4) a container box gets: by the box's fill colour, as in the palette table of the W0-B
 * preview (docs/ai/PLAN-SIMPLECONTAINERS-2026-10-08.md, "Paletten (Vorschau W0-B)", column "Motiv"), with the seed the
 * preview used for that screen (length of its key) so the marks land where the preview shows them. Unknown fills get no
 * marks. Loads on a server (no client classes).
 */
public final class BoxMotifs {
    /** Motif and placement seed of one box colour. */
    public record Entry(UiMotif motif, int seed) {}

    /** No marks. */
    public static final Entry NONE = new Entry(UiMotif.NONE, 1);

    private static final Map<Integer, Entry> BY_FILL = new HashMap<>();

    static {
        // G1 storage (preview keys: truhe, fass, ender, shulker1, trichter, spender, crafter, pferd)
        put(0xFFCE9148, UiMotif.WOOD, 5);
        put(0xFFB9774F, UiMotif.WOOD, 4);
        put(0xFF597880, UiMotif.ENDER, 5);
        put(0xFF876C99, UiMotif.SHULKER, 8);
        put(0xFF5C97B8, UiMotif.SHULKER, 8);
        put(0xFF5A5C63, UiMotif.METAL, 8);
        put(0xFF878787, UiMotif.STONE, 7);
        put(0xFF7A736A, UiMotif.REDSTONE, 7);
        put(0xFF8B5E3C, UiMotif.LEATHER, 5);
        // G2 work I (werkbank, ofen, schmelz, raeucher, brau, leuchtfeuer, zauber)
        put(0xFFB7935B, UiMotif.WOOD, 8);
        put(0xFF929699, UiMotif.STONE, 4);
        put(0xFF6E7179, UiMotif.METAL, 7);
        put(0xFF7D6B57, UiMotif.SMOKE, 8);
        put(0xFF847D7D, UiMotif.STONE, 4);
        put(0xFF6FB4B1, UiMotif.GLASS, 11);
        put(0xFFA1282B, UiMotif.RUNE, 6);
        // G3 work II (amboss, schleif, saege, web, karte, schmied, handel, spieler)
        put(0xFF666666, UiMotif.METAL, 6);
        put(0xFF9E9A92, UiMotif.STONE, 7);
        put(0xFF857A72, UiMotif.STONE, 5);
        put(0xFF9C8262, UiMotif.WOOL, 3);
        put(0xFF6B5A45, UiMotif.PAPER, 5);
        put(0xFF4B1E19, UiMotif.METAL, 7);
        put(0xFF3F8A55, UiMotif.LEATHER, 6);
        // copper chests and dyed shulker boxes: their family's marks
        for (UiPalette copper : StorageStyles.COPPER) BY_FILL.putIfAbsent(copper.fill(), new Entry(UiMotif.METAL, 5));
        for (DyeColor dye : DyeColor.values()) BY_FILL.putIfAbsent(StorageStyles.dyed(dye).fill(), new Entry(UiMotif.SHULKER, 8));
    }

    private BoxMotifs() {}

    private static void put(int fill, UiMotif motif, int seed) {
        BY_FILL.put(fill, new Entry(motif, seed));
    }

    /** The marks of a box in {@code palette}; {@link #NONE} for unknown colours and the light inventory box. */
    public static Entry of(UiPalette palette) {
        return BY_FILL.getOrDefault(palette.fill(), NONE);
    }
}
