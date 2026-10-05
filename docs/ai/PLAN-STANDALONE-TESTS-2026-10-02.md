# Plan: Standalone-Testziel je Modul (Prinzip 8) – 2026-10-04

Queue „Besitzer 2026-10-04 (Nachtrag Modul-Unabhängigkeit)“, Punkte 1 + 2. Branch `claude-standalone`
(Basis `20919e57`). Regeln: `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`.

## Ist-Zustand
- Fabric-Modul-Targets laufen alle in `integration/run-fabric-263` mit **allen** Mods aus
  `integration/enabled-mods.json` (+ `:framework`/`:common` auf dem Klassenpfad). Ein Modul-Test sieht
  also jedes andere Modul – versteckte Kopplungen fallen nie auf.
- NeoForge-Modul-Runs laden je `loadedMods` Modul + SimpleBuilding (Money/Dimensions: alle Mods im
  Container, SB per `runtimeOnly`), gemeinsames `integration/run-neoforge-263`.
- Testrunner `module_targets` und Launch-Zentrale (`targets.test_targets` = `run.ALL_TARGETS`) leiten
  alles aus `modules.json` `tests` ab; Validierung in `tools/multimod.py validate_tests`.

## Umsetzung
1. **Manifest** `tests.standalone` je Modul mit Tests:
   `{"requires": [<nur harte Modul-Abhängigkeiten>], "devMods": [<harte Libs>], "loaders": {"fabric": {task, report}, "neoforge": {task, report}}}`
   oder `{"exempt": "<Begründung>"}`. Konvention: Fabric `:integration:run<Id>StandaloneGameTest`,
   Report `integration/build/standalone/<id>-junit.xml`; NeoForge `<projekt>:runModuleStandaloneGameTest`,
   Report `modules/<id>/neoforge/build/standalone-junit.xml`.
   Simple Tweaks = erklärtes Add-on → standalone = Tweaks + SimpleBuilding (+ Cloth), ohne Dimensions.
2. **multimod.py**: `standalone` validieren; `requires` ⊆ Modul-`requires` (sonst wäre es kein
   Standalone), Task-Owner wie bei `loaders`, `devMods` aus `tools/devmods.json`.
3. **Fabric** (`integration/build.gradle`, generisch, kein Modul-Block): je Modul eigener Sync-Task
   `prepareStandalone<Id>` (nur Modul-Jar + `standalone.requires` + `standalone.devMods`) nach
   `integration/run-standalone-fabric-263/<id>/mods`, eigene Loom-Run mit eigenem runDir und eigenem
   Source-Set `standalone`, dessen Klassenpfad `:framework`, `:common` und den Integrations-Harness
   herausfiltert → fehlendes Framework-Bündeln (Befund 1) wird sichtbar.
4. **NeoForge** (`gradle/module-neoforge.gradle`, generisch): Run `moduleStandaloneGameTest`,
   `gameDirectory integration/run-standalone-neoforge-263/<id>`, `loadedMods` = eigener Mod +
   `standalone.requires` (nach Auswertung des Modul-Skripts). Grenze: `:framework` bleibt auf NeoForge
   im Dev-Klassenpfad (implementation), Bündeln prüft hier nur Fabric.
5. **Testrunner**: `module_targets` erzeugt `module-<id>-standalone-<loader>-263` (gleicher Katalog/
   `mcLine`, also gleiche erwartete Test-IDs: übersprungene Tests melden „succeed“ mit Hinweis).
   Integrations-Targets unverändert.
6. **Launch-Zentrale**: Targets erscheinen automatisch (ALL_TARGETS); zusätzlich Preset
   „standalone“ (alle Standalone-Targets). Tests in `tools/launchhub/tests/test_mods.py`.
7. **newmod.py** legt `standalone` gleich mit an (neue Module bekommen das Ziel automatisch).
8. **Modul-Tests** (Befund 3/7): SB-/Partner-Teile hinter loader-neutralem Schalter
   (`isModLoaded`-Helfer je Modul über Registry-Namespace bzw. vorhandene Loader-Schalter
   `MoneyLinks.loaded`, `Riding.SIMPLEBUILDING`); fehlt der Partner, `succeed()` mit Hinweis.
   Echte Abstürze/Registry-Fehler allein = Verstoß → im Modul fixen.
9. Doku: `docs/MULTIMOD.md`, `docs/LAUNCHHUB.md`, Prinzipien-Befundliste, Queue, Memory.

## Annahmen (selbst entschieden)
- „isModLoaded“ in `shared`-Testcode = Registry-Namespace-Prüfung (loader-neutral, keine Loader-API in
  `shared`); wo es schon einen Loader-Schalter gibt, wird der genutzt.
- Forge bekommt kein Standalone-Target (Forge-Modul-Läufe bleiben opt-in, Auftrag nennt nur Fabric/NeoForge).
- SimpleBuilding-Kern: `fabric-263`/`neoforge-263` sind bereits Standalone (nur SB + Libs).
- wiringexample hat keine Tests → kein Target.

## Risiken
- Loom-Run mit eigenem Source-Set: Klassenpfad-Filter muss Minecraft/Loader/Fabric API behalten.
- NeoForge: SB per `runtimeOnly` (Money, Dimensions) könnte trotz `loadedMods` geladen werden → im
  Log prüfen; ggf. Abhängigkeit nur für Integrationsläufe.
- Versteckte Modul-Kopplungen tauchen erst im Lauf auf (z. B. Sounds ↔ Visuals).

## Verifikation
Alle `module-*-standalone-*-263` „alles gruen“, bestehende `module-*-fabric-263/-neoforge-263` und
`fabric-263` grün, `python -m unittest discover -s tools/launchhub/tests`, `.\gradlew.bat check -q`.
Kein Client.
