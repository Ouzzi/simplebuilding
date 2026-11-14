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

## Config-Schema `layout_<variant>.json` (Ablage `common/src/shared/resources/data/simplebuilding/testcentre/`)
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
