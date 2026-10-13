# Plan: Modulaufteilung SimpleBuilding / Simple QoL / Simple Aesthetics (2026-10-11)

Auftrag Besitzer 11.10. Grundlagen: `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md` (Regeln 1–8, 5a, 6a),
`docs/ai/KONZEPT-SUPERMOD-SUBMOD-2026-10-06.md`, Vorbild `docs/ai/PLAN-QOL-AUFTEILUNG-2026-10-10.md`
(QoL bündelt Weather, `framework.api.SubMods`), `docs/ai/PLAN-N18-SIMPLEMAPS-TRIMS-2026-10-07.md` (Trims: ID-Wechsel mit
Migration), `docs/ai/PLAN-CRUCIBLE-2026-10-04.md`. Basis der Erhebung: `claude-wave1` 71b2d9f2e (nur git grep, kein Gradle).
Nur Plan, kein Produktivcode. **Umsetzung vom Besitzer freigegeben (11.10.)** mit den Zusätzen: gemeinsame Materialien an
eine Stelle (§2.5), Axt/Hammer-Prinzip und gleicher Stil überall (§2.6), Enderit selbst entscheiden (§2.5), jede Stufe
einzeln gate-grün nach master mit Migrations- und Standalone-Tests (§4).

**Besitzer-Entscheidungen (fest):** neue Namespaces `simpleoctets:`, `simplehammers:`, `simplestorage:`, `simpleovens:`,
`simplecrucibles:` mit Migration alter Welten (Items, Blöcke in Chunks, Block-Entities, Rezeptbücher, Advancements,
Configs); Simple Tweaks von SB entkoppeln; keine Funktion darf brechen; jede Sub-Mod allein spielbar.

---

## 1. Ist-Analyse

### 1.1 Größe SB heute

- `common/src/shared/java/com/simplebuilding`: 815 Java-Dateien (davon gametest 103, mixin 116, tweaks 95, util 85,
  client 77); 26.3-Overlay `mc26_3/overlay/java`: 43; 26.2-Overlays `common/src/mc26_2`.
- Lang `en_us`: 800 Item-, 456 Block-Schlüssel (3672 Schlüssel gesamt). Rezepte generiert: 671
  (`mc26_3/generated/data/simplebuilding/recipe`). Texturen: 724 PNG.
- Registrierung zentral: `blocks/ModBlocks`, `items/ModItems`, `src/main/java/.../ModBlockEntities`,
  `ModScreenHandlers`, `ModRecipes`, `ModMessages` + `networking/ModMessageHandlers` (26 KB, alle Pakete),
  `component/ModDataComponentTypes`, `entity/ModEntities`, `util/ModRegistries`, `util/ModTags`.

### 1.2 Inventar je künftiger Sub-Mod

| Ziel | Klassen (ca.) | Items / Blöcke / BE / Menüs | Rezepte | Texturen | Lang | Kern-Dateien |
|---|---|---|---|---|---|---|
| **Octets** | 7 + `util/MaterialOctets` (+2 Tests) | ~117 Items, ~106 Blöcke (12 Holz, ~92 Material, Melone, Schach-Octets je Farbe), 0 BE | 377 (Steinsäge, 8 je Basisblock) + 377 Rezept-Advancements | 0 eigene im Spiel (Modelle nutzen Basistexturen), 119 Wiki-Renders, 104+104 Modelle | ~230 Zeilen | `blocks/OctetMaterials` (**generiert** von `tools/textures/octets_2026_10_09.py` + `octet_materials.json`), `MaterialOctetBlock`, `CheckerOctetBlock`, `OctetCellBlock`, `MaterialOctetItem`, `CheckerOctetItem`; Registrierung `ModBlocks` 474–560/722, `ModItems` 210, `chess/ChessItems` 34–97 |
| **Hammers** | 13 Kern (`SledgehammerItem`, `util/Sledgehammer*` ×8, `HammerCorners`, `DiscFlips`, `InWorldTransformations`, `TransformTargets`, `PlacedTemplates`, `data/SledgehammerUpgradeData`) + 4 Compat (`InWorldRecipeCatalog`, `CauldronWorldCatalog`, JEI/REI `InWorld*Category`) | 7 Items (Stein…Enderit), 0 Blöcke | 10 + 3 `sledgehammer_upgrades/*.json` (netherite, enderite, reinforced) + 9 Advancements `advancement/hammer` + 2 Dorfbewohner-Trades | 7 | ~75 EN | Tag `simplebuilding:sledgehammer_tools` (`ModTags.Items.SLEDGEHAMMER_ENCHANTABLE`), Hammer-Verzauberungen `ModEnchantments` 169–197 |
| **Storage** | ~93 (Blöcke, BE, Menüs, Screens, Renderer, 4 Fahrzeugklassen, 13 Utils, 2 Rezeptklassen, 13 Mixins, 3 Pakete) | ~50 Items, 36 Blöcke (3 Truhen, 3 Fallen-Truhen, 8 Kupfer-Fallentruhen, 3 Shulker, 4 Rucksäcke, 12 Kisten, Lager-Werkbank, platziertes Bündel, Enderit-Fass), 7 BE, 3 Menüs, Bündel ×3, Köcher ×4, Kisten-Boote/-Loren ×6 | ~70 | ~170 (obere Grenze) | ~200–250 | `ChestTier`, `TieredChest*`, `TieredShulkerBox*`, `Backpack*`, `ReinforcedBundle*`, `QuiverItem`, `woodwork/Crate*`, `TieredStorageVehicle` |
| **Ovens** | 11 | 9 Blöcke/Items (Verstärkt/Netherit/Enderit × Ofen/Räucherofen/Hochofen), 3 BE, 0 Menüs (Vanilla-Menü + `AbstractFurnaceMenuMixin`), Ofen-Loren | 4 + Schmiede-Stufen | ~48 | ~45 | `ModFurnaceBlock`, `ModBlastFurnaceBlock`, `ModSmokerBlock`, `*BlockEntity`, `FurnaceTierPerks`, `TieredFurnaceMinecart`, Tags `furnace_bonus_excluded`, `blast_furnace_bonus` |
| **Crucibles** (SB-Teil) | `crucible/CrucibleCompat` (26.3-Overlay, 202 Z.), Seelenlava 3 + `SoulLavaModel` + 3 Mixins + 2 Loader-Glue, `CrucibleRecipeCatalog`, `CauldronWorldCatalog`, JEI `CrucibleCategory` | 2 (Enderit-Tiegel, Enderit-Fass) + Seelenlava-Fluid/-Block + 3 Eimer-Varianten | 0 (In-World) | ~10 | – | registriert über `SimpleLibApi.newCrucible/newBarrel` |
| **Crucibles** (simplelib-Teil) | 16 (`crucible/` 14, `cauldron/` 2) + 4 Client + 6 Mixins | 8 Blöcke (3 Tiegel, Rohling, verstärkter Kessel, 3 Fässer), 7 Items, 2 BE, 2 Menüs | 1 (`copper_barrel`), Rest In-World (`AxeWays`, `CrucibleUpgrades`) | 37 | – | `simplelib:*`, gebündelt von SB, Sandwiches, QoL, Interfaces, Maps |

Nicht eindeutig zugeordnet (Entscheidung in §2.1): Upgrade-Vorlagen `basic_upgrade_template`/`enderite_upgrade_template`
(`ModItems` 317–321, genutzt von Werkzeugen, Rüstung, Rucksack, Köcher, Kisten-Fahrzeugen, Ofen-Lore, platzierten Vorlagen),
Trichter (`ModHopperBlockEntity`, `TieredHopperMinecart`, kennt Truhen + `OversizedStacks`), Fahrzeug-Stufen (`VehicleTiers`,
`ModEntities`), Kupfer-/Enderit-Eimer (`ModBucketItem`), Enderit als Material.

### 1.3 Was schon in SimpleLib liegt

`modules/simplelib` (0.2.0, 60 Klassen): Tiegel/Fass/verstärkter Kessel (Registry-Inhalt, Regel 6a), Warm-Essen,
`api/SimpleLibApi` (Fabriken `newCrucible`, `newBarrel`, `registerToolUse`, `disableAxeWays`, `registerCauldronBucket`,
`setExtremeCauldronEffect`), `api/InWorldStrikes` (437 Z., wachsende Zielvorschau; SB 26.3 delegiert über 84-Zeilen-Hülle,
26.2 behält eigene Kopie), `api/StackLimits`, UI-Bausteine (`api/client/ui`), Dorf-Küche. `framework/api` (inhaltsfrei):
`SubMods`, `CreativeTabSettings`, `ModuleQuestDefaults`, `TransformHints`, `Protection`, `ConfigFiles`, `ContainerStyleHints`.

### 1.4 Abhängigkeitsgraph (wer ruft wen)

```
                 ┌──────────── Astral-Verzauberer (bleibt SB) ──uses──► SledgehammerUpgrades
                 │             EnchantingTableBlockMixin ──uses──► SledgehammerUpgrades
Hammers ◄──instanceof SledgehammerItem── Öfen, Truhen, Shulker, Trichter, PlacedTemplate/SmallParts,
   │                                    DimensionalScrap, 8 Mixins, Outline/Highlight-Renderer, Guide,
   │                                    MiningUtils, Strip/VeinMiner, NihilVault, ChunkLoader, Testzentrale
   │──SledgehammerUpgrades-Tabelle──► Truhen/Shulker (TieredChests.upgradeInPlace), Öfen, Tiegel/Fass
   │                                   (CrucibleCompat.enderiteCrucible/-Barrel), Astral-Tisch
   │──HammerCorners/cellFor──► Octets (MaterialOctetBlock, OctetCellBlock)   ◄─┐ ZYKLUS H↔O
Octets ──(Tests, Item-Use)──► Hammer ──────────────────────────────────────────┘
Storage ──► SB-Kern: Upgrade-Vorlagen, ModTags, Komponenten (BackpackContents, ContainerCounts),
            Verzauberungen (Lager-Verzauberungen), ModMessageHandlers, Tabs        ◄─ ZYKLUS S↔Kern↔H
Ovens   ──► Upgrade-Vorlagen (Ofen-Lore), Hammer-Aufwertung                     ◄─ ZYKLUS O↔H
Crucibles(SB) ──► simplelib API; Hammer-Weg; Enderit; Seelenlava; Eimer (SB)    ◄─ ZYKLUS C↔H (über Tabelle)
simplelib ──Tag/ID──► simplebuilding:sledgehammer_tools, simplebuilding:enderite_barrel (Tests)
simpletrims ──Tag──► simplebuilding:sledgehammer_tools
simplemoney ──IDs──► 275 simplebuilding:-Einträge in money/simplebuilding/prices.json, 29 Trade-Links Storage/Ofen, 7 Hammer
Bauzauberstäbe (SB) ──Konstanten──► DURABILITY_*_SLEDGEHAMMER
Tabs: items/ModItemGroupsContent + items/SearchTabPlacement (Zeilen 100–199 Octets, 444 Vorlagen) kennen alles
Testzentrale: dev/testcentre/{FeatureStations, TestCentreSections, TestCentreKits, TestCentreLayout}
Guide: guide/GuideContent, client/guide/*; Wiki: wiki/manual.json (SB-Kapitel)
JEI/REI/Jade: common/src/{jei,rei,jade} (InWorld-, Crucible-, Cauldron-Kategorien, RecipelessJeiInfo, BlockInfo)
```

**Zyklen und Auflösung**

| Zyklus | Auflösung |
|---|---|
| Hammer → Upgrade-Ziele (Storage/Ovens/Crucibles/Astral) → Hammer | **SimpleLib-Datentyp `simplelib:in_world_upgrades`** (Ablösung von `simplebuilding:sledgehammer_upgrades`, `SledgehammerUpgradeData`): jede Mod liefert ihre Einträge als JSON in eigenem Namespace (`from`, `to`, `material`, `strikes`, `tool_tag`, Komponenten-Übernahme-Regel). Die Ausführung (Fortschritt, Blockwechsel mit BE-Inhalt) liegt in SimpleLib (`InWorldStrikes` existiert schon); BE-Übernahme über Vanilla-`Container` bzw. `SimpleLibApi.registerUpgradeHandler(blockTag, handler)`. Werkzeug = Tag `#simplelib:upgrade_tools`: Hammers trägt seine Hämmer ein, ohne Hammers gilt die Axt (Regel 5a, `AxeWays` vorhanden). |
| Hammer ↔ Octets (`HammerCorners` schnitzt Octet-Zellen) | SimpleLib-Dienst `OctetCells` (`register(Block base, Block octet, Block cell)`, `cellFor`); Octets registriert, Hammers fragt ab. Ohne Octets schnitzt der Hammer nicht (heutiges Verhalten, wenn kein Octet existiert). |
| Kern-Code `instanceof SledgehammerItem` (≈40 Stellen) | Tag `#simplehammers:sledgehammers` + Komponente `simplelib:hammer_tier` (oder `Tool`-Komponente) statt Klasse; Hilfsfunktion `SimpleLibApi.isHammer(stack)`. |
| Storage/Ovens → Upgrade-Vorlagen (SB) | **Entschieden:** Vorlagen nach `simplelib` (`simplelib:basic_upgrade_template`, `simplelib:enderite_upgrade_template`, Alias), §2.5. |
| Storage → `ModMessageHandlers`/`ModDataComponentTypes`/Lager-Verzauberungen | Pakete, Komponenten und lager-spezifische Verzauberungen wandern mit; der zentrale Handler wird je Mod aufgeteilt (eigene Payload-Registrierung je Loader). |
| Enderit-Stufen in Hammers/Storage/Ovens/Crucibles brauchen SB-Material | **Entschieden:** Enderit-Grundmaterial nach `simplelib` (§2.5); Enderit-Stufen der Sub-Mods werden immer registriert (keine SB-abhängige Registry → keine verschwindenden Blöcke), Rezepte über `#c:ingots/enderite`/`#c:nuggets/enderite`. |
| Tabs/Suchtab kennen alle Items | Jede Sub-Mod eigener Tab (`framework.api.CreativeTabSettings`); SB-`SearchTabPlacement` ordnet fremde Items nur über ID-Listen (fehlende IDs übersprungen, kein Import). |
| Testzentrale/Guide/JEI im Kern | Je Sub-Mod eigene Stationen/Kapitel/Plugins; SB sammelt per `ModuleQuestDefaults`-artigem framework-Vertrag (`TestCentreStations`, `GuideChapters`) – reine Java-Verträge in `framework`, keine Klassenimporte. |
| Money-Preise | Neue Tabellen `money/<submod>/prices.json` + Links mit `links:<submod>`-Condition; `generate_links.py` erzeugt aus neuen IDs. |

---

## 2. Ziel-Architektur

### 2.1 Übersicht

```
SimpleBuilding (simplebuilding, Super-Mod, 2.0.0)
 ├─ required, gebündelt: simpleoctets, simplehammers, simplestorage, simpleovens, simplecrucibles
 ├─ optional, gebündelt (Schalter "Enable Simple X"): simpleriding, simplemaps, simplesandwiches,
 │    simplequestionary (neu, Konzept KONZEPT-QUESTIONARY-TABLE-2026-10-11), simpletweaks, simplemoney,
 │    simplefun, simpledimension(s), simpletrims, simplemobs
 └─ gebündelte Libs: simplelib, framework
Simple QoL (simplequalityoflife, Super-Mod): simpleweather, simpleinterfaces (+ später simpleloot,
    simplemobmanners, simplefarming, simpletools, simplemovement laut PLAN-QOL-AUFTEILUNG)
Simple Aesthetics (simpleaesthetics, NEU, Super-Mod): simplevisuals, simplesounds, simplemodels
```

Bedeutung **required** vs. **optional** im Bündel: beide liegen per Jar-in-Jar in der Super-Mod und sind damit immer
geladen. *Required* = SB-Code/Daten setzen sie voraus (`depends`/`type="required"`), Schalter nur in der Sub-Mod-Config
(Inhalt bleibt registriert, Welten bleiben ladbar). *Optional* = SB kennt sie nur über IDs/Conditions (`suggests`/
`type="optional"`), Schalter `enableSimpleX` in der SB-Config über `framework.api.SubMods` (Vorbild QoL/Weather:
`SimplequalityoflifeConfig.enableSimpleWeather`, `QolTests.subModSwitch`). Abschalten deaktiviert Spielstellen, nie
Registrierungen (sonst Datenverlust).

### 2.2 Je Mod

| Mod | ID / Namespace | Inhalt | hart | optional | Tab | Guide/Wiki |
|---|---|---|---|---|---|---|
| SimpleBuilding | `simplebuilding` | Rest: Enderit/Nihilith/Astralit-Materialien, Bauzauberstäbe, Meißel, Blaupausen, Astral-Verzauberer, End-Systeme, Tweaks-Port (`com.simplebuilding.tweaks`), Trichter, Werkzeuge/Rüstung, Schach, Hängematten, Musik …; gemeinsame Config-Oberfläche, Mega-Guide, Testzentrale-Rahmen | cloth-config, simplelib, 5 Pflicht-Sub-Mods | 10 optionale Sub-Mods, jei/rei/jade… | bestehende SB-Tabs | Mega-Guide bindet Kapitel der Sub-Mods per ID ein |
| Simple Octets | `simpleoctets` | alle 1/8-Blöcke, Octet-Zellen, Holz/Melone/Material-Octets, Steinsäge-Rezepte, Generator `octets_*.py` (Ausgabe in Modulpfad) | simplelib | simplebuilding (Schach-Octets bleiben in SB, F3) | eigener Tab, 12 Gruppen aus `OctetMaterials.GROUPS` | eigenes `wiki/manual.json` + Guide |
| Simple Hammers | `simplehammers` | 7 Vorschlaghämmer, Hammer-Verzauberungen (Break-Through, Radius, Ignore-Blocktype …), Hammer-Ecken/-Schnitzen, Schritt-Ergebnisse, In-World-Crafting, Chips, Platten-Drehen; JEI/REI In-World-Kategorie; schaltet beim Laden alle Axt-Wege ab (§2.6) | simplelib | simpleoctets (Schnitzen), simplebuilding | eigener Tab | eigenes Wiki/Guide |
| Simple Storage | `simplestorage` | Stufen-Truhen/-Fallentruhen/Kupfer-Fallentruhen, Shulker-Stufen + Hüllen, verstärkte Bündel, Rucksäcke, Köcher, Kisten, Lager-Werkbank, platziertes Bündel, Enderit-Fass (aus SB), Kisten-Boote/-Loren, Lager-Verzauberungen, seltene Truhen/Shulker | simplelib | simplehammers, simplebuilding | eigener Tab | eigenes Wiki/Guide |
| Simple Ovens | `simpleovens` | 9 Öfen, Stufen-Vorteile, Ofen-Loren, Bonus-Tags | simplelib | simplehammers, simplebuilding | eigener Tab | eigenes Wiki/Guide |
| Simple Crucibles | `simplecrucibles` | Tiegel (alle Stufen), Fässer + Andocken, verstärkter Kessel, Seelenlava (Fluid, Weltgen, Nebel), Enderit-Tiegel, In-World-Wege, JEI-Kategorien | simplelib | simplebuilding (Kupfer-/Enderit-Eimer), simplehammers, simplestorage | eigener Tab | eigenes Wiki/Guide |
| Simple QoL | `simplequalityoflife` | wie PLAN-QOL-AUFTEILUNG; neu: `requires`+Bündeln `simpleinterfaces` | simpleweather, simpleinterfaces, simplelib | simplebuilding | – | QoL-Guide |
| Simple Aesthetics | `simpleaesthetics` | nur Super-Mod-Hülle: Config „Enable Simple Visuals/Sounds/Models“, Guide-Buch, FTB-Startquest | simplevisuals, simplesounds, simplemodels, framework | simplebuilding | – | eigenes Wiki-Kapitel |

**Crucibles und simplelib:** Der Besitzer zählt Tiegel, Fass-Andocken und verstärkten Kessel zu Simple Crucibles; diese
liegen heute als `simplelib:*` vor und werden von Simple Sandwiches mitbenutzt. Vorschlag: Inhalt wandert nach
`simplecrucibles:` (Alias `simplelib:<id>` → `simplecrucibles:<id>`), simplelib behält nur API, Warm-Komponente,
UI-Bausteine, Migrations- und Upgrade-Dienste. Sandwiches bündelt dann `simplecrucibles` (oder nutzt Tiegel nur optional) –
Frage F1.

### 2.3 Bündelung (Muster QoL/Weather, unverändert übernehmen)

- Fabric: `dependencies { include project(':modules:<sub>:fabric') }` in der Super-Mod (`mc26_3/fabric/build.gradle` für SB),
  `fabric.mod.json` `depends` (required) bzw. `suggests` (optional).
- NeoForge: `jarJar project(...)` + `runtimeOnly`, `evaluationDependsOn`, `neoForge.mods { <sub> {...} }`,
  `runs.configureEach { loadedMods.add(...) }`; `neoforge.mods.toml` `type="required"`/`"optional"`.
- Forge: `ext.forgeBundleModules = [':modules:<sub>:forge', ...]` (`gradle/forge-framework.gradle` schreibt
  `META-INF/jarjar/metadata.json`), `ext.forgeBundleSimplelib = true`; `mods.toml` `mandatory`.
- `modules/modules.json`: neue Felder `parent` (`simplebuilding`/`simplequalityoflife`/`simpleaesthetics`) und
  `bundle` (`required`/`optional`); `tools/multimod.py` prüft Gleichstand Metadaten ↔ `modules.json` ↔ Gradle.
- Verschachtelte Libs (SB nestet Money, Money nestet simplelib) sind erlaubt; der Loader wählt eine Fassung.
- Risiko Jar-Größe/Startzeit: SB-Jar enthält danach ~17 Jars; Messung in Stufe 1.

### 2.4 Configs

Jede Sub-Mod eigene Config-Datei (`config/<sub>.json` bzw. Server-Teil), SB-Config bekommt Abschnitt „Sub-Mods“ mit
`enableSimpleX` (EN „Enable Simple X“ / DE „Simple X aktivieren“) als jeweils oberstem Punkt. Bestehende SB-Schlüssel der
extrahierten Bereiche (z. B. `server.soulLava`, Hammer-Schläge `ServerTuning.sledgehammerBlows`, Ofen-/Truhen-Optionen)
werden beim ersten Start übernommen (§3.4).

### 2.5 Gemeinsame Materialien: eine Stelle, niedrigste gemeinsame Ebene = SimpleLib

**Entscheidung: SimpleLib (`simplelib:`), keine eigene Material-Mod.** Begründung:
- Regel 6a sagt genau das: Inhalte, die mehrere Mods allein brauchen, liegen in **einer** `*lib`-Bibliothek mit einem
  Namespace, Jar-in-Jar gebündelt. Eine zweite Bibliothek (`simplematerials`) wäre ein weiterer Pflicht-Jar in jeder
  Mod, mit gleicher Bündel-Technik, ohne Mehrwert.
- simplelib wird bereits von SB, Sandwiches, QoL, Interfaces, Maps gebündelt und bekommt ohnehin die neuen Dienste
  (`IdMoves`, `in_world_upgrades`, `OctetCells`) – die Sub-Mods brauchen simplelib also sowieso.
- Gegenargument „Bibliothek mit Weltinhalt“: gelöst durch **Bezug nur auf Anfrage** (unten).

**Was nach simplelib zieht** (jeweils mit Alias `simplebuilding:<id>` → `simplelib:<id>`):

| Material | heutige IDs | Nutzer |
|---|---|---|
| Rissiger Diamant (Besitzer-Beispiel) | `cracked_diamond`, `cracked_diamond_block` (Amboss-Zerschlagen `AnvilDiamondCrushing`) | Hammers (Diamant-Stufe), Storage/Ovens (Verstärkt-Stufe), SB-Werkzeuge, Money |
| Netherit-Nugget | `netherite_nugget` | Hammers, Storage, Ovens, Crucibles, Sandwiches (Tag), SB |
| Enderit-Grundmaterial | `raw_enderite`, `layered_raw_enderite`, `enderite_scrap`, `enderite_ingot`, `enderite_nugget`, `enderite_block` + Loot-Einspeisung (`ModLootTableModifications`) und Schmelzrezepte | alle fünf Pflicht-Sub-Mods, Riding, Sandwiches, SB |
| Upgrade-Vorlagen | `basic_upgrade_template`, `enderite_upgrade_template` (+ Rezepte; platzierte Vorlage bleibt SB) | Storage, Ovens, Crucibles, SB-Werkzeuge/-Rüstung |
| Gemeinsame Tags | `c:ingots/enderite`, `c:nuggets/enderite`, `c:nuggets/netherite`, `c:gems/cracked_diamond`, neu `#simplelib:upgrade_tools`, `#simplelib:hammers` (leer, Hammers füllt), `simplelib:upgrade_netherite`/`upgrade_reinforced` (gibt es schon) | alle |

**Bleibt in SB:** Astralit/Nihilith (nur Astral-Verzauberer/End-Systeme), Dimensions-Schrott-Erze (zusätzlicher,
besserer Enderit-Weg – „Partner macht es besser“), Enderit-Werkzeuge/-Rüstung/-Äpfel, platzierte Vorlagen.

**Bezug nur auf Anfrage:** Items/Blöcke registriert simplelib immer (Welten bleiben ladbar, egal welche Mod fehlt).
Bezugswege (Loot-Einspeisung, Schmelz-/Crafting-Rezepte, Handel, Creative-Tab-/JEI-Sichtbarkeit) aktiviert simplelib nur,
wenn eine geladene Mod das Material anfordert: `SimpleLibApi.requestMaterial("enderite")` im Mod-Konstruktor bzw.
Daten-Condition `simplelib:material_requested`. Sandwiches allein bekommt also kein Enderit in die Welt, Simple Storage
allein schon. Die Bibliothek kennt dabei keinen Nutzer (Regel 6a bleibt erfüllt).

**Enderit-Entscheidung (Besitzer bat um eigene Entscheidung):** Grundmaterial nach simplelib, wie oben. Begründung nach
den Prinzipien: Regel 1 (jede Sub-Mod allein voll spielbar, inkl. höchster Stufe) und Regel 6 (ein Besitzer je Material)
sind nur so gleichzeitig erfüllbar; die Alternative „Enderit-Stufen nur mit SB registrieren“ (Horseshoes-Muster) ließe
Enderit-Truhen in einer Welt verschwinden, sobald SB fehlt – Datenverlust widerspricht „keine Funktion brechen“.
Enderit-Ausrüstung und der bessere Bezugsweg bleiben SB-Inhalt.

### 2.6 Einheitliche Prinzipien für alle Mods

- **Axt/Hammer-Prinzip (wie beim Tiegel, `AxeWays` + `SimpleLibApi.disableAxeWays()`):** Jede In-World-Herstellung und
  -Aufwertung (Tiegel bauen, Fass andocken, Truhe/Shulker/Ofen/Kessel aufwerten, Octet-Bauwege) läuft über den einen
  Datentyp `simplelib:in_world_upgrades` mit Werkzeug-Tag `#simplelib:upgrade_tools`. Ohne Simple Hammers enthält er die
  Äxte (Vanilla-Material statt Partner-Material, Regel 5a); **ist Simple Hammers geladen, ruft Hammers
  `disableAxeWays()` auf und es geht nur noch mit Hammer** (heute macht das SB in `CrucibleCompat.init` – wandert nach
  Hammers). Gilt in allen Mods gleich, auch Trims (`TemplateTools`) und Sandwiches.
- **Gleicher Stil:** In-World-Schläge immer über `simplelib.api.InWorldStrikes` (wachsende Zielvorschau, Schlagzahl aus
  Config); Filter nach Filter-Prinzip (echtes Item bleibt im Slot, erst ab dem 2. verarbeiten, gleicher Filter-Knopf aus
  simplelib-UI); Screens aus `simplelib.api.client.ui` + `framework ContainerStyleHints`; Config je Mod über
  `ConfigFiles`, „Enable Simple X“ ganz oben in der Super-Mod; Tabs über `framework CreativeTabSettings`; Tooltips nach
  PLAN-TOOLTIPS-2026-10-02 (kurze übersetzte Zeilen, gemeinsamer Umbruch); Wiki-Notiz „mit X“ (Regel 7).
- **Prüfung:** `tools/multimod.py` bekommt Prüfungen „In-World-Weg ohne Axt-Fallback“, „Mod ohne Enable-Schalter in
  Super-Mod“; je Mod ein Gametest „Axt-Weg allein“ (Standalone-Target) und „Hammer-Weg mit Hammers, Axt aus“ (Integration).

---

## 3. Migration alter Welten

### 3.1 Vorhandene Technik (wiederverwenden, nicht neu erfinden)

- `datafix/LegacyItemIds` + `mixin/MappedRegistryAliasMixin` (Fabric, NeoForge) beantworten **verfehlte** Registry-Abfragen
  (`get(Identifier/ResourceKey)`, `getValue`, `containsKey`) mit der neuen ID. Damit funktionieren bereits Items in Stacks,
  Blöcke in Chunk-Paletten (`astralit_powder` → `astral_redstone`) und Entity-Typen. Kein DataFixer nötig, weil der nur bei
  steigender MC-Datenversion läuft (Begründung steht in der Klasse). **Fabric braucht damit keine Missing-Mappings.**
- Forge: `mixin/forge/NamespacedWrapperAliasMixin` + `ForgeRegistryBootstrap.registerLegacyAliases`
  (`ForgeRegistry.addAlias`).
- simpletweaks: `LegacyAliases` (namespace-übergreifend `simpletweaks:*` → `simplebuilding:*` für BLOCK, ITEM,
  BLOCK_ENTITY_TYPE, DATA_COMPONENT_TYPE) – genau das Muster für den Namespace-Wechsel.
- Tests: `gametest/TradeAndMigrationTests`.

### 3.2 Neuer Mechanismus (Stufe 3)

1. **`simplelib.api.IdMoves`** (SimpleLib, weil jede Sub-Mod allein migrieren muss): Tabelle `oldId → newId` je Registry.
   Keine Datapack-Datei, weil Registry-Lookups vor dem Datapack-Laden laufen, sondern eine **generierte Java-Tabelle** (`tools/split/id_moves.py` erzeugt `<Sub>IdMoves.java` aus der Verschiebeliste), jede Sub-Mod
   registriert ihre Tabelle in ihrem Mod-Konstruktor (vor Weltladen).
2. Alias-Mixins wandern nach simplelib (`MappedRegistryAliasMixin`, Forge-Wrapper + `addAlias`), SB-`LegacyItemIds` speist
   ebenfalls ein (keine Doppel-Mixins; SB entfernt seine Kopie im selben Schritt).
3. Abgedeckte Registries: ITEM, BLOCK, BLOCK_ENTITY_TYPE, ENTITY_TYPE (Loren/Boote), FLUID (Seelenlava),
   DATA_COMPONENT_TYPE (Rucksack-Inhalt, Zähler), MENU (nicht gespeichert, nur zur Sicherheit), dynamisch ENCHANTMENT
   (Hammer-/Lager-Verzauberungen auf Items; Lookup über `MappedRegistry` der Registry-Access – im Test eigens prüfen).
4. **Rezeptbuch:** `ServerRecipeBook` lädt `ResourceKey<Recipe>`; unbekannte fallen still weg → Mixin auf das Laden
   (Schlüssel durch `IdMoves.recipe(...)` umschreiben; Rezept-IDs wechseln mit dem Namespace, Pfade bleiben gleich).
5. **Advancements:** `PlayerAdvancements` lädt Fortschritt per ID → Mixin beim Einlesen der JSON-Map remappen
   (`simplebuilding:hammer/*` → `simplehammers:hammer/*`, Rezept-Advancements mit).
6. **Statistiken/Tags/Loot:** Statistiken nutzen Registry-IDs (Alias greift); Tags fremder Datapacks mit `simplebuilding:<alt>`
   lösen über Alias auf; alte Tag-IDs (`#simplebuilding:sledgehammer_tools`) bleiben als SB-Tag-Datei bestehen, die auf
   den neuen Tag verweist (`{"id":"#simplehammers:sledgehammer_tools","required":false}`), mindestens 2 Versionen lang.
7. **Block-Entity-NBT:** Chunk-BE tragen `id` → `BLOCK_ENTITY_TYPE`-Alias; Inhalte sind Vanilla-Container-NBT bzw.
   Komponenten (Komponenten-IDs mit Alias). Strukturdateien des Mods werden neu erzeugt.
8. **Configs:** beim ersten Start einer Sub-Mod: fehlt `config/<sub>.json`, aber `config/simplebuilding*.json` enthält
   zugehörige Schlüssel → kopieren, Original unverändert lassen, Logzeile. Über `framework.api.ConfigFiles`.
9. Reihenfolge: Alias greift nur bei Fehlschlag → solange SB eine alte ID noch selbst registriert, gewinnt diese. Deshalb
   alte Registrierung im selben Commit entfernen, in dem die neue entsteht.

### 3.3 Tests mit gespeicherter Test-Welt

- `testing/worlds/split-before-1.3.x/` (gezippte Welt, erzeugt mit dem letzten Stand **vor** Stufe 4 auf allen drei
  Loadern): je verschobenem Block eine platzierte Instanz, gefüllte Truhen/Shulker/Rucksäcke/Bündel/Fässer/Tiegel
  (mit Verzauberungs- und Komponenten-Items), Octets aller Gruppen, laufender Ofen, Kisten-Lore, Seelenlava-Quelle,
  Spieler mit Rezeptbuch, Advancements, Statistiken, Inventar mit allen Hämmern.
- Erzeugung per Testzentralen-Befehl `/sbtest migration-world build` + Manifest `expected.json` (Position → ID, NBT-Hash
  der Inhalte, Spieler-Daten).
- Neuer Runner-Target-Typ `migration-<loader>-263`: Welt kopieren, Server starten, Gametest `MigrationWorldTests`
  vergleicht gegen `expected.json` mit **neuen** IDs, speichert, startet neu, vergleicht erneut (zweiter Lauf beweist,
  dass unter neuen IDs gespeichert wurde). Zusätzlich je Sub-Mod standalone (nur ihre Einträge).
- Unit-Test (JUnit) `IdMovesTest`: Tabelle vollständig = alle entfernten `simplebuilding:`-IDs haben ein Ziel (Diff der
  Registry-Dumps vor/nach, Dump über Datagen-Export).

### 3.3a Umsetzung 3a + 3b (Branch `claude-split3`, 2026-10-11)

- **`com.simplelib.api.IdMoves`**: explizite Verschiebungen je Registry (`move`/`moveAll`, auch `minecraft:recipe` und
  `minecraft:advancement`), Namensraum-Verschiebung (`moveNamespace`, nur über die Mixins), Schleifen-/Doppelziel-Schutz,
  `adoptConfig` (Config-Übernahme, darunter `framework.api.ConfigFiles.adoptLegacy`, framework 0.1.8).
- **Mixins in SimpleLib**: `IdMovesRegistryMixin` (MappedRegistry: get/getValue/containsKey, nur Fehlschläge),
  `IdMovesRecipeBookMixin` (`ServerRecipeBook#loadUntrusted`: unbekannter Schlüssel → verschobener, wenn bekannt),
  `IdMovesAdvancementsMixin` (`ServerAdvancementManager#get`, darüber liest `PlayerAdvancements` den Fortschritt);
  Forge: `NamespacedWrapperIdMovesMixin` + `ForgeRegistry.addAlias` für alle expliziten Verschiebungen (`SimpleLibForge`).
- **SB 26.3**: `datafix/IdMovesHook` (Overlay) speist `LegacyItemIds` unverändert ein (gleiche Tabellen, gleiche Registries);
  `AliasMixinPlugin` schaltet SBs eigene Alias-Mixins auf 26.3 ab, `ForgeRegistryBootstrap` fügt dort keine Aliase mehr hinzu.
  26.2 (keine SimpleLib) bleibt beim alten Weg (Twin `IdMovesHook` mit `LIB_ALIASES=false`).
- **Testwelt (3b, vereinfacht)**: statt gezippter Welt + eigenem Runner-Target liegen die Spielstände als SNBT/JSON unter
  `testing/fixtures/migration-before/` (Chunk mit Truhen-Stufen, Öfen, Enderit-Tiegel, Octets und gefüllten BEs; Spieler mit
  allen Hämmern + Rezeptbuch; Advancement-Fortschritt), erzeugt mit `SIMPLEBUILDING_WRITE_MIGRATION_FIXTURES=1`.
  `MigrationWorldTests` lädt sie über die echten Ladepfade (`SerializableChunkData.parse`, `BlockEntity.loadStatic`,
  `Player.load`, `PlayerAdvancements`) einmal unverändert (Nullprobe) und einmal mit allen SB-IDs als `simpletest:`
  (Test-Verschiebung `simpletest → simplebuilding`), Orakel = unverändertes Laden, plus Zurückschreiben unter neuen IDs.
  Ab Stufe 4 ist dieselbe Welt der Altbestand für echte Verschiebungen.
- Lib-Tests `module_game_test_id_moves_*` (Item/Block/BE/Komponente, Rezeptbuch, Advancements, Schleifen, Config).

### 3.4 Rückfallplan

- Vor Release: Hinweis „Welt sichern“ im Changelog/Wiki; SB 2.0 schreibt beim ersten Laden einer alten Welt
  `world/simplebuilding-split-backup.marker` und loggt; optional automatische Sicherung von `level.dat`+`playerdata`.
- **Brücken-Version SB 1.3.x-bridge**: kennt die neuen IDs als Rück-Alias (`simpleoctets:*` → `simplebuilding:*` …), damit
  ein Downgrade ohne Datenverlust möglich ist.
- Jede Extraktion ist ein eigener Branch/Release; schlägt der Migrationstest fehl, wird die Stufe nicht gemergt. Alias-Tabellen
  werden nie entfernt (Kosten nur bei Fehlschlag-Lookups).

---

## 4. Stufenplan

Reihenfolge: erst Struktur ohne ID-Änderung, dann Entkopplung, dann Infrastruktur, dann Extraktionen (leichteste zuerst).
Jede Stufe: eigener Branch von `claude-wave1`, **für sich gate-grün nach master mergbar** (keine Stufe hinterlässt einen
Zwischenzustand, der eine spätere braucht; alte IDs bleiben bis zur eigenen Extraktion registriert). Pflicht je Stufe:
Gate über `/root/sb-gate.sh`, alle Standalone-Targets (`run.py --targets standalone`), Migrations-Targets
`migration-*-263` (ab 3b; davor `TradeAndMigrationTests` als Migrationsnachweis), dazu die genannten neuen Tests.
Schätzung in Agenten-Sitzungen (1 S ≈ ein Queue-Auftrag inkl. Tests).

| # | Branch | Umfang | Risiken | Verifikation | Schätzung |
|---|---|---|---|---|---|
| 1a | `claude-split-s1-super` | SB als Super-Mod: `requires`+Bündeln der optionalen Sub-Mods (Riding, Maps, Sandwiches, Tweaks, Money, Fun, Dimensions, Trims, Mobs; Questionary sobald vorhanden), Metadaten `suggests`/optional, `enableSimpleX` über `SubMods` in SB-Config, `modules.json` `parent`/`bundle`; **keine ID-Änderung** | Jar-Größe, Doppelladen in Dev-Runs (Mods-Container), Forge-jarjar-Metadaten; Money/Dimensions hatten SB früher fest im Runtime-Pfad | Gate 3 Loader; alle Standalone-Targets; neuer Test `SubModSwitchTests` (je Schalter aus → Spielstelle inaktiv, Registry unverändert); Jar-Inhalt prüfen (`unzip -l`) | 2 S |
| 1b | `claude-split-s1-qol` | QoL `requires`+bündelt `simpleinterfaces`; Schalter | gering | QoL- + Interfaces-Standalone, `QolTests.subModSwitch` erweitert | 0,5 S |
| 1c | `claude-split-s1-aesthetics` | neues Modul `simpleaesthetics` (Gerüst wie `wiringexample`), bündelt Visuals/Sounds/Models, Config, Guide, Wiki; `modules.json`-Eintrag, Testkatalog | Sounds hängt optional an Visuals (`CosmeticIntensity`) – bleibt optional | neues Standalone-Target, Modul-Targets der drei Sub-Mods, Wiki-Check | 1 S |
| 2 | `claude-split-s2-tweaks` | simpletweaks: `requires simplebuilding` → optional (modules.json, fabric `depends`→`suggests`, toml `optional`, NeoForge `mods.simplebuilding` aus Dev-Run); Tests ohne SB; dann optionale Sub-Mod von SB. Der SB-Tweaks-Port (`com.simplebuilding.tweaks`, 95 Klassen, `simplebuilding:`-IDs) bleibt vorerst in SB (F2) | Claims nutzen `framework Protection` – keine SB-Klassen, Import-frei (geprüft: 0 SB-Importe, 0 `simplebuilding:`-IDs im Code); `LegacyAliases` zielt auf `simplebuilding:` → nur sinnvoll mit SB, hinter `isModLoaded` | `module-simpletweaks-standalone-*` neu, Claims-Tests grün mit/ohne SB | 1 S |
| 3a | `claude-split-s3-idmoves` | `simplelib.api.IdMoves` + Alias-Mixins nach simplelib, SB-`LegacyItemIds` speist ein; Rezeptbuch-/Advancement-Remap-Mixins; Config-Übernahme in `framework.ConfigFiles`; Probe-Verschiebung in `wiringexample` | Mixin doppelt (SB+lib) → SB-Kopie im selben Commit entfernen; Forge-Wrapper | `TradeAndMigrationTests` grün; neuer `IdMovesTests` (Item/Block/BE/Komponente/Rezeptbuch/Advancement, Namespace-Wechsel) auf 3 Loadern | 2 S |
| 3b | `claude-split-s3-world` | Migrations-Testwelt (vorher) bauen, Target `migration-*-263`, `expected.json` | Welt muss vor Stufe 4 erzeugt werden, je Loader | Runner-Lauf grün gegen unveränderte IDs (Nullprobe) | 1 S |
| 3c | `claude-split-s3-apis` | SimpleLib-Dienste: `in_world_upgrades`-Datentyp (Ablösung `SledgehammerUpgrades`-Tabelle, SB-Daten umziehen), `#simplelib:upgrade_tools`, `isHammer`, `OctetCells`, `registerUpgradeHandler`; framework `TestCentreStations`, `GuideChapters`; SB nutzt intern schon die APIs (noch alles `simplebuilding:`) | Verhaltensgleichheit Hammer-Aufwertungen (Schlagzahl, Haltbarkeit, Fortschrittsanzeige, Astral-Tisch-Freischaltung) | `SledgehammerUpgradeTests`, `WorldUpgradeTests`, `AstralEnchantingTests`, `CrucibleTests`, `SledgehammerOctetTests` unverändert grün | 3 S |
| 3d | `claude-split-s3-materials` | Gemeinsame Materialien nach simplelib (§2.5): rissiger Diamant (+Block), Netherit-Nugget, Enderit-Grundmaterial, Upgrade-Vorlagen, `c:`-Tags, `requestMaterial`, Condition `material_requested`; Axt/Hammer-Umschaltung als eigener Aufruf (wandert in Stufe 7 nach Hammers) | erste echte Namespace-Verschiebung (Muster für alle weiteren); Riding nennt feste ID `simplebuilding:enderite_ingot` (Befund 6) → vorher auf Tag | Migrationswelt (Stacks, Blöcke, Rezeptbuch, Advancements), `MaterialRequestTests` (Sandwiches allein: kein Enderit-Loot; Storage/SB: ja), Money-Links, Standalone aller Module | 2 S |
| 4 | `claude-split-s4-ovens` | Extraktion **Ovens** (11 Klassen, 9 Blöcke, 3 BE) → `modules/simpleovens`, `simpleovens:`, Ofen-Loren, Alias-Tabelle, Money-Tabelle, Wiki, Tab | Ofen-Lore = Entity (ENTITY_TYPE-Alias), Vanilla-Menü-Mixin doppelt registriert? | Modul-+Standalone-Targets; `FurnaceTests`, `SmeltingTests`, `EnderiteMachineTests`, `VehicleTests`; Migrationswelt | 2 S |
| 5 | `claude-split-s5-crucibles` | **Crucibles**: SB-Teil (Enderit-Tiegel/-Fass, Seelenlava inkl. Weltgen-Mixins, Kataloge, JEI) + simplelib-Inhalt (F1) → `simplecrucibles:`; Aliase `simplebuilding:`/`simplelib:` → `simplecrucibles:` | Sandwiches-Abhängigkeit; Fluid-Alias; Seelenlava-Weltgen in vorhandenen Chunks | `CrucibleTests`, simplelib-`LibTests`, Sandwiches mit/ohne, Standalone, Migrationswelt | 3 S |
| 6 | `claude-split-s6-octets` | **Octets**: Generator schreibt in Modulpfad, 106 Blöcke/117 Items/377 Rezepte, `OctetCells`-Registrierung, Tab-Gruppen; Schach-Octets nach F3 | Masse (Lang/Modelle/Rezepte), Steinsäge-Rezeptbuch-Remap | `DataIntegrityTests`-Anteil, `SledgehammerOctetTests` (mit Hammers), Standalone (Steinsäge), Migrationswelt | 2 S |
| 7 | `claude-split-s7-hammers` | **Hammers**: 7 Hämmer, 13 Kernklassen, Hammer-Verzauberungen (ENCHANTMENT-Alias), Ecken/Schnitzen, In-World-Crafting, JEI/REI-Kategorie; Kern-Aufrufer (~40) auf Tag/`isHammer` | breiteste Kopplung (Renderer, Mixins, Bauzauberstab-Konstanten → eigene Konstanten in SB), Tag-Umzug (`simpletrims`, `simplelib` nennen `simplebuilding:sledgehammer_tools`) | `SledgehammerTests`, `SledgehammerUpgradeTests`, Trims mit/ohne, Client-Test Outline, Standalone (Hammer ohne Ziele = Abbau-Werkzeug + Axt-Wege), Migrationswelt | 4 S |
| 8 | `claude-split-s8-storage` | **Storage** (~93 Klassen, 36 Blöcke, 7 BE, 3 Menüs, 3 Pakete, 13 Mixins, Fahrzeuge, Lager-Verzauberungen, Enderit-Fass) | größter Umzug; `ModMessageHandlers` teilen; Trichter bleibt SB und kennt Stufen-Truhen nur über `Container`/Tag; Loader-Menü-Shims (`platform/*Menus`) je Loader; QoL/Interfaces-Layout-Aliase (`ContainerStyles.LAYOUT_ALIASES` nennt SB-Klassennamen als String) | 10 Storage-Testklassen + 2 Client-Tests, Interfaces-Tests, Sandwiches „aus Bündeln essen“, Standalone, Migrationswelt (gefüllte Container!) | 5 S |
| 9 | `claude-split-s9-release` | Aufräumen: SB-Tabs/Suchtab/Guide/Testzentrale nur noch über IDs/Verträge, Wiki-Umzug, Versionen, Changelog, Brücken-Version 1.3.x-bridge | – | volles Gate + alle Standalone + alle Migrationstargets einmal | 2 S |

**Status Stufe 1 (11.10., Branch `claude-split1`, drei Commits 1a/1b/1c statt drei Branches):**
- 1a: SB bündelt zusätzlich Riding, Sandwiches, Tweaks, Money, Fun, Dimensions, Trims, Mobs (Maps/Questionary waren
  schon drin): Fabric nur `include`, NeoForge nur `jarJar` (kein `evaluationDependsOn` – mehrere Sub-Mods werten
  `:mc26_3:neoforge` für ihre Integrationsläufe aus, sonst Zyklus), Forge `forgeBundleModules`. SB-Dev-/Testläufe laden
  die neuen Sub-Mods bewusst **nicht** (unverändertes SB-Testumfeld); das Zusammenspiel prüfen die Integrations-Targets
  der Sub-Mods. Schalter `enableSimpleX` im Tab „Simple Mods“, `SimplebuildingConfig.applySubModSwitches` setzt
  `framework SubMods` beim Laden der Live-Config (nicht in `validatePostLoad`, das auch Test-Kopien durchlaufen).
  Jede Sub-Mod: `on()`-Helfer, aus = Tabs leer, keine Loot-/Trade-Injektion, zentrale Spielstellen inert, Registrierungen
  unverändert; Gametest `sub_mod_switch` je Modul, SB `config_option_game_test_super_mod_switches_reach_the_bundled_sub_mods`.
  `modules.json`: `parent`/`bundle`, `tools/multimod.py` prüft Liste im Elternteil + Gradle-Bündelung je Loader.
- 1b: QoL `requires`+bündelt `simpleinterfaces`, Schalter `enableSimpleInterfaces`, `QolTests.subModSwitch` erweitert.
- 1c: neues Modul `simpleaesthetics` 1.0.0 (keine Inhalte; `config/simpleaesthetics.json` mit drei Schaltern, ohne
  eigenen Config-Bildschirm), `requires`+bündelt Visuals/Sounds/Models, Standalone-Target, Wiki-Kapitel, Money-Preisliste.

,
**Status Stufe 2 (11.10., Branch `claude-split2` auf `claude-split1`):** Simple Tweaks hängt nicht mehr hart an SB:
`modules.json` `requires: []`, `optional` + `simplebuilding`; Fabric `depends` → `suggests`, NeoForge `type="optional"`,
Forge `mandatory=false`. Standalone-Target lädt nur Simple Tweaks (`standalone.requires: []`). Tests mit SB-Bezug
(Legacy-Aliase, SB-Werkzeuge/-Kolben/-Trichter/-Magnet, `WorldPermissions`) laufen mit SB unverändert und überspringen
allein mit Log-Hinweis; allein prüft `every_legacy_registry_lookup`, dass die Aliase leer bleiben (kein Treffer, Vanilla
verwirft unbekannte Einträge). Die Alias-Suche selbst brauchte keinen `isModLoaded`-Schalter: das Ziel `simplebuilding:*`
fehlt dann einfach in der Registry. Integrations-Targets (`module-simpletweaks-*-263`) laden SB weiter. Pads/Chunk-Loader
bleiben in SB (F2).

**Status Stufe 3c (13.10., Branch `claude-split-s3-apis` auf `claude-wave1`):** _Plan_
- SimpleLib `api/InWorldUpgrades`: Datentyp `data/<ns>/in_world_upgrades/*.json` (`from`, `to`, `material`,
  `material_count`, `duration_factor`, `tool` = Item-Tag, Standard `#simplelib:upgrade_tools`, `min_tool` (alt
  `min_hammer`), `damage_per_hit`, `tier`; `remove`), Parser + wirksame Tabelle, Reihenfolge „Standard-Namespaces zuerst“
  (`registerDefaults`), Übergabe-Dienst `registerUpgradeHandler(Block-Prädikat/Tag, handler)` + `replace(level, pos, to)`
  (SimpleLib meldet seine Tiegel/Fässer selbst an). Tags `#simplelib:hammers` (leer, SB füllt mit
  `#simplebuilding:sledgehammer_tools`) und `#simplelib:upgrade_tools` (= `#simplelib:hammers`; Axt nur solange die
  Axt-Wege an sind), `SimpleLibApi.isHammer/isUpgradeTool/registerUpgradeHandler`. `api/OctetCells`
  (`register(base, cell, piece)`, `cellFor`, `pieceOf`).
- framework 0.1.10: `TestCentreStations`, `GuideChapters` (reine Java-Verträge, JUnit im framework).
- SB 26.3 (Brücke `util/LibServices`, 26.2-Zwilling ohne SimpleLib): Aufwertungsdateien ziehen von
  `sledgehammer_upgrades/` nach `in_world_upgrades/` (Datagen; alter Ordner wird weiter gelesen, gleiche ID gewinnt
  der alte = Modpack-Überschreibungen wirken wie bisher), `SledgehammerUpgradeData` liest über SimpleLib,
  Truhe/Shulker/Tiegel ziehen über `replace` um, Hammer-Werkzeugprüfung über den Eintrag, Octet-Zellen über
  `OctetCells`, `CrucibleCompat` erkennt den Hammer über `isHammer`; Testzentrale zeigt angemeldete fremde Stationen
  (Abschnitt `modules`, nur wenn welche angemeldet sind).
- Verhalten unverändert; Prüfung: Compile 3 Loader + 26.2, SB-Filter inworld*, sledgehammer*, crucible*, data_integrity*
  (+ modpack*, testcentre*), neue Lib-Tests, simplelib-Module + Standalone, Wiki --strict, discover/module_guides --check.

_Umgesetzt (13.10.):_ wie geplant. Neue Tests: Lib `module_game_test_in_world_upgrades_data_type`,
`_in_world_upgrade_handlers`, `_octet_cells` (laufen auch allein, SB-Teile nur wenn geladen); SB
`test_centre_game_test_foreign_module_stations_get_their_own_section`; framework-JUnit `ContributionContractsTest`
(läuft in `:framework:check`). Die SB-Tabelle entsteht auf 26.3 aus SimpleLibs geparster Tabelle
(`ModpackTests` vergleicht weiter Datei = eingebaute Tabelle). Testzentrale: Brücke `dev/testcentre/ModuleStations`
(26.2-Zwilling leer, weil 26.2 kein framework im Klassenpfad hat).
Abweichungen/Grenzen (bewusst):
- **Laden + Client-Sync** der `in_world_upgrades`-Dateien macht in 3c noch SBs `ModDataTables` (liest alle Namespaces,
  sendet an Clients). Eine Mod ohne SB (ab Stufe 4, Simple Ovens allein) braucht einen eigenen Reload-Listener + Sync in
  SimpleLib – in Stufe 4 nachziehen. Die Axt-Wege der Lib (`CrucibleUpgrades`) sind noch fest im Code, nicht als Datei.
- `GuideChapters`: Vertrag + Test fertig, SBs Handbuch liest ihn noch nicht (Kapitel-Schlüssel sind dort indexbasiert,
  Umbau gehört zu Stufe 9).
- Ein ungültiges `min_tool` überspringt jetzt nur den Eintrag (vorher: Rest der Datei); betrifft nur kaputte Datapacks.
- Die ~40 `instanceof SledgehammerItem`-Stellen bleiben bis Stufe 7; `isHammer` nutzt bisher `CrucibleCompat`.
- simplelib-Version bleibt 0.2.0 (wie bei 3a); framework 0.1.10.

**Status Stufe 3d (13.10., Branch `claude-split-s3-materials` auf `claude-wave1`):** _Plan_ (vorab)
- Erst die Vorher-Welt: `MigrationWorldTests` legt zusätzlich die Material-Blöcke, -Items, ihre 15 Rezepte (Rezeptbuch)
  und deren Rezept-Advancements ab; Fixture mit dem Stand **vor** dem Umzug neu geschrieben (eigener Commit).
- SimpleLib `registry/LibMaterials`: registriert immer (Welten bleiben ladbar) `diamond_pebble`, `cracked_diamond`,
  `cracked_diamond_block` (+Block), `netherite_nugget`, `raw_enderite`, `layered_raw_enderite`, `enderite_scrap`,
  `enderite_ingot`, `enderite_nugget`, `enderite_block` (+Block), `basic_upgrade_template`, `enderite_upgrade_template`
  – gleiche Eigenschaften; Registrierung idempotent (`ensureBlocks/ensureItems`), damit die Ladereihenfolge
  SB/SimpleLib auf allen Loadern egal ist. `SimpleLibApi.requestMaterial/materialRequested/materialItem/materialBlock`,
  Condition `simplelib:material_requested` (`{"material": "enderite"}`; Fabric/NeoForge/Forge), Gruppen
  `cracked_diamond`, `netherite_nugget`, `enderite`, `upgrade_templates`. Bezugswege nur auf Anfrage: die 15 Material-
  Rezepte + Rezept-Advancements (Condition) und das Amboss-Zerschlagen (`api` Automationsprüfung setzbar, Standard
  framework `Protection`). Ressourcen (Modelle, Blockstates, Items, Loot der zwei Blöcke, `c:`-Tags inkl.
  `c:gems/cracked_diamond`, Abbau-/Beacon-Tags, Lang EN/DE, Texturen als Kopie) über `tools/gen_resources.py`.
- `diamond_pebble` zieht mit (Abweichung von §2.5: ohne ihn hätte das Amboss-Zerschlagen in der Lib kein Ergebnis).
- SB 26.3: `util/LibMaterials` (Zwilling 26.2 = eigene Registrierung wie bisher) liefert die Lib-Objekte an
  `ModItems`/`ModBlocks` (alle ~300 Java-Verweise bleiben), fordert alle vier Gruppen an; `IdMovesHook` meldet
  Items/Blöcke/Rezepte/Rezept-Advancements `simplebuilding:` → `simplelib:`; Datagen 26.3 lässt Material-Rezepte,
  -Modelle und -Loot weg (`McVersion.MATERIALS_IN_LIB`), Rest nennt automatisch die neuen IDs. Enderit-Barren behält
  das Rüstungsbesatz-Material über einen SB-Mixin (`finalizeInitializer`, wie Glitzermelone). Amboss-Zerschlagen von
  SB auf 26.3 aus (Lib macht es, SB setzt `WorldPermissions` als Prüfung). Axt/Hammer-Umschaltung als eigener Aufruf
  `LibServices.hammersReplaceAxes()` (heute aus `CrucibleCompat.init`, Stufe 7 → Hammers).
- Rezeptbuch-Mixin folgt Verschiebungsketten (`simpletest` → `simplebuilding` → `simplelib`).
- Riding: Tests über `#c:ingots/enderite` statt fester ID; Daten nutzen den Tag schon.
- Bleibt (bewusst) in SB: Loot-Einspeisung (`ModLootTableModifications`, SB-Config-Schalter; SB fordert ohnehin alles
  an) – wandert, wenn eine Mod ohne SB Enderit anfordert (Stufe 4/8); Rohenderit-Synthese (Nihilith/Astralit).
- Prüfung: Compile 3 Loader + 26.2, `migration_world*`, Lib `module_game_test_material_requests*` (allein: keine
  Rezepte/kein Zerschlagen; mit SB: alles), SB-Filter data_integrity*, sledgehammer*, crucible*, modpack*, inworld*,
  anvil*, smelting*, Riding/Money/Sandwiches/Lib je Modul + Standalone, Wiki --strict, discover/module_guides --check.

_Umgesetzt (13.10.):_ wie geplant. Migrationsnachweis: die Vorher-Welt (vor dem Umzug geschrieben) lädt auf allen drei
Loadern Blöcke, BE-Inhalte, Inventar, Rezeptbuch und Advancement-Fortschritt unter `simplelib:` und speichert nichts mehr
als `simplebuilding:<Material>` (`migration_world*` grün). Neue Tests: Lib `module_game_test_material_requests_registered`,
`_recipes`, `_anvil_crushing` (allein: keine Rezepte, kein Zerschlagen), Sandwiches `no_enderite_without_requester`
(allein und mit SB). Zusätzlich nötig (Abweichungen):
- Simple Money: Preise lösen verschobene IDs über die Registry auf (`MoneyLinks.priceOf`), sonst verloren die Material-
  Trades Preisboden und Budget; `prices.json` nennt weiter `simplebuilding:<id>` (Link-Bedingung `links:simplebuilding`).
- Wiki: Loot-Quellen kennen die Lib-Materialien (`obtain_sources`, nur bei `MATERIALS_IN_LIB`).
- SB-Prüfungen (DataIntegrity, Enderit-Regel, Suchtab, In-World-Export, Schmelzen) zählen die geteilten Materialien als
  eigene bzw. nennen den Namensraum über `McVersion.MATERIAL_NAMESPACE`; die Void-Schutz-Regel umfasst `simplelib:*enderite*`.
- Hand-Ressourcen von SB (26.2 und 26.3 gemeinsam, z. B. `c:`-Tags, Handel, FTB-Quests) nennen weiter `simplebuilding:<id>`;
  auf 26.3 lösen sie über IdMoves auf (Tags/Rezepte/Handel). Texturen liegen als Kopie in SimpleLib (SB-Modelle und 26.2
  brauchen die eigenen).
- Offen: Loot-Einspeisung und Lib-eigener Reload/Sync (Stufe 4), Sichtbarkeit (Tab/JEI) nach Anfrage, `prices.json`-Umzug.

**Status Stufe 4 (13.10., Branch `claude-split-s4-ovens` auf `claude-wave1`):** _Plan_
- Vorher-Welt zuerst (eigener Commit, Stand vor dem Umzug): `MigrationWorldTests` legt alle drei Familien in mehreren
  Stufen ab (Ofen mit Eingabe/Brennstoff/Ausgabe), dazu `entities.snbt` mit je einer Ofen-Lore je Stufe (Brennstoff,
  Schub), die 8 Ofen-Rezepte im Rezeptbuch und ihre Rezept-Advancements; Fixture neu geschrieben.
- Neues Modul `modules/simpleovens` (1.0.0, `simpleovens:`), `requires simplelib`, optional `simplebuilding`; SB
  `requires`+bündelt es (Pflicht-Sub-Mod, `bundle: required`, Fabric `depends`, NeoForge/Forge `required`/`mandatory`,
  SB-Dev-/Testläufe laden es). Inhalt: 9 Öfen (gleiche Pfade), 3 Block-Entities (`simpleovens:furnace|smoker|blast_furnace`
  ← `simplebuilding:mod_*`), 3 Ofen-Loren (Entity + Item, gleiche Pfade), Stufen-Vorteile (doppelte Erfahrung ab
  Netherit, Tag `simpleovens:furnace_bonus_excluded` ⊇ `#simplebuilding:furnace_bonus_excluded` optional), Tempo aus
  eigener Config `config/simpleovens.json` (übernimmt beim ersten Start `server.machines.*FurnaceSpeed` aus SBs Config),
  `AbstractFurnaceMenuMixin` + `LongCookContainerData` (SB wendet sein Mixin auf 26.3 nicht mehr an), 8 Rezepte +
  Rezept-Advancements, Loot, Modelle/Texturen (Kopie), Lang EN/DE, eigener Tab + Vanilla-Tabs, Guide-Buch, Fragentisch,
  Discover, Testzentrale-Station (framework `TestCentreStations`), Wiki, Money-Preise (`money/simpleovens`).
  `blast_furnace_bonus` gibt es auf 26.3 nicht mehr (nur 1.21.11) – nichts zu verschieben.
- Aufwertung: `data/simpleovens/in_world_upgrades/ovens.json` (Netherit-Nugget / Enderit-Nugget wie bisher, Werkzeug
  `#simplelib:upgrade_tools`). Mit SB führt der Vorschlaghammer sie aus (SB liest alle Namensräume); allein die Axt
  (Regel 5a) über neuen Lib-Dienst `InWorldUpgrades.axeStrike` (`InWorldStrikes`-Vorschau, Schläge wie SB). Lib bekommt
  eigenen Reload-Listener + Client-Sync der `in_world_upgrades` (alle drei Loader); SB meldet `InWorldUpgrades.loadedByPartner()`
  an, dann lädt/synct nur SB (Altordner `sledgehammer_upgrades` bleibt so wirksam).
- Enderit allein: Ovens fordert `cracked_diamond`, `netherite_nugget`, `enderite`, `upgrade_templates` an; Lib speist
  die Enderit-Beute (Endstadt: Schrott, Rohenderit/Nugget, Enderit-Vorlage) ein, solange kein Partner das übernimmt
  (`SimpleLibApi.materialLootByPartner()`, SB ruft es auf – SB behält seine Pools).
- SB 26.3: `McVersion.OVENS_IN_SB=false`, `OVENS_NAMESPACE`; SB registriert Öfen/BE/Loren/Items nicht mehr (Felder
  `null`, Muster Trims), alle verbleibenden Verweise (Tabs, Suchtab, JEI-Info, Jade-Tempo, Testzentrale-Kisten,
  Advancement-Symbole, Tests) gehen über `util/OvenRefs` (Registry-ID, 26.2 `simplebuilding:`), Datagen 26.3 lässt
  Ofen-Modelle/Loot/Rezepte/Tags/Aufwertungen weg. 26.2 bleibt monolithisch. IdMoves im Modul-Konstruktor: Items,
  Blöcke, BE-Typen, Entity-Typen, 8 Rezepte, 8 Rezept-Advancements.
- Prüfung: Compile 3 Loader + 26.2, Datagen 26.3, SB-Filter furnace*/smelting*/data_integrity*/test_centre*/migration*,
  Modul (3 Loader) + Standalone (Fabric/NeoForge), simplelib-Modul, Wiki --strict, discover/module_guides --check,
  Money-Links --check, check_data.

Summe ≈ 32 Agenten-Sitzungen. Stufen 1a–2 sind unabhängig voneinander parallelisierbar (verschiedene Dateien bis auf
`modules.json` – additiv). Ab Stufe 3 strikt nacheinander (gemeinsame Registrierungsdateien).

---

## 5. Auswirkungen

- **Ports 26.2 / 1.21.11 / 26.4:** Module (inkl. simplelib) existieren nur für 26.3 (`settings.gradle`: „Additive 26.3
  modules“); 26.2 hat `McVersion.CRUCIBLE=false`. Vorschlag: 26.2 und 1.21.11 bleiben monolithisch mit `simplebuilding:`-IDs
  (nur Bugfix-Port). Welten, die von 26.2 auf 26.3 wechseln, laufen durch dieselben Alias-Tabellen (Dekodieren nach dem
  DataFixer). 26.4 folgt der 26.3-Struktur im nächsten Port-Run. Gemeinsamer Code `common/src/shared` muss weiter für 26.2
  kompilieren: extrahierte Klassen dort über Overlay-Stubs ausblenden oder in 26.2 belassen (Kopie im 26.2-Overlay) –
  je Stufe entscheiden, `check` bleibt grün.
- **Wiki/Zentrale:** `wiki/modules.py discover()` liest `modules.json` → neue Module automatisch; SB-Kapitel aus
  `wiki/manual.json` in `modules/<sub>/wiki/manual.json` umziehen; Zentrale-Seite zeigt Super/Sub-Baum aus
  `parent`/`bundle`; Wiki-Renders der Octets (119) wandern mit. `wiki/generate.py --all --check` je Stufe.
- **Testrunner:** neue Targets automatisch aus `modules.json` (`module-<id>-{fabric,neoforge,forge}-263`,
  `module-<id>-standalone-*`, `-client`); neu `migration-<loader>-263`; SB-Target lädt die Pflicht-Sub-Mods (Dev-Mods-
  Container/`hub-project-mods.gradle`); SB-Tests der extrahierten Bereiche ziehen in Modulkataloge um, Integrations-
  Tests (Hammer × Storage × Crucible) in den Integrations-Katalog. Fabric braucht je Sub-Mod `@GameTest`-Wrapper.
- **Release/Versionierung:** SB 2.0.0 (Strukturbruch), neue Sub-Mods starten bei 1.0.0, Simple Aesthetics 1.0.0;
  Super-Mods geben Versionsbereiche an (`>=1.0.0 <2.0.0`). Jede Sub-Mod eigenes Modrinth-/CurseForge-Projekt
  (Besitzer legt an). Changelog mit Migrationshinweis und Backup-Empfehlung; Brücken-Version 1.3.x-bridge.
- **Money:** `prices.json` (275 SB-Einträge) und Trade-Links aufteilen; Bedingungen `links:<sub>`.
- **Dev-Server/Testzentrale/Launch-Hub:** Testzentrale-Stationen je Sub-Mod über framework-Vertrag; Hub-Presets um neue
  Module ergänzen.

---

## 6. Offene Fragen an den Besitzer (max. 5)

Enderit und Upgrade-Vorlagen sind entschieden (§2.5). Die Agenten setzen die folgenden Punkte nach der jeweiligen
*Empfehlung* um, falls der Besitzer nichts anderes sagt:

- **F1 Tiegel-Kern:** Tiegel/Fässer/verstärkter Kessel aus `simplelib` nach `simplecrucibles` (Sandwiches bündelt dann
  Simple Crucibles) oder bleibt der Kern in simplelib und Simple Crucibles enthält nur SB-Teile + Seelenlava?
  *Empfehlung: umziehen (passt zu „Simple Crucibles inkl. Kessel“).*
- **F2 Tweaks-Port:** Reicht „entkoppeln“ = Simple Tweaks (Claims) läuft ohne SB, oder sollen auch Pads/Chunk-Loader/
  Teleporter (`com.simplebuilding.tweaks`, 95 Klassen) zurück nach `simpletweaks:` (mit Rück-Migration)?
  *Empfehlung: erst nur entkoppeln.*
- **F3 Grenzfälle:** Köcher, Kisten-Boote/-Loren und Rucksäcke → Storage; Ofen-Lore → Ovens; Trichter, Eimer und
  Schach-Octets bleiben in SB. Einverstanden?
