package com.simplebuilding.chess;

/**
 * Die sechs Schachfiguren. {@link #height} ist die Hoehe der 3D-Figur in Pixeln (1/16 Block); die flache Figur ist
 * immer {@link #FLAT_HEIGHT} hoch. Der Fuss jeder Figur ist {@link #FOOT} Pixel breit und steht mittig auf einem
 * Viertel (8 x 8 Pixel = ein Feld des Quarz-Schachbretts). Die Modelle schreibt {@code tools/textures/chess_2026_10_06.py}.
 */
public enum ChessPiece {
    PAWN("pawn", 7),
    ROOK("rook", 9),
    KNIGHT("knight", 10),
    BISHOP("bishop", 11),
    QUEEN("queen", 12),
    KING("king", 14);

    /** Hoehe der flachen Figur (Spielstein mit erhabenem Symbol) in Pixeln. */
    public static final int FLAT_HEIGHT = 2;
    /** Breite des Fusses in Pixeln. */
    public static final int FOOT = 6;

    private final String id;
    private final int height;

    ChessPiece(String id, int height) {
        this.id = id;
        this.height = height;
    }

    public String id() {
        return this.id;
    }

    /** Hoehe der Figur in Pixeln. */
    public int height(boolean flat) {
        return flat ? FLAT_HEIGHT : this.height;
    }
}
