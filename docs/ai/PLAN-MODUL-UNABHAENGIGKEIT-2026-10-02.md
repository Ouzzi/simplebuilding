# Plan: Modul-Unabhängigkeit (2026-10-04, Branch `claude-modprinciples`)

Besitzer-Wunsch: Prinzip, wie jedes Mod auch allein spielbar ist, und diese Prinzipien festhalten.

## Ist-Zustand (erhoben)
- Manifest `modules/modules.json`: nur `simpletweaks` verlangt `simplebuilding` hart (Fabric `depends`,
  NeoForge/Forge `required`) – gewollt, das Modul ist ein Claim-/Alias-Add-on für SB-IDs.
  Alle anderen Module nennen SB höchstens unter `optional`.
- Keine Java-Importe über Modulgrenzen (weder Modul→Modul noch Modul→SB-Implementierung).
  Gemeinsame Verträge laufen über `framework/` (`CosmeticIntensity`, `Protection`, `TransformHints`).
- Fremde IDs in Daten: `simpleriding` (Enderit-Hufeisen-Rezept mit `mod_loaded`, Tags mit
  `required:false`), `simplemoney` (238 SB-Trades + Fun/Tweaks/Dimensions-Preise hinter
  `simplemoney:config links:<mod>` = Config **und** `isModLoaded`), Sandwiches (Branch, Tags `required:false`).
- Kern (SB) referenziert kein Modul.
- Lücken: (a) `simplequalityoflife` nutzt `framework` (`TransformHints`), bündelt es aber auf Fabric/NeoForge
  nicht (`include`/`jarJar` fehlt) → allein installiert NoClassDefFoundError beim Hacken-Hinweis.
  (b) Forge-/NeoForge-Testadapter von fun/models/money/riding/visuals nutzen die Teststruktur
  `simplebuilding:empty`. (c) Kein Testrunner-Target „nur dieses Modul + Vanilla“; alle Modul-Suites
  laden SB (`tests.requires`) und mehrere Tests setzen SB voraus.
  (d) Manifest-`optional` und Loader-Metadaten (`suggests`/`type="optional"`) laufen auseinander.

## Umsetzung
1. `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md` (Regeln, Beispiele, Abhängigkeitstabelle, Befundliste).
2. Direkte Fixes: (a) QoL `include`/`jarJar project(':framework')`; (b) Testadapter auf eigene
   `<modul>:empty` (Money bekommt eine Kopie der leeren Struktur).
3. Größere Punkte als Queue „Modul-Unabhängigkeit“ in `.claude/QUEUE.md`.
4. Verweis in `AGENTS.md`/`CLAUDE.md`, Memory-Eintrag.

## Risiken
- `jarJar`/`include` ändert nur Release-/Dev-Jar-Inhalt; Muster identisch zu sounds/visuals/dimensions.
- Teststruktur-Wechsel: `simpledimensions` nutzt das Muster auf Forge bereits erfolgreich.

## Verifikation
- `module-simplequalityoflife-fabric-263`, `-neoforge-263`; Jar-Inhalt (`META-INF/jars`, `META-INF/jarjar`) prüfen.
- `module-simplemoney-neoforge-263`, Forge-Targets der fünf Module (soweit lauffähig), `gradlew check -q`.
