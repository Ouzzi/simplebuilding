# PLAN-B-HAMENDERITE27 (11.11.2026) – Enderit-Hammer an Simple Ores (prio27 #9, RELEASE-CHECK TODO)

Branch `b-hamenderite27`, Basis `hub/b-bundle179-gen` 089fdc551. Nur 26.3 Fabric.

## Ist-Stand
- `SimpleHammers.ENDERITE_NEEDS_SIMPLE_ENDERITE = false` + `TODO(Simple Enderite, stage P4)` (SimpleHammers.java:48): Ziel war das nie gebaute Simple Enderite. Das Flag wurde nirgends gelesen (grep: nur die Deklaration).
- Enderit liefert heute Simple Ores (MACH32-ENDSYS). Das Schmiede-Rezept `enderite_sledgehammer_smithing` und seine Freischaltung tragen bereits `fabric:all_mods_loaded simpleores` / `neoforge:mod_loaded simpleores`; SimpleLibs `CreativeTabPolicy.isEnderiteVariant` blendet den Hammer aus, solange kein Mod Enderit liefert (SimpleLib-Test `enderite_only_with_ores`).
- Handbuch (manual.json) sagte noch „bleibt herstellbar, bis es Simple Enderite gibt“.

## Entscheidung
Flag streichen, nicht umstellen: die Bedingung existiert schon in den Daten (Modul-Unabhängigkeit: Enderit-Stufe nur bedingt mit Simple Ores). Ersetzt durch die Konstante `ENDERITE_MOD = "simpleores"` mit Kommentar, wo die Bedingung wirkt. Das Item bleibt registriert (alte Welten behalten ihre Enderit-Hämmer).

## Umsetzung
- SimpleHammers.java: Flag + TODO weg, `ENDERITE_MOD`, Kommentar F3 aktualisiert.
- Gametest `enderite_needs_ores` (HammerTests + Fabric-Adapter): Rezept und Rezept-Freischaltung genau dann da, wenn Simple Ores geladen ist; Item registriert.
- manual.json EN/DE neu formuliert, wiki/data (simplehammers.*, modules.js-Hash) mit `python3.12 wiki/generate.py --all` neu erzeugt (Dimensions-Datei-Drift des Generators verworfen).
- RELEASE-CHECK-2026-11-08.md Z.100 als erledigt vermerkt.

## Tests
- Lokal: javac SimpleHammers/HammerTests gegen MC 26.3 ohne neue Fehler; `wiki/generate.py --all --check`; precheck --fast.
- sbt: Compile Kern + simplehammers, `module-simplehammers-fabric-263` (mit Simple Ores im Super-Mod-Ziel?) und `module-simplehammers-standalone-fabric-263` (ohne Simple Ores) → beide Zweige des Tests.
