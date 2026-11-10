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

## 6. Revisionen nach der Implementierung
(werden nach dem Bau ergänzt, ≥ 5)
