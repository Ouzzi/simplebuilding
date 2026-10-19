# Plan b-brushanim (18.10.2026): Farbpinsel-Schwenkanimation

Ist: `ColorBrushItem extends BrushItem` -> hat Benutzungsdauer + UseAnim BRUSH wie Vanilla, `use_cycle` liefert also Werte.
Item-Definition `items/color_brush.json` = `select(brush_ink)` mit Modellen plain/tip(getoent)/palette, kein `use_cycle`.
Vanilla `items/brush.json`: `range_dispatch use_cycle` (period 10, scale 0.1, Schwellen 0.25/0.5/0.75 -> brush_brushing_0/1/2, Fallback brush).
Format in 26.2 und 26.3 identisch; Datagen laeuft nur 26.3 (mc26_3/generated), 26.2-Linie hat keine color_brush-Dateien -> nur 26.3.

Soll: aussen `select(brush_ink)` (bestehend), je Tinte innen `range_dispatch(use_cycle)` mit Fallback = bisheriges Modell und
3 Schwenk-Modellen. Keine neuen Texturen: neue Modelle `color_brush[_tip|_palette]_brushing_0..2` mit Parent
`minecraft:item/brush_brushing_N` (liefert die display-Transforms) und denselben Texturen wie die Standardmodelle (9 Modelle;
die 16 Farbstoff-Faelle teilen sich die 3 Tip-Modelle, nur die Tints unterscheiden sich).
Dateien: `src/main/java/com/simplebuilding/datagen/ModModelProvider.java` (generateColorBrush), `mc26_3/generated/**` (Datagen auf sb-test).
Pruefung: tools/check_item_properties.py, tools/check_missing_textures.py, Gametests `simplebuilding:*brush*` (fabric-263, neoforge-263),
Compile aller Loader, gradle check. Client-Sicht offen (kein Client-Lauf).
