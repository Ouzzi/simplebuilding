# Audit simplechisels (a-audsb31, 12.11.2026)

Basis `hub/claude-wave1` 388f19991, nur MC 26.3 Fabric (Forge/NeoForge nicht Teil des Releases). Aufbau wie `modules/simplestorage/AUDIT.md`.
Umfang: 14 Java-Dateien / 1,6k Zeilen (ohne Tests), keine Menues, keine Block-Entities, keine Mixins, **1 Payload** (S2C `ChiselTablesPayload`,
kein C2S). Alles ausser Discover/Guide/Viewer gelesen.

## Vollstaendig gelesen
- Netzwerk/Daten: `net/ChiselTablesPayload.java`, `data/ChiselTables.java` (Datenpaket, Sync, Obergrenze), `data/BuiltInChains.java`, Registrierung
  `ChiselsFabric.java` + `ChiselsFabricClient.java`, die 265 Bloecke der 8 mitgelieferten Ketten (`chisel_transformations/*.json`).
- Werkzeug: `item/ChiselItem.java` (Rechtsklick, Abbau, Ausrichtung), `item/ChiselProtection.java`, `ChiselTier.java`, `registry/ChiselRegistry.java`,
  `config/ChiselConfig.java`, `SimpleChisels.java`.
- Nicht gelesen: `discover/ChiselsDiscover.java`, `guide/ChiselsGuide.java` (generiert), `compat/ChiselsViewer.java`, Tests.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk-Payloads | Nur S2C `ChiselTablesPayload` (Dateikennung -> JSON): beim Beitritt und nach `/reload` (`SYNC_DATA_PACK_CONTENTS`), nur an Clients mit dem Kanal; die Groesse wird auf 900 000 Byte begrenzt (Paket-Limit 1 MiB), zu grosse Dateien kommen nicht an. Der Client wendet pro Datei fehlertolerant an (`applyText`, Fehler = Datei uebersprungen). Es gibt kein C2S: Block und Tabelle bestimmt der Server beim Rechtsklick. |
| C2 Server-Autoritaet / Reichweite | `ChiselItem#useOn`: Umformen nur serverseitig (`tryChiselBlock`), Reichweite durch Vanillas Paketpruefung; Client sagt nur voraus (`canChisel`, gleiche Tabelle). Cooldown und Haltbarkeit serverseitig. |
| C3 Dupes / Verlust | Der Meissel formt Bloecke in der Welt um, ohne Drops. Mitgelieferte Ketten enthalten keine Block-Entity-Bloecke. Rueckwaerts (Schleichen) kostet 2 Haltbarkeit. **P2-1 (Rueckfrage)**: Erntewerkzeug ohne Werkzeugstufe. |
| C4 NBT/Komponenten | Komponente `last_target` (BlockPos, begrenzt durch den Codec). Config-Zahlen 2..200 geklammert. |
| C5 Tick-Kosten | Keine Ticks. `tier(...)`-Zugriffe sind Map-Lookups auf unveraenderlichen Karten. |
| C6 Leaks | Nur `volatile` Statics (`loaded`, `lastFiles`), keine Spieler/Welten. |
| C7 Sicherheit/Rechte | `ChiselProtection#mayTransform`: Zuschauer, Abenteuer (`mayBuild`/`mayUseItemAt`), Spawnschutz/Weltrand (`mayInteract`), Claims (`Protection.allows`, Admin nur LEVEL_OWNERS), Partner-Erlaubnisse; danach erst der Partner-Weg (`ChiselActions.use`). Keine Mixins, keine Befehle ausser `/simplechisels config` (SimpleLib `ConfigCommands`). |
| C8 Unabhaengigkeit | allein spielbar (`depends` nur simplelib, Standalone-Ziel vorhanden); Enderit-Stufe nur mit Simple Ores. |
| C9 Doku/Pflege | Keine TODO/FIXME; Lang EN=DE (check_data gruen). 2 Kern-Dubletten (`ChiselTables`, `ChiselItem` mit `common/src/shared/...`). |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simplechisels-P2-1 | P2 (Fortschritt umgangen, Grenzfall P1) | `ChiselItem#isCorrectToolForDrops` (`ChiselItem.java:89-91`) gilt fuer jeden Block der Tags `mineable_with_pickaxe/axe/shovel` **ohne Werkzeugstufe**. Vanillas `Player#hasCorrectToolForDrops` fragt genau diese Methode (Meissel hat keine Tool-Komponente), also erntet schon der Stein-Meissel Diamanterz, Obsidian oder Antiken Schutt mit Drops (Tempo `(Materialtempo + Bonus) * 0.5`, `:94-99`, dadurch z. B. Diamanterz in rund 2 s). Das `ToolMaterial` jeder Stufe traegt den Tag `incorrectBlocksForDrops` (`ChiselTier.java`), der nie gelesen wird. Gleich im Kern (`common/.../ChiselItem.java:388`); in Doku und Tests nirgends als gewollt vermerkt. | **Rueckfrage** (Mechanik): A) Stufe zaehlt wie bei der Spitzhacke: `&& !state.is(tier.material.incorrectBlocksForDrops())` (eine Zeile, dazu ein Gametest); B) so lassen und im Handbuch sagen "erntet alles"; C) nur Eisen und hoeher ohne Stufenpruefung. Empfehlung A. |
| simplechisels-P3-1 | P3 | `tryChiselBlock` ersetzt per `setBlockAndUpdate` ohne Block-Entity-Pruefung (`:148`); eine Datenpaket-Kette mit Block-Entity-Block (Kiste, Ofen) wuerde den Inhalt verlieren. Die mitgelieferten 265 Bloecke haben keine. | Hinweis (Schutz `world.getBlockEntity(pos) == null`) |
| simplechisels-P3-2 | P3 (Balance) | Rueckwaerts-Ketten (Stufe -> Treppe -> Block) schaffen Material (wie simplehammers-P3-3); eine Doppelstufe wird zur Treppe (Verlust). Gewollt laut Handbuch. | Hinweis |
| simplechisels-P3-3 | P3 | 2 Kern-Dubletten: jeder Fix im 26.2-Kern nachziehen (P2-1, falls umgesetzt). | Liste |

## Urteil
**shipbar mit offener Rueckfrage** (0 P1, 1 P2 Rueckfrage, 3 P3). Netzwerk, Schutz und Datenpaket sind sauber; offen ist nur die Werkzeugstufe beim Abbau.

## Testergebnis
Lokal: nichts geaendert (kein Fix); `modules/simplechisels/tools/check_data.py` gruen, `tools/check_dup_impl.py` 0 Probleme, `tools/check_lang_keys.py` und `check_tooltip_text.py` gruen.
sbt: siehe `docs/ai/PLAN-A-AUDSB31-2026-11-12.md`. Nicht getestet: der Abbau mit Stein-Meissel am Diamanterz (aus dem Code gelesen, nicht im Spiel), Client, Forge/NeoForge.
