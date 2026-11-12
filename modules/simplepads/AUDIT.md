# Audit simplepads (b-audsb31-a, 11.11.2026)

Basis `hub/b-bundle182` 9b5764995, nur MC 26.3 Fabric. Aufbau wie `modules/simplemodels/AUDIT.md` (b-rel32).
Umfang: 93 Java-Dateien / 13,6k Zeilen (davon ~45 % Tests). Risikogeleitet geprueft; fruehere Audits im Code vermerkt
(`docs/AUDIT-2026-09-26.md` #32 Flypad-Tag, Nach-Audit N16, 2026-09-28 Chunk-Loader nur mit Besitzer online).

## Vollstaendig gelesen
- Netzwerk: `network/ElytraBoostPayload.java` (C2S), `network/PadsConfigPayload.java` (S2C), `network/PadsNetwork.java`,
  Registrierung `PadsFabric.java`/`PadsFabricClient.java`, `SimplePads#setSynced`.
- `block/entity/FlypadBlockEntity.java` (Flug geben/nehmen), `block/entity/ChunkLoaderBlockEntity.java` (Erzwingen/Freigeben),
  `ChunkLoaderRegistry` (Laufrecht), `PotionPadRules` (Sperren), `PadsContent`, Befehle (`PadsCommands`, `ChunkLoaderCommand`).
- Stichproben: `LaunchSafety`, `PlayerScan`, `FilterPressurePlateBlock`, `DiamondPressurePlateBlock` (Entity-Abfragen).
- Nicht gelesen: Launch-/Elytra-/Trank-Pad-Physik im Detail, Spawn-Teleporter, Easter-Rezepte, Screens, Tests.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | C2S nur `ElytraBoostPayload` (leer): Server prueft Spawn-Elytra in der Brust, Gleitflug, Ladung, 3 je 100 Ticks, Geschwindigkeit gedeckelt und endlich. S2C Config als JSON (max. 64 KiB), Client validiert (`validate()`), Fehler = ignorieren; Trennen setzt zurueck. |
| C2 Server-Autoritaet | Flug, Boost, Chunk-Laden entscheidet nur der Server. |
| C3 Dupes | keine Item-Menues mit Logik gefunden; Launchpad-Container nur `stillValid`. |
| C4 NBT | Chunk-Loader speichert eigene Chunks als Long-Liste; Registry-Codec mit optionalem Besitzer. |
| C5 Tick-Kosten | Flypad alle 5 Ticks (Spielersuche ueber `PlayerScan`), Chunk-Loader alle `CHECK_INTERVAL`. |
| C6 Leaks | `LOCKOUTS` (Spieler x Effekt) und `LAUNCHED_AT` werden beim Lesen/Tick bereinigt; Fremdwelt-Eintraege begrenzt. |
| C7 Sicherheit/Rechte | Befehle `chunkloader`, `teleporterspawn`, `elytracenter` nur Operatoren. **P1-1 gefunden und gefixt.** |
| C8 Unabhaengigkeit | Pflicht simpleores + simplemobheads (AUDIT-A, als Add-on erklaert). |
| C9 Doku | Handbuch 1 Feature fuer 35 Bloecke, 14 JEI-/Jade-Keys alt (AUDIT-A). |

## Befunde
| ID | Schwere | Befund | Stand |
|---|---|---|---|
| simplepads-P1-1 | **P1 (Exploit)** | Flypads merken ihre Flieger nur zur Laufzeit. Wer fliegend auslogte - oder dessen Pad waehrend seiner Abwesenheit abgebaut, entladen oder abgeschaltet wurde, oder nach einem Neustart - kam mit Flug-Tag und Kreativflug zurueck, und kein Pad nahm ihn je wieder: **dauerhafter Flug im Ueberlebensmodus**. | **gefixt** 56ba7d09c: alle 20 Ticks verliert jeder Spieler mit Tag, den kein eingeschaltetes Flypad abdeckt, den Flug (mit Sicherheitsnetz); Gametest `stale_flypad_flight_is_swept_away`. Die 26.2-Kern-Kopie (`common/.../tweaks/block/entity/FlypadBlockEntity.java`, auf 26.3 nicht registriert) hat denselben Fehler -> beim 26.2-Port nachziehen (Drift in `tools/dup_pairs.txt` vermerkt). |
| simplepads-P2-1 | P2 (Server-Last) | Chunk-Loader haben kein Limit je Spieler oder Server; jeder Spieler kann beliebig viele bauen (Stufe III = mehrere Chunks). Abgemildert durch `chunkLoaderRequireOwnerOnline` (Standard an) und Dimensionssperren. | **Rueckfrage** (Besitzer: Limit ja/nein, Standardwert) |
| simplepads-P3-1 | P3 | `PadsNetwork.setSenders` wird im Client-Einstieg ueberschrieben (gleiche Wirkung, beide schicken nur bei `canSend`). | Hinweis |
| simplepads-P3-2 | P3 | 53 Kern-Dubletten (6140 Zeilen) - Flypad-Fix siehe P1-1. | Liste |

## Urteil
**shipbar nach Fix** (1 P1 gefixt, 1 P2 Rueckfrage, 2 P3).

## Testergebnis
Lokal: javac der geaenderten Dateien (nur bekannte Access-Widener-Meldungen), `check_data.py` gruen, `check_dup_impl` 0 Probleme. sbt siehe Plan.
Nicht getestet: echter Logout/Login-Ablauf im Client (der Gametest setzt Tag + Flug direkt), Forge/NeoForge.
