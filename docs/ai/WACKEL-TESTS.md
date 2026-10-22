# Wackel-Tests

Tests, die im Gate rot waren und beim automatischen Zweitversuch (einzeln, `tools/testrunner/retry_red.py`) grün wurden.
Das Gate schreibt sie auf sb-test fortlaufend nach `/root/gate/WACKEL-TESTS.md`; die Sitzungen übernehmen neue Zeilen
hierher und reparieren die Tests (Ursache suchen, nicht nur wiederholen). Reparierte Tests wandern nach „Erledigt“.

Ablauf im Gate (`/root/sb-gate.sh` auf sb-test, seit 20.10.2026): Ist das Gate nur wegen roter Tests rot (check grün,
kein FEHLER, höchstens 6 rote Tests), läuft jeder rote Test einmal allein nach. Alle grün → Gate grün (Zeile
`retry: GREEN` in `/root/gate/chain-<ref>.txt`) und Eintrag hier; einer wieder rot → Gate rot.

## Offen

| Datum | Ref | Ziel | Test |
|---|---|---|---|
| 2026-10-22 | 04ec2d56a | module-simpletweaks-standalone-fabric-263 | simpletweaks:admin_game_test_vanish_hides_and_ignores |
| 2026-10-21 | 5b877f7ac | module-simpledimensions-standalone-fabric-263 | simpledimension:module_game_test_player_inventory_roundtrip |

## Erledigt

| Datum | Test | Ursache / Fix |
|---|---|---|
| 2026-10-20 | simplemobs:module_game_test_fear_watcher_hit_new_victim | Schweine außerhalb der 8×8-Testfläche, Nachbar-Struktur räumte sie weg; Fix 371df6b7f |
| 2026-10-20 | simpletweaks:claims_game_test_claims_rights_action_matrix | Probe-Zelle im Nachbar-Chunk (außerhalb der Struktur) wird von anderen Tests/Mock-Spielern geteilt, Platzieren (Recht B) auf Tick 0 blockiert (probes 0, expected 1); Fix: freie Zelle suchen (`freeGround`), Commit d7b12fc97. Vermutete Ursache (nicht reproduzierbar), 3 Läufe + simpletweaks:* grün |
| 2026-10-20 | simpleend:module_game_test_end_leaves_catch_fall | Test-Laub war nicht persistent und konnte per Random-Tick zerfallen, bevor das Schwein ankam (~15 Ticks) -> voller Fallschaden (5.0); Fix: PERSISTENT, Commit 5e6e6fb59. 3 Läufe + simpleend:* grün |
| 2026-10-22 | b1fc76886 | module-simpledimensions-standalone-fabric-263 | simpledimension:module_game_test_player_inventory_roundtrip (erneut, Retry grün) |

## Delta-Gate

Nach einem roten Gate auf `<base>` prüft `/root/sb-gate.sh <fix-ref> delta <base>` nur, was der Fix ändern kann
(`tools/testrunner/delta_gate.py`): `gradlew check`, jeden roten Test der Basis einzeln und die Ziele der berührten
Module samt Modulen, die sie voraussetzen (SimpleBuilding-Kernziele, wenn SB eines davon voraussetzt), dazu
`integration-263`. Docs/Queue/Wiki brauchen nur `check`. Änderungen an common/, simplelib, framework, Build-Dateien,
tools/ oder Fixes, die nicht auf der Basis aufsetzen, führen automatisch zum vollen Gate. Grün → Push nach master wie
beim vollen Gate; der Nachtlauf (`sb-night.sh`) bleibt der volle Lauf über alles.
