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

## Erledigt

| Datum | Test | Ursache / Fix |
|---|---|---|
| 2026-10-20 | simplemobs:module_game_test_fear_watcher_hit_new_victim | Schweine außerhalb der 8×8-Testfläche, Nachbar-Struktur räumte sie weg; Fix 371df6b7f |
