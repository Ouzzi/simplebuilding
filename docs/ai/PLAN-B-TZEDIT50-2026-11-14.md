# PLAN B-TZEDIT50 (N50) Testzentrale-Editor - Phase 1 Prototyp + Phase 2 Umfang

Basis hub/b-bundle194. Phase 1 (dieser Commit): Plan, Export-Skript, Prototyp. Phase 2 erst nach Besitzer-Antwort TZEDIT50.

## Ist-Stand
- Layout entsteht rein algorithmisch in `common/src/shared/java/com/simplebuilding/dev/testcentre/OrderAreas.java` (2424 Zeilen):
  `Slot`-Enum (Muster `{p}{m}_button`...), `CORE`/`WOOD_EXTRA`/`METAL_EXTRA`, `Kind` (METAL/WOOD/AGING/REST/STONE),
  `columns(Kind)`, `rawRows`/`familyRows` (Reihen = Familien), `Variant` (LINES, ...), Sektionen `order_families/order_compact/order_items`.
  `TestCentreLayout.java` platziert die Bereiche; Befehle `/sbtestcentre build lines here` (TestCentreCommand).
- Ids gibt es erst zur Laufzeit (Item-Registry) -> ein Export ohne Spiel ist nur ein Auszug (Prototyp: Slot-Muster x vorhandene PNG-Dateien).
- Muster Textur-Editor: `tools/zentrale-web/devqueue.py` (`POST /api/textures`, Z.270/317, `read_json(MAX_BODY)`),
  `tools/textures/editor_save.py` (`load_whitelist`, `check_save`, `apply_save` -> Branch per `git`-Plumbing, `pickup`/`pull` auf dem Hub),
  `tools/zentrale-web/setup_texture_save.sh` (Checkout/nginx `client_max_body_size`/Service-Neustart), Tests `tools/zentrale-web/tests/test_devqueue.py`.
- Texturen: `wiki/assets/textures/...` (Wiki) und `modules/*/.../assets/<ns>/textures/block|item`.

## Config-Schema `layout_<variant>.json` (Ablage `src/main/resources/data/simplebuilding/testcentre/`)
```
{ "schema":1, "variant":"lines",
  "areas":[ { "id":"metals", "name":"Metalle",
     "bands":[ { "id":"copper", "columns":["button","plate",...],          // Spalten = Slot-Namen (Slot-Typ)
         "rows":[ { "key":"exposed_copper","label":"...","cells":["id"|null|"#grey|#light|#red|#frame", ...] } ] } ] } ],
  "excluded":["mod:id",...] }   // bewusst nicht platzierte Ids (gelten nicht als "fehlend")
```
Ids ohne Namespace = minecraft. Zelle `null` = leer. Gleichlanger `cells`-Array je Band (Schema-Pruefung). Ids, die in keiner Zelle und nicht in `excluded` stehen,
baut OrderAreas weiter algorithmisch in den Bereich "Neu" (markiert, Warnung in der Pruefung) - nichts geht verloren.

## Verbesserungen am Konzept
1. `excluded`-Liste (Ids, die absichtlich fehlen) damit "fehlend" eindeutig ist; sonst bleibt die Warnung ewig.
2. Zellen tragen nur Ids, Slot-Typ steht in der Spalte; Drag in eine Spalte mit anderem Slot-Typ warnt (Typ-Mismatch) statt zu sperren.
3. Layout-Version + Hash des Registry-Standes im Export; Seite zeigt "veraltet", wenn neue Ids seit dem Export existieren.
4. Speichern als Commit mit Basis-Hash: Konflikt (zwei Editoren) -> 409, Seite laedt neu statt zu ueberschreiben.
5. Override-Datei im Welt-Config-Ordner hat Vorrang vor der Jar-Config; `layout reload` meldet Quelle (jar/override) und Schema-Fehler pro Zelle.

## Phase 2 (nach OK)
1. Erstexport: Gametest/Befehl `/testcenter layout export` schreibt layout_<variant>.json aus dem heutigen OrderAreas-Algorithmus; Gametest prueft Config == Welt (jede Id einmal oder excluded).
2. OrderAreas liest Config (Fallback Algorithmus fuer unplatzierte Ids -> "Neu"); `/testcenter layout reload` (+ Override); Unit-Tests Schema/Parser.
3. `tools/tzedit/tzedit_save.py` (Whitelist-Pfad, Schema-Check, Limit 512 KB, Commit auf Hub-Branch `tz-layout`, Autor/Zeit, Basis-Hash), `devqueue.py` `POST /api/testcentre` + `GET /api/testcentre/history` (letzte Speicherungen + Diff), Tests in test_devqueue.py, `setup_testcentre_save.sh` (Muster setup_texture_save.sh).
4. Dev-Seite `wiki/dev/testcentre.html` (nur Dev-Login) aus dem Prototyp: alle Bereiche, Mod-Filter, Verlauf/Diff, "Layout geaendert -> ins naechste Buendel".
5. Verifikation: Playwright-Klicktest auf der Dev-Instanz (Drag, Undo, Speichern), Gate durch A, Deploy sb-web nur nach Besitzer-OK.
6. Optional: Import aus Blaupause (eigener Schritt).

## Risiken
Ids nur zur Laufzeit bekannt (Export braucht Gametest); Merge-Konflikte der Config bei parallelen Editoren (Basis-Hash); sb-web-Aenderung braucht Besitzer-OK; Config-Groesse (Hunderte Ids je Reihe: JSON < 512 KB).

## Verifikation Phase 1
Export-Skript laeuft ohne Spiel (53 Spalten, 8 Reihen, 86 Zellen, 258 Texturen); Prototyp: `node --check` + DOM-Stub-Smoketest (kein Browser/Playwright im CT). Nicht getestet: echter Browser-Klick-/Drag-Test.

## Besitzer-Abnahme 14.11. ~21:50 (TZEDIT50 = Einwand „ersteinmal deployen da sowieso nur in dev“)
Entscheidungen (Empfehlung B, offene Fragen): 1. Layout **A** (Raster + Seitenleiste mit Live-Prüfung) Standard, B bleibt umschaltbar.
2. Bedienung wie vorgeschlagen (Klick/Strg-Klick, Ziehen Zelle/Reihe/Spalte, Entf). 3. Endpunkt: Besitzer-Freigabe für genau diesen Dev-Endpunkt.

### Schritt 1.5 Dev-Deploy (dieser Commit)
- `wiki/tz-editor.html` aus `preview_template.html` (Daten weiter der statische Metall-Auszug), Dev-Sperre wie isDevSite + noindex,
  Speichern = `POST /api/testcentre` (+ JSON-Download), Verlauf aus `GET /api/testcentre`.
- `tools/tzedit/tzedit_save.py` (check_save/apply_save/pull, Muster editor_save: Web-Host speichert JSON, Hub-Pull committet Branch tz-layout-*),
  devqueue.py GET/POST `/api/testcentre`, `setup_testcentre_save.sh`, README-Abschnitt, Tests `tools/tzedit/tests/test_tzedit_save.py`.
- Weg auf sb-web: Stand muss in claude-wave1 sein (/srv/wiki-dev folgt wave1 minütlich, A bündelt), dann einmal setup-Skript (Service-Neustart).
- Verifikation: Unit 4/4; lokaler Harness (devqueue in-process + Playwright, KEINE Abnahme): Sperre öffentlich, Tausch, Rückgängig, Platzhalter,
  Speichern + Verlauf, Ansicht B, keine JS-Fehler. Abnahme danach auf der Dev-Instanz.
- Bekannt: test_devqueue.py test_texture_save_flow flakt (BrokenPipe beim 413-Test, auch ohne diese Änderung ~2/12).

### Phase 2 bleibt wie oben (Erstexport aus dem Spiel, OrderAreas liest Config, alle Bereiche, Seite lädt Config statt Inline-Daten).

## Phase 2 – Umsetzung (Besitzer „die Testzentrale per Config“, 14.11. abends)
Priorität nach A 22:20 („warum nicht der ganze Bereich?“): erst (1) Export aller Bereiche + (2) Editor lädt echte Config,
pushen, melden; danach (3) OrderAreas liest Config + „Neu“ + Reload in eigenem Commit.

### Schema final (tzedit_save.check_save, schema 1, eine Datei je Bereich)
`layout_<variant>_<area>.json` = `{schema, variant, area, columns, rows:[{key,label,cells,tops?}], excluded}`
- Bereiche **lines**: `metals | wood | aging | rest | stone` (je Reihen-Art eine Datei; gleiche Spaltenstruktur je Art wie
  `share()`), Reihenfolge der Reihen = Welt (stone: erst Seite B, dann die auf Seite A gewanderten Stein-Reihen).
  Spalten = feste Slot-Spalten (Slot-Name klein), gemeinsame Zusatzspalten (`otherKeys`, Muster `{m}`→`m`, z. B. `hollow_m_log`),
  dann Sonderbereich `special_1..n` (je Reihe verschieden viele Einzelstücke → `others` als zusätzliche Spalten, kein eigenes Feld).
- Zellen: Item-Id (immer mit Namensraum); `#red` = roter Platzhalter (sollte existieren), `#light` = hellgrauer Platzhalter
  (gibt es nicht), `null` = kein Feld (hinter der Seitentiefe der Reihe); `#grey`/`#frame` bleiben für den Editor erlaubt.
- `tops` (neu, optional): `{spalte: id}` = Achtel, das auf dem Block dieser Zelle steht (Speicher-/Rohblock-/Stamm-Achtel).
- `label` = Text des Steh-Schilds der Reihe (aus dem Spiel). `excluded` = [] im Erstexport.
- **blocks**: `compact` (je Mod ein dichtes Quadrat; Reihe = eine z-Zeile `<ns>-<n>`, Spalten `c0..`, c0 der ersten Zeile = Mod-Schild → null).
- **lines|blocks**: `items` (Items je Mod der Variante, gleiche Rasterform). blocks/items sind ein Abbild (noch nicht config-gesteuert).

### Dateien
- `TestCentreDump` (SB_TC_DUMP): zusätzlich `#label`-Zeilen in `lines-rows.tsv`, `compact-cells.tsv`, `items-<variant>-cells.tsv`.
- `tools/tzedit/export_layout.py <dump>`: Tabellen → Configs (check_save + layout_text) in `src/main/resources/data/simplebuilding/testcentre/`.
- `tools/tzedit/build_editor.py [--check]`: Configs → `wiki/data/testcentre/{index.json,layout_*.json}` + `wiki/tz-editor.html`
  (Index: Bereiche + Id→Textur aus den Wiki-Modul-Texturen; Vanilla zur Laufzeit nur aus den Dev-Kopien `assets/vanilla/`,
  `assets/textures/minecraft/`, sonst Name). `export_area.py`/`build_preview.py` (Phase-1-Auszug) entfallen.
- Editor (Abweichung nach Besitzer-Vorgabe A 22:25 „vollständig und im selben Stil wie simplewiki“): KEINE eigene HTML-Seite mehr,
  sondern Dev-Seite im Wiki-SPA `wiki/index.html` (Dev-Gruppe „Testzentrale-Editor“, Route `#/tzeditor`, nur bei `isDevSite()`,
  noindex per Meta beim Anzeigen), Oberfläche `wiki/assets/tzeditor.js` (lazy, Wiki-Tokens/.btn/.box, Hell/Dunkel), `wiki/tz-editor.html`
  = Weiterleitung. Bereichsauswahl (alle 7 Dateien), Ansicht A/B, Ziehen/Undo/Platzhalter/Live-Prüfung, `tops` als kleines Bild,
  Speichern je Bereich (+tops), Warnung bei ungespeichert. Texturen: Wiki-Moduldaten-Icons, `data/textures.js`-Gruppen, Basisblock
  von Formen (Achtel/Stufe/... → Block); Vanilla nur zur Laufzeit aus den Dev-Kopien. `preview_template.html` entfällt.
- Erstexport-Ergebnis (Integration 26.3, alle Module, 15.11. 00:28, Lauf grün 1/1): lines/metals 28 Reihen x 114 Spalten 653 Ids (+30 Achtel)
  10 rot; wood 19 x 54, 509 (+28) 4 rot; aging 1 x 15, 15; stone 95 x 15, 493, 4 rot; rest – (keine REST-Reihen im Stand);
  lines/items 48 x 113, 924; blocks/compact 150 x 17, 1371; blocks/items 46 x 113, 658. Abgleich: 1728 Ids/Achtel der Dump-Tabelle =
  1728 in den lines-Configs, nichts verloren. Texturen: 1613 von 2029 Modul-Ids mit Bild, Vanilla (623) nur auf dem Dev-Host.
- Weg Erstexport: sbt-Lauf `SB_TC_DUMP=1 run.py --targets integration-263 --filter 'sbintegration:*test_centre*'`, Tabellen von sb-test holen.

### Schritt 3 (danach, eigener Commit)
`LayoutConfig` (Jar-Ressource, Override `config/simplebuilding/testcentre/` im Spielordner hat Vorrang), `families(config)`:
Config bestimmt Reihen-Reihenfolge/Zellen je lines-Bereich; unbekannte Id im geladenen Namensraum = Warnung + rot, Id einer
nicht geladenen Mod = hellgrau (Kern-Lauf ohne Module); doppelte Id nur einmal; Algorithmus-Items ohne Zelle/excluded →
eigenes Band „Neu“ (Schild). Ohne Config exakt wie heute. `/testcenter layout export|reload`. Gametests Config==Welt, „Neu“.

### Risiken
Erstexport nur aus der Integration (alle Module) → im Kern-Lauf fehlen Modul-Ids (hellgrau statt rot, s. o.);
Seitenschnitt A/B bleibt algorithmisch; Größe stone-Datei (< 512 KB prüfen).

### Verifikation
Unit `tools/tzedit/tests` (Schema tops, Export aus Beispiel-Tabellen), precheck, Harness-Klicktest (alle Bereiche wählbar,
Speichern), sbt: Integration-Dump, später test_centre*-Gametests.

### Schritt 3 – Ergebnis (Branch b-tzedit50c, 15.11.)
Umgesetzt wie geplant (LayoutConfig, families(config) + Band „Neu“, /testcenter layout reload|export, configProblems auch in der
Integration, 2 Gametests + Adapter). Abweichung: `layout export` schreibt die Dump-Tabellen, die Configs macht export_layout.py.
sbt (b299eb134): kompiliert; fabric-263 `simplebuilding:test_centre*` 24/24 grün (inkl. layout_config_matches_the_built_lines,
layout_config_without_arow_puts_its_items_into_new; Kern: 138 Reihen, 14 rot – Reihen nicht geladener Mods entfallen);
integration-263 `sbintegration:*test_centre*` 1/1 grün (143 Reihen, 18 rot = Export, Config == Welt, nichts in „Neu“). precheck grün.
Nicht getestet: `/testcenter layout reload` mit Override-Datei in einer laufenden Welt (nur Code-Pfad), Bau in der Welt per Befehl.
