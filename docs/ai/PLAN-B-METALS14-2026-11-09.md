# PLAN B-METALS14 (09.11.2026) – N9 Metalle nur kosmetisch

Auftrag: /root/b-auftrag-20261109.md Abschnitt „N9“ (Besitzer 09.11. ~18:15): „Silber, Blei, Zink, Nickel, Zinn und Messing sollen keine Tools oder
Rüstung besitzen. Nutzen soll erstmal nur kosmetisch sein: Baublöcke mit Chiseled-Varianten und weiteren Blöcken, z. B. pro Material 3 kosmetische
Blöcke, und dann noch weitere Schmuckstücke.“ Branch `b-metals14`, Basis hub/b-bundle163-gen (ac42dcfdb) + Merge hub/b-fix163 vor dem sbt.

## 1. Ist-Stand (erhoben 09.11.)
Geprüft: `modules/simpleores` (OresRegistry, gen_data.py, Rezepte, Tags), alle Module (`grep` nach `<metall>_(sword|pickaxe|axe|shovel|hoe|helmet|chestplate|leggings|boots|hammer|spear)`,
nach `simpleores:<metall>_ingot|nugget|block` und `c:ingots/<metall>`), Simple Hammers, Simple Arrows, Simple Trims, Kern (`common`, `src`), Konzept RG30 (Abschnitt 1.4: „keine Werkzeug-Tiers außer Roségold“), Test `silver_is_no_tool_material`.

| Metall | Werkzeuge | Rüstung | Hämmer | Trims (Besatz-Material) | Pfeile/Munition | sonstige Verwender |
|---|---|---|---|---|---|---|
| Silber | keine | keine | keine | keins | **Silberpfeil** (`simplearrows:silver_arrow`, 1 Silberklumpen + Stock + Feder → 4) | Silberblech, Silberspiegel (Deko) |
| Blei | keine | keine | keine | keins | **Blei-Spitze** im Pfeil-Baukasten (`simplearrows:fletching/tip/lead`, Tag `fletching/tip/lead` = Bleiklumpen) | Bleiplatte, Blei-Octet (Schalldämmung) |
| Zink | keine | keine | keine | keins | – | Messing-Rezept |
| Nickel | keine | keine | keine | keins | – | Edelstahl-Rezept |
| Zinn | keine | keine | keine | keins | – | Bronze-Rezept |
| Messing | keine | keine | keine | keins | – | – |
| (Edelstahl, Bronze) | keine | keine | keine | keins | – | – |
| (Roségold, Rosenkupfer – nicht genannt) | 5 je Legierung | 4 je Legierung | Vorschlaghammer je Legierung (Simple Hammers, bedingt) | – | – | Äpfel, Karotten |

**Ergebnis:** Die sechs genannten Metalle (und Edelstahl/Bronze) haben heute **weder Werkzeuge noch Rüstung noch Rezepte dafür** – das war schon die
Entscheidung von RG30 (a-rg08: „building metals, no tools“; Test `metal_recipes` prüft `brass/stainless_steel/bronze` ohne Schwert/Spitzhacke/Helm,
`silver_is_no_tool_material` für Silber). Es gibt also nichts zu entfernen; Welten können nicht brechen.

## 2. Entfernen – Entscheidungen
1. **Nichts zu entfernen, kein IdMove, kein `c:hidden_from_recipe_viewers`.** Weil keine Items existieren, wäre jeder Eingriff ein Leerlauf. Damit es so bleibt, wird der
   Test `silver_is_no_tool_material` zu `metals_are_cosmetic` verallgemeinert (alle sechs Metalle + Edelstahl/Bronze: kein Item mit TOOL-, WEAPON- oder EQUIPPABLE-Komponente,
   kein Item-Pfad mit Werkzeug-/Rüstungs-Endung), `check_data.py` prüft dasselbe statisch.
2. **Silberpfeil bleibt.** Munition, kein Werkzeug und keine Rüstung (Besitzer nannte „Tools oder Rüstung“); er gehört Simple Arrows (bedingt mit Simple Ores) und hat einen
   eigenen Vanilla-Nutzen (Bogen, A12). Entfernen wäre ein Eingriff in ein anderes, abgenommenes Modul. Gleiches gilt für die Blei-Pfeilspitze (Bauteil im Pfeil-Baukasten).
   → **Rückfrage R1** (falls der Besitzer auch Munition meint).
3. **Edelstahl/Bronze:** haben bereits nur Barren + Block (keine Werkzeuge) – sie sind schon kosmetisch, also „gleich behandelt“. Empfehlung: dieselbe Block-Familie wie
   die sechs Metalle **nach** der Abnahme von METALS14 nachziehen (gleicher Generator, eine Zeile je Metall). Nicht jetzt gebaut, weil der Besitzer sie nicht nannte. → **Rückfrage R2**.
4. **Roségold/Rosenkupfer** behalten Werkzeuge, Rüstung und Hämmer (Besitzer-Wahl ABN30-10/RG30, nicht genannt). → **Rückfrage R3** nur zur Sicherheit.

## 3. Kosmetische Blöcke (gebaut)
Je Metall M ∈ {silver, lead, zinc, nickel, tin, brass}; Basis ist der vorhandene `M_block`:

| Block | Form | Rezepte (Werkbank / Steinsäge) | Vorbild |
|---|---|---|---|
| `cut_M` Geschnittener M-block | Würfel, vier Platten | 2×2 Block → 4; Steinsäge Block → 4 | Geschnittener Kupferblock |
| `cut_M_stairs` / `cut_M_slab` | Treppe / Stufe aus `cut_M` | 6 → 4 / 3 → 6; Steinsäge cut → 1 / 2, Block → 4 / 8 | Geschnittene Kupfertreppe/-stufe |
| `M_tiles` M-Fliesen | Würfel, kleine Fliesen | 2×2 cut → 4; Steinsäge Block → 4, cut → 1 | Tiefenschieferfliesen |
| `M_pillar` M-Säule | Säule (ausrichtbar wie Quarzsäule) | 2 Blöcke übereinander → 2; Steinsäge Block → 1 | Quarzsäule |
| `chiseled_M` Gemeißelter M-block | Würfel, Rahmen + Zierraute | 2 cut-Stufen übereinander → 1; Steinsäge Block → 4, cut → 1 | Gemeißelter Kupferblock |

- **Familienlogik:** Treppe/Stufe nur am geschnittenen Block, wie Vanilla-Kupfer (Metallblöcke haben sonst keine Formteile; Mauern gibt es bei Vanilla-Metallen nicht → keine).
- **Octet-Regel:** `cut_M` hat Treppe/Stufe → bekommt nach PRINZIPIEN ein Octet. **Abweichung (bewusst):** die sechs `cut_M_octet` gehören nach `simpleoctets`
  (fremdes Modul, eigene Tests, bedingt mit Simple Ores wie `lead_block_octet`) und hängen an den Texturen, die der Besitzer erst wählt. Sie kommen als Folgepunkt nach der
  METALS14-Wahl (Queue-Zeile + Memory), Muster `lead_block_octet` (OctetMaterials + Sonderfall nur für `lead_block`).
- **Ort:** `simpleores` (Fachort: das Metall gehört Simple Ores; Simple Blocks nimmt nur Baublöcke ohne Fachort, Memory baubloecke-in-sb). Allein spielbar, nur Vanilla-Teile.
- **Eigenschaften:** Kopien des Basisblocks (Eisenblock bzw. Goldblock für Silber); Blei-Varianten wie der Bleiblock (Härte 25, Widerstand 1000, Bleiklang). **Schalldämmung:**
  volle Blei-Würfel (cut, tiles, pillar, chiseled) zählen wie der Bleiblock (`simpleores:soundproofing`, `occludes_vibration_signals`), Treppe/Stufe halb
  (`soundproofing_half`) – sonst dämmt eine Wand aus geschnittenem Blei überraschend nicht (A14 Wirkung an der Quelle).
- **Werkzeug-Stufe:** wie der Basisblock (`needs_iron_tool`, mineable/pickaxe). **Loot:** drop self (Stufe doppelt: 2 bei `type=double`). **Tabs:** `OresRegistry.BUILDING`
  hinter dem jeweiligen Metallblock (CreativeTabPolicy lenkt in den Simple-Ores-Tab). **Lang** EN/DE nach Vanilla-Kupfer-Schema. **Wiki/Guide:** Zeile in `FEATURES`
  (gen_data), Handbuch/Wiki/Discover werden im Bündel erzeugt.
- **A12:** Nutzen = Bau-Vielfalt für vorhandene Barren (die Metalle haben sonst kaum Senken), ändert nichts am Spielgeschehen, kein Eingriff in Vanilla.
- **Oxidation (nur Konzept):** realistisch nur bei Blei (weißgraue Patina, Bleicarbonat) und Zink (matt-grau „Weißrost“); Silber läuft schwarz an (Silbersulfid), Zinn/Nickel/Messing
  kaum (Messing nur dunkler). Vorschlag für später: 2 Stufen (frisch → matt) nur für Blei und Zink, Wachs wie Kupfer; nicht gebaut (A12: Kupfer-Mechanik gibt es schon,
  Mehrwert gering). → Rückfrage R4.

## 4. Schmuck (nur Konzept + Vorschau, NICHT gebaut)
Varianten in der Vorschau (je ein 16×16-Entwurf):
- **A Ketten-Block je Metall** (wie Vanilla-Kette, senkrecht/waagrecht) – geringster Aufwand, Vanilla-Form.
- **B Kronleuchter je Metall** (hängend, 3 Kerzenplätze, Licht 12 wie Laterne) – Deko mit Licht.
- **C Gitter/Stangen je Metall** (wie Eisenstangen) – Fenster/Zäune.
- **D Kleinteile als Items**: Ring, Anhänger, Kette (nur Inventar-Deko / Handelsgut für Simple Money) – Rückfrage, ob platzierbar.
- **E Glocken-Variante** je Metall (Klanghöhe je Metall) – nur Klang, Vanilla-Mechanik.
Empfehlung: A + B (klar sichtbar am Bau, wenig neue Mechanik). Besitzer wählt (Antwortcode METALS14).

## 5. Texturen (A8/A8b/A8c)
Eigene Pixel (keine Vanilla-Pixel): Formen als Ton-Raster (0 hell … 9 dunkel) in `tools/textures/metals14_2026_11_09.py`, je Metall auf die vorhandene
10-Ton-Rampe aus `ores_2026_10_30.RAMPS` (eigene Farbwerte; Blei `lead_a`, Messing `brass` …). Eingecheckt mit `.layers.json` (Ebenen „Grundfläche“, „Kanten“, „Glanz“).
Je Form ≥ 3 Runden, Bewertung unten und in der Vorschau. 5 Texturen je Metall (cut, tiles, pillar_side, pillar_top, chiseled) = 30.

### Textur-Runden (Bewertung, A8c)
Werkzeug: `python3.12 tools/textures/metals14_2026_11_09.py --rounds DIR --jar <26.3-client.jar>` (jede Runde je Metall/Form, Härte nach A8b); Kontaktbogen 5×/6× nearest neben
Vanilla (geschnittenes/gemeißeltes Kupfer, Tiefenschieferfliesen, Quarzsäule, Eisenblock) und neben dem vorhandenen Speicherblock, Wand 3×3 in Spielgröße, Würfel-Ansicht mit Spiel-Licht.
Vanilla: geschnittenes Kupfer 8 %, gemeißeltes 7 %, Eisenblock 6 %, Tiefenschieferfliesen 5 %, Quarzsäule 0 %.
- **R1** harte Kanten (Ton 2/7), helle Ecken, Säule 2↔6: 12–50 % hart → zu hart.
- **R2** Fase nur ein Ton (3/6), Fliesen-Schachbrett: Säule 7–20 %, geschnitten/Fliesen 17/35 % (Fugen zu dunkel).
- **R3** Fugen 5, Glanzstreifen je Platte versetzt, gemeißelt mit Raute: 1–20 %. Fliesen-Ecken wirken wie Nieten, Raute leuchtet wie ein Edelstein.
- **R4** Fliesen 8×4 im Versatz (Tiefenschiefer-Rhythmus), gemeißelt = eingravierte Raute, Ton-Spanne je Metall verengt (Nickel/Messing haben größere Rampenschritte): 0–8 %.
  Aber alle Familien deutlich dunkler als der Speicherblock (Zink/Nickel/Zinn-Block fast weiß) → Familie bricht. (Vorschau-Variante B)
- **R5** Grundfläche auf die mittlere Helligkeit des Speicherblocks: passt, aber Zink/Nickel/Zinn/Silber werden ein blasses Weiß, Kanten in Spielgröße kaum lesbar.
- **R6 (gebaut)** Grundfläche 0,4 Stufen unter dem Speicherblock, Spanne 6 Stufen: Familie liest sich mit dem Block, Kanten lesbar, 0–4 % hart, keine Verläufe.

## Abweichungen während der Umsetzung
- Test `silver_is_no_tool_material` bleibt (Name im Fabric-Adapter); `metals_are_cosmetic` kommt dazu statt ihn umzubenennen (geringerer Eingriff).
- Octets der geschnittenen Blöcke zurückgestellt (siehe 3., Folgepunkt).
- Handbuch/Wiki/Discover-Daten werden im Bündel erzeugt (`-gen`), hier nur `manual.json` über gen_data.

## 6. Dateien
`modules/simpleores/shared/java/com/simpleores/OresRegistry.java` (Blöcke), `tools/gen_data.py` (Modelle, Blockstates, Loot, Rezepte, Tags, Lang, Wiki), `tools/check_data.py`,
`test/OresTests.java` + Fabric-Adapter `ModuleGameTest.java` (neue Tests), `tools/textures/metals14_2026_11_09.py` + 30 PNG/.layers.json, `.claude/QUEUE.md`,
Vorschau `/root/sb-tools-b/previews/b-metals14-preview.html` (METALS14).

## 7. Tests (ein sbt am Ende)
Gametests: `metals_are_cosmetic` (statt `silver_is_no_tool_material`), `metal_blocks` (36 Blöcke registriert, Rezepte Werkbank + Steinsäge, Drops inkl. doppelter Stufe,
Blei-Varianten in den Dämm-Tags), Zählung in `registered`. Statisch: `gen_data.py --check`, `check_data.py`, `layers.py check`.
sbt: Compile + `module-simpleores-fabric-263,module-simpleores-standalone-fabric-263` + `fabric-263 --filter 'simplelib:module_game_test_creative_tab*'` + `simplebuilding:data_integrity*`.

## Rückfragen (Besitzer)
- R1: Silberpfeil/Blei-Pfeilspitze sind Munition – bleiben sie? (Empfehlung: ja)
- R2: Edelstahl und Bronze dieselbe Block-Familie geben? (Empfehlung: ja, nach Wahl der Texturen)
- R3: Roségold/Rosenkupfer behalten Werkzeuge/Rüstung? (Empfehlung: ja, Besitzer-Wahl RG30)
- R4: Patina-Stufen für Blei/Zink? (Empfehlung: nein, vorerst)
- Schmuck: A–E wählen.
