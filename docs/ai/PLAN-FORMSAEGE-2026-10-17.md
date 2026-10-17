# Plan: Formsäge / Shaping Saw (Simple Blocks, Besitzer 17.10., Prinzip A4b)

Branch `claude-formsaege` (Sitzung A, von `claude-wave1` 088792b8c). Queue-Punkt „Fortgeschrittene Säge“ (`[~] A`).

## Ziel
Ein neuer Block `simpleblocks:shaping_saw` (Arbeitsname „Formsäge / Shaping Saw“) übernimmt die Rolle der Vanilla-Steinsäge:
er schneidet Stein **und** Holz und alles, was Treppen-/Stufen-/Octet-Varianten hat – als **strikte Obermenge** der Steinsäge.

## Ist-Zustand (erhoben)
- Simple Blocks: 37 Baublöcke, Daten aus `modules/simpleblocks/tools/generate_data.py` (eingecheckt, `--check` in `check_data.py`),
  keine Mixins, kein Client-Einstieg, kein Netzwerk, eigenständig (nur `framework`).
- Vanilla 26.3: Steinsäge-Rezepte reisen als `SelectableRecipe.SingleInputSet` zum Client; eigene Rezepttypen nicht.
  Muster im Repo für Server→Client-Sync: `simplelib` `UpgradeTablePayload` (Fabric `SYNC_DATA_PACK_CONTENTS`,
  NeoForge/Forge `OnDatapackSyncEvent`).
- Vanilla-Daten 26.3 ausgewertet: ohne Steinsägen-Rezept sind alle Holz-Treppen/-Stufen/-Zäune (13 Holzarten inkl.
  **Pappel** und Bambus, Bambusmosaik), **Woll-Treppen/-Stufen** (16 Farben) und der Netherziegelzaun.
  Repo: `simplebuilding:dirt/grass/sand/gravel_slab` und Ashwood/Duskwood (`simpleend`) Treppe/Stufe/Zaun.
  Alle übrigen Treppen/Stufen im Repo haben schon ein Steinsägen-Rezept. Octets (`claude-s6-octets`) bringen
  eigene Steinsägen-Rezepte (auch für Holz) mit.

## Entwurf
1. **Rezepttyp `simpleblocks:sawing`** (`SawingRecipe extends SingleItemRecipe`, JSON wie `minecraft:stonecutting`:
   `ingredient`, `result`). Kein Rezeptbuch (leere `display()`, keine Freischalt-Fortschritte nötig).
2. **Rezeptliste der Säge** = alle `minecraft:stonecutting` + alle `simpleblocks:sawing` (Duplikate gleicher
   Eingabe/Ausgabe/Anzahl einmal), sortiert nach Ergebnis-ID. Dadurch Obermenge ohne doppelte JSON-Dateien;
   jedes Modul, das Steinsägen- oder Sawing-Rezepte liefert (z. B. `simpleoctets`), erscheint **ohne Codeänderung**.
   Fremde Module können eigene `simpleblocks:sawing`-Rezepte mit Bedingung „simpleblocks geladen“ mitbringen.
3. **Generator** (`generate_data.py`): Sawing-Rezepte je Familie mit Vanilla-Verhältnissen der Steinsäge
   (1 Block → 1 Treppe / 2 Stufen / 1 Mauer bzw. Zaun):
   - Holz (Eiche, Fichte, Birke, Tropen, Akazie, Schwarzeiche, Mangrove, Kirsche, Blasseiche, Pappel, Karmesin, Wirr):
     Bretter → Treppe 1, Stufe 2, Zaun 1; Stamm/Holz/entrindet → Bretter 4; Stamm → entrindeter Stamm 1, Stamm → Holz 1,
     entrindeter Stamm → entrindetes Holz 1, Holz → entrindetes Holz 1; `#minecraft:planks` → Stöcke 2 (wie Werkbank).
   - Bambus: Bambusblock/entrindet → Bretter 2, Bambusblock → entrindet 1, Bretter → Treppe/Stufe/Zaun/Mosaik 1,
     Mosaik → Mosaiktreppe 1 / -stufe 2.
   - Wolle 16 Farben → Treppe 1 / Stufe 2; Netherziegel → Netherziegelzaun 1.
   - Optional (Ladebedingung): `simpleend` Ashwood/Duskwood (wie Holz), `simplebuilding` Erde/Gras/Sand/Kies → Stufe 2.
   - Keine Zauntore/Türen/Falltüren (brauchen Stöcke bzw. mehr Material; Steinsäge macht so etwas auch nicht).
4. **Block** `ShapingSawBlock extends StonecutterBlock` (Form, Ausrichtung wie Steinsäge), eigenes Menü.
   **Rezept:** geformt ` I ` / `P#P` (Eisenbarren, 2 Bretter, Stein) und Aufwertung formlos
   Steinsäge + Kupferbarren + Brett. Beides frühes Spiel, sichtbar anders als die Steinsäge.
5. **Menü/Bildschirm**: wie Vanilla-Steinsäge (Eingabe, Ergebnisliste, Ergebnis), aber 6 × 4 sichtbare Ergebnisse,
   Scrollen (Rad + Leiste) und ein Suchfeld. Farben aus der vorhandenen INVENTORY-Palette (`UiPalette.INVENTORY` in
   simplelib: Fläche C6C6C6, Schrift 404040, Schatten 555555/373737, Slot 8B8B8B) – als Konstanten übernommen, weil
   Simple Blocks ohne simplelib laufen muss; Kontrast Schrift 404040 auf C6C6C6 = 6,1:1 (Test).
   Kachel-Sprites der Vanilla-Steinsäge (`container/stonecutter/recipe*`, nur referenziert, nichts kopiert).
6. **Sync**: `SawRecipesPayload` (Server→Client, beim Beitreten und nach `/reload`), je Loader registriert; der Client
   nutzt genau die Serverliste, damit die Indizes der Knopf-Klicks passen.
7. **Integration**: JEI-Kategorie „Formsäge“ (nur Sawing-Rezepte; Säge zusätzlich Station der Vanilla-Steinsägen-
   Kategorie), Forge ohne JEI. Jade: nichts anzuzeigen (keine Zustände) → entfällt. Wiki-Eintrag `shaping_saw`
   (Handbuch-Seite kommt daraus), Entdeckungs-Tipp, Simple-Money-Ausschluss (Station, nicht gehandelt),
   Testzentrale-Station, Kreativtab Simple Blocks + Vanilla „Funktionsblöcke“ hinter der Steinsäge, EN/DE.
8. **Textur**: eigene 16×16-Pixelkunst (nichts aus Vanilla abgeleitet), Vanilla-nah: Tisch mit Stein- und Holzfläche,
   breiteres, gezahntes Sägeblatt (animiert, 3 Bilder). 3 Varianten A/B/C, A eingebaut; Generator
   `tools/textures/shaping_saw_2026_10_17.py` (`--check`), Vorschau `/tmp/claude-0/ash/formsaege.html` (nicht veröffentlicht)
   mit Namensvorschlägen.

## Dateien
- `modules/simpleblocks/shared/java/com/simpleblocks/saw/*` (Rezept, Typen/Liste, Payload, Block, Menü, Palette, Tests),
  `saw/client/ShapingSawScreen.java`, `compat/jei/*`.
- `BlocksRegistry`, `SimpleBlocks` (Station), Loader-Einstiege Fabric/NeoForge/Forge (+ Client-Klassen), `fabric.mod.json`,
  `neoforge.mods.toml`, Forge `build.gradle` (JEI-Quellen ausgeschlossen), Fabric/NeoForge `build.gradle` (JEI compileOnly).
- `tools/generate_data.py`, `tools/check_data.py`, `wiki/manual.json`, `tools/discover/discover.py`, `tools/guides/module_guides.py` (Einleitung),
  `modules/simplemoney/.../simpleblocks/prices.json`, `docs/MODS-UND-FEATURES.md`, Client-Smoke.

## Risiken
- Index-Abgleich Client/Server → Client nutzt die gesendete Serverliste; Server prüft Index.
- JEI startet evtl. vor dem Payload → Plugin fügt die Rezepte nach, sobald das Payload da ist.
- Rezepte mit Ladebedingungen müssen auf allen drei Loadern greifen (Formen `fabric:/neoforge:/forge:`).

## Prüfung
- sb-test: compile 26.3 Fabric/NeoForge/Forge, 26.2 `:compileJava`; GameTests `simpleblocks` Fabric + NeoForge
  (Modul + Standalone): Registrierung, alle Holzarten, Obermenge der Steinsäge, Formen ohne Steinsägen-Rezept,
  Menü schneidet wirklich, optionale Integration nur bei geladenem Modul, Kontrast.
- Regenerieren auf sb-test: Datagen-Generator, discover.py, module_guides.py, generate_links.py, `wiki/generate.py --all`;
  `./gradlew -q checkModuleGuides checkModuleData checkWiki checkDiscover checkMultimod`.
- Client-Smoke mit Screenshots (Block in der Welt, Säge-Bildschirm), falls der Harness es erlaubt.

## Stand 17.10. (umgesetzt, Branch `claude-formsaege`)
- Alles aus „Entwurf“ umgesetzt; Kontrast Schrift 404040 auf C6C6C6 = 6,1:1, Hinweistext E0E0E0 auf 373737 > 10:1 (Test `saw_ui_contrast`).
- Geprüft auf sb-test: Compile 26.3 Fabric/NeoForge/Forge + 26.2 `:compileJava` grün; GameTests Simple Blocks 18/18 je Ziel
  (Fabric- und NeoForge-Modul, Fabric- und NeoForge-Standalone); Client-Smoke 6/6 mit Screenshots des Säge-Bildschirms;
  `checkModuleGuides checkModuleData checkWiki checkDiscover checkMultimod` grün; Wiki/Guides/Discover auf sb-test erzeugt.
- Abweichungen: kein Jade-Plugin (Block hat keinen Zustand); keine Rezeptbuch-Einträge/Fortschritte für `sawing`
  (die Säge hat kein Rezeptbuch); Forge ohne JEI und ohne Laufzeittest (nur Compile).
- Offen: Besitzer wählt Name und Textur (Vorschau-Seite), Merge nach `claude-wave1`; Holz-Octets liegen in `claude-s6-octets`
  noch als Steinsägen-Rezepte vor (erscheinen damit auch in der Säge) – ggf. später auf `simpleblocks:sawing` umstellen.

## Nachtrag Besitzer 17.10. (umgesetzt)
1. Aussehen: die Vanilla-Steinsäge, neu texturiert – gleiches Modell; Kupfer an Eckkappen, Schlitzenden, Rahmenkante und
   Querleiste, das animierte Sägeblatt mit hellem Kupfer. Eine Textur statt A/B/C, beim Build aus Vanilla abgeleitet
   (`tools/textures/shaping_saw_2026_10_17.py` in `vanilla_derived.MODULE_OUTPUTS['simpleblocks']`, `.gitignore`);
   Unterseite = Vanilla-Textur direkt.
2. Rezept: Steinsägen-Muster mit Kupfer – `CIC` / `###` (Kupfer-, Eisen-, Kupferbarren über 3 Stein). Das formlose
   Aufwertungsrezept entfällt (ersetzt durch 3.).
3. Aufwertung in der Welt (`SawUpgrade`): platzierte Steinsäge mit Axt (ohne SimpleBuilding) bzw. Hammer
   `#simplelib:hammers` (mit SimpleBuilding) benutzen, 4 Eisennuggets aus dem Inventar werden verbraucht (Entscheidung:
   auch im Axt-Weg 4 Nuggets), Ausrichtung bleibt, Werkzeug 1 Haltbarkeit. Abweichung: SimpleLibs `in_world_upgrades`
   nicht genutzt – braucht SimpleLib (Simple Blocks läuft ohne) und nimmt das Material aus der Nebenhand mit mehreren
   Schlägen, der Besitzer wollte die Nuggets aus dem Inventar. JEI-Infoseite auf Säge und Steinsäge, Wiki/Handbuch.

## Nachtrag 2 Besitzer 17.10.: Octets und Holz exklusiv in der Formsäge (Folgearbeit)
- Auf diesem Branch: alle Holz-Schnitte sind `simpleblocks:sawing` (keine neuen `minecraft:stonecutting`-Rezepte); die Säge
  zeigt weiter alle Vanilla-Steinsägen-Rezepte. Regel in `docs/ai/PRINZIPIEN.md` A4b.
- Folgearbeit nach dem Merge von `claude-s6-octets` (hier bewusst nicht angefasst): die Octet-Rezepte von `simpleoctets`
  (heute `minecraft:stonecutting`, auch für Holz) werden `simpleblocks:sawing` mit Ladebedingung „simpleblocks geladen“
  plus ein `minecraft:stonecutting`-Ersatz mit „simpleblocks nicht geladen“ (Simple Octets bleibt allein spielbar).
  Gleiches gilt für die Octet-Steinsägen-Rezepte, die heute noch in SimpleBuilding liegen (in der Säge sichtbar als
  „8×“ bei Brettern/Steinziegeln). Prüfen: GameTest „kein Octet/Holz-Rezept in der Steinsäge, wenn Simple Blocks geladen“.

## Prüfung Nachtrag (17.10. spät, nach Merge `claude-wave1` 05a9b6e8c)
- sb-test: Compile 26.3 Fabric/NeoForge/Forge + 26.2 `:compileJava` grün; GameTests Simple Blocks 21/21 je Ziel
  (Fabric/NeoForge Modul mit SimpleBuilding, Fabric/NeoForge Standalone ohne) = 84/84, darin `saw_upgrade_axe`,
  `saw_upgrade_hammer` (echter Hammer mit SimpleBuilding, Ersatzwerkzeug standalone), `saw_upgrade_mode`; Client-Smoke 6/6.
- Regeneriert (Datagen, discover, module_guides, generate_links, `wiki/generate.py --all`); `checkModuleGuides checkModuleData
  checkWiki checkDiscover checkMultimod` exit 0. Vorschau `/tmp/claude-0/ash/formsaege.html` (nicht veröffentlicht).
- Offen: Besitzer-Abnahme Textur/Upgrade, Merge nach `claude-wave1`, Octet-Folgearbeit (siehe Nachtrag 2), Forge-Laufzeit.
