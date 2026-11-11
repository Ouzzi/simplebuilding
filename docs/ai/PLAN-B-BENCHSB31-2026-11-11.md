# Plan b-benchsb31 (11.11.): L9 Headless-Messung SimpleBuilding + Pflicht-Mods gegen Vanilla

Basis `hub/b-bundle182-gen` (4f676c1c9) + Merge `hub/b-relsb31` (RELEASE-SIMPLEBUILDING.md, L9). Nur MC 26.3 Fabric, keine Client-Läufe.
Besitzer RELSB31 f5=B: **Grenze <= 10 % mehr MSPT als Vanilla**; Heap nur berichten. f1=A: Welle 1 = SB + 20 Pflicht-Abhängigkeiten.

## Ist
- `bench-opt-headless-263` + `tools/bench/headless_bench.py` (b-opte4): misst nur Simple Optimization (Bereiche aus/Standard/an),
  Messtest liegt im Super-Mod Simple Optimization -> in einem Vanilla-Server nicht vorhanden.
- Für SB + Pflicht-Sub-Mods kein Messwert (RELEASE-SIMPLEBUILDING 4.11).

## Umsetzung
1. **Messtest `sbintegration:sb_bench_game_test_measure`** (`integration/.../SbBenchGameTest.java`, im Harness-Mod `sbintegration`,
   der nur Vanilla-/Fabric-API nutzt, also in beiden Setups laden kann): dieselbe Szene wie b-opte4 (120 Kühe im Barriere-Pferch,
   64 Trichter, 32 Öfen, 6³ Eichenblätter), 100 Ticks Warmup, Heap nach 2x GC, 4x100 Server-Ticks aus `getTickTimesNanos()`
   -> MSPT Mittel/p95; Zeile `SB-BENCH sb {json}` (auch Anzahl geladener Mods). Nur aktiv mit `-Dsb.bench=true`, sonst sofort
   grün (normaler Integrationslauf unberührt). Optional `-Dsb.bench.profile=true`: Stack-Sampler (alle 2 ms) auf dem
   Server-Thread, zählt den innersten `com.simplebuilding.*`-Frame je Mod-Paket und Methode (headless-Profiler).
2. **Mod-Auswahl:** `integration/bench-sb-vanilla.json` (keine Module/Dev-Mods = Fabric API + Messmod) und
   `integration/bench-sb-full.json` (SB + Abschluss der `requires` aus modules.json = 20 Mods: simplelib, simpletools + 18 Sub-Mods;
   dazu Cloth Config, das SB zwingend braucht). Über `-PintegrationMods=` (vorhandener Schalter). `integration/build.gradle`:
   `-PbenchSb` setzt `-Dsb.bench`, `-Xmx4G` und lässt die Kreuz-Mod-Prüfung (simplebuilding+wiringexample) aus.
3. **`headless_bench.py --profile sb`**: Setups `vanilla`/`sb`, je Lauf frische JVM, **frische Welt** (world/ gelöscht, gleiche
   Seed durch die Standard-Gametest-Welt) und frische Config (config/ gelöscht = Spielerstand), verschachtelt, `--runs` >= 3
   (Standard 5), Median +- Std.-Abw. Bewertung: Median MSPT-Mittel sb gegen vanilla **PASS** wenn <= +10 %, sonst **FAIL** (Wert in
   Ausgabe, `results.json`/`results.md`). Bei FAIL (oder `--hot`) ein zusätzlicher `sb`-Lauf mit Stack-Sampler -> Top-Tick-Kosten.
   Keine Optimierung (eigener Folgepunkt).
4. **Runner-Ziel `bench-sb-headless-263`** (`tools/testrunner/run.py`, opt-in wie das Opt-Ziel, `SB_BENCH_RUNS`, Standard 5):
   Messläufe = Tests, zusätzlich Test `bench:sb_mspt_limit` (grün = PASS, Meldung mit Wert) -> Ergebnis als Runner-Ausgabe mit Bewertung.

## Entscheidungen (selbst)
- Messgröße für die 10 %-Grenze ist der Median des MSPT-Mittels (p95 nur berichtet, weil bei 400 Ticks verrauscht).
- Vanilla enthält das Messmod `sbintegration` (nur Test, kein Spielinhalt); `:common`/`:framework` sind keine Mods (keine
  fabric.mod.json) -> kein SB-Code im Vanilla-Setup (Mod-Anzahl in der Ausgabe als Beleg).
- Szene bewusst Vanilla-Last (Tiere/Trichter/Öfen/Blätter): misst den Grundaufwand, den die Mods auf Vanilla-Inhalt (Mixins, Tick-Hooks) erzeugen,
  nicht ihre eigenen Blöcke im Einsatz (Folgepunkt, wenn gewünscht: Szene mit Mod-Blöcken).
- Messlauf nur bei leerem sb-test (Regel 16 B09): Wartezeit in 10-Min-Schritten, Vorab-Kompilieren/Rauchlauf (1 Lauf) zählt nicht als Messung.

## Risiken
- sb-test geteilt: Messung verrauscht -> nur bei leerem Rechner; Streuung im Ergebnis.
- Gametest-Id der Methode (`sb_bench_game_test_measure`) per Rauchlauf geprüft.

## Prüfung
Lokal: Skript-Trockenlauf (Verdict/Markdown), Syntax. sb-test (einzeln): `:integration:compileJava`-Pfad über Rauchlauf,
dann Messlauf `headless_bench.py --profile sb --runs 5`.
