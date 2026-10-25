# Plan B-WD25EARTH (25.10.2026) – Simple Earth: Render-Config, Detailstufen, Biom-Farbtabelle, V10–V12

Branch `b-wd25earth` (Basis `b-bundle69`), nur 26.3 Fabric. Besitzer 25.10. (`/root/sb-tools-b/wd25.md`):
WD25-16 (Render-Einstellungen in die Config, Detail „zu undetailiert“, E1–E12 alles konfigurierbar, N7/N8 Farbtabelle bauen),
WD25-17 (V7–V9 behalten, V10–V12 betonen die Würfelform, V6 bleibt Standard).

## Ist-Zustand
- `EarthSettings`: je Version eine Werteliste (SHARED + eigene), Menü = Scroll-Liste der gewählten Version,
  `/simpleclient simpleearth` und `/simpleearth config` über SimpleLib-`ConfigCommands` (JSON, verschachtelt erlaubt).
- Detail: alle Flächen gleich fein (`faceResolution` 256 → 256 Blöcke/Pixel), Auflösung nur nach Augenhöhe gestuft.
  Malen: höchstens 3 ganze Bilder je Frame, unvollständige Bilder malen bei jeder Kachel-Ankunft neu.
- Farben: feste Regeln je Vanilla-Biom (`EarthColors`), Mod-Biome → nächste Kartenfarbe zur Grasfarbe, keine Tabelle.
  Pro Abfrage String-Operationen (kein Cache). Nur Biom → Farbe, keine Oberflächen-Regeln (Schnee, Hänge, Bänder).
- E1/E2/E3/E7/E8/E11 schon Config; E4/E5/E6/E9 (Client)/E10 nicht; E12 = Mod-ID (nicht konfigurierbar).

## Umsetzung
1. **Darstellung (global, Client)** – neue Gruppe `render` in `simpleearth-client.json`, im Menü oben „Darstellung (alle Versionen)“:
   `detail` 1–5 (Standard 3: nahe Flächen 2× feiner als bisher; 2 = bisher), `detailRange` 0–2 Ringe mit voller Stufe
   (Standard 0, jeder Ring weiter halbiert), `renderDistance` 1–2 gezeichnete Ringe (Standard 2 = alles wie bisher),
   `paintBudget` Kilopixel je Frame (Standard 192 ≈ bisherige 3 × 256²), `textureCache` Bilder (64), `tileCache` Kacheln (2048),
   `faceEdge` Kachelrand-Abdunklung (0,28 = bisher), `moonPhases` (E10, an), `showOthers` (E9 Client-Seite, an).
   Malen schrittweise (Zeilen bis Budget, Mitte zuerst), unvollständige Bilder höchstens alle 250 ms neu, Kacheln Mitte zuerst angefragt.
2. **Server** (`simpleearth-server.json` / `/simpleearth config`): `minGrid` feinstes Raster (5 = 32 Blöcke/Pixel, Schutz vor Detail 5),
   `biomeVotes` 1–3 (N10, Standard 2 = 2×2 wie bisher), `surfaceDetail` (an: Oberflächen-Regeln, s. 3),
   E4 `globalGrid` (aus = je Spieler), E5 `requireFlight` (aus = jede Flugart), E6 `reentryAtOrbit` (an; aus = zurück zum Parkpunkt, auch N3).
   `minGrid` geht im `OrbitStatePayload` an den Client. Kachel-Cache auf der Platte bekommt eine Signatur (Tabelle + Regeln) – Wechsel löscht alten Cache.
3. **„Block-Nutzung“** (`TerrainSampler`, ohne Chunks): Schnee nach Vanillas Temperatur in der Höhe (`Biome#coldEnoughToSnow`),
   gefrorenes Wasser in kalten Biomen, Stein auf steilen Hängen (Höhenunterschied der Nachbarproben, in Berg-Biomen schon ab halber Steigung),
   Badlands: roter Sand unten, Terrakotta-Bänder nach Höhe.
4. **N7/N8 Farbtabelle**: Datapack-Datei `data/<ns>/simpleearth/biome_colors.json` (alle Namensräume, spätere überschreiben),
   `biomes` (exakt) + `tags` (Reihenfolge, erste passt). Eigene Tabelle: Simple-End-Biome + `c:`-Konventions-Tags; Fallback Grasfarbe.
   Laden bei Serverstart und `/reload`, Cache je Biom. Doku im Wiki-Text + Javadoc.
5. **V10–V12** (`CubeShapeView`, je eigene Werte): V10 Kippwürfel (scharfer Würfel, Oberseite immer waagrecht, an der Kante kippt er
   wie ein Würfel 90° über die Kante, Kantenlinien), V11 Zauberwürfel (Kippen + n×n Zellen mit Fugen auf dunklem Kern),
   V12 Eckansicht (Kippen; mit der Höhe dreht eine Ecke zum Betrachter, drei Flächen wie das Block-Icon). V6 bleibt Standard.
   Menü: Versionsknöpfe in zwei Reihen.
6. Lang de/en, `check_data.py` (12 Versionen, Toggles), Wiki-Text, Modulbeschreibung, FRAGEN-Dokument (Stand), Queue WD25-16/17.

## Verifikation
JUnit `EarthViewTest` (+ V10–V12: endlich, Oberseite eben, Kippen stetig, Eckansicht), Gametests (Fabric-Adapter `ModuleGameTest`):
`render_config` (Standards, Klemmen, JSON-Rundlauf), `orbit_options` (E4/E5/E6), `biome_table` (Parser, Tag-Regel, Simple-End falls geladen),
`surface_rules` (Schnee in der Höhe, Stein am Hang, Badlands, Eis), bestehende angepasst. Ziele `module-simpleearth-fabric-263`,
`module-simpleearth-standalone-fabric-263`, `check_config_commands.py`, Generatoren, `./gradlew check -q`.
Ein Client-Lauf `module-simpleearth-client-263` (Screenshots V10–V12 + Detailstufen) → `/root/sb-tools-b/previews/wd25earth-preview.html`.

## Risiken
- Detail 5 / großer Ring kostet Server-Abtastzeit → `minGrid`, Token-Budget `tilesPerSecond`, Standard moderat (Detail 3, Ring 0 = weniger Kacheln als bisher).
- Kipp-Animation: Drehung um den Berührpunkt (Kante) – JUnit prüft Stetigkeit.
