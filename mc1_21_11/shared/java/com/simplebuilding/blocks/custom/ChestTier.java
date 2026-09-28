package com.simplebuilding.blocks.custom;

/**
 * Die drei Stufen der Mod-Truhen ueber der Vanilla-Kupfertruhe (27 Plaetze):
 * Verstaerkt 36 (9x4), Netherit 45 (9x5, doppelte Stapel), Enderit 54 (9x6, vierfache Stapel).
 *
 * <p>Eine Doppeltruhe hat doppelt so viele Plaetze und waechst in die Breite statt in die Hoehe,
 * damit das Fenster auch bei GUI-Skala 4 auf 1080p (480x270 Punkte) ohne Blaettern passt:
 * immer sechs Reihen, 12/15/18 Spalten (Fenster 230/284/338 x 222). Jede Truhenhaelfte fuellt
 * genau drei ganze Reihen, die erste Haelfte oben, die zweite unten.
 *
 * <p>Der Stapelfaktor folgt dem Rucksack mit Tiefe Taschen (x2/x4, siehe
 * {@code BackpackItem#maxStackSizeIn}): nicht stapelbare Items bleiben bei 1.
 */
public enum ChestTier {
    REINFORCED("reinforced", 4, 12, 1),
    NETHERITE("netherite", 5, 15, 2),
    ENDERITE("enderite", 6, 18, 4);

    private final String textureName;
    private final int singleRows;
    private final int doubleColumns;
    private final int stackMultiplier;

    ChestTier(String textureName, int singleRows, int doubleColumns, int stackMultiplier) {
        this.textureName = textureName;
        this.singleRows = singleRows;
        this.doubleColumns = doubleColumns;
        this.stackMultiplier = stackMultiplier;
    }

    /** Name der Texturen unter {@code textures/entity/chest/} (Einzel-, _left-, _right-Datei). */
    public String textureName() {
        return this.textureName;
    }

    /** Plaetze einer einzelnen Truhe (= einer Haelfte der Doppeltruhe). */
    public int slots() {
        return this.singleRows * 9;
    }

    public int stackMultiplier() {
        return this.stackMultiplier;
    }

    /** Spalten des Menues: 9 fuer eine einzelne Truhe, 12/15/18 fuer eine Doppeltruhe. */
    public int columns(boolean isDouble) {
        return isDouble ? this.doubleColumns : 9;
    }

    /** Reihen des Menues: 4/5/6 fuer eine einzelne Truhe, immer 6 fuer eine Doppeltruhe. */
    public int rows(boolean isDouble) {
        return isDouble ? 6 : this.singleRows;
    }

    public static ChestTier byId(int id) {
        ChestTier[] values = values();
        return values[Math.floorMod(id, values.length)];
    }
}
