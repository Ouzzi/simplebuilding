# Plan: Besitzer-Texturen Wollknäuel + Diamant-Kiesel, Scrap-Kontur (2026-10-05)

**Was:** Zwei Besitzer-Zeichnungen (Resprite-Screenshots) 1:1 als 16×16 übernehmen, nur Farben auf Vanilla-Paletten
mappen; Raw Enderite Scrap (`item/layered_raw_enderite`) bekommt die dunkle Außenkontur des Raw Enderite Fragment
(`item/raw_enderite`, `#1c0a33`).

**Werkzeug:** `tools/textures/owner_round_2026_10_05.py`
- Rekonstruktion: Canvas-Rahmen gemessen (Wolle x 931–1599, y 336–1004; Kiesel x 1052–1367, y 449–764 bei
  2420×1668), Zellmitten gemittelt. Wolle: Creme-Hintergrund per Flutfüllung vom Rand = transparent, umschlossene
  Hintergrundzellen = weiße Wolle; Töne als 8 Helligkeitsstufen (Abtastrauschen ≤ 3). Kiesel: neutrales Grau = transparent.
  Ergebnis in `tools/textures/hand/owner/{yarn_ball,diamond_pebble}_owner.png`.
- Farben: Wolle → `block/white_wool` (helle Stufen = Wollfarbe gleicher Helligkeit, dunklere Stufen = dunkelste
  Wollfarbe auf die Besitzer-Helligkeit skaliert, gleicher Farbton). Kiesel → `item/diamond`, Helligkeitsspanne auf die
  Diamant-Rampe gestreckt, nächste Stufe.
- Scrap: jedes Silhouettenpixel mit transparentem 4-Nachbarn → `#1c0a33`; Form und Innenpixel bleiben.

**Dateien:** `src/main/...item/{diamond_pebble,layered_raw_enderite}.png` + 1.21.11-Kopien, `mc26_3/overlay/...item/yarn_ball.png`,
Wiki-Assets (über `wiki/generate.py --all`). `silent_dandelion_2026_10_03.py` liest das Wollknäuel jetzt aus
`tools/textures/hand/yarn_ball.png`; `generate_textures.py` nimmt den Kiesel als Handtextur aus dem Hauptbaum.

**Annahmen:** Wollknäuel hat keine Farbvarianten (ein Item `yarn_ball`). Der Faden unten rechts reicht bis an den
Bildrand (Besitzer-Zeichnung, nicht beschnitten). Pfeilspitzen-Texturen (Diamant, aus der Kiesel-Rampe abgeleitet)
nicht neu erzeugt.

**Verifikation:** Vorschau `previews/besitzer-wolle-kiesel-scrap-vorschau.png`, `silent_dandelion --check`,
Wiki `--all` + `--all --check`, Testrunner `fabric-263` gefiltert `data_integrity*`, `check -q -PskipWiki`.
