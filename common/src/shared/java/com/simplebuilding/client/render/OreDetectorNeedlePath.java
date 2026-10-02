package com.simplebuilding.client.render;

/**
 * Kopfpixel der 32 Erzdetektor-Nadelbilder ({@code item/detector_needle_NN}), je Bild von der Nabe
 * zur Spitze als x, y, Schritt im 16x16-Feld (senkrecht ist die Nadel zwei Pixel breit: zwei Pixel je
 * Schritt). Geschrieben von {@code tools/textures/ore_detector_centred_2026_10_02.py} aus denselben
 * Zahlen wie die Texturen - nicht von Hand aendern. {@link OreDetectorGlint} laesst den
 * Auswahl-Schimmer Schritt fuer Schritt darauf laufen.
 */
public final class OreDetectorNeedlePath {
    private static final int[][] HEAD = {
            {7, 8, 0, 8, 8, 0, 7, 9, 1, 8, 9, 1, 7, 10, 2, 8, 10, 2},  // 00
            {7, 8, 0, 7, 9, 1, 7, 10, 2},  // 01
            {7, 8, 0, 7, 9, 1, 6, 9, 2, 6, 10, 3},  // 02
            {7, 8, 0, 6, 8, 1, 6, 9, 2, 5, 9, 3},  // 03
            {7, 8, 0, 6, 8, 1, 5, 8, 2, 5, 9, 3, 4, 9, 4},  // 04
            {6, 7, 0, 6, 8, 1, 5, 8, 2, 4, 8, 3, 4, 9, 4},  // 05
            {6, 7, 0, 6, 8, 1, 5, 8, 2, 4, 8, 3, 3, 8, 4},  // 06
            {6, 7, 0, 5, 7, 1, 4, 7, 2, 4, 8, 3, 3, 8, 4},  // 07
            {6, 7, 0, 5, 7, 1, 4, 7, 2, 3, 7, 3},  // 08
            {6, 7, 0, 5, 7, 1, 4, 7, 2, 4, 6, 3, 3, 6, 4},  // 09
            {6, 7, 0, 6, 6, 1, 5, 6, 2, 4, 6, 3, 3, 6, 4},  // 10
            {6, 7, 0, 6, 6, 1, 5, 6, 2, 4, 6, 3, 4, 5, 4},  // 11
            {7, 6, 0, 6, 6, 1, 5, 6, 2, 5, 5, 3, 4, 5, 4},  // 12
            {7, 6, 0, 6, 6, 1, 6, 5, 2, 5, 5, 3},  // 13
            {7, 6, 0, 7, 5, 1, 6, 5, 2, 6, 4, 3},  // 14
            {7, 6, 0, 7, 5, 1, 7, 4, 2},  // 15
            {7, 6, 0, 8, 6, 0, 7, 5, 1, 8, 5, 1, 7, 4, 2, 8, 4, 2},  // 16
            {8, 6, 0, 8, 5, 1, 8, 4, 2},  // 17
            {8, 6, 0, 8, 5, 1, 9, 5, 2, 9, 4, 3},  // 18
            {8, 6, 0, 9, 6, 1, 9, 5, 2, 10, 5, 3},  // 19
            {8, 6, 0, 9, 6, 1, 10, 6, 2, 10, 5, 3, 11, 5, 4},  // 20
            {9, 7, 0, 9, 6, 1, 10, 6, 2, 11, 6, 3, 11, 5, 4},  // 21
            {9, 7, 0, 9, 6, 1, 10, 6, 2, 11, 6, 3, 12, 6, 4},  // 22
            {9, 7, 0, 10, 7, 1, 11, 7, 2, 11, 6, 3, 12, 6, 4},  // 23
            {9, 7, 0, 10, 7, 1, 11, 7, 2, 12, 7, 3},  // 24
            {9, 7, 0, 10, 7, 1, 11, 7, 2, 11, 8, 3, 12, 8, 4},  // 25
            {9, 7, 0, 9, 8, 1, 10, 8, 2, 11, 8, 3, 12, 8, 4},  // 26
            {9, 7, 0, 9, 8, 1, 10, 8, 2, 11, 8, 3, 11, 9, 4},  // 27
            {8, 8, 0, 9, 8, 1, 10, 8, 2, 10, 9, 3, 11, 9, 4},  // 28
            {8, 8, 0, 9, 8, 1, 9, 9, 2, 10, 9, 3},  // 29
            {8, 8, 0, 8, 9, 1, 9, 9, 2, 9, 10, 3},  // 30
            {8, 8, 0, 8, 9, 1, 8, 10, 2},  // 31
    };

    private OreDetectorNeedlePath() {
    }

    /** Anzahl der Schritte (Nabe bis Spitze) von Bild {@code frame} (0..31). */
    public static int steps(int frame) {
        int[] head = HEAD[Math.floorMod(frame, 32)];
        return head[head.length - 1] + 1;
    }

    /** Die Pixel von Schritt {@code step} (0 = an der Nabe) in Bild {@code frame}, je {x, y}. */
    public static int[][] pixels(int frame, int step) {
        int[] head = HEAD[Math.floorMod(frame, 32)];
        int n = 0;
        for (int i = 2; i < head.length; i += 3) {
            if (head[i] == step) n++;
        }
        int[][] out = new int[n][];
        n = 0;
        for (int i = 0; i < head.length; i += 3) {
            if (head[i + 2] == step) out[n++] = new int[]{head[i], head[i + 1]};
        }
        return out;
    }
}
