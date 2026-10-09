package com.simplebuilding.blueprint;

import java.util.Locale;

/**
 * Reine Geometrie des Blaupausen-Editors (Queue N23): Ueberschrift, darunter drei Bereiche
 * Materials | Code | Preview, jeder mit seiner Fusszeile. Ohne Minecraft-Abhaengigkeit, damit
 * "alles im Blatt, nichts ueberlappt" ohne Spiel-Boot pruefbar ist ({@code BlueprintEditorLayoutTest}).
 *
 * <ul>
 *   <li>Materials: Raster aus Icon + Zahl; darunter "X×Y×Z", in der naechsten Zeile "= n Bloecke",
 *       darunter Stab-Icon + Fortschrittsbalken.</li>
 *   <li>Code: Codeblock; darunter Status und Zeichenzahl untereinander, rechtsbuendig der Buch-Knopf.</li>
 *   <li>Preview (breiter): Vorschau mit Reset-Icon oben rechts im Fenster; darunter Signieren | Fertig.
 *       Das Hilfe-Buch liegt in derselben Flaeche: Reiter Blocks | Guide, im Blocks-Reiter Textfeld +
 *       Einfuegen, im Guide-Reiter der Text und unten der Kopier-Knopf.</li>
 * </ul>
 */
public final class BlueprintEditorLayout {

    /** Ein Rechteck in GUI-Pixeln; rechts/unten exklusiv. */
    public record Rect(int x, int y, int w, int h) {
        public int right() {
            return x + w;
        }

        public int bottom() {
            return y + h;
        }

        public boolean overlaps(Rect o) {
            return x < o.right() && o.x < right() && y < o.bottom() && o.y < bottom();
        }

        public boolean contains(Rect o) {
            return o.x >= x && o.y >= y && o.right() <= right() && o.bottom() <= bottom();
        }

        public boolean contains(double px, double py) {
            return px >= x && px < right() && py >= y && py < bottom();
        }
    }

    public static final int MAX_W = 560, MAX_H = 340;
    public static final int MARGIN = 8, HEADER = 30, FOOTER = 40, GAP = 6;
    /** Breite einer Materialzelle (Icon 16 + Zahl bis 4 Zeichen). */
    public static final int CELL_W = 46;
    /** Hoehe einer Materialzeile. */
    public static final int CELL_H = 18;
    public static final int ICON = 16;
    /** Breite eines Hilfe-Reiters (Symbol 16 + Rand). */
    public static final int TAB_W = 20;
    public static final int INSERT_W = 50;
    /** Zeilenhoehe der Hilfe-Blockliste. */
    public static final int HELP_ROW = 18;

    public final Rect panel;
    public final Rect list, code, view;
    public final Rect dimsLine, blocksLine, wandIcon, wandBar;
    public final Rect status, chars, book;
    public final Rect sign, done, doneWide, titleBox, confirm, cancel;
    public final Rect reset, example;
    public final Rect blocksTab, guideTab, insertField, insertButton, helpList, guideText, copy;

    private BlueprintEditorLayout(int screenW, int screenH, int statsTextW) {
        int pw = Math.min(screenW - 12, MAX_W);
        int ph = Math.min(screenH - 12, MAX_H);
        panel = new Rect((screenW - pw) / 2, (screenH - ph) / 2, pw, ph);
        int inner = pw - 2 * MARGIN;

        int listW = Math.min(Math.max(2 * CELL_W, statsTextW), Math.max(CELL_W + 8, inner * 28 / 100));
        int viewW = Math.max(120, inner * 40 / 100);
        int codeW = inner - listW - viewW - 2 * GAP;
        if (codeW < 120) {
            viewW = Math.max(100, viewW - (120 - codeW));
            codeW = inner - listW - viewW - 2 * GAP;
        }
        int listX = panel.x() + MARGIN;
        int codeX = listX + listW + GAP;
        int viewX = codeX + codeW + GAP;
        int bodyY = panel.y() + HEADER;
        int footerY = panel.bottom() - 6 - FOOTER;
        int bodyH = footerY - 4 - bodyY;
        list = new Rect(listX, bodyY, listW, bodyH);
        code = new Rect(codeX, bodyY, codeW, bodyH);
        view = new Rect(viewX, bodyY, viewW, bodyH);

        dimsLine = new Rect(listX, footerY + 1, listW, 9);
        blocksLine = new Rect(listX, footerY + 12, listW, 9);
        wandIcon = new Rect(listX, footerY + 23, ICON, ICON);
        wandBar = new Rect(listX + ICON + 3, footerY + 28, Math.max(1, listW - ICON - 3), 6);

        int textW = Math.max(1, codeW - ICON - 4);
        status = new Rect(codeX, footerY + 1, textW, 9);
        chars = new Rect(codeX, footerY + 12, textW, 9);
        book = new Rect(codeX + codeW - ICON, footerY + 2, ICON, ICON);

        int bw = (viewW - 4) / 2;
        sign = new Rect(viewX, footerY, bw, 20);
        done = new Rect(viewX + viewW - bw, footerY, bw, 20);
        doneWide = new Rect(viewX, footerY, viewW, 20);
        titleBox = new Rect(viewX, footerY, viewW, 18);
        confirm = new Rect(viewX, footerY + 20, bw, 20);
        cancel = new Rect(viewX + viewW - bw, footerY + 20, bw, 20);

        reset = new Rect(viewX + viewW - ICON - 2, bodyY + 2, ICON, ICON);
        example = new Rect(viewX + 6, bodyY + bodyH - 24, viewW - 12, 18);

        // Reiter und Kopieren als Symbole (Queue N29): links die Reiter, rechts in derselben Zeile Kopieren.
        blocksTab = new Rect(viewX + 2, bodyY + 2, TAB_W, ICON);
        guideTab = new Rect(blocksTab.right() + 2, bodyY + 2, TAB_W, ICON);
        copy = new Rect(viewX + viewW - ICON - 2, bodyY + 2, ICON, ICON);
        insertField = new Rect(viewX + 3, bodyY + 19, viewW - 6 - INSERT_W - 2, 16);
        insertButton = new Rect(insertField.right() + 2, bodyY + 19, INSERT_W, 16);
        helpList = new Rect(viewX + 1, bodyY + 38, viewW - 2, bodyH - 39);
        guideText = new Rect(viewX + 1, bodyY + 19, viewW - 2, bodyH - 20);
    }

    /**
     * Die Geometrie fuer einen Bildschirm {@code screenW × screenH} (GUI-Pixel). {@code statsTextW}
     * ist die Breite der laengsten Materials-Fusszeile im groessten Fall ("256×256×256",
     * "= 4194304 Bloecke" in der Spielsprache); so breit wird die Spalte, soweit Platz ist.
     */
    public static BlueprintEditorLayout of(int screenW, int screenH, int statsTextW) {
        return new BlueprintEditorLayout(screenW, screenH, statsTextW);
    }

    /** Spalten des Materialrasters. */
    public int materialColumns() {
        return Math.max(1, list.w() / CELL_W);
    }

    /** Verkleinerung, damit ein Text der Breite {@code textW} in {@code avail} passt (nie groesser als 1). */
    public static float fitScale(int textW, int avail) {
        if (textW <= 0 || textW <= avail) {
            return 1f;
        }
        return Math.max(0.25f, avail / (float) textW);
    }

    /** Menge kurz fuers Raster: bis 9999 genau, dann 12k / 1.2M (genau im Tooltip). */
    public static String compactCount(long n) {
        if (n < 10_000) {
            return Long.toString(n);
        }
        if (n < 1_000_000) {
            return n / 1000 + "k";
        }
        long tenths = n / 100_000;
        return tenths % 10 == 0 || tenths >= 100 ? tenths / 10 + "M" : String.format(Locale.ROOT, "%d.%dM", tenths / 10, tenths % 10);
    }
}
