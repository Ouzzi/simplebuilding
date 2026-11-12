# Audit simplelayers (a-audsb31e2, 12.11.2026)

Basis `hub/b-bundle188` 9dfb3f643, nur MC 26.3 Fabric (Forge/NeoForge nicht Teil des Releases). Aufbau wie `modules/simplemobheads/AUDIT.md`.
**Modul PAUSIERT** (Ebenen 1-10 nach BACK22c): nur Fehler-Audit (Abstuerze, Dupes, Rechte, Leaks, Datenverlust), keine neuen Ebenen/Bloecke/Items.
Umfang: 85 Java-Dateien / 6,0k Zeilen ohne Tests (Kataloge `test/LayersTests` + `BandsTests` + 11 Fabric-Adapter 2,1k Zeilen, `clienttest/` 9 Zeilen). Reiner Client-Code: keiner
(kein Client-Entrypoint, keine Client-Mixins). Der Mod hat **keine** Netzwerk-Payloads, **keine** Menues und keine eigenen Block-Entities: C1 und der Menue-Teil von C3 entfallen.
Risikogeleitet gelesen: Teleport/Tuer-Portale, Eingaenge, Backroom-Effekt/Abbau-Sperre, Kolben, Kreide, Wolken-Uebergang, SavedData, statische Maps, Weltgen-Features und
Generator-Huelle. Die eigentliche Portal-Mechanik (`PortalTransit`, `PortalDoorBlock`, `PortalLinks`) liegt in SimpleLib und gehoert nicht zu diesem Paket.

## Vollstaendig gelesen
- Einstieg/Registry/Config: `SimpleLayers.java`, `LayersFabric.java`, `LayersRegistry.java` (Z. 60-236), `config/LayersConfig.java`, `simplelayers.mixins.json`, `fabric.mod.json`.
- Teleport/Wege: `world/BackroomsTravel`, `CloudTransition`, `BackroomsEntries`, `BackroomsRescue`, `block/RiftBlock`, `ExitDoorBlock`, `LevelDoorBlock`, `BackroomsDoorBlock`,
  `MaintenanceBoxBlock`, `backrooms/door/DoorNet`, `DoorStamp`, `Entrances`.
- Abbau-Sperre/Kolben: `world/BackroomEffect`, `block/BackroomBlock`, `GlitchedBedrockBlock`, `NeonLightBlock`, `mixin/BlockStateBaseMixin`, `mixin/PistonBaseBlockMixin`.
- Blocks/Items: `block/CloudBlock`, `MistBlock`, `CushionBlock`, `CautionTapeBlock`, `CautionSignBlock`, `KeyItem`, `ChalkMarkBlock`, `ChalkItem`, `ChalkBoxItem` (Z. 40-170).
- Weltgen: `world/EntranceShaftFeature`, `BackroomsCacheFeature`, `GlitchedBedrockFeature`, `MistPitFeature`, `SkyChestFeature` (nur ueberflogen), `backrooms/stack/BackroomsStackGenerator`,
  `world/gen/Maze`, `world/gen/EscapeCheck` (nur Test-/Bild-Hilfe), `backrooms/gen/Puzzle` (dito), Dimensions-/Biom-JSON (Mob-Spawns, `dimension_type`).
- Ambiente/Loot: `world/BackroomsAmbience`, `ListenerAmbience`, `BackroomsLoot`, Aufruf `LayersGuide#installQuests`.
- Nicht gelesen: Generatoren der Ebenen (`world/gen/LobbyGen`, `PoolsGen`, `PipesGen`, `WarehouseGen`, `HouseTemplate`, `backrooms/gen/*Band`, `DoorPlacement`, `StairRule`, `BuildRules`, 10 `*Band`),
  `BackroomsBlocks`, `LayersGuide` (ausser Quest-Install), Tests/Kataloge, Texturen/Wiki.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | keine eigenen Payloads. |
| C2 Server-Autoritaet | Alle Entscheidungen serverseitig (Tuer-/Rift-/Kasten-Nutzung `instanceof ServerLevel/ServerPlayer`). Abbau-Sperre ueber `getDestroyProgress` = 0 (der Server rechnet den Fortschritt in `ServerPlayerGameMode` selbst nach; ein manipulierter Client kommt damit nicht durch). Befund P2-1 (Effekt allein). |
| C3 Dupes/Verlust | Teleports ueber Vanilla `Entity#teleport` (Inventar bleibt, Reittiere: `getRootVehicle` mit Passagieren; Mitfahrer nutzen Rift/Tuer nicht). Hintertuer-Schluessel nur bei geglueckter Teleportation verbraucht (`ExitDoorBlock` Z. 41). Wartungskasten kostenlos, einmalig (`USED`). Kreide-Kiste: `absorb` nimmt nur die Restschlaege (`take = min(left, CAPACITY-have)`), Kreide schrumpft oder verliert Haltbarkeit: kein Dupe; Glimmtintenbeutel `shrink(1)`. Rift/Tueren/Boxen ohne Loot-Tabelle, nicht abbaubar. Kein Dupe-Pfad gefunden. |
| C4 NBT/SavedData | `BackroomsEntries` (Codec mit `optionalFieldOf`, eine Position je Spieler, bereinigt bei Ausgang/Respawn/Portal), `BackroomsRescue.Checked` (UUID-Liste, 1 je Spieler). Kreidekasten: `custom_data` per `getIntOr`. Beide SavedData am Overworld-Speicher. |
| C5 Tick-Kosten | Tick: Wolken (je Spieler O(1)), `tickVoid` (nur Overworld-Spieler), Effekt (alle 10 Ticks), Ambiente (Zaehler je Spieler). Weltgen rein funktional (Tests `bandsGenBudget`); `DoorNet.ENDS` LRU 4096 mit `synchronized`, `Maze.TREES` ConcurrentHashMap (leert sich bei 4096). Nachlade-Schleifen begrenzt (`freeY` < 500, `thickness` < 64). Wiederholung bei fehlgeschlagener Teleportation: P3-1. |
| C6 Leaks | `LAST_Y`, `ListenerAmbience.STATE`, `BackroomsAmbience.NEXT` (alle bei Logout bereinigt; `LAST_Y` auch wenn Wolken aus), Caches begrenzt. Keine Spieler-/Level-Referenzen in statischen Strukturen. |
| C7 Sicherheit/Rechte | Befehl `/simplelayers config` ueber SimpleLib (Stufe 2), Werte geklemmt. Mixins: `BlockStateBaseMixin` (HEAD, nur wenn `heldFor`), `PistonBaseBlockMixin` (HEAD `isPushable`, nur im Stack): beide `defaultRequire 1`, keine Client-Mixins noetig. Kolben-Sperre unabhaengig vom Effekt (`heldInPlace`). Spawnschutz/Claims fuer Kreide laufen ueber `handleUseItemOn`/`UseBlockCallback` (P3-5). Explosionen: P2-2. |
| C8 Unabhaengigkeit | `fabric.mod.json` depends simplelib >=0.4.0 (suggests simplebuilding), `modules.json` requires simplelib, optional simplebuilding: kein Widerspruch. Standalone-Ziel vorhanden. Fremdbezuege nur bedingt (`BackroomsLoot`: Echo Sounder/Light Pane per Registry-Abfrage; FTB-Quests per `isModLoaded`; Netherit-Kolben nur im Test per ID). In der Standalone-Welt fehlen die Datenpaket-Dimensionen (Tests pruefen dort nur die Registries). |
| C9 Doku/Pflege | Lang EN=DE 185 = 185, `check_data.py` gruen, keine TODO. Tote Teile: P3-6. Metadaten veraltet: P3-7. `tools/dup_pairs.txt` Z. 108: P3-8. |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simplelayers-P2-1 | P2 (Rechte-Umgehung der Abbau-Sperre) | `block/BackroomBlock#protectedFor` (alt Z. 28-30) verlangte `player.hasEffect(Backroom)`. Der Effekt wird nur alle 10 Ticks (Dauer 40) vergeben (`BackroomEffect#tick`). Milch, Totem oder `/effect clear` nehmen ihn weg: bis zum naechsten Vergabe-Tick (<= 9 Ticks) ist jeder Wandblock abbaubar; mit Axt/Effizienz faellt eine Tapete in einem Tick. Betroffen: alle `BackroomBlock` (Direktpfad) und die Vanilla-Palette (`BlockStateBaseMixin` ueber `heldFor`). | **gefixt**: `protectedFor` haelt den Spieler auf dem Server auch ohne Effekt, wenn `BackroomEffect.due` (Stack, Config an, kein Zuschauer) gilt; der Client folgt weiter dem synchronisierten Effekt (kein Geisterfortschritt). Reine Regel `BackroomBlock#protects`; Gametest `backroom_walls_hold_without_effect` (Adapter `backroomWallsHoldWithoutEffect`) |
| simplelayers-P2-2 | P2 (Rueckfrage, Abbau-Sperre) | Explosionen umgehen `backrooms.unbreakableWalls`: Tapete/Teppich/Deckenplatte/Neonlicht/EXIT-Schild haben Vanilla-Sprengresistenz 0,3-3,0 (`LayersRegistry` Z. 97-104), die Sperre schuetzt nur Spieler-Abbau und Kolben. Die Biome haben keine Mob-Spawns; ein Spieler kann aber TNT/Endkristalle mitbringen und sich durch das Labyrinth sprengen (Lobby-Ausgang, Ebenen ueberspringen). | **Rueckfrage** an Besitzer: A) so lassen (TNT ist teures Spielerwerkzeug, kein Fehler) B) im Stack keine Explosionsabtragung der `backrooms_structure`-Bloecke und `BackroomBlock` (kleiner Mixin/ExplosionDamageCalculator, nur wenn `unbreakableWalls`) C) Sprengresistenz der Backroom-Bloecke auf 3600000 setzen (wirkt auch ausserhalb der Dimension). **Empfehlung:** A bis das Modul wieder aufgenommen wird, dann B. |
| simplelayers-P3-1 | P3 | `BackroomsTravel#tickVoid` (Z. 105-111) und `RiftBlock#entityInside` -> `enterFromRift` (Z. 35-39) setzen keinen Portal-Cooldown, wenn `entity.teleport` `null` liefert (z. B. Wither/Drache im Rift, fremder Teleport-Abbruch): jede Tick wieder `Layout.arrival` + `getChunk` (Schleife nur bei diesem Sonderfall; Rifts entstehen nur noch als Boden des Eingangs/Bodenloecher). | bei `null` Cooldown setzen |
| simplelayers-P3-2 | P3 | `tickVoid` teleportiert `getRootVehicle()`; `travel` legt den Eintrag in `BackroomsEntries` nur fuer `ServerPlayer` an (Z. 56-58). Wer auf einem Pferd in den Void faellt, hat keinen Eintrag: die Ausgangstuer fuehrt zum Welt-Spawn statt zum Fallpunkt (kein Verlust). | Eintrag fuer alle Passagiere der Wurzel anlegen |
| simplelayers-P3-3 | P3 | `BackroomsTravel#exit` mit Schluessel (Z. 80-87): Ziel 1500-3000 Bloecke vom Eingang ohne Weltrand-Pruefung; bei kleinem Weltrand landet der Spieler ausserhalb (Vanilla-Randschaden). | Ziel in den Rand klemmen |
| simplelayers-P3-4 | P3 | `BackroomsRescue#onJoin` (Z. 66-76): die einmalige BACK18-Migration kann einen Spieler mit veraltetem Eintrag (z. B. per `/tp` aus dem Stack genommen) beim ersten Beitritt nach dem Update in 0..64 unter Grund an den alten Eingang setzen; nur Alt-Welten (kein Release), `Checked` waechst um 1 UUID je Spieler. | Hinweis |
| simplelayers-P3-5 | P3 | `ChalkMarkBlock#draw` (Z. 161-184) / `ChalkItem#useOn` / `ChalkBoxItem#useOn` pruefen kein `mayBuild()`: Adventure-Spieler koennen mit Kreide ersetzbare Bloecke (Gras, Schnee, Feuer) entfernen. Spawnschutz und Claims greifen (Vanilla-Paketpruefung `mayInteract`, `UseBlockCallback`). | `player.mayBuild()` in `draw` |
| simplelayers-P3-6 | P3 | Tote Teile seit BACK22: `world/GlitchedBedrockFeature` und `LayersConfig.entranceRarity` ("backrooms.entranceRarity", Z. 85) werden nicht mehr verwendet (`LayersFabric` Z. 31-34 haengt nur `entrance_shaft` an die Biome); Block/Item `glitched_bedrock` bleibt im Tab. | Config-Schluessel/Feature entfernen oder dokumentieren |
| simplelayers-P3-7 | P3 | Metadaten: `fabric.mod.json` und `modules/modules.json` beschreiben noch "the Backrooms below the bedrock"; seit BACK22 eigene Dimension mit 10 Ebenen (Haus + Keller-Doppeltuer). PRINZIPIEN/MODSTRUKTUR laut BEW39 ebenso. | Hauptagent (modules.json); `fabric.mod.json` nach Rueckkehr der Ebenen |
| simplelayers-P3-8 | P3 | `tools/dup_pairs.txt` Z. 108 paart `common/.../mixin/BlockStateBaseMixin` (Vorschlaghammer-/Strip-Miner-Tempo, Inject `RETURN`) mit dem `BlockStateBaseMixin` dieses Moduls (Backrooms-Sperre, Inject `HEAD`): nur gleicher Name, keine Kopie. Beide Mixins stoeren sich nicht (andere Einhaengepunkte). | Hauptagent: Paar streichen |
| simplelayers-BEW39 | Hinweis | Ebenen 3 und 6-10 sind `StubBand`-Platzhalter und im Release erreichbar (`backrooms.enabled` Standard true; BEW39 prio 1: Tueren ab Ebene 2 sperren oder Release verschieben); Durchsicht-Portale, Hoerabnahme, Texturen: Besitzer-Fragen. | BEW39: Feature offen, nicht Teil des Audits |

## Urteil
**shipbar im Fehlersinn** (0 P1, 1 P2 gefixt, 1 P2 Rueckfrage, 9 P3 inkl. BEW39-Hinweis). Teleport, Eingaenge und SavedData sind sauber begrenzt; Dupe-Pfade gibt es nicht (keine Items aus Bloecken,
keine Menues, Schluessel nur bei Erfolg verbraucht). Offen bleibt die Frage der Sprengungen (P2-2). Dass sechs Ebenen Platzhalter sind, ist eine Produktentscheidung (BEW39), kein Fehler dieses Audits.

## Testergebnis
Lokal: `javac.sh simplelayers` auf `BackroomBlock.java`, `LayersTests.java`, `ModuleGameTest.java`: sauber (nur bekannte Access-Widener-Meldungen in den unveraenderten `LayersRegistry.java`
(`displayItems`) und `simplelib/.../LibMaterials.java`); `tools/check_gametest_adapters.py`: nur die bekannte Kern-Luecke; `modules/simplelayers/tools/check_data.py` gruen; Lang EN=DE per Skript.
Neue Tests: `backroom_walls_hold_without_effect` (Wahrheitstafel der reinen Regel, laeuft auch standalone; die Stack-Dimension gibt es dort nicht) und `config_command` (nach dem Muster
`ConfigCommandTests.module`, wie simplefarming/simpledimensions). sbt-Lauf: siehe `docs/ai/PLAN-A-AUDSB31E-2026-11-12.md`.
Nicht getestet: Gametest-Lauf (kein Gradle), echter Milch-/Totem-Fall im Stack (nur in der Integrationswelt mit Dimension moeglich), Explosionen, Mehrspieler, Client.
