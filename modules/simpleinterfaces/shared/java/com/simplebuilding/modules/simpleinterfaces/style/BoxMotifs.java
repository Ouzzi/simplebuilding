package com.simplebuilding.modules.simpleinterfaces.style;

import com.simplelib.api.client.ui.UiMotifs;
import com.simplelib.api.client.ui.UiPalette;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.DyeColor;

/**
 * Which faint marks (owner image 4, {@link UiMotifs}) a box of group "storage" gets: by the box's fill colour, as in the
 * palette table of the W0-B preview (docs/ai/PLAN-SIMPLECONTAINERS-2026-10-08.md, column "Motiv"), with the seed the
 * preview used for that screen (length of its key) so the marks land where the preview shows them. Unknown fills get
 * no marks (the work screens pick theirs in WorkScreens). Loads on a server (no client classes).
 */
public final class BoxMotifs {
    /** Motif and placement seed of one box colour. */
    public record Entry(UiMotifs.Kind motif, int seed) {}

    /** No marks. */
    public static final Entry NONE = new Entry(UiMotifs.Kind.NONE, 1);

    private static final Map<Integer, Entry> BY_FILL = new HashMap<>();

    static {
        // G1 storage (preview keys: truhe, fass, ender, shulker1, trichter, spender, crafter, pferd)
        put(0xFFCE9148, UiMotifs.Kind.WOOD, 5);
        put(0xFFB9774F, UiMotifs.Kind.WOOD, 4);
        put(0xFF597880, UiMotifs.Kind.ENDER, 5);
        put(0xFF876C99, UiMotifs.Kind.SHULKER, 8);
        put(0xFF5C97B8, UiMotifs.Kind.SHULKER, 8);
        put(0xFF5A5C63, UiMotifs.Kind.METAL, 8);
        put(0xFF878787, UiMotifs.Kind.STONE, 7);
        put(0xFF7A736A, UiMotifs.Kind.REDSTONE, 7);
        put(0xFF8B5E3C, UiMotifs.Kind.LEATHER, 5);
        // copper chests and dyed shulker boxes: their family's marks
        for (UiPalette copper : StorageStyles.COPPER) BY_FILL.putIfAbsent(copper.fill(), new Entry(UiMotifs.Kind.METAL, 5));
        for (DyeColor dye : DyeColor.values()) BY_FILL.putIfAbsent(StorageStyles.dyed(dye).fill(), new Entry(UiMotifs.Kind.SHULKER, 8));
    }

    private BoxMotifs() {}

    private static void put(int fill, UiMotifs.Kind motif, int seed) {
        BY_FILL.put(fill, new Entry(motif, seed));
    }

    /** The marks of a box in {@code palette}; {@link #NONE} for unknown colours and the light inventory box. */
    public static Entry of(UiPalette palette) {
        return BY_FILL.getOrDefault(palette.fill(), NONE);
    }
}
