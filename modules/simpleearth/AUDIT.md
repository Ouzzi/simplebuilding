# Audit simpleearth (a-audsb31e2, 12.11.2026)

Basis `hub/b-bundle188` 9dfb3f643, nur MC 26.3 Fabric (Forge/NeoForge nicht Teil des Releases). Aufbau wie `modules/simplemobheads/AUDIT.md`.
Umfang: 48 Java-Dateien / 5,4k Zeilen ohne Tests (Katalog `test/EarthTests` + Fabric-Adapter 0,5k Zeilen, `clienttest/` 0,5k, JUnit `EarthViewTest` 0,4k); davon
11 Dateien / 1,6k Zeilen reiner Client (`client/`, `mixin/client/`). Der Mod hat **keine** Items, Menues, Block-Entities, Befehle ausser `/simpleearth config`
und keine SavedData: der Menue-/Dupe-Teil von C3 und C4 (SavedData) entfaellt. Risikogeleitet: Server-Teil (Netzwerk, Kachel-Dienst, Sampler-Threads, Orbit,
Config) zuerst, Client-Rendern nur ueber die Mixins und die Zustandsklassen. Der Orbit ist rein anzeigend: der Server bewegt den Spieler nie.

## Vollstaendig gelesen
- Einstieg/Netzwerk: `SimpleEarth.java`, `EarthFabric.java`, `EarthFabricClient.java`, `net/TileRequestPayload`, `EarthTilePayload`, `OrbitConfigPayload`, `OrbitStatePayload`.
- Kachel-Dienst: `terrain/TileService.java`, `TileStore.java`, `TerrainSampler.java`, `TileMath.java`, `TileCodec.java`, `EarthColors.java` (Z. 1-140 + per grep der Rest: Cache,
  `load`/`parse`).
- Orbit/Config: `orbit/OrbitTracker.java`, `OrbitView.java`, `config/EarthServerConfig.java`.
- Client-Zustand und Mixins: `client/EarthClient.java`, `client/ClientTiles.java`, `mixin/client/EarthWeatherMixin`, `EarthAmbientMixin`, `EarthCloudMixin`, `EarthCloudStatusMixin`,
  `simpleearth.mixins.json`, `fabric.mod.json`, `ModuleGameTest.java`, aus `EarthTests` die Faelle `tile_requests_validated`, `tile_codec_and_store`, `orbit_lifecycle`, `config_command`,
  `sub_mod_switch`.
- Nicht gelesen: Render-/Ansichtsmathematik (`view/*` 21 Dateien, `client/EarthRenderer`, `FaceTextures`, `EarthScreen`, `MenuProbe`), `config/EarthSettings`/`EarthClientConfig`,
  `guide/EarthGuide`, `ModuleClientSmoke`.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | 2 C2S, beide per `registerGlobalReceiver` mit `c.server().execute(...)` auf dem Server-Thread. `OrbitConfigPayload`: drei VarInts, der Server klemmt (`renderHeight` >= max(`minActivationY`, Bauhoehe+64), <= 30000; `edge` in `minEdge..maxEdge`); keine Drosselung noetig (ein Paket setzt Zahlen, hoechstens ein `OrbitStatePayload` je Tick). `TileRequestPayload`: <= 64 Schluessel je Paket, nur wenn `OrbitTracker.allowed` (Mod an + Dimension), je Spieler Warteschlange <= 512 mit Duplikat-Filter, jeder Schluessel geprueft (`validKey`: Gitterexponent `max(0,minGrid)..14`, Kachelbereich im Weltrand), Token-Budget `tilesPerSecond` (<= 512, 1 s Burst), globale Obergrenze `workerThreads*4` laufende Jobs. S2C: Kachel <= 64 KiB (`MAX_BYTES`, roh 12 KiB deflatet), Orbit-Zustand <= 16 andere Spieler. Negative Listenlaenge: P3-4. |
| C2 Server-Autoritaet | Orbit ist nur Anzeige: `OrbitTracker` setzt keine Position/Geschwindigkeit (Test `orbit_lifecycle`: "no teleport, no hover"); IG31-36 (Grenzuebertritt) liegt rein in `view/*` (Client). Client-Klassen nur in `client/` und `mixin/client/` (9 Dateien mit `net.minecraft.client`-Import, alle dort; `simpleearth.mixins.json`: `mixins`/`server` leer, 4 Mixins unter `client`). |
| C3 Dupes/Verlust | entfaellt (keine Items, Menues, BEs, Teleports). |
| C4 NBT/Komponenten | keine SavedData/Komponenten. Config robust (`ConfigFiles.readOrQuarantine`, jede Zahl geklemmt, fehlende Schluessel = Default, Datei atomar neu geschrieben). `TileCodec.decode` begrenzt (roh 12288 Byte, Eingabe <= 64 KiB). Datei-Pfade der Kachel-Ablage nur aus `int`-Werten + `ResourceKey` (kein Pfad-Traversal). |
| C5 Tick-Kosten | Sampler in 1..(CPUs-1) Daemon-Threads mit `MIN_PRIORITY`, nie im Server-Thread, nie Chunks laden/erzeugen (nur `getBaseHeight`/Dichtefunktion/Biomquelle, dieselben Aufrufe wie Strukturplatzierung). Server-Tick: `TileService.tick` je Spieler mit Warteschlange, `OrbitTracker.tick` je Spieler O(1), `send` alle 20 Ticks (O(Spieler der Dimension)). Kein Fairness-Mechanismus zwischen Spielern beim Sampler (P3, kein Befund). |
| C6 Leaks | `STATES` (Logout + `SERVER_STOPPED`), `QUEUES` (Logout, Offline-Bereinigung im Tick, `stop`), `STORES` (`stop`, `invalidate`), `SURFACE` (`WeakHashMap`), `EarthColors.CACHE` (je Biom, bei neuer Tabelle geleert), Client-LRUs begrenzt (`tileCache`, `MAX_IN_FLIGHT`, `WANTED` <= 1024). Pool beim Stop: **P2-1** (gefixt). |
| C7 Sicherheit/Rechte | `/simpleearth config` ueber SimpleLib `ConfigCommands` (Stufe 2), jeder Wert durch `EarthServerConfig.apply` geklemmt. Vier Client-Mixins ohne `@Overwrite`: jeder bricht in der ersten Zeile ab, solange `OrbitView.active` (static volatile, nur vom Server-Zustand und der Hoehe gesetzt) false ist; `EarthWeatherMixin` prueft zusaetzlich `isClientSide()`. |
| C8 Unabhaengigkeit | `fabric.mod.json`: `depends` simplelib >=0.4.0, `suggests` simplebuilding; `modules.json`: `requires ["simplelib"]`, `optional ["simplebuilding"]`: kein Widerspruch. Standalone-Ziel `runSimpleearthStandaloneGameTest` vorhanden; kein Fremd-Mod-Zugriff ausser dem Framework (`SubMods`). |
| C9 Doku/Pflege | Lang EN=DE: 254 = 254 Schluessel, `modules/simpleearth/tools/check_data.py` gruen; keine TODO/FIXME; keine Eintraege in `tools/dup_pairs.txt`. |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simpleearth-P2-1 | P2 (Funktionsverlust, nur Einzelspieler-JVM) | `TileService#stop` (alt Z. 58-63) rief `pool.shutdownNow()`; nie gestartete Jobs fallen aus der Warteschlange, ihr `finally { RUNNING.decrementAndGet() }` laeuft nie. `tick` vergibt nur bei `RUNNING < workerThreads*4`. Jede im selben JVM geschlossene Welt kann so bis zu `4*workerThreads` Plaetze dauerhaft verlieren; nach wenigen Welten (oder Reload/Neustart des integrierten Servers) wird keine Kachel mehr ausgeliefert, der Orbit bleibt leer. Dedizierte Server starten den JVM neu und merken nichts. | **gefixt**: `TileService#shutdown` gibt `shutdownNow().size()` zurueck, `submit` kapselt die Zaehlung (inkl. `RejectedExecutionException`), `running()` fuer Tests; Gametest `tile_service_stop_releases_slots` (Adapter `tileServiceStopReleasesSlots`) |
| simpleearth-P3-1 | P3 | Schlaegt das Sampeln einer Kachel fehl (`catch RuntimeException`, TileService.java Z. 174 nach dem Fix), bleibt der Schluessel in `Queue.known`: dieselbe Kachel wird in dieser Sitzung nie wieder angenommen (der Client fragt nach 20 s erneut, der Server dedupliziert). | im `catch` `known.remove(k)` (3 Zeilen) |
| simpleearth-P3-2 | P3 | `TileStore#put` schreibt die Datei nicht atomar (`Files.write` direkt), `get` prueft gelesene Bytes nicht: Absturz mitten im Schreiben oder zwei Worker auf demselben Schluessel (zwei Spieler) liefern abgeschnittene Bytes, die im Speicher-LRU landen und an Clients gehen; der Client lehnt sie ab (`TileCodec.decode`) und fragt erneut (Schleife begrenzt durch `tilesPerSecond`). | temp-Datei + `ATOMIC_MOVE`, und/oder `TileCodec.decode` beim Disk-Treffer (kaputte Datei loeschen, neu sampeln) |
| simpleearth-P3-3 | P3 | `exploredOnly`: der Speicher-LRU behaelt "unbekannt"-Kacheln bis zur Verdraengung/zum Neustart (`TileService#store`: bei exploredOnly nur Speicher); neu erkundetes Gelaende erscheint verzoegert. Kein Datenleck (Kacheln enthalten nur Kartenfarben/Hoehen erkundeter Regionen). | Hinweis; ggf. Kacheln mit Unbekannt-Anteil nicht cachen |
| simpleearth-P3-4 | P3 | `TileRequestPayload#read` (Z. 19) und `OrbitStatePayload#read` (Z. 29): `Math.min(MAX, readVarInt())` laesst negative Werte durch, `new ArrayList<>(n)` wirft `IllegalArgumentException`. Folge: nur der sendende Client wird wegen Decode-Fehler getrennt, kein Server-Risiko. | `Math.max(0, ...)` |
| simpleearth-P3-5 | P3 | `EarthServerConfig#apply` Z. 93: der Default fuer `workerThreads` (`max(2, ...)`) wird nicht auf das Maximum `CPUs-1` geklemmt (`integer()` klemmt nur gelesene Werte): auf 1-2-Kern-Hosts laufen 2 Sampler-Threads. | Default durch `Math.min(max, ...)` |
| simpleearth-P3-6 | P3 | `TileStore#prune` laeuft ohne Sperre parallel auf mehreren Workern; `diskBytes = size` ueberschreibt den Zuwachs anderer Worker, das Disk-Limit kann leicht ueberschritten werden (Drift bis zum naechsten Start). Nach `invalidate` koennen laufende Jobs den alten Signatur-Ordner neu anlegen, bis `dropOthers` ihn beim naechsten Start entfernt. | Hinweis |
| simpleearth-BEW39 | Hinweis | "Performance (Sampler-Threads, FPS) nicht gemessen": die Zaehler `TerrainSampler.NANOS/TILES` und der Test `sampling_speed` sind da, eine Last-Messung mit mehreren Spielern fehlt. "27 Darstellungsversionen sind Entwicklungsstaende", Besitzer-Abnahme "Earth-UI fuer Production", Sodium/Iris ungeprueft. | BEW39: Feature offen, nicht Teil des Audits |

## Urteil
**shipbar** (0 P1, 1 P2 gefixt, 6 P3). Der Server-Teil ist eng begrenzt: jede Eingabe wird geprueft, geklemmt oder gedeckelt, der Sampler laeuft nur in Daemon-Threads ohne Chunk-Zugriff, und
der Orbit aendert nie die Spielerposition. Das einzige echte Problem war der Pool-Stop im Einzelspieler (P2-1).

## Testergebnis
Lokal: `javac.sh simpleearth` auf `TileService.java`, `EarthTests.java`, `ModuleGameTest.java` (Fabric 26.3): sauber (einzige Meldung: bekannte Access-Widener-Meldung in der
unveraenderten `simplelib/.../LibMaterials.java`); `tools/check_gametest_adapters.py`: nur die bekannte Kern-Luecke `test_centre_game_test_order_areas_show_everything_in_owner_order`;
`modules/simpleearth/tools/check_data.py` gruen; Lang-Schluessel EN=DE per Skript. sbt-Lauf: siehe `docs/ai/PLAN-A-AUDSB31E-2026-11-12.md`.
Nicht getestet: Gametest-Lauf (kein Gradle), echter Mehrspieler-Betrieb mit Paket-Spam, Sampler-Last, Client/Render (Sodium/Iris), `tools/check_client_mixins.py` (braucht kompilierte Ausgaben).
