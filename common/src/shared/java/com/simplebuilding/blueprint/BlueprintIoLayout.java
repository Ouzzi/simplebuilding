package com.simplebuilding.blueprint;

import com.simplebuilding.blueprint.BlueprintEditorLayout.Rect;

/**
 * Reine Geometrie des Import-/Export-Fensters der Blaupause (Queue N55), ohne Minecraft-Abhaengigkeit
 * ({@code BlueprintIoLayoutTest}): Kopfzeile mit Titel und Format-Knopf, darunter das Textfeld, dann
 * zwei Statuszeilen und eine Knopfzeile mit bis zu drei Knoepfen (Export: Kopieren | Fertig; Import:
 * Einfuegen | Uebernehmen | Abbrechen).
 */
public final class BlueprintIoLayout {
    public static final int MAX_W = 420, MAX_H = 280;
    public static final int MARGIN = 8, GAP = 4, TITLE_H = 20, BUTTON_H = 20, STATUS_H = 10, FORMAT_W = 128;

    public final Rect modal, title, format, text, status1, status2;
    private final int buttonsY, buttonsX, buttonsW;

    private BlueprintIoLayout(int screenW, int screenH) {
        int w = Math.max(200, Math.min(screenW - 24, MAX_W));
        int h = Math.max(150, Math.min(screenH - 24, MAX_H));
        modal = new Rect((screenW - w) / 2, (screenH - h) / 2, w, h);
        int inner = w - 2 * MARGIN;
        int x = modal.x() + MARGIN;
        format = new Rect(x + inner - FORMAT_W, modal.y() + 4, FORMAT_W, 16);
        title = new Rect(x, modal.y() + 4, inner - FORMAT_W - GAP, 16);
        buttonsY = modal.bottom() - MARGIN - BUTTON_H;
        buttonsX = x;
        buttonsW = inner;
        status2 = new Rect(x, buttonsY - GAP - STATUS_H, inner, STATUS_H);
        status1 = new Rect(x, status2.y() - 1 - STATUS_H, inner, STATUS_H);
        int textY = modal.y() + TITLE_H + 6;
        text = new Rect(x, textY, inner, status1.y() - GAP - textY);
    }

    public static BlueprintIoLayout of(int screenW, int screenH) {
        return new BlueprintIoLayout(screenW, screenH);
    }

    /** Knopf {@code index} (0-basiert) von {@code count} in der Knopfzeile, gleich breit. */
    public Rect button(int index, int count) {
        int w = (buttonsW - (count - 1) * GAP) / count;
        return new Rect(buttonsX + index * (w + GAP), buttonsY, w, BUTTON_H);
    }
}
