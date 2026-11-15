# SimpleBuilding gegen Vanilla headless (2026-11-15, sb-test)

Runs je Setup: 5; Median (± Std.-Abw.). vanilla = Fabric API + Messmod; sb = SimpleBuilding + 20 Pflicht-Mods (Welle 1) + Cloth Config. Szene: 120 Kühe, 64 Trichter, 32 Öfen, 6³ Blätter, 400 Server-Ticks nach 100 Ticks Warmup, frische Welt. Kein Client.

| Kennzahl | vanilla | sb |
|---|---|---|
| MSPT Mittel ms | 3.191 ± 0.999 | 3.427 ± 0.791 (+7.4%) |
| MSPT p95 ms | 4.347 ± 2.246 | 5.12 ± 1.494 (+17.8%) |
| Heap nach GC MB | 237.3 ± 2.995 | 411.2 ± 1.747 (+73.3%) |
| Mods geladen | 44 ± 0.0 | 68 ± 0.0 (+54.5%) |

**Bewertung: PASS** - MSPT Mittel +7.4 % gegen Vanilla (Grenze +10 %), p95 +17.8 %, Heap +173.9 MB (nur berichtet).

sb-test ist geteilt: nur bei leerem Rechner messen (Wachlauf in docs/ai/PLAN-B-BENCHSB31-2026-11-11.md).

Einzelläufe (MSPT-Mittel ms, vanilla / sb): 1.867/2.17, 2.394/3.849, 3.302/4.285, 3.191/3.427, 4.491/3.343 (sb-test geteilt mit anderen Jobs, daher die Streuung).
