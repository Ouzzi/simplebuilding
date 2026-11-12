# Audit simpleresonance (a-audsb31, 12.11.2026)

Basis `hub/claude-wave1` 388f19991 (Buendel 186), nur MC 26.3 Fabric (Forge/NeoForge nicht Teil des Releases). Aufbau wie `modules/simplestorage/AUDIT.md`.
Umfang: 20 Java-Dateien / 2,0k Zeilen ohne Tests (Katalog `test/ResonanceTests` 0,7k Zeilen zusaetzlich). Der Mod ist **kein** Menue-/Block-Mod: er misst Spielerwert und Gefahrenzone
(Dienst fuer andere Mods ueber `ResonanceContracts`), speichert je Spieler einen Datensatz und schickt einmal je Sekunde eine Statistik an den Spieler. Es gibt **keine C2S-Payload**,
**kein Menue** und **keine Weltaenderung** (keine Bloecke, keine Chunks, keine Teleports): C1 (Eingang), die Menue-Punkte von C3 und die Fernwirkungen entfallen. Risikogeleitet,
aber nahezu vollstaendig gelesen.

## Vollstaendig gelesen
- Dienst/Regeln: `ResonanceService.java` (383 Zeilen, Messen, Besitzer-Wahl, Punkte, Tod/Respawn), `ResonanceMath.java` (reine Formeln), `config/ResonanceConfig.java` (alle Werte geklemmt),
  `PlayerRecord.java`, `ResonanceData.java` (Codec, Weltspeicher), `LegacyImport.java` + `mixin/ServerPlayerLegacyMixin.java` (Uebernahme des alten SB-Blocks).
- Netzwerk: `net/ResonanceStatsPayload.java` (S2C, 6 VarInt + 1 Double), Registrierung und Sendung in `fabric/ResonanceFabric.java`, Empfang in `fabric/ResonanceFabricClient.java`,
  `client/ResonanceClient.java` (Speicher, Leeren beim Trennen), `ResonanceBridge.java` (Aufrufe des Kerns per Name).
- Befehle: `ResonanceCommand.java` (`/simpleresonance`, `debug`), Konfig-Befehl ueber `ConfigCommands.server(...)`.
- Client/Oberflaeche: `client/ResonancePanel.java`, `client/ResonanceClientInit.java`, `mixin/client/InventoryScreenResonanceMixin.java` (nur Zeichnen, nichts an den Server).
- Pflege: `SimpleResonance.java`, `simpleresonance.mixins.json`, `fabric.mod.json`, `modules.json`-Eintrag, `guide/ResonanceGuide.java` (Registrierung/`use`), Lang EN/DE.
- Nicht gelesen: `client/ResonanceLayout.java` (reine Masse), `discover/ResonanceDiscover.java` (Entwicklerpruefung), Rest von `ResonanceGuide` (generierte Seiten), `clienttest/`, Tests im Einzelnen.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | Nur S2C `simpleresonance:stats` (Fabric `clientboundPlay`, vor Senden `canSend`): 6 VarInt + Double, ~30 Byte je Sekunde und Spieler, nur die eigenen Werte des Empfaengers. Empfang auf den Client-Thread verschoben (`context.client().execute`). Kein C2S -> nichts zu pruefen/drosseln. Der Client traut dem Server (kein Pruefen von `base` auf NaN, P3-3). |
| C2 Server-Autoritaet | Punkte, Zonen und Besitzer rechnet nur der Server; der Client rechnet aus der Payload dieselben Faktoren nur zum Anzeigen. **Thread-Grenze verletzt: P2-1 (gefixt).** |
| C3 Dupes/Verlust | Keine Items, Menues oder Container. Datensatz: `deathKeep` auf 0..1 geklemmt (kein Wachstum durch Tode), Boss-Punkte je Quelle mit Abklingzeit nach Spielzeit, Kill-Punkte mit Stundenfenster (Farm-Schutz). |
| C4 NBT/Komponenten | `PlayerRecord` mit `optionalFieldOf`-Standards; Weltspeicher `Codec.unboundedMap` (so gross wie die Zahl der je gesehenen Spieler, P3-5); Config: jede Zahl endlich und geklemmt, falscher Typ -> Standard, kaputte Datei -> Quarantaene. Alter SB-Block: nur lesen, nie loeschen. |
| C5 Tick-Kosten | Dienst-Tick nur alle 20 Ticks (je Spieler: 5 Statistik-Abfragen + Datensatz); Besitzerwahl O(Spieler im Server) je Monster-Spawn mit Zonen-Effekt (nur Kategorie MONSTER und nur zugelassene Gruende), Wert je Spieler 1 s zwischengespeichert. |
| C6 Leaks | `live` wird bei `ServerPlayerEvents.LEAVE` und beim Stopp geleert; `LegacyImport.PENDING` waechst bis zum Stopp (P3-1). Datensaetze der Spieler bleiben dauerhaft (gewollt). |
| C7 Sicherheit/Rechte | `/simpleresonance` zeigt jedem nur die eigenen Werte, `debug` und die Konfig-Befehle nur Stufe 2; keine Weltaenderung, daher kein Schutz-/Claim-Thema. 1 Mixin (`ServerPlayer#readAdditionalSaveData`, nur Lesen, immer aktiv und folgenlos bei abgeschaltetem Mod) + 1 Client-Mixin unter `"client"`. |
| C8 Unabhaengigkeit | Keine Pflichtabhaengigkeit ausser SimpleLib; SB und Simple Mobs nur als `suggests`/Vertrag (`ResonanceContracts`, kein Import auf beiden Seiten). Standalone-Target vorhanden. |
| C9 Doku/Pflege | Kein TODO/FIXME; Lang EN=DE (61 = 61 Schluessel). Keine Kern-Dubletten. `compatibilityLevel` JAVA_21 in `simpleresonance.mixins.json` (Rest des Projekts JAVA_25), P3-4. |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simpleresonance-P2-1 | P2 (Thread-Sicherheit, seltener Absturz/Hang) | Simple Mobs ruft `ResonanceService.sample` aus `Mob#finalizeSpawn` (`MobZoneSpawnMixin`). Vanilla fuehrt Spawns bei der Chunk-Erzeugung (`EntitySpawnReason.CHUNK_GENERATION`, im Standard von `zoneSpawnReasons`) auf einem **Weltgenerierungs-Arbeiter** aus (`ChunkStatusTasks#generateSpawn`, per javap geprueft: nur `FULL` nutzt den Hauptthread). Dort liefen `candidates()` (ArrayList der Spielerliste), `live()` (HashMap schreibt), `valueOf()` -> `data.record()` (LinkedHashMap schreibt, `setDirty`) neben dem Hauptthread: `ConcurrentModificationException` beim Speichern oder bei Beitritt/Austritt, verlorene/verhedderte Map-Eintraege. Erreichbar nur, wenn dort ein Monster entsteht (Vanilla: Tierliste; Biome aus Datapacks/Fremd-Mods mit Monstern in der Tierliste) - selten, aber dann nicht vorhersagbar. Die Klassendoku sagt „Server thread only“, nichts erzwang es. | **gefixt**: `ResonanceService#candidates` und `#nearbyCaps` liefern abseits des Server-Threads niemanden (`server.isSameThread()`), `sample`/`pickOwner` behandeln den Spawn dann als herrenlos (Zonenstufe mit `ownerlessCap`, reine Mathematik) - das ist fuer Chunk-Erzeugung ohnehin gemeint (Config-Doku `ownerlessCap`). Geaenderte Dateien: `shared/.../ResonanceService.java`, `shared/.../test/ResonanceTests.java`, `fabric/.../ModuleGameTest.java`. Gametest `sample_off_server_thread` (Adapter `sampleOffServerThread`; ohne Fix rot: der Arbeiter-Aufruf liefert den Besitzer) |
| simpleresonance-P2-2 | P2 (Logik, Spielerfolgen) | Die Aktivitaets-/AFK-Erkennung (`totalDistance` 91-96, `update` 315-335, `candidates` 232) zaehlt nur Gehen, Sprint, Schleichen, Luft (`fly_one_cm`) und Klettern. **Reiten, Boot, Lore, Schwimmen/Tauchen, Waten und Elytra zaehlen nicht**: wer eine Minute so unterwegs ist, sammelt keine aktive Zeit/Punkte mehr, nach `ignoreAfkMinutes` (5) gilt er als AFK und besitzt keinen Spawn (Spawns um ihn sind herrenlos, Stufe <= `ownerlessCap`). Gleiche Regel wie die Kern-Kopie `SurvivalTracerMixin` (`getStatTotalDistance`), also kein Fehler der Auslagerung. | **Besitzer AUDSB31B F9 = B, umgesetzt (a-audsb31b)**: `ResonanceService.movedDistance` = `totalDistance` + Wasser (an/unter/schwimmen), Elytra, Boot, Lore, Pferd, Schwein, Schreiter, Gluecklicher Ghast, Nautilus - nur fuer "bewegt" (`update`), die Wegstrecken-Formel (`totalDistance`) bleibt. Gametest `riding_counts_as_moved`, Wiki EN/DE. Kern-Kopie `SurvivalTracerMixin` (26.2) unveraendert |
| simpleresonance-P3-1 | P3 (Leak) | `LegacyImport.PENDING` (Z. 18, `park` 24) bekommt bei **jedem** Beitritt den alten `SimpleBuildingData`-Block (`ServerPlayerLegacyMixin` 15-18), auch fuer schon uebernommene Spieler (`update` nimmt ihn nur bei `!legacyImported`): ein kleiner Eintrag je Spieler bis zum Serverstop. | Hinweis (`park` nur ohne `legacyImported`, oder beim Verlassen entfernen) |
| simpleresonance-P3-2 | P3 (Last) | Jede Sekunde geht an jeden Spieler eine Payload, auch wenn sich nichts geaendert hat (ResonanceService.tick 302-312). ~30 Byte; bei 100 Spielern ~3 KB/s. | Hinweis (nur bei Aenderung senden) |
| simpleresonance-P3-3 | P3 | `ResonanceStatsPayload.read` (Z. 23-26) prueft `base` nicht auf NaN/Unendlich; der Client rechnet damit nur die Anzeige. Der Absender ist der eigene Server. | Hinweis |
| simpleresonance-P3-4 | P3 (Pflege) | `simpleresonance.mixins.json`: `compatibilityLevel` JAVA_21, das Projekt laeuft auf Java 25 (`fabric.mod.json` `java >=25`). | Hinweis |
| simpleresonance-P3-5 | P3 (Wachstum) | `ResonanceData` (je Spieler ein Satz, nie bereinigt) waechst mit der Zahl der je gesehenen Spieler; jeder Sekundentick, der Punkte vergibt, setzt `setDirty`, und gespeichert wird die ganze Map. Bei zehntausenden Spielern mehrere MB je Speichern. | Hinweis (Bereinigung nach langer Abwesenheit waere ein eigenes Vorhaben) |
| simpleresonance-P3-6 | P3 | Simple Mobs ruft den Dienst ohne Thread-Pruefung (`ZoneSpawns#onSpawn`, `MobZoneSpawnMixin`); der Dienst ist jetzt geschuetzt, saubere Loesung waere zusaetzlich dort `level.getServer().isSameThread()` (anderer Mod, hier nicht geaendert). | Hinweis an die Auditoren von simplemobs |

## Urteil
**shipbar nach Fix** (0 P1, 2 P2 gefixt - P2-2 nach Besitzer-Antwort AUDSB31B F9 = B -, 6 P3). Der Mod hat keinen Eingang von Clients, keine Weltaenderung und alle Zahlen geklemmt; das einzige echte Risiko war die
Thread-Annahme des Dienstes gegenueber Simple Mobs (jetzt abgefangen). Die AFK-Erkennung (P2-2) zaehlt seit a-audsb31b auch Reiten, Boot, Lore, Wasser und Elytra.

## Testergebnis
Lokal: javac der geaenderten Dateien gegen 26.3 + Fabric-API (`/root/sb-tools-a/audsb31/javac.sh simpleresonance ...`): nur die bekannte Access-Widener-Meldung in der ungeaenderten
`LibMaterials.java`, in den drei geaenderten Dateien nichts. `modules/simpleresonance/tools/check_data.py` gruen; Katalog und Fabric-Adapter decken sich (je Eintrag ein `@GameTest`).
`sample_off_server_thread` ist nicht in `ResonanceTests.PURE` (braucht den laufenden Dienst, wie `spawn_service_levels`). sbt: siehe `docs/ai/PLAN-A-AUDSB31-2026-11-12.md`.
Nicht getestet: der Gametest selbst (kein Gradle erlaubt), echte Chunk-Erzeugung mit Monstern in der Tierliste, Client-Anzeige, Mehrspieler, Forge/NeoForge.
a-audsb31b (F9): javac von `ResonanceService`, `ResonanceTests`, Adapter lokal sauber (nur die bekannte `LibMaterials`-Meldung); `riding_counts_as_moved`
braucht den laufenden Dienst (nicht in `PURE`). sbt siehe `docs/ai/PLAN-A-AUDSB31B-2026-11-12.md`. Nicht getestet: echtes Reiten/Elytra im Client (Statistik per Test gesetzt).
