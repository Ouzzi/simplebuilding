package com.simplebuilding.util;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.DyedItemColor;

/**
 * Gefaerbte Rucksaecke, Buendel und Koecher.
 *
 * <p>Gefaerbt wird wie Vanillas Lederruestung ueber die Komponente {@code minecraft:dyed_color},
 * nicht ueber eigene Items je Farbe wie beim Oktanten: Rucksaecke sind zugleich Bloecke, haben
 * Aufwertungsrezepte zwischen den Stufen und viele Stellen fragen {@code item == ModItems.BACKPACK}
 * bzw. die Stufe ab - mit 16 x 7 weiteren Items haette jede davon Farben kennen muessen. Die
 * Komponente reist dagegen von selbst mit: Farbstoff-Rezept und Kessel lassen alle anderen
 * Komponenten (Inhalt, Name, Verzauberungen) liegen, Aufwertung und Abstellen/Aufheben kopieren
 * alle Komponenten.
 *
 * <p>Die Farbe zeigt sich im Item-Modell (Farbquelle {@code minecraft:dye} auf der Leder-Ebene),
 * am getragenen Rucksack ({@code BackpackLayer}), als leichte Toenung der Rucksack-Reihen im
 * Bildschirm und der Felder im Buendel-Tooltip.
 */
public final class DyedStorage {
    /** Kein Farbstoff: der Rucksack bzw. das Buendel sieht aus wie seine Stufe. */
    public static final int UNDYED = -1;
    /** Deckkraft der Slot-Toenung im Rucksack-Bildschirm (ungefaerbt: {@code TINT_BACKPACK_ROW}, 0x1C). */
    public static final int SLOT_TINT_ALPHA = 0x30;
    /** Anteil der Farbe an den Feldern des Buendel-Tooltips; der Rest bleibt Vanillas Grau. */
    public static final float TOOLTIP_SLOT_STRENGTH = 0.35F;

    private DyedStorage() {
    }

    /** Die Farbe (0xRRGGBB) aus {@code minecraft:dyed_color}, sonst {@link #UNDYED}. */
    public static int colour(DataComponentGetter components) {
        DyedItemColor dyed = components.get(DataComponents.DYED_COLOR);
        return dyed == null ? UNDYED : dyed.rgb() & 0xFFFFFF;
    }

    /** Die leichte Slot-Toenung (ARGB) fuer eine Farbe; {@code fallback} fuer {@link #UNDYED}. */
    public static int slotTint(int rgb, int fallback) {
        return rgb == UNDYED ? fallback : SLOT_TINT_ALPHA << 24 | rgb & 0xFFFFFF;
    }

    /**
     * Farbe (ARGB, deckend), mit der ein grauer Slot-Hintergrund multipliziert wird: Weiss, zu
     * {@link #TOOLTIP_SLOT_STRENGTH} zur Farbe hin gezogen. Weiss (keine Aenderung) fuer {@link #UNDYED}.
     */
    public static int spriteTint(int rgb) {
        if (rgb == UNDYED) {
            return 0xFFFFFFFF;
        }
        int r = mix(rgb >> 16 & 0xFF);
        int g = mix(rgb >> 8 & 0xFF);
        int b = mix(rgb & 0xFF);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static int mix(int channel) {
        return Math.round(255 + (channel - 255) * TOOLTIP_SLOT_STRENGTH);
    }
}
