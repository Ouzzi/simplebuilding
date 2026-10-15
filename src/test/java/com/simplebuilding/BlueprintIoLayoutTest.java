package com.simplebuilding.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.simplebuilding.blueprint.BlueprintEditorLayout.Rect;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Geometrie des Import-/Export-Fensters (Queue N55) und der beiden neuen Knoepfe im Editor.
 *
 * <p><b>Was diesen Test bricht:</b> ein Fenster, das aus dem Bildschirm ragt; Textfeld, Statuszeilen und
 * Knoepfe, die sich decken; Knopfzeilen mit ueberlappenden Knoepfen; Import-/Export-Knopf im Editor, die
 * sich mit Status, Zeichenzahl oder untereinander ueberschneiden oder aus dem Blatt ragen.
 */
class BlueprintIoLayoutTest {

    private static final int[][] SCREENS = {{320, 240}, {427, 240}, {480, 270}, {640, 360}, {960, 540}, {1920, 1080}};

    @Test
    void fensterBleibtImBildschirmUndNichtsUeberlappt() {
        for (int[] s : SCREENS) {
            BlueprintIoLayout l = BlueprintIoLayout.of(s[0], s[1]);
            String where = s[0] + "x" + s[1];
            assertTrue(l.modal.x() >= 0 && l.modal.y() >= 0 && l.modal.right() <= s[0] && l.modal.bottom() <= s[1], where + ": modal on screen");
            List<Rect> parts = new ArrayList<>(List.of(l.title, l.format, l.text, l.status1, l.status2));
            for (int count = 2; count <= 3; count++) {
                for (int i = 0; i < count; i++) {
                    parts.add(l.button(i, count));
                }
                for (int a = 0; a < parts.size(); a++) {
                    assertTrue(l.modal.contains(parts.get(a)), where + ": part " + a + " inside the modal");
                    for (int b = a + 1; b < parts.size(); b++) {
                        assertTrue(!parts.get(a).overlaps(parts.get(b)), where + ": parts " + a + " and " + b + " overlap");
                    }
                }
                parts.subList(5, parts.size()).clear();
            }
            assertTrue(l.text.w() >= 150 && l.text.h() >= 40, where + ": the text field is usable: " + l.text);
        }
    }

    @Test
    void knopfzeileTeiltBreiteGleichmaessig() {
        BlueprintIoLayout l = BlueprintIoLayout.of(640, 360);
        assertEquals(l.button(0, 3).w(), l.button(1, 3).w());
        assertEquals(l.button(0, 3).y(), l.button(2, 3).y());
        assertTrue(l.button(0, 3).right() <= l.button(1, 3).x() && l.button(2, 3).right() <= l.modal.right());
    }

    @Test
    void importUndExportKnopfImEditor() {
        for (int[] s : SCREENS) {
            BlueprintEditorLayout l = BlueprintEditorLayout.of(s[0], s[1], 92);
            String where = s[0] + "x" + s[1];
            assertTrue(l.panel.contains(l.importButton) && l.panel.contains(l.exportButton), where + ": buttons inside the sheet");
            assertTrue(!l.importButton.overlaps(l.exportButton), where + ": import and export do not overlap");
            assertTrue(l.importButton.x() < l.exportButton.x(), where + ": import left of export");
            for (Rect r : List.of(l.importButton, l.exportButton)) {
                assertTrue(!r.overlaps(l.status) && !r.overlaps(l.chars) && !r.overlaps(l.code) && !r.overlaps(l.done)
                        && !r.overlaps(l.sign) && !r.overlaps(l.book), where + ": " + r + " overlaps a neighbour");
            }
        }
    }
}
