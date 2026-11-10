# Plan b-texrate17: Runde 17 der roten Texturen außerhalb der TEXRATE16-Abnahme + Vanilla-Umfärbungen nach vanilla_derived.py (10.11., N19c)

Auftrag B N19c (/root/b-auftrag-20261109.md), Basis hub/b-bundle171-gen (5e6ba421f). Antwortcode TEXRATE17.
Kein Gate (A bündelt).

## Ist-Stand (erhoben)
- Bewertung `texrate15_2026_11_09.py --base HEAD` über die Basis: 2036 Texturen, 198 rot / 611 orange / 878 grün / 349 grau.
  (Runde 16 endete bei 101 rot; seitdem sind die Pads auf Besitzer-Entscheid zurück (89 rot) und Bündel 169–171 brachten
  neue Texturen, z. B. BACK18 caution_sign/level_door.)
- TEXRATE16-Abnahme: 464 Ziele (Stand c8ece50d6 = Vorschau `/root/sb-tools-b/previews/b-texrate16-preview.html`), davon
  448 noch in `texrate16_targets.json` (16 durch b-fix169tex/Bündel 169/170 herausgenommen: Dino-Eier trex/triceratops,
  melon_flesh, questionary_table_book/_side, 9 PAL16-Meißel, cloud_block/cloud_stone). Alle 464 sind tabu.
- Kandidaten N19c: 105 rote außerhalb der Abnahme (ohne Pads) + 43 orange aus `not_improved` (ohne Pads) = 148.
- veined/crystalline/layered_astralit, veined/crystalline/frosted_nihilith: seit 259a382d1 (03.10.) unverändert, pixelweise
  Umfärbung von Vanilla calcite/amethyst_block/dripstone_block/packed_ice nach Helligkeitsrang
  (`astralit_nihilit_alternates_2026_10_03.py`). Geprüft: aus dem 26.3-Jar + den astralit/nihilith_block-PNGs von 259a382d1
  exakt reproduzierbar (6/6 pixelgleich). Die heutigen Block-PNGs sind anders (texrate15/16) → die alten Block-PNGs (eigene
  Kunst, keine Mojang-Pixel) werden als Generator-Eingabe abgelegt.

## Ausschlüsse (je Kategorie begründet, im Werkzeug als EXCLUDE mit Grund)
| Kategorie | Texturen | Grund |
|---|---|---|
| TEXRATE16-Abnahme | 464 | Auftrag: liegen beim Besitzer zur Abnahme |
| Pads | 89 rot + orange | Besitzer-Originale (Entscheid 09.11.) |
| Besitzer-Texturen | cracked_diamond_block (Kern+simplelib), upgrade_band_cracked_diamond, Guide-Bücher, Trim-Vorlagen, bridge_redstone, extra_head | nie ändern |
| leuchtende Motive | *_lamp_on/_top_on, *_furnace_front_on, *_enchanter_top, dimensional_scrap, paint_box, glitched_bedrock | Runde 16: Stufung dämpft den Schein; Runde 17 erneut gemessen – kein Gewinn ohne Dämpfung |
| Vanilla-artiges Glas | display_case_glass* (17), transparent_light_pane* (3) | wie Vanilla-Glas/-Buntglas (selbst rot) |
| Tönungsmasken | ashwood/duskwood_leaves(_inner/_loose), end_grass | grau eingefärbt per BlockColorsMixin – Maske, nicht bewertbar |
| Teilebenen | arrow/shaft_*, arrow/fletching_*, sandwich/layer_*, custom_disc_(composed_)label, party_popper_confetti | werden zur Laufzeit/im Generator zusammengesetzt bzw. Partikel; 2–3 Farben gewollt |
| andere offene Abnahme | caution_sign, level_door_* (BACK18) | nicht vermischen (Memory texturen-nicht-vermischen) |
| Vanilla-Umfärbungen | 6 veined/crystalline/layered/frosted | → vanilla_derived.py (siehe unten) |

## Vorgehen Runde 17 (A8c-Runden, A8d-Methode)
Werkzeug `tools/textures/texrate17_2026_11_10.py` (Auswahl + Runden + Vorschau-Daten), Rezepte in
`texrate16_rework_2026_11_09.py` (ein Rework-Motor, ein `finish()`-Haken für alle Generatoren). Ziel-Liste
`tools/textures/texrate17_targets.json` (Ziele mit Gruppe/Methode/Runden-Messwerten, Ausschlüsse mit Grund, nicht verbessert).
- **R1 „größter Ampel-Gewinn“**: alle Runde-16-Methoden auf jede Kandidatin, beste Ampel, kleinste Härte.
- **R2 „sanftester Gewinn“**: dazu sanftere Rezepte (soft 16/20/24, Knie 40/60, quant 10, Flächenbrecher allein); unter den
  Methoden mit bester Ampel die kleinste mittlere Helligkeitsänderung.
- **R3 „Motiv-Schutz“** (gebaut): wie R2, aber nur Methoden, die die Kontrastspanne (p5–p95 Helligkeit) ≥ 80 % und die
  stärkste Sättigung ≥ 85 % behalten (R1/R2-Blick: Glimmer, Gold, Ofenöffnung, Bücherfarben, schwarze Fugen gingen verloren).
  Danach Blick 1×/8× neben Vanilla; Einzelentscheide als OVERRIDE im Werkzeug.
- Nur Ziele mit Ampel-Gewinn werden eingebaut (rot → orange/grün, orange → grün), wie Runde 16.

## Verankerung in den Generatoren
`texrate16_rework.load_targets()` liest zusätzlich `texrate17_targets.json` → `layers.save`, generate_textures, light panes
usw. wenden Runde 17 automatisch an. Generatoren, deren `--check` in einem check_data läuft und die nicht über `layers.save`
vergleichen, bekommen `finish()` explizit (je Ziel geprüft, Tabelle im Abschnitt Ergebnis).

## Vanilla-Umfärbungen → vanilla_derived.py
- `astralit_nihilit_alternates_2026_10_03.py`: `derived(jar)` (Sprites aus dem Jar, Körperfarben/Akzente aus
  `tools/textures/hand/alternates_2026_10_03/{astralit,nihilith}_block.png` = Block-PNGs von 259a382d1).
- `vanilla_derived.py`: 6 OUTPUTS-Zeilen (einzeln, Groovy-Fallback), OLDER_GENERATORS, WIKI_RENDERS (6 Renders).
- `.gitignore`: src/main-PNGs, Wiki-Kopien (block/, render/, sheets/), `git rm` PNG + .layers.json + Wiki-Kopien.
- texrate15 VANILLA_OLD bleibt (Dateien fehlen jetzt einfach in der Bewertung).
- Ports (mc1_21_11/mc26_4) bekommen diese 6 Texturen wie alle vanilla_derived-Ausgaben erst im Port-Run.

## Verifikation
layers.py check, texrate --selftest, check_missing_textures, Unittests tools/textures/tests (vanilla_derived, layers),
`vanilla_derived.py --out` mit Jar (6 neue Dateien pixelgleich zu den alten PNGs), check_data aller betroffenen Module,
`texrate16_rework --check` + `texrate17 --check`, generate_textures `--check` soweit lauffähig. sbt nur für den Jar-Inhalt
(ein gezielter Lauf `:mc26_3:fabric:processResources` + Blick in build/), falls lokal nicht belegbar.

## Ergebnis
- 148 Kandidaten → **23 eingebaut** (11 orange → grün, 7 rot → orange, 5 rot → grün), 82 begründet ausgeschlossen, 36 ohne
  Gewinn mit Motiv-Schutz. Gesamt 198 → 181 rot, 878 → 894 grün. Tabellen, Runden-Befunde und Verankerung je Ziel:
  docs/ai/TEXTUR-BEWERTUNG-2026-11-09.md „Runde 17“. Vorschau `/root/sb-tools-b/previews/b-texrate17-preview.html`
  (Generator `/root/sb-tools-b/b-texrate17-review.py`), Gruppen g1 Holz/Kristall-Laub (7), g2 Froschlichter (6),
  g3 Baublöcke/Modellflächen (10); Antwortcode `TEXRATE17 g<n>: tex=ok tex2=einwand(…)`.
- Runden-Bewertung: R1 (größter Gewinn) dämpfte Motive (Fugen, Glimmer, Gold, Ofenöffnung, Bücher grau); R2 (sanftester Gewinn)
  verlor 2–3-px-Akzente durch Quantisierung; R3 (Motiv-Schutz, gebaut) behält Kontrastspanne/Sättigung, Blick-Overrides
  Froschlichter (nur Brecher/Streifen) und ashwood_door_bottom (Streifen).
- 6 Vanilla-Umfärbungen jetzt Build-Erzeugung (vanilla_derived.py), pixelgleich (lokal mit Jar geprüft).

## Abweichungen / Entscheidungen
- Froschlichter (in Runde 16 bewusst gelassen) werden in Runde 17 vorgelegt: R3 bricht die Mitte mit Tönen der eigenen Rampe,
  ohne graue Flecken; Vanillas Mitte ist ruhiger – Entscheidung beim Besitzer (Einwand → alte Textur zurück).
- Fossil-Stufen nicht angefasst, obwohl Stufe 0–2 gewinnen würden (Stufenfolge muss gleich bleiben).
- questionary_table_gem / strip_station_bars ausgelassen statt Generator-Umbau (Gewinn nur messbar, Änderung ≤ 1,3 Helligkeit).
- generate_textures `--check` auf der Basis nicht lauffähig (fehlende Handvorlage enderite_carrot und item/reinforced_bundle seit
  a-mojang08 vanilla-abgeleitet) → Nihilith-Schachbrett über `texrate17 --check` (gleiche finish()-Rechnung) geprüft.
- Kein sbt: keine Java-/Namensänderung; der Gradle-Schritt ruft dasselbe `vanilla_derived.py`, das lokal mit Jar 216 Texturen
  inkl. der 6 neuen pixelgleich schrieb; Groovy-Fallback-Zeilen per Unittest (GroovyFallbackParse) geprüft.
- Wiki-Kopien der 23 Texturen (wiki/assets/textures, textures.js) erneuert das Bündel (-gen), wie in Runde 16.

## Verifikation (ausgeführt)
layers.py check 3146/0 Probleme; texrate --selftest ok; check_missing_textures 0 Fehler; Unittests tools/textures/tests 64 OK;
`texrate17 --check` 23 aktuell, `texrate16_rework --check` 448 aktuell; light_panes/copper_light_panes `--check` OK;
simpledinos gen_blocks `--check` 140 aktuell, simple_shops_2026_10_15 `--check` aktuell; simpleend gen_textures Neulauf ohne
Änderung an den Zielen; check_data simpleblocks/simpledinos/simpleend/simplelights/simplemusic/simplesandwiches/simpleshops grün;
vanilla_derived `--out` mit Jar 6/6 pixelgleich.
