# PLAN B-FLAKYDIM24 (24.10.2026)

## Fehlerbild
`simpledimension:module_game_test_player_inventory_roundtrip` (Targets module-simpledimensions-fabric-263 und -standalone-)
war im 1. Gate-Lauf rot, im Retry gruen: Gate 9815ae661 (24.10.), b1fc76886 (22.10., standalone), 5b877f7ac (21.10., standalone).
Siehe /root/gate/WACKEL-TESTS.md. Einzeln gestartet ist der Test gruen; rot nur im Modul-Lauf neben den anderen Tests.

## Ursache (Hypothese, am Code belegt)
Alle Reise-Tests laufen parallel auf einem Server und teilen `DimensionRuntime` (lastBuildTick, returns, portals).
`DimensionRuntime.travel` bricht den Erst-Aufbau eines Ziel-Portals mit `return false` ab, wenn in DEMSELBEN Server-Tick
schon ein anderes Portal gebaut wurde (`lastBuildTick==server.getTickCount()`). Die Tests entschaerfen das nur durch
versetzte Fixverzoegerungen (80/82/84/86 ...), aber ihre Startticks unterscheiden sich je nach Strukturplatzierung;
dann treffen zwei Reisen denselben Tick -> "Travel with SimpleBuilding backpack" schlaegt fehl.

## Beleg
Gate-Log standalone-b1fc76886: `Travel with SimpleBuilding backpack on tick 86` = die ERSTE travel-Assertion (Erst-Aufbau
des Ziel-Portals gab false). Mehrere Reise-Tests teilen einen Server; Abbruchgruende im Erst-Aufbau, die von Parallelitaet
abhaengen, sind genau `lastBuildTick==server.getTickCount()`. 5 lokale Laeufe (1 einzeln + 4 Modul) waren gruen, nicht
reproduzierbar; Ursache daher aus Code + Fehlertext abgeleitet, nicht per Repro bewiesen.

## Fix
`DimensionRuntime.travelTest/tickPlayerTest` setzen vor dem Aufruf den Throttle zurueck; alle 32 `travel(`-Aufrufe der
Tests (DimensionTests, PortalProtectionTests) und die `rt.tickPlayer`-Aufrufe nutzen sie. Produktverhalten (Throttle
fuer echte Spieler) und alle Assertions bleiben unveraendert; verneinende Assertions pruefen jetzt echte Ablehnung
statt eventuell den Throttle. Journey-Tests (DimensionSettingsTests) laufen bewusst ueber den echten Loader-Tick, unveraendert.

## Verifikation
module-simpledimensions-fabric-263 3x, module-simpledimensions-standalone-fabric-263, `./gradlew check` (pipefail).
