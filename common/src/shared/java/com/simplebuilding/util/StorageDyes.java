package com.simplebuilding.util;

import java.util.Map;
import net.minecraft.world.item.DyeColor;

/**
 * Faerben wie Vanillas Buendel (Besitzer 2026-10-02, {@code McVersion.VANILLA_DYEING}): Rucksack, Buendel oder Koecher
 * plus ein Farbstoff ergibt genau eine feste Farbe - kein Mischen wie bei Leder, kein Auswaschen im Kessel. Die Farben
 * sind die Lederfarben der Vanilla-Buendel (gemessen mit {@code tools/textures/storage_dye_palette.py}), die
 * {@code *_dyed}-Ebenen der Mod sind dafuer neutral grau. Rezepte: {@code simplebuilding:<farbe>_dyed_storage}
 * ({@code crafting_transmute} auf den Tag {@code simplebuilding:dyeable_storage}).
 */
public final class StorageDyes {
    public static final Map<DyeColor, Integer> COLOURS = Map.ofEntries(
            Map.entry(DyeColor.WHITE, 0xE6E6E6),
            Map.entry(DyeColor.LIGHT_GRAY, 0xB1ACA3),
            Map.entry(DyeColor.GRAY, 0x6C7B83),
            Map.entry(DyeColor.BLACK, 0x38364F),
            Map.entry(DyeColor.BROWN, 0xD18A59),
            Map.entry(DyeColor.RED, 0xD2382E),
            Map.entry(DyeColor.ORANGE, 0xFB9320),
            Map.entry(DyeColor.YELLOW, 0xF2C705),
            Map.entry(DyeColor.LIME, 0x9BDF39),
            Map.entry(DyeColor.GREEN, 0x77A119),
            Map.entry(DyeColor.CYAN, 0x14B4B4),
            Map.entry(DyeColor.LIGHT_BLUE, 0x30AFE5),
            Map.entry(DyeColor.BLUE, 0x4573C7),
            Map.entry(DyeColor.PURPLE, 0x942ACA),
            Map.entry(DyeColor.MAGENTA, 0xCC49B9),
            Map.entry(DyeColor.PINK, 0xF8A6BD));

    private StorageDyes() {
    }

    public static int colour(DyeColor dye) {
        return COLOURS.get(dye);
    }
}
