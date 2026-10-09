package com.simplebuilding.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.simplebuilding.blueprint.BlueprintEditorLayout.Rect;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Geometrie des Blaupausen-Editors (Queue N23): alles liegt im Blatt, die drei Bereiche und ihre
 * Fusszeilen ueberlappen sich nicht, "X×Y×Z" und "= n Bloecke" stehen in eigenen Zeilen, und die
 * Knoepfe in der Vorschau (Reset, Reiter, Einfuegen, Kopieren) liegen im Vorschaufenster, ohne sich
 * zu decken. Geprueft ueber die GUI-Groessen von 320×240 bis 1920×1080.
 *
 * <p><b>Was diesen Test bricht:</b> eine Spalte, die in die naechste ragt; Masse und Blockzahl wieder
 * in einer Zeile; Status/Zeichenzahl unter dem Buch-Knopf; ein Knopf ausserhalb des Blatts oder der
 * Vorschau; Reiter, Textfeld und Liste der Hilfe uebereinander.
 */
class BlueprintEditorLayoutTest {

    private static final int[][] SCREENS = {{320, 240}, {427, 240}, {480, 270}, {640, 360}, {683, 384}, {960, 540}, {1920, 1080}};
    private static final int[] STATS = {20, 92, 140};

    @Test
    void allesImBlattUndNichtsUeberlappt() {
        for (int[] s : SCREENS) {
            for (int stats : STATS) {
                BlueprintEditorLayout l = BlueprintEditorLayout.of(s[0], s[1], stats);
                String where = s[0] + "x" + s[1] + " stats " + stats;
                Map<String, Rect> top = new LinkedHashMap<>();
                top.put("list", l.list);
                top.put("code", l.code);
                top.put("view", l.view);
                top.put("dims", l.dimsLine);
                top.put("blocks", l.blocksLine);
                top.put("wandIcon", l.wandIcon);
                top.put("wandBar", l.wandBar);
                top.put("status", l.status);
                top.put("chars", l.chars);
                top.put("book", l.book);
                top.put("sign", l.sign);
                top.put("done", l.done);
                disjointInside(l.panel, top, where);

                Map<String, Rect> signing = new LinkedHashMap<>(top);
                signing.remove("sign");
                signing.remove("done");
                signing.put("title", l.titleBox);
                signing.put("confirm", l.confirm);
                signing.put("cancel", l.cancel);
                disjointInside(l.panel, signing, where + " (signing)");
                assertTrue(l.panel.contains(l.doneWide) && !l.doneWide.overlaps(l.code), where + ": done (read-only)");

                disjointInside(l.view, Map.of("reset", l.reset, "example", l.example), where + " (preview)");
                disjointInside(l.view, Map.of("blocksTab", l.blocksTab, "guideTab", l.guideTab, "insertField", l.insertField,
                        "insertButton", l.insertButton, "list", l.helpList), where + " (help: blocks)");
                disjointInside(l.view, Map.of("blocksTab", l.blocksTab, "guideTab", l.guideTab, "text", l.guideText,
                        "copy", l.copy), where + " (help: guide)");
                assertTrue(l.code.w() >= 90, where + ": the code column shrank to " + l.code.w());
            }
        }
    }

    @Test
    void masseUndBlockzahlStehenUntereinanderInSpaltenbreite() {
        BlueprintEditorLayout l = BlueprintEditorLayout.of(960, 540, 92);
        assertTrue(l.blocksLine.y() >= l.dimsLine.bottom(), "'= n blocks' starts below 'X×Y×Z'");
        assertEquals(l.list.w(), l.dimsLine.w());
        assertEquals(l.list.w(), l.blocksLine.w());
        assertTrue(l.wandIcon.y() >= l.blocksLine.bottom() && l.wandBar.x() >= l.wandIcon.right(),
                "wand icon below the counts, bar to its right");
        assertEquals(92, l.list.w(), "the materials column takes the width of its longest line");
        assertEquals(2, l.materialColumns());
        assertTrue(l.view.w() > l.list.w() && l.view.w() >= l.code.w() * 9 / 10, "the preview is the wide column");
        assertTrue(l.book.right() == l.code.right(), "the help book sits right-aligned under the code");
        assertTrue(l.blocksTab.x() < l.guideTab.x(), "Blocks is the first tab");
        assertTrue(l.insertButton.x() >= l.insertField.right() && l.insertButton.y() == l.insertField.y(),
                "Insert sits right next to the text field");
        assertTrue(l.copy.y() == l.blocksTab.y() && l.copy.x() > l.guideTab.right() && l.copy.w() == BlueprintEditorLayout.ICON,
                "the copy icon sits in the tab row, right of the tabs");
        assertTrue(l.blocksTab.w() <= BlueprintEditorLayout.TAB_W && l.guideTab.w() <= BlueprintEditorLayout.TAB_W,
                "the tabs are icons, not text buttons");
    }

    @Test
    void textPasstSichEin() {
        assertEquals(1f, BlueprintEditorLayout.fitScale(50, 92));
        assertEquals(1f, BlueprintEditorLayout.fitScale(92, 92));
        float s = BlueprintEditorLayout.fitScale(120, 80);
        assertTrue(s < 1f && 120 * s <= 80.001f, "scaled text fits: " + s);
    }

    @Test
    void kurzeMengen() {
        for (Object[] c : List.of(new Object[]{0L, "0"}, new Object[]{64L, "64"}, new Object[]{9999L, "9999"},
                new Object[]{12_345L, "12k"}, new Object[]{999_999L, "999k"}, new Object[]{1_000_000L, "1M"},
                new Object[]{1_234_567L, "1.2M"}, new Object[]{4_194_304L, "4.1M"}, new Object[]{16_777_216L, "16M"})) {
            String text = BlueprintEditorLayout.compactCount((Long) c[0]);
            assertEquals(c[1], text, "compact count of " + c[0]);
            assertTrue(text.length() <= 4, "fits a material cell: " + text);
        }
    }

    private static void disjointInside(Rect outer, Map<String, Rect> rects, String where) {
        List<Map.Entry<String, Rect>> list = List.copyOf(rects.entrySet());
        for (int i = 0; i < list.size(); i++) {
            Rect a = list.get(i).getValue();
            assertTrue(a.w() > 0 && a.h() > 0, where + ": " + list.get(i).getKey() + " is empty " + a);
            assertTrue(outer.contains(a), where + ": " + list.get(i).getKey() + " " + a + " leaves " + outer);
            for (int j = i + 1; j < list.size(); j++) {
                assertTrue(!a.overlaps(list.get(j).getValue()), where + ": " + list.get(i).getKey() + " " + a
                        + " overlaps " + list.get(j).getKey() + " " + list.get(j).getValue());
            }
        }
    }
}
