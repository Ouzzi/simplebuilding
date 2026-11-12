# Audit simplegadgets (b-audsb31-a, 11.11.2026)

Basis `hub/b-bundle182` 9b5764995, nur MC 26.3 Fabric. Aufbau wie `modules/simplemodels/AUDIT.md` (b-rel32).
Umfang: 54 Java-Dateien / 11,6k Zeilen (davon ~45 % Tests). Risikogeleitet geprueft; fruehere Audits im Code vermerkt
(`docs/AUDIT-2026-09-26.md` #17 Laser-Relay, #25 Detektor-Scan, #33 Echolot zahlt erst nach dem Sprung, Review F-06 Aufraeumen).

## Vollstaendig gelesen
- Netzwerk: `network/LaserPayload.java`, `network/RodNetwork.java` (Relay), Registrierung `GadgetsFabric.java`.
- `GadgetProtection.java`-Aufrufe in Rotator, Echolot, Stab, Attractor; `rotator/RotatorItem` (Ausholen/Tausch, Nachpruefung),
  `rotator/RotatorSwap#canSwap/canSwapTarget/blockEntityAllows`, `echo/EchoSounderItem` (Verknuepfen, `canJump`, `teleport`),
  `attractor/PlacedAttractor` (Tick, Zug, Schweben), `detector/DetectorItem#findNearestReachable` (Scan-Kosten).
- Nicht gelesen: Clients (Gauge-HUD, Renderer), Rod-Strahl im Detail, Tests.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | Einziges C2S `LaserPayload`: nur mit Schalter, waehrend der Spieler einen Stab benutzt, Koordinaten endlich und in Reichweite + 8, max. 12/s, UUID durch die des Senders ersetzt, nur an Spieler in 128 Bloecken. |
| C2 Server-Autoritaet | Tausch, Sprung, Zug, Zuenden nur serverseitig; Rotator prueft beim Abschluss Block, Blick, Ladung und Schutz erneut. |
| C3 Dupes | Rotator tauscht keine Bloecke mit BE ausser leeren Containern ohne Loot-Tabelle, keine Doppelbloecke, keine Wertbloecke. |
| C4 NBT | Komponenten: Blockzustand (Detektor), Lodestone-Tracker (Vanilla), Strings Vanilla-Limit. |
| C5 Tick-Kosten | Detektor-Scan mit Abschnitts-Vorfilter, ungeladene Chunks werden nicht geladen; Attractor im Takt `INTERVAL`, je Item ein Zug je Takt. |
| C6 Leaks | `WeakHashMap`s fuer Spieler/Items/Welten; `PENDING_ECHOES`, `LAST_FAIL` beim Trennen/Stopp bzw. Erfolg geraeumt. |
| C7 Schutz | `GadgetProtection.mayTransform/mayAct/mayTeleport/mayAffectEntity/mayBreak/mayPlace` (Claims, Spawnschutz, Weltgrenze, Admin-Bypass nur LEVEL_OWNERS). |
| C8 Unabhaengigkeit | allein spielbar (Standalone-Target), nur Fabric. |
| C9 Doku | WE25-8 Teil 2 Rueckfrage, 3 JEI-Keys alt (AUDIT-A). |

## Befunde
| ID | Schwere | Befund | Stand |
|---|---|---|---|
| simplegadgets-P3-1 | P3 (Last) | `EchoSounderItem#canJump` liest beim Ladebeginn `targetLevel.getBlockState(target)`; ein entfernter, ungeladener Ziel-Chunk wird dafuer synchron geladen. Gedrosselt durch Sperre nach Fehlversuch und Abklingzeit; der Sprung selbst laedt ohnehin. | Hinweis |
| simplegadgets-P3-2 | P3 | 8 Kern-Dubletten (AUDIT-A). | Liste |

## Urteil
**shipbar** (0 P1, 0 P2, 2 P3). Keine Code-Aenderung in diesem Mod.

## Testergebnis
Lokal: `check_data.py` gruen. Modul-Gametests im sbt mitgefahren (keine Aenderung, Regressionsschutz).
Nicht getestet: Client, Mehrspieler-Paketflut.
