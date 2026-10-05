# Prinzipien: Jedes Modul allein spielbar (verbindlich)

Besitzer-Wunsch 2026-10-04: Die Mods sollen sich auch einzeln genießen lassen, obwohl sie Dinge
voneinander nutzen. Diese Regeln gelten für SimpleBuilding (Kern) und jedes Modul unter `modules/`
(auch neue wie Simple Sandwiches). Technische Grundlage: `docs/MULTIMOD.md`. Stand der Erhebung:
Branch `claude-modprinciples` (Basis `2a01fea6`).

## Die Regeln

1. **Allein voll spielbar.** Jedes Modul startet und ist komplett spielbar mit nur Vanilla + seinen
   harten Bibliotheken (Fabric API, Cloth Config). Jedes Item/Block hat dann einen Bezugsweg
   (Rezept, Loot, Handel) aus Vanilla-Material. Ein Partner-Mod macht es *besser*, nie *erst möglich*.
2. **Harte Abhängigkeit nur als erklärtes Add-on.** Ein Modul darf ein anderes nur dann `required`
   machen, wenn es inhaltlich ein Add-on dafür ist – dann identisch in `modules.json` (`requires`),
   `fabric.mod.json` (`depends`), `neoforge.mods.toml`/`mods.toml` (`type="required"`/`mandatory=true`)
   und im Wiki genannt. Heute einziger Fall: `simpletweaks` → `simplebuilding` (Alias-/Claim-Add-on).
3. **Keine Klassenimporte über Modulgrenzen.** Kein `import` aus fremden Implementierungspaketen
   (`com.simplebuilding.*` außer `framework.api`, `com.simplefun.*` …). Gemeinsames Verhalten läuft
   über `framework/` (reine Java-Verträge, keine Registry-Inhalte) oder über öffentliche Registry-IDs
   + Vanilla-Schnittstellen (`Container`, Tags, Komponenten). Wer `framework` nutzt, **bündelt es selbst**
   (Fabric `include project(':framework')`, NeoForge `jarJar project(':framework')`, Forge über
   `gradle/forge-framework.gradle`) – sonst stürzt das Modul allein ab.
4. **Fremde Inhalte nur bedingt.** Jede Datei, die eine fremde ID nennt, ist abgesichert:
   - Rezepte/Loot/Handel: `fabric:load_conditions` `fabric:all_mods_loaded` + `neoforge:conditions`
     `neoforge:mod_loaded` (+ Forge-Form) oder eine eigene Condition, die `isModLoaded` prüft.
   - Tags: fremde Einträge immer `{"id": "...", "required": false}`.
   - Java: Schalter aus `isModLoaded`/`ModList.isLoaded` beim Start (Loader-Adapter setzt, `shared` liest),
     nie `Class.forName` auf fremde Klassen in Spielcode.
5. **Vanilla-Fallback zuerst, Partner als Upgrade.** Das Grundrezept nutzt Vanilla-Material; mit Partner
   kommen zusätzliche Wege oder höhere Stufen dazu. Beispiel Besitzer: der Crucible (Simple Sandwiches)
   wird ohne SimpleBuilding mit der **Axt** gebaut; ist SB da, gibt es zusätzlich eine bedingte Variante
   mit `#simplebuilding:sledgehammer_tools`. Höhere Tiers erscheinen nur mit Partner und werden ohne ihn
   gar nicht registriert (Vorbild `Horseshoes.register(simplebuilding)`: Enderit-Hufeisen nur mit SB).
6. **Geteilte Grundstoffe: ein Besitzer, Zugriff über Tags.** Keine gemeinsame „Core“-Jar mit Items und
   keine Doppel-Items. Ein Material (Enderit, Netherit-Nugget, Hammer) gehört genau einem Mod. Dieser
   veröffentlicht es in `c:`-Tags (SB: `c:ingots/enderite`, `c:nuggets/enderite`, `c:nuggets/netherite`)
   bzw. eigenen Werkzeug-Tags (`simplebuilding:sledgehammer_tools`). Andere Module verwenden **den Tag**,
   bedingt nach Regel 4, und haben immer einen Vanilla-Weg. Braucht ein Modul allein einen Grundstoff
   zwingend, nimmt es eine Vanilla-Entsprechung (Eisen-Nugget, Stock, Axt) statt eines Eigenbaus.
7. **Wiki/Guide sagen, was fehlt.** Jede Funktion, die einen Partner braucht, ist im Modul-Wiki als
   „mit SimpleBuilding“ / „requires X“ markiert (Vorbild `simpleriding/wiki/manual.json`: „(with
   SimpleBuilding) Enderite horseshoes“). Optionale Partner stehen in `modules.json` `optional` **und**
   als `suggests` (Fabric) / `type="optional"` (NeoForge/Forge).
8. **Beweis durch Test: Standalone-Target.** Jedes Modul hat ein Testziel „nur dieses Modul + Vanilla
   (+ harte Libs)“. Tests, die einen Partner brauchen, prüfen vorher `isModLoaded` oder liegen im
   Integrations-Katalog. Teststrukturen gehören dem Modul selbst (`<modul>:empty`, nicht `simplebuilding:empty`).

## Abhängigkeiten heute (Modul × wer braucht es)

| Modul | hart benötigt | optional genutzt von (Weg) |
|---|---|---|
| simplebuilding (Kern) | Cloth Config | simplemoney (238 Trades + Preise, `links:simplebuilding`), simpleriding (Enderit-Hufeisen/-Rüstung, `mod_loaded` + Tags), simplesandwiches (Netherit/Enderit-Äpfel, Tags), simpletweaks (**hart**, Add-on) |
| framework (API, keine Items) | – | simpledimensions, simplequalityoflife, simplesounds, simpletweaks, simplevisuals (gebündelt) |
| simplevisuals | Cloth Config | simplesounds (`CosmeticIntensity`, framework) |
| simpledimensions | Cloth Config | simpletweaks (Claims via `framework Protection`), simplemoney (Preise) |
| simplefun | Cloth Config | simplemoney (Köpfe/Schneebälle-Trades, `links:simplefun`) |
| simpletweaks | SimpleBuilding | simplemoney (Preise) |
| simplemoney, simpleriding, simplequalityoflife, simplemodels, simplesounds | Cloth Config (models: keine) | simplemoney (Preise je Modul) |

Kern → Modul: keine Referenz (sauber). Modul → Modul per Java-Import: keine (sauber).

## Befundliste (Stand 2026-10-04, priorisiert)

| # | Prio | Befund | Datei | Fix |
|---|---|---|---|---|
| 1 | P1 | QoL nutzt `framework` (`TransformHints`), bündelte es aber auf Fabric/NeoForge nicht → allein `NoClassDefFoundError` | `modules/simplequalityoflife/shared/java/com/simplequalityoflife/event/HoeHarvestHint.java:3`, `fabric/build.gradle`, `neoforge/build.gradle` | **gefixt**: `include`/`jarJar project(':framework')` |
| 2 | P1 | Kein Standalone-Testziel: alle Modul-Targets laden SB (`modules.json` `tests.requires`) | `modules/modules.json` (`tests`), `tools/testrunner/run.py:289` `module_targets`, `integration/build.gradle`, `modules/*/neoforge/build.gradle` (`loadedMods`) | **gefixt** (`claude-standalone`): `tests.standalone` je Modul → `module-<id>-standalone-{fabric,neoforge}-263`, eigener Mods-/Laufordner, Fabric ohne `:framework`/`:common` im Klassenpfad; `run.py --targets standalone`, Hub-Preset |
| 3 | P1 | Modul-Tests setzen SB unbedingt voraus | `QolTests.java:44,90,127-129`, `MoneyTests.java:43,104,174`, `LinkTests.java:30`, `ModelTests.java:41,183`, `DimensionTests.java:64,114,131`, `FunTests.java:436`, `RidingTests.java:171,173`, `HorseshoeTests.java:73,95` | **gefixt** (`claude-standalone`): SB-Teile hinter `isModLoaded("simplebuilding")` (Registry-Namespace) bzw. `Riding.SIMPLEBUILDING`, sonst `succeed` mit `[standalone]`-Logzeile oder Vanilla-Ersatz (Shulker-Kiste, Eisenbarren, Netherit-Nautilusrüstung); Tweaks-Portaltest hinter Dimensions-Prüfung |
| 4 | P2 | Forge/NeoForge-Testadapter nutzten `simplebuilding:empty` | `modules/{simplefun,simplemodels,simplemoney,simpleriding,simplevisuals}/forge/.../ModuleForgeTests.java:17`, `modules/simplemoney/neoforge/.../MoneyGameTests.java:16` | **gefixt**: eigene `<modul>:empty` (Money bekam `data/simplemoney/structure/empty.nbt`) |
| 5 | P2 | Optionale Partner (`modules.json optional`) fehlen in Loader-Metadaten: Riding, Money, QoL, Models, Visuals (SB/Partner nirgends), Dimensions (nur Fabric `suggests`), Sounds (SB fehlt; Visuals korrekt) | `modules/*/fabric/src/main/resources/fabric.mod.json` (`suggests`), `modules/*/neoforge/.../neoforge.mods.toml` | Queue: `suggests` + `type="optional"` aus `modules.json optional` erzeugen; `tools/multimod.py` prüft Gleichstand |
| 6 | P3 | Riding nutzt feste ID `simplebuilding:enderite_ingot` statt `#c:ingots/enderite` | `modules/simpleriding/generated/.../recipe/enderite_horseshoe_smithing.json`, `tags/item/repairs_enderite_horseshoe.json` | Queue: auf `c:ingots/enderite` umstellen (Condition bleibt) |
| 7 | P3 | QoL-Test erkennt SB per `Class.forName("com.simplebuilding.Simplebuilding")` | `QolTests.java:160` | **gefixt**: `isModLoaded("simplebuilding")` |
| 8 | P1 | Forge-Konverter schrieb `neoforge:mod_loaded` in `forge:condition` → Forge-Registry-Ladefehler beim optionalen Enderit-Hufeisen-Rezept (Riding-Forge-Lauf brach ab, auch **mit** SB) | `gradle/module-forge.gradle` (processResources), `modules/simpleriding/generated/.../recipe/enderite_horseshoe_smithing.json` | **gefixt**: vorhandene `forge:conditions` bevorzugen, sonst `neoforge:mod_loaded` → `forge:mod_loaded` |
| 9 | P1 | Simple Sandwiches nutzt `framework` (`TransformHints`), bündelte es nicht → allein `ClassNotFoundException` beim Start (vom Standalone-Target gefunden) | `modules/simplesandwiches/{fabric,neoforge}/build.gradle` | **gefixt**: `include`/`jarJar project(':framework')` (Forge bündelt generisch) |
| 10 | P2 | Money/Dimensions NeoForge hatten SB als `runtimeOnly project(':mc26_3:neoforge')` → SB lud immer mit, auch ohne Auswahl (Integrationsläufe bewiesen nichts über „optional“) | `modules/{simplemoney,simpledimensions}/neoforge/build.gradle` | **gefixt**: SB nur über den Mods-Container |
| 11 | P3 | Visuals-/Sounds-Tests werfen `AssertionError` statt `GameTestAssertException` → NeoForge-Testserver stürzt bei Rot ab statt Test rot zu melden | `VisualsTests`, `SoundTests` | **gefixt** (`gpt-befunde`): alle Bedingungen über `GameTestHelper.assertTrue`, direkte Fehlerpfade über `fail`; keine `AssertionError` mehr |
| 12 | P3 | Riding `horseshoe_handling` ist auf das volle Enderit-Set geeicht → läuft standalone nur als „übersprungen“ | `HorseshoeTests.handling` | **gefixt** (`gpt-befunde`): dieselben Handling-/Sync-/Mixin-Prüfungen für Kupfer, Eisen, Gold, Diamant und Netherit; Enderit zusätzlich nur mit `Riding.SIMPLEBUILDING` |

Standalone-Stand 2026-10-05: alle 20 Targets (10 Module × Fabric/NeoForge) „alles gruen“ 566/566.
Befunde 11/12 verifiziert 2026-10-05 (`gpt-befunde`): Visuals, Sounds und Riding jeweils
mit SB und standalone auf Fabric/NeoForge 26.3: **alles gruen: 296/296 bestanden, 0 rot**
(12 Targets, Lauf `2026-10-05T00-54-26Z-0f77`).

NeoForge-Grenze: `framework` liegt dort im Dev-Klassenpfad (`implementation`), fehlendes Bündeln fällt nur
auf Fabric auf.

## Checkliste für neue Features/Module
- Geht es mit nur diesem Modul + Vanilla? Wenn nein: Vanilla-Fallback oder Feature hinter Partner-Schalter.
- Fremde ID genannt? → Condition / `required:false` / Tag statt ID.
- Fremde Klasse gebraucht? → `framework`-Vertrag (und bündeln) oder öffentliche ID.
- Wiki-Notiz „mit X“ und `optional`/`suggests` eingetragen?
- Standalone-Test grün (sobald Target existiert), Integrations-Test für die Kopplung.
