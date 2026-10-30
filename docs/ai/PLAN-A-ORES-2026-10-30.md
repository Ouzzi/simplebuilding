# Plan A31: Simple Ores (ABN30-10, Agent a-ores31)

Stand 30.10.2026. Nur MC 26.3 Fabric. Konzept: `docs/ai/KONZEPT-A-RG30-2026-10-30.md` (Wahl des Besitzers: Queue ABN30-10). Neues Sub-Mod `simpleores` (Muster `simpleanimals`/`simpledinos`).

## 1. Ist-Zustand
- `simpleores` gab es nicht; Roségold/Silber waren nur Konzept. Vorbild für Aufbau, Wiring, Tests, Generatoren: `simpleanimals`, `simpledinos` (Worldgen als Datenpaket + Fabric `BiomeModifications`/NeoForge `biome_modifier`).
- Werkzeug-/Rüstungsmaterial im Code: `ModToolMaterials`/`ModArmorMaterials` (Enderit), `HammerTiers` (sieben Stufen, Werte Vanilla 26.3: `ToolMaterial.IRON` 250/6/2/14, `GOLD` 32/12/0/22, `ArmorMaterials.IRON` Faktor 15, Schutz 2/6/5/2, Verz. 9; `GOLD` Faktor 7, 2/5/3/1, Verz. 25). Gegen die Klassen geprüft über Gametests (`tool_values`, `armor_values` vergleichen mit `ToolMaterial.IRON/GOLD`, `ArmorMaterials.IRON`).
- Hochofen 26.3: das Rezept speichert die Ofenzeit, der Hochofen halbiert sie (`CookingChecks`): 600 Ticks Hochofen = `cookingtime` 1200.
- Texturen: alle aus Vanilla-Gold abgeleitet, also Build-Ableitung (`vanilla_derived.MODULE_OUTPUTS`), nichts eingecheckt (AGENTS Regel 7).

## 2. Entscheidungen des Besitzers (ABN30-10) und Umsetzung
| Punkt | Umsetzung |
|---|---|
| Alles in Simple Ores, Hammers/Tools/Rüstung nur wenn Ores geladen | Werkzeuge + Rüstung in Ores; Hammers: zwei `HammerTiers` mit `requiredMod = "simpleores"`, nur dann registriert (`enabled()`), Rezepte/Fortschritte mit Lade-Bedingung; kein `requires`, Hammers kopiert die Werte (importiert Ores nie). Simple Tools hat keine Tiers (Konzept 0) |
| Farben | hart = Palette B „Kupfer-Rosé“, weich = Palette A „Zart-Rosa“, aus Gold per Luminanz-Rampe umgefärbt (`tools/textures/ores_2026_10_30.py`) |
| Namen | Rosenbronze (hart), Blütengold (weich), Pressling |
| Rezepte (Barren) | Empfehlung des Konzepts: hart 6/5/4 Gold + 2 Kupfer + 1 Silber, weich 7 Gold + 1 Kupfer + 1 Silber(barren) oder Silberklumpen; 1 Pressling je Rezept (Besitzer 30.10. korrigiert) (Empfehlung „4“) |
| Hochofen | nur Hochofen (kein Ofen/Räucherofen/Lagerfeuer), 30 s, Ausbeute je Pressling immer 1 Barren |
| Silber | Overworld 1–2 Adern je Chunk (Größe 5, Y -48..16, Tiefenschiefer-Variante), End-Silbererz (äußere Inseln, 2–6 Klumpen); KEIN Werkzeugmaterial |
| Werte | Konzept §3: hart Eisen-Haltbarkeit 250/Tempo 11/Verz. 20, Rüstung 15 · 2/4/3/1 · Verz. 22; weich 225/12/22, Rüstung 13 · 2/5/3/1 · Verz. 25. Mining-Stufe Eisen (selbst entschieden: sonst wäre Gold-Stufe zu schwach für das knappe Silber; kein Tier schlägt Diamant in der Haltbarkeit) |
| Hammer | Angriff 5, Tempo −2,8, Haltbarkeit 250/225, Schlagabstand 23/22 |

Selbst entschieden (Begründung): Legierungsblöcke (Beacon-/Baunutzen, Speicher) nur als reine Bausteine ohne Beacon-Basis (A12: kein Eingriff); kein Silber-Hammer; Handbuch-Rezept Buch + Rohgold.

## 3. A12 je Teil
| Teil | Vanilla-Nutzen | Änderung | Eingriff |
|---|---|---|---|
| Silbererz (Oberwelt, End) | – | neues Erz, seltener als Gold | gering |
| Roségold, Pressling | – | Rezept-Kette Barren → Pressling → Hochofen | gering (Vanilla-Rezepte unverändert) |
| Werkzeuge, Rüstung | Gold-Alternative | zwei Zwischenstufen Gold/Eisen | gering |

## 4. Dateien
`modules/simpleores/**` (neu; Daten per `tools/gen_data.py`), `modules/simplehammers/**` (HammerTiers, Registry, Tests, Daten), `modules/simplemoney/.../money/simpleores/prices.json`, `tools/textures/{ores_2026_10_30,ores_derived,ores_hammers_derived,vanilla_derived,mod_icons,module_guide_books_2026_10_05}.py`, `tools/{guides/module_guides,discover/discover,audit/module_purposes}`, SB-Wiring (`modules.json`, `integration/enabled-mods.json`, `mc26_3/*/build.gradle`, `fabric.mod.json`, `neoforge.mods.toml`, `SimplebuildingConfig`, `ConfigOptions`, `TcContext`, `ConfigOptionTests`, Lang), `docs/MODS-UND-FEATURES.md`, `.gitignore`.

## 5. Verifikation
Billig lokal: `modules/simpleores/tools/check_data.py`, `tools/check_*.py`. Ein `sbt`-Lauf: Generatoren (Guide, Wiki, Discover, Texturen) → `-gen`, Compile `:mc26_3:fabric` + `:modules:simpleores:fabric` + `:modules:simplehammers:fabric`, Targets `module-simpleores-fabric-263`, `module-simpleores-standalone-fabric-263`, `module-simplehammers-fabric-263`, `./gradlew check -q`.
Gametests Ores: Registrierung, Silber kein Werkzeugmaterial, Modul-Schalter, Werkzeugwerte, Rüstungswerte, Press-Rezepte, Pressling nur im Hochofen/1 Barren/600 Ticks, Silber-Schmelzen, Erz-Drops, Weltgen-Häufigkeit (seltener als Gold), Handbuch, Discover. Hammers: `rose_gold_optional`.

## 6. Nur Konzept (nicht gebaut)
Rezept-Auswahl mit Checkboxen, neue Nutzen für Zink, Nickel (neu), Platin, Blei (Woll-Nutzen, explosionsfest, Schalldämmung), weiterer Silber-Nutzen, Silberspiegel + Silberpfeil (Simple Arrows Ist-Stand): `docs/ai/KONZEPT-A-ORES31-2026-10-30.md` und Vorschau `/root/sb-tools-a/previews/a-ores31-preview.html`.

## 7. Risiken / Rest
Texturen: Probelauf gegen das echte Client-Jar steht aus (lokal nur mit synthetischem Jar geprüft); Optik erst nach Besitzer-Sicht. NeoForge/Forge nicht kompiliert (nur 26.3 Fabric). Client-Sicht (Rüstungsschicht, Block-Optik) offen.
