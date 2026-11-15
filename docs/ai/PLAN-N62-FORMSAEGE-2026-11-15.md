# PLAN N62 Teil 2 – Formsäge: Vanilla-UI, Takt-Formsäge (Redstone) und Auto-Formsäge (15.11.2026, Branch `b-saw62`)

Besitzer (wörtlich): „Formsäge: UI wie Vanilla-Steinsäge OHNE Kategorien (Feldbreite Vanilla). Neue Blöcke: (a) Redstone-Formsäge wie
Crafter (pro Signal ein Schnitt, Ziel-Form wählbar) – 2–3 Namensvorschläge, (b) Auto-Formsäge ohne Signal, teurer (Rezept analog
Crafter→Autocrafter). Rezepte/Wiki/Tests; Texturen als Vorschau in Blöcken."

Rahmen dieser Sitzung: sb-test ist aus → **nichts kompiliert, kein Gradle, keine GameTests gelaufen**. Nur statische Prüfungen
(Python-Generatoren/Checker, JSON, sorgfältiges Lesen). Die Befehle für den späteren Lauf stehen unten.

## 1. Ist-Zustand (erhoben)

- `modules/simpleblocks/shared/java/com/simpleblocks/saw/`: `Sawing` (Block, Item, Menü-Typ, Rezepttyp `simpleblocks:sawing`,
  Schnittliste = alle `stonecutting` + `sawing`, per Payload an den Client), `ShapecutterBlock` (erbt `StonecutterBlock`, `METAL`),
  `ShapecutterMenu` (Vanilla-Slots 20,33 / 143,33 / Inventar y 84), `client/ShapecutterScreen` (4 × 3 Kacheln à 16 × 18, Raster x 52 y 14,
  Leiste x 119 y 15, Vanilla-Sprites oder `UiBoxes`-Kasten), `SawTests` (14 Tests), `SawUpgrade`, `SawPalette`.
- **UI-Teil ist bereits erfüllt:** Der Bildschirm hat keine Kategorien/Filter-Tabs und kein breites Raster (geprüft auf `hub/master`,
  `b-bundle197`; auch `claude-formsaege` hat keinen anderen Stand). Feldbreite/-höhe, Raster, Leiste, Slots = Vanilla-Konstanten,
  der Test `saw_menu_layout` vergleicht die Slots mit `StonecutterMenu`. → Keine Änderung nötig außer der Verallgemeinerung für die
  neuen Blöcke (s. u.). Containerrand-Regel (N62 Teil 1, SimpleLib/simpleinterfaces, Branch `b-n62`): hier **nicht** nachgebaut; der
  Bildschirm benutzt weiter `UiBoxes.box` bzw. `StationScreens.draw` (Mixin `ShapecutterStationMixin`), die Randregel greift dort
  automatisch, sobald `b-n62` gelandet ist – auch für die neuen Blöcke, weil sie dieselbe Screen-Klasse nutzen.
- Vorbild Auto-Crafter: `modules/simplemachines/.../crafter/*` (Block mit `TRIGGERED`/`CRAFTING`, BlockEntity mit Takt
  `CRAFT_INTERVAL_TICKS = 4` als Konstante, Ausgabe nur in Trichter darunter, nur wenn das Ergebnis ganz passt, Signal stoppt ihn,
  nichts per Automatik herausziehbar). Rezept: formlos `minecraft:crafter + minecraft:hopper + minecraft:comparator`.
- Daten von Simple Blocks kommen ausschließlich aus `modules/simpleblocks/tools/generate_data.py` (`--check` in `check_data.py`).
  Wiki-Prosa: `modules/simpleblocks/wiki/manual.json`; Handbuch-Seiten: `tools/guides/module_guides.py` (aus manual.json);
  Entdeckbarkeit `BlocksDiscover.DOCUMENTED`: `tools/discover/discover.py` (aus manual.json). GameTest-Katalog: `SawTests.register`
  + Fabric-Adapter `ModuleGameTest` (Prüfer `tools/check_gametest_adapters.py`), NeoForge/Forge registrieren den Katalog automatisch.
- Block-Entities: Fabric baut den Typ in `BlocksFabric`, NeoForge/Forge im `RegisterEvent` (`BLOCK_ENTITY_TYPE`; NeoForge-Konstruktor
  mit 3. Argument `false`, Forge mit 2 Argumenten – wie `OctetsNeoForge`/`OctetsForge`).

## 2. Entscheidungen

1. **Modul:** Simple Blocks (`saw`-Paket), nicht Simple Machines. Grund: die Blöcke brauchen die Schnittliste/den Rezepttyp der
   Formsäge (Modul-Kopplung nur über öffentliche APIs; Simple Machines müsste sonst Simple Blocks fest voraussetzen). Rückfrage an den
   Besitzer im Bericht (Umzug später per IdMoves möglich).
2. **IDs (namensneutral, bleiben bei Umbenennung):** `simpleblocks:redstone_shapecutter`, `simpleblocks:auto_shapecutter`; ein
   gemeinsamer Block-Entity-Typ und Menü-Typ `simpleblocks:shapecutter_machine`.
3. **Namen (vorläufig, Vorschau N62-N):** Redstone-Variante „Takt-Formsäge“ / „Pulse Shapecutter“ (Empfehlung), Alternativen
   „Redstone-Formsäge“ / „Redstone Shapecutter“, „Schalt-Formsäge“ / „Trigger Shapecutter“. Auto: „Auto-Formsäge“ / „Auto Shapecutter“
   (wie „Auto-Crafter“).
4. **Bauform:** Vollblock wie der Crafter, horizontale Ausrichtung `FACING` (Vorderseite = Auswurf, zeigt beim Setzen vom Spieler weg wie
   Werfer/Crafter), `TRIGGERED`. Ein Eingabe-Slot (Stapel bis 64), gewählter Schnitt als Rezept-ID gespeichert (`Selected`), nicht als
   Index – so bleibt die Wahl erhalten, wenn der Slot leerläuft und ein Trichter dasselbe Material nachfüllt.
5. **Takt-Formsäge (Redstone):** wie der Vanilla-Crafter: steigende Flanke → nach 4 Ticks (Crafter-Verzögerung) genau ein Schnitt. Das
   Ergebnis geht in den Container vor der Vorderseite (Trichter, Truhe …); passt es nicht oder steht dort keiner, wird der Rest vor die
   Vorderseite geworfen (Crafter-Auswurf). Kein Signal → kein Schnitt; Dauersignal → nur ein Schnitt.
6. **Auto-Formsäge:** schneidet ohne Signal alle `AUTO_INTERVAL_TICKS = 8` Ticks (Trichtertempo; Konstante wie beim Auto-Crafter,
   kein Config-Schalter – Abweichung von „konfigurierbar falls Muster“: der Auto-Crafter als direktes Vorbild hat auch keinen), nur
   wenn ein Container vor der Vorderseite steht und das Ergebnis ganz hineinpasst (kein Item-Regen in der Welt); ein Redstone-Signal
   hält sie an (wie Auto-Crafter).
7. **Automatisierung:** Eingabe von oben, hinten und den Seiten (alle Flächen außer der Vorderseite); nichts kann per Trichter/Lore
   herausgezogen werden (wie Auto-Crafter: der Eingabestapel bleibt drin). Komparator: 0–15 nach Füllstand des Eingabe-Slots.
8. **Menü/UI:** derselbe Bildschirm wie die Formsäge (Vanilla-Steinsäge-Layout): links der echte Eingabe-Slot des Blocks, Mitte die
   Schnitte, rechts ein **Vorschau-Slot** (nicht nehm-/belegbar) mit dem, was ein Schnitt erzeugt. Dafür wird `ShapecutterScreen`
   generisch über ein kleines Interface `SawMenu` (cuts, selectedIndex, hasInput, registerUpdateListener); Slot-Zahl/-Lage bleiben
   identisch zur Steinsäge. Stil-Mixin und `StationStyles.LOOKALIKES` greifen über den Klassennamen automatisch mit.
9. **Rezepte:** Takt-Formsäge = Crafter-Muster mit der Formsäge statt der Werkbank: `III / ISI / RDR` (I Eisenbarren, S Formsäge,
   R Redstone, D Spender/Dropper – wie Vanilla-Crafter: 5 Eisen, Werkbank, 2 Redstone, Dropper). Auto-Formsäge = formlos
   Takt-Formsäge + Trichter + Komparator (exakt die Zugabe Crafter → Auto-Crafter). Rezept-Fortschritte (Rezeptbuch) per Generator.
10. **Texturen:** eigene Pixel (kein Mojang-Pixel), Generator `tools/textures/proposals/n62/n62_saws_2026_11_15.py`, je Block 2 Varianten
    (A: Eisengehäuse mit Holz-Eckpfosten wie die Formsäge, B: Steingehäuse wie Crafter/Werfer), Flächen top/front/side/bottom.
    Vorschau (8×, flach + 3D-Würfel) nach `/root/sb-tools-b/previews/n62/tex/` + `items.json`. Die empfohlene Variante wird
    **vorläufig** nach `assets/simpleblocks/textures/block/` kopiert (mit `.layers.json`), damit das Spiel lädt – **„Vorschau, wartet auf
    Besitzer“**. Formsäge selbst bleibt unverändert.
11. **Kreativtab:** direkt hinter der Formsäge im Simple-Blocks-Tab; JEI: beide als Station der Formsägen- und Steinsägen-Kategorie.
12. **Preise (Simple Money):** beide in `excluded` (Station, kein Handel – wie die Formsäge).

## 3. Dateien

- Java (shared): `saw/SawMenu.java` (neu), `saw/ShapecutterMachineBlock.java` (neu), `saw/ShapecutterMachineBlockEntity.java` (neu),
  `saw/ShapecutterMachineMenu.java` (neu), `saw/ShapecutterMenu.java` (implements SawMenu), `saw/client/ShapecutterScreen.java`
  (generisch), `saw/client/SawProbe.java` (Typ-Anpassung), `saw/Sawing.java` (Registrierung), `BlocksRegistry.java` (Block/Item/Menü,
  Tab), `SimpleBlocks.java` (Teststation), `compat/jei/BlocksJeiPlugin.java` (Stationen), `saw/SawTests.java` (Tests).
- Loader: `BlocksFabric`, `BlocksFabricClient`, `BlocksNeoForge`, `BlocksNeoClient`, `BlocksForge`, `BlocksForgeClient`
  (BE-Typ + Screen), Fabric-Adapter `ModuleGameTest`, `clienttest/ModuleClientSmoke` (Generics).
- Daten: `tools/generate_data.py` (Modelle, Blockstates, Items, Rezepte + Fortschritte, Loot, Pickaxe-Tag, Lang EN/DE inkl. JEI-Info)
  → generierte Dateien; `tools/check_data.py` (neue Blöcke geprüft); `wiki/manual.json` (Formsäge-Feature erweitert) → `wiki/generate.py`,
  `tools/guides/module_guides.py`, `tools/discover/discover.py`; `simplemoney/.../simpleblocks/prices.json`; `docs/MODS-UND-FEATURES.md`.
- Texturen: `tools/textures/proposals/n62/*` (Generator + PNG + Vorschau-Quellen), vorläufige Kopien in
  `assets/simpleblocks/textures/block/{redstone,auto}_shapecutter_{top,front,side,bottom}.png` (+ `.layers.json`).

## 4. Tests (GameTests in `SawTests`, Adapter in `ModuleGameTest`)

- `saw_machines_registered`: Blöcke, Items, BE-Typ akzeptiert beide, Menü, beide Rezepte vorhanden und ergeben das Richtige
  (Takt: Formsäge + Dropper; Auto: Takt-Formsäge + Trichter + Komparator), beide im Tab hinter der Formsäge.
- `saw_pulse_one_cut_per_pulse`: Wolle ×4, Wahl Wollstufe … ein Puls → genau 1 Schnitt in der Truhe davor; Dauersignal → kein
  zweiter; Signal aus/an → zweiter Schnitt.
- `saw_pulse_no_cut_without_signal`: ohne Signal bleibt alles, auch über viele Ticks.
- `saw_pulse_ejects_without_container`: ohne Container vor der Front liegt das Ergebnis als Item davor.
- `saw_auto_cuts_without_signal`: Auto-Formsäge mit Truhe davor schneidet von selbst; Signal stoppt; ohne Container kein Schnitt.
- `saw_machine_selection_saved`: Wahl (Rezept-ID) + Inhalt überstehen Speichern/Laden (`saveWithFullMetadata` → `loadStatic`).
- `saw_machine_menu`: Menü-Layout = Steinsäge, Klick wählt Schnitt im Block, Vorschau-Slot zeigt Ergebnis, nicht entnehmbar,
  Automatik kann nichts herausziehen.

## 5. Risiken

- Nicht kompiliert: API-Namen aus Vorbildern im Repo übernommen (`HopperBlockEntity.addItem`, `getContainerAt`, `ValueInput/Output`,
  `ContainerData`, `scheduleTick`); `getContainerAt` und `BlockEntity.saveWithFullMetadata` sind die unsichersten Stellen → beim
  ersten sbt-Lauf zuerst `compileJava` lesen.
- Generischer Screen: Raw-Type-Warnungen in Loader-Registrierungen vermieden durch `ShapecutterScreen<ShapecutterMenu>`-Lambdas.
- Rezept-Konflikt: `III/ISI/RDR` mit Formsäge ist einzigartig (Formsäge ist kein Vanilla-Item).

## 6. Verifikation (diese Sitzung, statisch)

`python3 modules/simpleblocks/tools/generate_data.py --check`, `python3 modules/simpleblocks/tools/check_data.py`,
`python3 tools/check_gametest_adapters.py`, `python3 tools/guides/module_guides.py --check`, `python3 wiki/generate.py --all --check`,
`python3 tools/discover/discover.py --check` (falls vorhanden), `python3 tools/check_missing_textures.py`, `python3 tools/textures/layers.py
check <neue>`, `python3 tools/textures/texrate.py <neue>`, JSON-Validierung, simplemoney `check_data.py`.

## 7. Später (sb-test wieder da), einzeln nacheinander

Siehe Abschlussbericht (`/root/sbt b-saw62 …`).

## 8. Umsetzung – Stand und Abweichungen (15.11.)

- Umgesetzt wie geplant (Commits auf `b-saw62`). **Nichts kompiliert, keine GameTests gelaufen** (sb-test aus).
- Abweichung 1: kein Config-Schalter für den Auto-Takt (Konstante `AUTO_INTERVAL_TICKS = 8`, wie der Auto-Crafter `CRAFT_INTERVAL_TICKS`);
  ein Schlüssel `saw.autoIntervalTicks` in `simpleblocks-server.json` bräuchte zusätzlich Config-Screen-Lang + Wiki-Konfigzeile – erst auf Wunsch.
- Abweichung 2: `triggered` ändert das Aussehen nicht (ein Modell je Block); Lampen-Zustand wäre eine zweite Texturrunde.
- Abweichung 3: keine Einsortierung in Vanilla-Tabs (Redstone-Tab) – nur Simple-Blocks-Tab hinter der Formsäge (wie seit PLAN40 die Formsäge selbst auf NeoForge/Forge).
- UI-Teil: keine Code-Änderung am Layout nötig (bereits Vanilla, keine Kategorien); nur Verallgemeinerung (`SawMenu`, generischer Screen).
- Texturen, Runden (A8c, `texrate.py`):
  - R1: 7-Ton-Rampen (~24 Lum.-Schritte) → wirkte wie Kies, hart 21–33 %, 2× rot (B-Front flach 67 %, A-Top hart 33 %).
  - R2: 9-Ton-Rampen (~13 Schritte), Cluster nie gleich wie links/oben → keine roten mehr, große Flächen kamen aus der einfarbigen Öffnung.
  - R3: Öffnung/Schlitz gemustert, Fugen gebrochen, Schattenspalte neben Holzpfosten → alle 16 Flächen orange (Top bleibt „hart“ 21–28 %
    durch Schlitz/Blatt/Akzente – bei Maschinenflächen gewollt, Entscheidung per Blick), größte Fläche ≤ 37 px.
  - Vorschau: `/root/sb-tools-b/previews/n62/tex/` (Blätter je Block/Variante 8×, `n62_cubes.png` 3D) + `items.json` (N62-N, N62-T1, N62-T2).
  - „Alt neben neu“ entfällt: die Blöcke sind neu; die Formsäge-Textur ist aus Vanilla abgeleitet und darf nicht in Vorschauen.

## 9. Offen

- Besitzer: Namen (N62-N), Textur A/B je Block (N62-T1/T2), Modul (Simple Blocks vs. Simple Machines).
- sb-test: Kompilieren + GameTests (Befehle im Abschlussbericht), danach Client-Sicht (Bildschirm, Texturen im Spiel).
- Nach Landung von `b-n62`: Containerrand im Formsägen-Bildschirm prüfen (gilt für alle drei Blöcke).
