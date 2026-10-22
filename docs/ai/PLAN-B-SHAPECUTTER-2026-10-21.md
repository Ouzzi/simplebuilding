# Plan: Formsäge → Shapecutter (Sitzung B, Branch `b-shapecutter`, 21.10.)

Von `claude-wave1` 156b2cbf1. Queue-Punkt „shaping saw shapecutter nennen …“ (`[~] (B, b-shapecutter, 21.10.)`).
Besitzer wörtlich: „shaping saw shapecutter nennen, dann die gleiche ui wie stonecutter mit mehr rezepten, dann die textur
einfach nur die stein base aus metall. also die blade wieder vanilla und dann etwas glenzender/polierter.“
Hintergrund: `docs/ai/PLAN-FORMSAEGE-2026-10-17.md` (Textur vom 19.10. wird bewusst ersetzt).

## Ist-Zustand (erhoben)
- Block/Item/Menü `simpleblocks:shaping_saw` (`saw/Sawing.java` NAME, `ShapingSawBlock/Menu`, `client/ShapingSawScreen`),
  Rezepttyp `simpleblocks:sawing`, Payload `simpleblocks:saw_recipes`. Daten aus `modules/simpleblocks/tools/generate_data.py`.
- Bildschirm: eigenes Panel 176×200 in INVENTORY-Grau, 6×4 Kacheln, eigenes Suchfeld, Hinweistexte, Pfeil; Kacheln/Scroller
  sind Vanilla-Sprites (referenziert).
- Textur: `tools/textures/shaping_saw_2026_10_17.py` (Vanilla-derived): Messingkappen → Kupfer, Kupferleisten am Holz,
  Sägeblatt kupfern umgefärbt; Unterseite = Vanilla-Textur.
- Simple Blocks hängt seit b-blocksmove an SimpleLib → `IdMoves` (Muster `SimpleBlocks.registerIdMoves`, Test
  `NatureTests.movedIdsFromSimplebuilding`) ist verfügbar.
- Vanilla 26.3 `StonecutterScreen`/`StonecutterMenu` (aus `/root/mc263src`): Bild 176×166, Eingabe (20,33), Ergebnis (143,33),
  Inventar (8,84), Kacheln 4×3 à 16×18 ab (52,14), Scroller (119,15) 12×15 auf 54 px, Hintergrund
  `minecraft:textures/gui/container/stonecutter.png`, Klick-Sound `UI_STONECUTTER_SELECT_RECIPE`, Shift-Klick über `quickMoveStack`.

## Entscheidungen
1. **Name.** EN „Shapecutter“ (wie Vanilla „Stonecutter“ gebaut). DE bleibt „Formsäge“: Vanilla-DE übersetzt Stonecutter
   mit „Steinsäge“ (Material + „säge“); „Formsäge“ ist dazu das passende Gegenstück (sie schneidet Formen aus jedem Material),
   ein Wort wie „Formschneider“ fiele aus der Vanilla-Reihe.
2. **Registry-ID → `simpleblocks:shapecutter`**, mit sauberer Migration: `IdMoves.move` für BLOCK, ITEM, RECIPE
   (`shaping_saw` → `shapecutter`) und ADVANCEMENT (`recipes/decorations/shaping_saw` → `…/shapecutter`) in
   `SimpleBlocks.registerIdMoves` (läuft schon in allen drei Loader-Einstiegen vor den Registry-Events, Forge-Aliase inklusive).
   Menütyp (nicht gespeichert) heißt mit. Rezepttyp `simpleblocks:sawing` und Payload bleiben (keine Namensfrage, Datenpakete
   anderer Module nutzen `sawing`). GameTest `saw_id_moved`: alte IDs lösen auf die neuen auf (Block, Item, Rezept, Fortschritt).
   Klassen heißen mit: `ShapecutterBlock`, `ShapecutterMenu`, `client/ShapecutterScreen`.
3. **UI = Vanilla-Steinsäge.** Gleiche Maße (176×166), Slots an Vanilla-Positionen, 4×3 Kacheln mit Scrollleiste, Klick-/
   Scroll-/Zieh-Verhalten und Sounds wie `StonecutterScreen`; Hintergrund ist die Vanilla-Datei selbst (nur referenziert →
   keine Mojang-Pixel im Repo, Ressourcenpakete greifen; ein Build-Derivat ist unnötig, weil die Geometrie identisch ist).
   **Suchfeld entfällt**: die längste Liste (Stein-/Ziegelfamilien mit Octets) sind ~2–3 Dutzend Schnitte, also ein paar
   Scrollzeilen – wie bei der Steinsäge, die auch keine Suche hat. Hinweistexte und Pfeil entfallen ebenso (Vanilla hat keine).
   **UI-Stil modern/alt:** der Bildschirm zeichnet immer den Vanilla-Look („alt“). Der moderne Steinsägen-Stil gehört
   simpleinterfaces (Mixins nur auf Vanilla-Klassen); ihn auf die Formsäge zu übertragen hieße Simple Blocks an simpleinterfaces
   zu koppeln oder dort einen neuen Haken zu bauen → bewusst Folgepunkt (Queue), nicht hier.
   Test `saw_menu_layout`: Slot-Positionen = Vanilla-`StonecutterMenu`; `saw_ui_contrast` prüft nur noch Titel 404040 auf C6C6C6.
4. **Block-Textur.** Vanilla-Steinsäge als Basis, nur die Stein-Base wird Metall: **Eisen/Stahl** (kühles, helles Grau mit
   Blauhauch; die Steinsäge hat Stein + Eisenbarren, die Formsäge Stein + Eisen + Kupfer – Eisen trägt die Base, die zwei Kupferbarren
   zeigen sich an den Eckkappen: Messing → Kupfer). Holzrahmen bleibt Vanilla. Poliert: Steinrauschen zu weichen Stufen geglättet,
   **Glanzkanten** (helle Oberkante/linke Kante, dunkle Unterkante) und hellere Highlights. Unterseite ebenfalls Metall
   (`shapecutter_bottom`, auch Partikel). **Sägeblatt exakt Vanilla**: Modell referenziert `minecraft:block/stonecutter_saw`
   (Animation über Vanillas `.mcmeta`), eigenes `shaping_saw_saw.png(.mcmeta)` entfällt.
   Umsetzung: `tools/textures/shapecutter_2026_10_21.py` (ersetzt `shaping_saw_2026_10_17.py`) in
   `vanilla_derived.MODULE_OUTPUTS['simpleblocks']`, `.gitignore` angepasst. Eigene Anteile (Glanzkanten/Highlights) als
   eigene Pixel in `tools/textures/shapecutter/gloss_top.png` + `gloss_side.png` mit `.layers.json` (layers.py, Ebenen
   „Glanzkanten“, „Schattenkante“, „Highlights“); der Generator legt sie zur Build-Zeit über das Metall.
   Variante B (Kupfer-Base) nur in der Vorschau zum Vergleich.
5. **Texte.** Lang EN/DE, JEI-Info, Discover-Tipp, Wiki `manual.json` (Feature-ID `shapecutter`), Guides (`module_guides.py`),
   Simple-Octets-Texte, Simple-Money-Preis-Schlüssel, `docs/MODS-UND-FEATURES.md`; Kommentare „Shaping Saw“ → „Shapecutter“.

## Dateien
- `modules/simpleblocks/shared/java/com/simpleblocks/saw/*` (Umbenennung, Menü/Bildschirm), `SimpleBlocks.java` (IdMoves),
  `discover/BlocksDiscover.java`, `compat/jei/*`, Loader-Clients (Fabric/NeoForge/Forge), Fabric-`ModuleGameTest` (neue Tests),
  `clienttest/ModuleClientSmoke.java` (nur Namen, kein Lauf).
- `modules/simpleblocks/tools/generate_data.py`, `check_data.py`, `wiki/manual.json`, erzeugte Daten (Blockstate/Modell/Item/
  Loot/Rezept/Fortschritt/Tag/Lang).
- `tools/textures/shapecutter_2026_10_21.py`, `tools/textures/shapecutter/*`, `tools/textures/vanilla_derived.py`, `.gitignore`.
- `tools/discover/discover.py`, `tools/guides/module_guides.py`, `modules/simpleoctets/...` Texte, `modules/simplemoney/.../prices.json`.

## Risiken
- Alte Welten: Migration nur, wenn die alte ID nicht mehr registriert ist (gleicher Commit) – erfüllt.
- Forge: Alias nur über explizite Moves – genutzt.
- Klick-Indizes: Liste bleibt die Serverliste (Payload), unverändert.

## Prüfung
- sb-test: Compile 26.3 Fabric/NeoForge/Forge (+ `:compileJava`); GameTests `simpleblocks:saw*` auf fabric-263, neoforge-263
  und Standalone (module-simpleblocks-standalone-fabric-263); data_integrity; `generate_data.py` + `check_data.py`;
  module_guides, wiki `--all`, discover; `./gradlew check -q`. Keine Client-Läufe (Bildschirm nur per Vorschau-Mock).
- Vorschau-HTML (lokal, nicht im Repo): Block alt/neu, Variante B, UI-Mock mit Vanilla-Hintergrund.

## Nachtrag Besitzer 21.10. abends
„a bitte und sägeblatt etwas dunkler in einer variante und in der anderen heller bzw polierter“: Sockel A (Eisen) bleibt,
Variante B (Kupfer) entfällt. Das Sägeblatt wird zur Build-Zeit aus `minecraft:block/stonecutter_saw` abgeleitet
(`shapecutter_saw.png`, alle Animationsbilder, eigene `.mcmeta` mit Vanillas Werten): S1 etwas dunkler, S2 heller/poliert
(hellere Glanzlichter, dunklere Zähne = mehr Kontrast). Schalter `BLADE` im Generator (`--blade s1|s2` für die Vorschau),
eingebaut S2. Die Besitzer-Wahl kommt über das Code-Feld der Vorschau („SC21 S1“/„SC21 S2“).
