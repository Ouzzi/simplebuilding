# Plan: Simple Maps Webkarte Stufe 1 (B, `b-webmap`, 19.10.2026)

Grundlage: `docs/ai/KONZEPT-WEBKARTE-2026-10-19.md`, Empfehlungen des Konzepts gelten (Besitzer: Konzept + Umsetzung).

## Ist-Zustand (erhoben)
- Erforschung lebt **je Karte** in `WayfinderData` (SavedData `simplemaps:wayfinder_<id>` im Overworld-Speicher; ids
  `0..WayfinderIds.next-1`): nur erforschte 128×128-Kacheln, je Block Vanilla-Packed-Map-Farbe (`MapColor` id·4+Helligkeit)
  und Höhenbyte. Es gibt keine weltweite Erforschung – nur die Vereinigung aller Karten.
- Kacheln entstehen in `Reveal.step` (nur `getChunkNow`, nie Chunk-Laden, Vanilla-Schattierung); `TileRequestPayload`/
  `MapsNetwork.handleTiles` schicken sie (`TileCodec`), `LayerService` zeigt Ebenen nur in erforschten Kacheln.
- Client färbt mit `MapColor.getColorFromPackedId` (ARGB) – dieselbe Palette nutzt der Export.
- Simple Maps hat bisher **keinen Befehl und keinen Server-Tick**; Loader-Einstiege `MapsFabric/MapsNeoForge/MapsForge`.
- autoOff-Vorlage `simpletweaks/play/AutoOff` (reine Funktion Liste × `isModLoaded`); kein gemeinsamer Ort in simplelib →
  im Modul nachbauen (keine Abhängigkeit auf Simple Tweaks).
- Claims: kein Import möglich; framework `Protection` (gebündelt) beantwortet „darf ein Fremder hier bauen?“ für jeden
  Schutz-Anbieter (Simple-Tweaks-Claims u. a.) → Schalter `webHideClaims` nutzt das, ohne harte Abhängigkeit.
- Tests: `MapsTests.ALL` (NeoForge/Forge registrieren automatisch), Fabric braucht Methoden in `ModuleGameTest`.

## Entscheidungen (selbst getroffen, begründet)
1. **Eigene kleine Canvas-Ansicht statt Leaflet**: kein Fremdcode/keine Fremdlizenz im CC0-Jar, kein CDN, ~150 Zeilen
   reicht für Ziehen/Zoomen/Dimension/Spieler. (Leaflet wäre BSD-2-Clause, Hinweis in der Doku.)
2. Kachelquelle = **Vereinigung aller Wegfinder-Karten** der Dimension (zeigt nie mehr, als Spieler erforscht haben);
   ist der Chunk gerade geladen, werden die **erforschten Pixel** frisch aus dem Chunk gelesen (`Reveal.sample`), sonst
   bleibt der gespeicherte Kartenstand. Kein Chunk wird geladen.
3. Pyramide `z = 0..4`: z4 = 1 Block/Pixel (Kachel 128 Blöcke), z0 = 16 Blöcke/Pixel; Pfad
   `tiles/<ns>_<pfad>/<z>/<x>/<y>.png` (y = Block-Z-Kachel). Niedrige Stufen aus den Kind-PNGs (Nächster Nachbar).
4. Export auf dem Server-Thread, gedrosselt: `webChunksPerTick` (Standard 32, 1–512) Chunks je Tick; eine
   Pyramiden-Kachel kostet 16. Leere Kacheln werden gelöscht statt geschrieben (z. B. nach Claim-Verbergen).
5. Schalter `webEnabled` Standard **aus**: Export (Befehl wie periodisch) nur, wenn an. `webIntervalMinutes` 0 = aus
   (0–1440). `webExportDir` leer = `<world>/simplemaps-web`, relativ = relativ zum Weltordner.
6. Spieler: Opt-in je Spieler `/simplemaps web show|hide` (SavedData `simplemaps:web_players`); nur online, nicht
   Zuschauer; `markers.json` enthält nur Freigeschaltete (Name, Dimension, x/z), keine UUIDs, keine Wegpunkte (Stufe 3).
7. `webHideClaims` (Standard aus): Chunks, in denen `Protection` einem Fremden das Bauen verbietet, bleiben leer.
8. `webAutoOffWith` Standard `bluemap, dynmap, squaremap, pl3xmap, journeymap`: einer geladen → Web-Export aus, eine
   INFO-Zeile im Log; Karten-Items bleiben. Leere Liste hält den Export an.
9. Rechte: `export`/`status` Stufe GAMEMASTERS (2), `show`/`hide` jeder Spieler.

## Dateien
- neu `modules/simplemaps/shared/java/com/simplemaps/web/WebMap.java` (Tick, autoOff, Opt-in-SavedData, Status)
- neu `.../web/WebExport.java` (Job: Vereinigung, Chunk-Auffrischung, PNG, Pyramide, markers.json, index.html)
- neu `.../web/WebCommands.java` (`/simplemaps web export|status|show|hide`)
- neu `modules/simplemaps/shared/resources/simplemaps/web/index.html` (Ansicht)
- `MapsConfig.java` (web*-Schlüssel), `Reveal.java` (`sample`/`brightness` öffentlich), `SimpleMaps.java` (`modLoaded`)
- Loader: `MapsFabric`, `MapsNeoForge`, `MapsForge` (Befehl, Tick, `modLoaded`), `ModuleGameTest` (Adapter)
- `test/MapsTests.java` (7 Tests), Lang EN/DE, `wiki/manual.json`, `tools/check_data.py`
- Doku `docs/SIMPLEMAPS-WEBKARTE.md` (Auslieferung nginx/sb-web, nur beschrieben), `docs/MODS-UND-FEATURES.md`, Queue.

## Tests (Gametests, alle Loader über `MapsTests.ALL`, Fabric-Adapter)
`web_tile_colors` (Gold/Smaragd → PNG-Pixel = Palette, Rot>Blau, Auffrischung aus geladenem Chunk, Pyramide da),
`web_throttle` (Schritt mit 4 → höchstens 4 Chunks, Config-Grenzen), `web_explored_only` (nur erforschte Kachel als
Datei, unerforschte Pixel transparent), `web_hide_claims` (geschützter Chunk leer nur mit Schalter),
`web_markers_opt_in` (ohne Opt-in kein Spieler, mit Opt-in Name), `web_auto_off` (simulierte Ids), `web_command_permissions`.

## Verifikation
sb-test: `module-simplemaps-fabric-263,module-simplemaps-neoforge-263` + Standalone mit Filter `simplemaps:*`;
Compile `:modules:simplemaps:forge:compileJava`; `check_data.py`; Guides/Wiki/Discover erzeugen; `./gradlew check -q`.
Nicht getestet: Ansicht im Browser (keine Client-/Browser-Läufe erlaubt) → Besitzer-Abnahme.

## Risiken
Viele Karten → alle `WayfinderData` beim Exportstart geladen (wie Vanilla-Kartenspeicher, einmal je Export);
Plattenplatz bei großen Welten; alte Kacheln gelöschter Karten bleiben liegen (Ordner leeren hilft).
