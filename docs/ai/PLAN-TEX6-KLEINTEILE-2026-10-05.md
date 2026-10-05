# Plan Textur-Runde 6 + neue Kleinteile (2026-10-05)

Branch `claude-tex6` (von `cb466f86`, dazu Cherry-Pick der Runde-5-Generatoren `7686802f`). Nur committen.

## Besitzer-Entscheidungen und Umsetzung
1. **Trainingspuppe-Icon**: Runde-5-Variante C „mehr Vanilla“ – neu gezeichnet in
   `tools/textures/texture_round6_2026_10_05.py` (nur Vanilla-Farben carved_pumpkin/hay_block_side/target_side/Stein,
   Licht oben links, Kontur im dunklen Materialton, Zielscheibe symmetrisch, weniger Einzelpixel). Item + Wiki-Kopie.
2. **Sage Orb**: `sage_orb_smaller_2026_10_04.py --install A` (10x10). `round3_settled` schreibt die alte Kugel noch –
   dort als „superseded“ markiert (wie schon beim Erz).
3. **Simple-Money-Fasern**: Mischung I (Schlaufe mit Perlen) + J (Spirale): zwei Pixel breiter Ring aus gewickelter
   Faser (Vorderwindungen hell, Rückwindungen dunkler) mit drei Perlen; Farben aus den Original-PNGs (vor den
   Vorschlagsrunden, `0c8aa50d` = `hand/q1`): Spezialfaser Lila/Blau + Lederbraun-Perlen, Harzfaser Weiß/Grau +
   Orange-Perlen. `vanilla_style_2026_10_02.py` führt die Fasern nicht mehr („keep“ entfernt, sonst würde --check
   die neuen Bilder als veraltet melden).
4. **Kontrast**: Modul-Handbücher (`module_guide_books_2026_10_05.py`): Deckeltöne S/L/C/D um ihren Mittelwert ×1,6
   (bei ×1,25 kaum sichtbar, weil C und D fast gleich sind), dazu das ganze Icon ×1,2. Mod-Verzauberungsbücher des
   Besitzers (`generate_textures.py`, Quelle bleibt `hand/q1/books`): Einband/Motiv ×1,35 um den Mittelwert (×1,25 war in der Vorschau kaum sichtbar), graue
   Seiten unverändert. Form/Motiv unverändert. Helfer `more_contrast`/`spread` in `texture_round6`.
5. **Simple-Riding-Bücher**: neue Motive im Stil der Besitzerbücher (dessen Buchkörper `linear`, Einband umgefärbt,
   Motiv in hellen Einbandtönen), je 3 Varianten. Eingebaut: Leaping A (Hufeisen-Bogen mit Nägeln, Sprungkraft-Grün),
   Tailwind A (Windstriche mit Wirbel, helles Wind-Petrol). `riding_books_settled_2026_10_04.py` ist jetzt ein Wrapper
   mit `--check` und Vorschau.
6. **Neue Kleinteile** (Konvention Steinkiesel/Feuersteinsplitter, `McVersion.SMALL_PLACEABLES`, nur 26.3):
   - `fire_chip` „Feuerkugelsplitter“: 1 Feuerkugel → 4; 4 im 2x2 → Feuerkugel. **Verwendung**: zündet wie die Feuerkugel
     die Kerzen eines Kleinteil-Haufens an (verbraucht 1 Splitter statt einer ganzen Kugel) – andocken an die vorhandene
     Kerzen-Logik in `PlacedSmallPartsBlock#useItemOn`.
   - `ice_chip` „Eissplitter“: 1 Eis → 4, 1 Packeis → 9 (Blaueis bewusst nicht: 81 Eis für 9 Splitter wäre sinnlos);
     4 im 2x2 → Eis. **Verwendung**: löscht brennende Kerzen eines Haufens (Gegenstück zum Feuersplitter, 1 Splitter).
   - Eigene Idee `obsidian_chip` „Obsidiansplitter“: 1 Obsidian → 9 (wie Bruchstein → 9 Kiesel), 9 im 3x3 → Obsidian.
     Vulkanglas-Splitter als dunkles Deko-Kleinteil neben Feuerstein (Logik: Block/Material → Splitter → zurück).
     Echo-/Prismarin-Splitter verworfen: Vanilla hat dort schon Scherben, die bereits ablegbar sind.
   - Alle drei: Tag `placeable_small` (ablegbar, Haufen), Kreativ-Reihe „small_parts“, Suchtab nach Feuerkugel/Eis/
     Obsidian, Rezepte + Modelle per Datagen (26.3), Lang EN+DE in beiden Bäumen, Wiki `manual.json`, Texturen
     `tools/textures/small_chips_2026_10_05.py`.
   - Tests: Reihe in `DataIntegrityTests#materialsTabIsLaidOutInRows`; neue GameTests in `PlacedTemplateTests`
     (Feuersplitter zündet, Eissplitter löscht, Rezeptmengen).

## Risiken
- Dupe-Freiheit: alle Rückwege kosten mindestens so viel wie der Hinweg (4→1, 9→1; Packeis 9 Eis → 9 Splitter → 2 Eis).
- Neue Items müssen die generischen Item-Checks bestehen (Kreativtab, Suchtab, Modelle, Namen EN≠DE, Wiki-Prosa).

## Verifikation
- Generatoren `--check`, Vorschau `previews/texturen-runde6-vorschau.png`.
- Datagen 26.3, `fabric-263`, `neoforge-263`, `module-simplemoney-fabric-263`, `module-simpleriding-fabric-263`,
  `gradlew check -q`, `tools/guides/module_guides.py --check`, Wiki venv `--all` + uv `--all --check`.
- Nicht getestet: Client-Ansicht (kein Client gestartet).
