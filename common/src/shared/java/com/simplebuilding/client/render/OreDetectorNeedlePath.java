package com.simplebuilding.client.render;

/**
 * Kopfpixel der 32 Erzdetektor-Nadelbilder ({@code item/detector_needle_NN}), je Bild von der Nabe
 * zur Spitze als x, y im 16x16-Feld. Geschrieben von
 * {@code tools/textures/ore_detector_centred_2026_10_02.py} aus denselben Zahlen wie die Texturen -
 * nicht von Hand aendern. {@link OreDetectorGlint} laesst den Auswahl-Schimmer darauf laufen.
 */
public final class OreDetectorNeedlePath {
    private static final int[][] HEAD = {
            {7, 8, 7, 9, 7, 10},  // 00
            {7, 8, 7, 9, 7, 10},  // 01
            {7, 8, 7, 9, 6, 9, 6, 10},  // 02
            {7, 8, 6, 8, 6, 9, 5, 9},  // 03
            {7, 8, 6, 8, 5, 8, 5, 9, 4, 9},  // 04
            {6, 7, 6, 8, 5, 8, 4, 8, 4, 9},  // 05
            {6, 7, 6, 8, 5, 8, 4, 8, 3, 8},  // 06
            {6, 7, 5, 7, 4, 7, 4, 8, 3, 8},  // 07
            {6, 7, 5, 7, 4, 7, 3, 7},  // 08
            {6, 7, 5, 7, 4, 7, 4, 6, 3, 6},  // 09
            {6, 7, 6, 6, 5, 6, 4, 6, 3, 6},  // 10
            {6, 7, 6, 6, 5, 6, 4, 6, 4, 5},  // 11
            {7, 6, 6, 6, 5, 6, 5, 5, 4, 5},  // 12
            {7, 6, 6, 6, 6, 5, 5, 5},  // 13
            {7, 6, 7, 5, 6, 5, 6, 4},  // 14
            {7, 6, 7, 5, 7, 4},  // 15
            {7, 6, 7, 5, 7, 4},  // 16
            {8, 6, 8, 5, 8, 4},  // 17
            {8, 6, 8, 5, 9, 5, 9, 4},  // 18
            {8, 6, 9, 6, 9, 5, 10, 5},  // 19
            {8, 6, 9, 6, 10, 6, 10, 5, 11, 5},  // 20
            {9, 7, 9, 6, 10, 6, 11, 6, 11, 5},  // 21
            {9, 7, 9, 6, 10, 6, 11, 6, 12, 6},  // 22
            {9, 7, 10, 7, 11, 7, 11, 6, 12, 6},  // 23
            {9, 7, 10, 7, 11, 7, 12, 7},  // 24
            {9, 7, 10, 7, 11, 7, 11, 8, 12, 8},  // 25
            {9, 7, 9, 8, 10, 8, 11, 8, 12, 8},  // 26
            {9, 7, 9, 8, 10, 8, 11, 8, 11, 9},  // 27
            {8, 8, 9, 8, 10, 8, 10, 9, 11, 9},  // 28
            {8, 8, 9, 8, 9, 9, 10, 9},  // 29
            {8, 8, 8, 9, 9, 9, 9, 10},  // 30
            {8, 8, 8, 9, 8, 10},  // 31
    };

    private OreDetectorNeedlePath() {
    }

    /** Anzahl der Kopfpixel von Bild {@code frame} (0..31). */
    public static int length(int frame) {
        return HEAD[Math.floorMod(frame, 32)].length / 2;
    }

    /** Pixel {@code index} (0 = an der Nabe) von Bild {@code frame} als {x, y}. */
    public static int[] pixel(int frame, int index) {
        int[] head = HEAD[Math.floorMod(frame, 32)];
        return new int[]{head[2 * index], head[2 * index + 1]};
    }
}
