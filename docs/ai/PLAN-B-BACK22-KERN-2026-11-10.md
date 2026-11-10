# Plan B-BACK22-KERN (10.11.2026) – Bau des gemeinsamen Kerns: Stapel-Dimension, Ebenen-API, Tür-Portale

Branch `b-back22-kern`, Basis `hub/b-bundle173-gen` (96c403256, enthält b-back20s; kein neueres `b-bundle17x-gen` mit b-fix173 vorhanden).
Auftrag N22c b) (/root/b-auftrag-20261109.md), Grundlage verbindlich: `KONZEPT-B-BACK22-KERN-2026-11-10.md` (K-R1–K-R9, Stand
hub/b-back22-konzept cf71bb170, hier übernommen), `KONZEPT-B-BACK22-EBENEN-2026-11-10.md`. Besitzer-Entscheide 10.11.: alle Empfehlungen
fk1–fg16 (Raster 112 = 96 offen + 16 Grundgestein, Reihenfolge R9, keine Gegner, Oberwelt-Tür ≤ 1024, Durchsicht an, Stufenplan fg12a:
S0 + K1–K6 + erste Ebenen-Welle, S2 nach Abnahme). Nur MC 26.3 Fabric. Ebenen-Inhalte (Items/Texturen) werden NICHT gebaut.

## 0. Ist-Stand (Retrieval-First)
- Simple Layers: 5 Dimensionen (`backrooms`, `_warehouse`, `_pipes`, `_pools`, `_end`, je 64 hoch) über `BackroomsChunkGenerator`
  (Block für Block `at()`), `BackroomsLevel`-Enum, gemeinsames `Piece`, Generatoren `LobbyGen/WarehouseGen/PipesGen/PoolsGen`, Übergänge
  per Riss/Wartungstür (`BackroomsTravel`, `RiftBlock`, `LevelDoorBlock`), Eingang Haus + zubetonierter Keller + Schacht mit Rissboden
  (`EntranceShaftFeature`, `HouseTemplate`), Jigsaw-Sonderräume (nur Lobby), Versorgungskisten-Feature, Kreide (b-back20s), STAIRS22.
- SimpleLib: Dienst-Muster ohne Import (`HeightHandover`), Payloads `com.simplelib.net`, Fabric-Registrierung in `SimpleLibFabric(Client)`.
  Kein Portal-Code vorhanden. Gametest-Server ohne Datapack-Dimensionen im Standalone-Lauf (`levelOrRegistryOnly`).

## 1. Umsetzung je Stufe (Dateien, Entscheidungen)
**S0 Messung** – Gametest `bands_measure` (Server): erzeugt eine Stapel-Säule (falls die Dimension geladen ist), zählt einwertige/gemischte
Sektionen, misst Bytes je Sektion (`PalettedContainer.getSerializedSize`), Erzeugungszeit je Säule über `BandWriter` in einen Zähl-Writer
(MC-frei, läuft immer) und meldet die Werte ins Log (`[S0]`-Zeilen). Client-Messung (Heap, F3, Vernetzungszeit) bleibt offen → Messplan
Konzept §3.2; nur bei leerem sb-test.
**K1 Dimension + Ebenen-API** – Paket `backrooms`:
- `backrooms/api`: `Band`, `BandLayout`, `BandPiece`, `CorePiece`, `BandWriter`, `DoorRule`, `BandView`, `DoorEnd`.
- `backrooms/stack`: `BandStack` (Raster 112, `worldY`, `bandAt`), `Bands` (`ORDER` = R9, `ALL`), `BackroomsStackGenerator`
  (Codec `simplelayers:backrooms_stack`; Trenn-/Deckel-/Bodensektionen und ganz feste Ebenen-Sektionen als **einwertige Sektionen**,
  sonst `fill()` je Ebene), `BandBiomeSource` (Biom je Höhe), Dimension `simplelayers:backrooms_stack` (min_y −816, Höhe 1136).
- `backrooms/level/<thema>/` je Ebene ein Paket mit `<Thema>Band` (10 Stück: lobby, pools, archive, warehouse, pipes, electro, hotel,
  lightless, suburb, wheatfield). Startinhalt (K-R6/§7): lobby/pools/warehouse/pipes über `LegacyLayout` aus den bisherigen
  Generatoren (y 0..63 → ly 0..63, darüber Hülle; Riss/Wartungstür → Wand). Übrige 6 Ebenen: Stummel-Raster `StubLayout` (Raumraster
  aus Vanilla-Blöcken in Ebenenfarbe, 8er-Zellen, Treppe nicht nötig).
- SimpleLib `com.simplelib.api.TrackingFilters` + Mixin `Entity.broadcastToPlayer` (Abweichung: Konzept nannte
  `ChunkMap.TrackedEntity.updatePlayer`; `broadcastToPlayer` wird genau dort abgefragt, ist öffentlich und mixin-stabil). Simple Layers
  meldet „nur gleiche Ebene“ in der Stapel-Dimension.
- Biome `backrooms_<thema>` (Nebel/Umgebungslicht aus Ebenen-Tabelle Konzept EBENEN §1), keine Spawns, keine Betten.
**K2 Bausteine** (`backrooms/gen`, MC-frei, API-Freeze danach): `StairRule` + `StairRun` (Regel STAIRS22 als Prüfer: Podest ≤ 8,
Sockel, 3 frei, Leiter-Austritt 3), `CellGraph` + `EscapeCheck`-Anbindung, `RegionNoise`, `RoomModule` + `CoreModules` (Gang, Halle,
Türraum, Sackgasse), `GalleryWell`, `Pointers.toward` + Zeiger-Block `pointer` (rotation 0–15, Vanilla-Texturen), `Puzzle` +
`PuzzleCheck` (jede Startstellung lösbar), `BuildRules` (Hülle, Rahmen, Schwebendes), `DoorPlacement`. Datei-Eigentum §6:
Sprach-Namespace je Ebene `assets/simplelayers_<thema>/lang/`, Daten-Skript-Glob `tools/backrooms/gen_<thema>.py` (Stummel-Dateien),
Klang-Ereignisse vorregistriert `backrooms.<thema>.ambient|door`.
**K3 SimpleLib-Portal (Server)** `com.simplelib.api.portal`: `PortalState` (OPEN/DORMANT/BROKEN) + Anzeige `PortalLook`
(active/dormant/broken/unframed), `PortalRule` + Registry (`simplelib:two_way`, `activate_once`, `break_on_transit`), `FrameRule`
(links/rechts je 2 hoch + Sturz, Doppeltür = gemeinsamer Rahmen), `PortalShape`, `PortalTarget`, `PortalDoorBlock` (abstrakt, erbt
`DoorBlock`), `PortalStates` (SavedData je Dimension, nur Abweichungen), `PortalLinks` (SavedData für gebaute/Keller-Türen),
`PortalTransit` (Erkennung „Füße in offener aktiver Tür“, Teleport mit k·90°-Drehung, Abklingzeit 10 Ticks, nur Spieler, Ziel-Chunk
vorher laden), `PortalPreload` (Ticket `simplelib:portal`, Spieler ≤ 8 Bl. an gespeicherter Tür).
Abweichung: **kein Block-Entity** – das Ziel gestapelter Türen ist eine reine Funktion der Position (`DoorNet`, Konzept §5.4 „kein Speicher
nötig“), gebaute/Keller-Türen stehen in `PortalLinks`. Ein Codepfad: `PortalDoorBlock.resolve(level, lower, state)`.
**K4 Client (S1 klein)**: Nebelpuls über `PortalPulsePayload` (Farbe, Dauer) → HUD-Fläche, die in 0,2 s ausblendet (rendererneutral).
Schleier als Modell-Teil der offenen aktiven Tür (Blockstate-Multipart, Vanilla-Glas-Textur als Platzhalter). Kulisse, Übergangsbild
beim Dimensionswechsel (Client-Mixin) und Vorhersage am Client: **offen** (K4b), weil ohne Client-Lauf nicht prüfbar.
**K5 Tür-Netz**: `DoorNet` (Paar n↔n+1 je Chunk nach `Band.doorDensity`, Enden über `BandLayout.doorEnd` – Standard `DoorPlacement`
in Chunk-Grenzen), Tür-Stempel im Generator (Tür, Rahmen, Rückwand, Boden, Wartungskasten als oberer linker Rahmenblock bei
nicht-zweiwegigen Regeln), Kern-Tür `backrooms_door` (Blockzustand `symbol` 0–10 = Ziel, Optik Eisentür als Stummel), Wartungskasten
`maintenance_box` (einmal, aktiviert/repariert seine Tür direkt, fg16 a), Ersatzteil-Haken `Band.repairPart()`.
**K6 Eingang/Migration**: Haus aus b-back20s mit **Keller-Doppeltür** statt Betonpfropfen und Schacht (Link in `PortalLinks`), Lobby-
Eingangs-Doppeltür (Symbol 0 = Haus) in derselben Säule (`Entrances`), EXIT bleibt (LobbyGen-Ausgänge). Alte 5 Dimensionen samt Typen,
Biomen, Jigsaw-Sonderräumen entfallen; Spieler, die in einer entfallenen Dimension ausgeloggt waren, landen per Vanilla in der Oberwelt
an ihren alten Koordinaten (unterirdisch) → `BackroomsRescue` beim Beitreten: Oberwelt + Eintrag in `BackroomsEntries` + Füße in festem
Block oder unter der Oberfläche → auf die Oberfläche am Eintrittsort. Riss/fehlerhaftes Grundgestein (fg11 a): Erzeugung entfällt
(Oberwelt-Feature und Schachtboden weg, Risse in Generatoren → Luft); die Block-Ids bleiben vorerst registriert (Konzept §7 „keine
IdMoves“; Preise Simple Money/Client-Test Rel10/Wiki hängen daran) → Entfernen der Ids als Folgepunkt. `LevelDoorBlock` führt (nur
noch per Befehl gesetzt) eine Ebene tiefer.
**K7** bewegte Plattform: additiv vor der Welle mit E4/E7 → **nicht in diesem Branch** (offen, Konzept §8).

## 2. Tests (Katalog `LayersTests` + Fabric-Adapter)
`bands_layout` (Raster, Grenzen, Biom je y), `bands_separators_bedrock` (Trennsektionen einwertig Grundgestein in echter Säule),
`bands_hull` (kein Grundgestein neben passierbar, alle Ebenen, Stichproben), `bands_gen_budget`, `bands_measure` (S0),
`bands_tracking_filter`, `bands_stairs_rule`, `bands_puzzle_solvable`, `bands_pointer`, `portal_frame`, `portal_double_door`,
`portal_rules` (zweiwegig/aktivieren/bricht+Reparatur), `portal_transit_rotation`, `portal_transit_loaded` (Ziel-Chunk geladen und Spieler
im Ziel-Level verfolgt – Lehre FIX172), `bands_doors_paired`, `bands_doors_framed`, `bands_box_at_every_door`, `cellar_door_roundtrip`,
`migration_rescue`, alte Tests angepasst/entfernt (Dimensionen, Risse, Sonderräume).

## 3. Risiken
Kein lokaler Compiler (sb-agent) → höchstens 5 sbt-Läufe; Mixin-Ziel `Entity.broadcastToPlayer` (öffentlich, stabil); Ersetzen ganzer
Sektionen im ProtoChunk vor Licht-Initialisierung (Vanilla-Status NOISE, sicher); Datapack-Dimensionen fehlen im Standalone-Gametest
→ Welt-Tests prüfen dort nur Registry/reine Funktionen. Speicher am Client ungemessen (S0 Client offen).

## 4. sbt-Läufe (Zielnamen geprüft: `module-simplelayers-fabric-263`, `module-simplelayers-standalone-fabric-263`,
`module-simplelib-fabric-263`, `fabric-263 --filter 'simplebuilding:data_integrity*'`)
1. Compile Kern + simplelib + simplelayers + Module-Tests simplelayers/simplelib (fabric + standalone) + data_integrity + check_data.
2.–5. nur bei Rot.

## 5. Abweichungen (bewusst)
1. Kein Portal-Block-Entity (siehe K3). 2. TrackingFilters über `Entity.broadcastToPlayer`. 3. Startinhalt per `LegacyLayout`-Adapter
statt mechanischem Umbau in eigene Piece-Enums (die Ebenen-Branches ersetzen den Adapter in ihrem Paket; die Altgeneratoren bleiben bis
dahin gemeinsame Kern-Dateien, die keine Ebene ändert). 4. Nebelpuls als HUD-Fläche statt Nebel-Shader. 5. Block-Ids Riss/fehlerhaftes
Grundgestein bleiben vorerst (s. K6). 6. K7 und S2 nicht in diesem Branch.

## 5b. Kern-Hinweise aus BACK22c §8 (Nachricht B-Hauptsitzung, während des Baus)
| Hinweis | Umsetzung im Kern |
|---|---|
| (1) Türdichte auf/ab getrennt, Ebene wählt Ort | `Band.upDoorShare()` (Anteil zweiseitiger Paare; Rest einwegig nach unten, Ankunft auf freiem Platz), `Band.doorDensity()` abwärts; Ort über `BandLayout.doorEnd(…, down)` (Haken, Standard `DoorPlacement`) |
| (2) Wartungskasten-Lage je Ebene | `Band.boxPlacement()` (`FRAME` gebaut; `BEHIND_PANEL`/`IN_ROOM`/`TRAIL` reserviert, fallen auf FRAME zurück) – Rest offen |
| (3) Bauzeichen-Lesbarkeit | reine Darstellung der Ebene, kein Kern-Code (offen, Ebene) |
| (4) Rätsel schaltet Tür/Abkürzung frei | `Puzzle` (rein) + `PortalTransit.mend(level, shape, null)` / `PortalStates.set` als Haken; eigene Gate-Blöcke bleiben in der Ebene |
| (5) Plattform per Vanilla-Redstone | K7 nicht gebaut; Vorgabe für K7 übernommen (Eingabe = Redstone-Signal, kein Gewichtsblock) |
| (6) Schlüsselbund gestrichen | nichts gebaut |
| (7) Beute mit Namen aus dem Seed | offen (Kern-Nachtrag: Loot-Funktion `set_name`/`custom_data` aus Seed+Position) |
| (8) Vorbauer-Pfeile Anteil | `Band.pointerShare()` + `Pointers.toward` |

## 6. Revisionen nach der Implementierung
Achsen wie im Konzept (MB Machbarkeit · PF Leistung · RR Risiko · MU Modul-Unabhängigkeit · A12 · PAR parallel baubar), 1–5.

**R1 – Übersetzen und alte Tests (sbt 1+2).** Prüfen: sbt 1 COMPILE_RC=1 (26.3 kennt keine `Blocks.GRAY_CONCRETE` u. ä. → `Blocks.CONCRETE.pick(DyeColor)`,
`DYED_TERRACOTTA`, `WOOL`; `PushReaction.BLOCK` fehlt → `IMMOVEABLE`; ein Test `backrooms_grid` war beim Entfernen des Nachbartests mit gelöscht).
sbt 2: COMPILE_RC=0, DATA_RC=0, DI_RC=0 (48/48), Module 150/154 – rot nur zwei Alt-Tests mit alten Ids (`registered`: Generator `backrooms`,
`ambience_sounds`: Biom `backrooms`). Verbessert: Ids auf `backrooms_stack`/`band_stack`/`backrooms_lobby`. Bewertung MB 4 → 4,5.

**R2 – Plan-Abgleich.** Gebaut: S0 (Server), K1, K2 (ohne Zeiger-Block, s. u.), K3, K4-klein (Nebelpuls, Schleier-Modell), K5, K6. Nicht gebaut und
benannt: Zeiger-Block (nur `Pointers.toward`, der Block kommt mit der ersten Ebene, die ihn braucht – E8/E10), Sprach-Namespaces je Ebene und
vorregistrierte Klang-Ereignisse (Klang braucht .ogg-Dateien, `check_data` prüft sie; als additiver Kern-Nachtrag vor der Welle), K7, S2,
Kulisse/Übergangsbild am Client. Bewertung PAR 4 (Java/Daten-Skripte/Tests je Ebene getrennt; Sprache bleibt gemeinsame Datei).

**R3 – Robustheit Tür-Netz.** Prüfen: Tür nach oben und Tür nach unten derselben Ebene (Wunschplätze 5/5 und 10/10) können bei
Ausweichsuche zusammenrücken; die Lobby-Eingangstür kann die Abwärtstür treffen; die Versorgungskiste kann auf dem Ankunftsplatz vor einer Tür
landen. Verbessert: `DoorPlacement.inFootprint/overlaps` – berührende Türkammern lassen die zweite Tür fallen (Paar wird einwegig bzw. kein
Lobby-Eingang, Haus führt dann auf den Rückfall), Kisten nie in einer Türkammer (`DoorNet.inChamber`); Test prüft „Kammern getrennt“.
Bewertung RR 3,5 → 4.

**R4 – Leistung (S0-Zahlen).** Server-Messung im Modul-Lauf: Säule 49 einwertige + 22 gemischte Sektionen, 45 461 Byte Blockdaten
(Netzgröße) je Säule; Generator 1,8 ms je Säule (warm, Modul-Lauf) bzw. 4,9 ms (Standalone-Lauf, kalt) – Budget 10 ms eingehalten. Bei Sichtweite 12
(625 Säulen) ≈ 28 MB Blockdaten statt geschätzter 130 MB, weil Stummel-Ebenen nur eine gemischte Sektion haben; mit ausgebauten Ebenen (3–5
gemischte Sektionen) ≈ 60–90 MB. Entscheid: **M2 Bandfenster nicht nötig** bis zur Client-Messung. Verbessert: nichts nötig; Messung als Test
(`bands_separators_bedrock`, `bands_gen_budget`) bleibt, damit jede Ebene ihr Budget sieht. Bewertung PF 4.

**R5 – Migration und Einträge.** Prüfen: (a) Rettung beim Beitreten hätte jeden Spieler mit altem Eintrag getroffen, der später unter y 64 gräbt;
(b) wer über die Kellertür hinein- und hinausgeht, bekam keinen bzw. behielt einen Eintrag (Ausgangstür führte zum Weltspawn).
Verbessert: Rettung nur beim ersten Beitreten nach dem Update (`back22_migration`, gespeichert); SimpleLib `PortalTransit.listen` (allgemeiner
Haken nach jedem Durchgang) – Simple Layers setzt den Eintrag beim Betreten über eine Tür und löscht ihn beim Verlassen. Tests erweitert.
Bewertung MB 4,5, RR 4.

**R6 – Modul-Unabhängigkeit und A12.** Prüfen: Kern (`common/src/shared`) unberührt (nur der Client-Test Rel10 auf die neue Dimension
umgestellt); SimpleLib-Portal kennt keine Backrooms-Klasse; Mixin `Entity.broadcastToPlayer` liegt in der gemeinsamen SimpleLib-Mixin-Datei (auch
NeoForge kennt die Methode, Regel 11 unberührt – Simple Layers hat keinen NeoForge/Forge-Build); nur Spieler gehen durch Türen (A12: keine
Logistik), keine Gegner (fg8 a). Ergebnis ohne Änderung. Bewertung MU 5, A12 4,5.

**Endstand:** MB 4,5 · PF 4 · RR 4 (S1) · MU 5 · A12 4,5 · PAR 4.

## 7. Andocken für Ebenen-Agenten (API-Freeze 10.11., Branch-Basis `b-back22-kern`)

**Eingefroren** (nur noch additive Default-Methoden, nie Signaturen ändern): `backrooms.api.*` (`Band`, `BandLayout`, `BandPiece`, `CorePiece`,
`BandWriter`, `BandRegistrar`, `BandView`, `DoorEnd`, `DoorRule`, `BoxPlacement`), `backrooms.gen.*` (`DoorPlacement`, `StairRule`/`StairRun`,
`Puzzle`, `Pointers`, `BuildRules`, `LegacyBand`, `StubBand`), `backrooms.door.*` (`DoorNet`, `DoorStamp`, `Entrances`), `backrooms.stack.*`
(`BandStack` Raster 112, `Bands.ORDER` = R9), SimpleLib `com.simplelib.api.portal.*` (`PortalDoorBlock`, `PortalRule`, `PortalStates`,
`PortalLinks`, `PortalTransit` inkl. `listen`/`mend`, `PortalPreload`, `FrameRule`, `PortalShape`, `PortalTarget`), `TrackingFilters`.

**Dateien je Ebene `<id>`** (lobby, pools, archive, warehouse, pipes, electro, hotel, lightless, suburb, wheatfield) – nur diese ändern:
| Was | Ort |
|---|---|
| Java | `modules/simplelayers/shared/java/.../backrooms/level/<id>/**` – `<Id>Band` (ersetzt `LegacyBand`/`StubBand` als Oberklasse durch eigene `Band`-Implementierung), eigenes `BandPiece`-Enum, Layout, Blöcke/Items, Rätsel, Client-Teile |
| Fabric-Gametest-Adapter | `modules/simplelayers/fabric/.../level/<Id>GameTest.java` (schon eingetragen in `fabric.mod.json`; je Test eine Methode, Name camelCase von `<id>_…`) |
| Daten | `modules/simplelayers/tools/backrooms/gen_<id>.py` (`generate(d, a)`; darf `worldgen/biome/backrooms_<id>.json` überschreiben: Nebel Anfang/Ende, Licht, Klang), `data/simplelayers/loot_table/backrooms/<id>/**`, `recipe/<id>/**`, `tags/block/backrooms/<id>_*.json` |
| Assets | `textures/block|item/backrooms/<id>/**` (Texturen von b-tex22d aus `/root/sb-tools-b/back22/tex22d/<id>/`), Modelle/Blockstates über `gen_<id>.py` |
| Sprache | eigener Namespace `assets/simplelayers_<id>/lang/en_us.json` + `de_de.json` (lädt Vanilla aus allen Namespaces; keine Zeilen in der Haupt-Sprachdatei) |
| Wiki | eigene Feature-Einträge erst im Bündel (manual.json ist gemeinsam; Texte im Plan der Ebene vorbereiten) |

**Registrieren:** `Band.register(BandRegistrar r)` überschreiben – `r.block("<id>_name", Factory, props)`, `r.item(...)`, `r.blockItem(block)`;
Ids immer mit Präfix `<id>_`; Items erscheinen im Simple-Layers-Reiter nach den Kern-Items. Ersatzteil: `Band.repairPart()` → Item der Ebene
(Tür-Reparatur/Aktivierung mit Rechtsklick, Wartungskasten bleibt). Beute: `Band.loot()` + `BandLayout.containers(...)` (Kisten nie in Türkammern).
Türen: `BandLayout.doorEnd(seed, cx, cz, down)` wählt den Ort (Türnische mit festem Boden, Kammer bleibt im Chunk – `DoorPlacement.fits`); der Kern
stempelt Tür, Rahmen, Rückwand, Kasten. Parameter: `doorDensity()` (abwärts), `upDoorShare()` (Anteil zweiseitiger Paare), `boxPlacement()`,
`pointerShare()`. Rätsel: `Puzzle` (rein, Test `solvable`), Freischalten über `PortalTransit.mend(level, shape, null)` bzw. `PortalStates.set`.
Hülle: `CorePiece.HULL` = fester Block der Ebene, ly 0 und ly 95 nie begehbar; ganze Hüll-Sektionen über `hullSection` melden (Budget).
Treppen nur nach `StairRule` (≤ 8 Stufen, Sockel, 3 frei, Leiter-Austritt 3).

**Tests je Ebene:** `Band.tests((name, body) -> …)` mit Namen `<id>_*` + Methode im eigenen Adapter. Gezielt laufen lassen:
`python3.12 tools/testrunner/run.py --targets module-simplelayers-fabric-263,module-simplelayers-standalone-fabric-263 --filter 'simplelayers:module_game_test_<id>*'`
und Kern-Filter `--filter 'simplelayers:module_game_test_bands*'` (Hülle, Budget, Türen gelten für alle Ebenen automatisch). Daten: `python3 modules/simplelayers/tools/gen_data.py` + `check_data.py`.

**Nicht in diesem Branch (additive Kern-Nachträge):** K7 bewegte Plattform (Eingabe = Vanilla-Redstone, vor Welle E4/E7), S2 Tiefenfenster,
Kulisse/Übergangsbild beim Dimensionswechsel, Beute-Namen aus dem Seed (§5b-7), Kasten-Lagen außer `FRAME`, vorregistrierte Klang-Ereignisse
je Ebene (die Ebene registriert eigene Klänge mit Präfix `<id>.` in `gen_<id>.py`/`register`, bis der Kern sie bündelt).
