# Plan b-chisels07 (Sitzung B, 07.11.2026) – Simple Chisels als eigener Pflicht-Sub-Mod (WE25-7)

Auftrag: /root/sb-tools-b/b07-auftrag.md, Queue WE25-7 „Untermod Simple Chisels (Meißel auslagern)“. Basis hub/claude-wave1
4f2722c8a (Bündel 120). NUR 26.3 Fabric. Muster: Split-Stufe 7 Hämmer (PLAN-S7-HAMMERS-2026-10-18: Felder auf 26.3 `null`,
`HammerRefs`, `McVersion.HAMMERS_IN_SB`, Constructor's Touch per Id), b-pads01 (newmod, Config-Übernahme, IdMoves, Tab, Guide).
Parallel baut b-btools07 „Simple Building Tools“ (Stäbe, Blaupausen, Oktanten): gemeinsame Dateien (modules.json, ConfigOptions,
SimplebuildingConfig, enabled-mods, Lang, creative_tab_order) nur additiv.

## 1. Ist-Stand (grep, 4f2722c8a)
- Kern `common/src/shared` (26.2 + 26.3): `items/custom/ChiselItem` (845 Z.: Tabellen je Stufe stone/iron/diamond/netherite/enderite,
  Meißel vorwärts, Schleichen rückwärts (2 Schaden), Constructor's Touch = erweiterte Tabelle, Fast Chiseling (Abbautempo,
  Abklingzeit), intuitive Ausrichtung, letzter Zielblock im Tooltip, Holzschnitzen über `woodwork/PotteryCarving`),
  `data/ChiselTables` (Datenpaket `data/<ns>/chisel_transformations/*.json`, Reihenfolge simplebuilding zuerst), Laden/Sync über
  `ModDataTables`/`DataTablesSyncPayload` (gemeinsam mit den Hammer-Upgrades), `ModItems` 7 Meißel + 6 Alt-Spatel
  (`LegacySpatulaMigration` macht beim Beitreten Meißel daraus), Verzauberung `fast_chiseling` (nur Meißel), Tags
  `simplebuilding:chisel_tools` (+ in `chisel_and_mining_tools`, `constructors_touch_enchantable`, Vanilla `enchantable/durability|vanishing`),
  Config `server.tools.<stufe>ChiselCooldownTicks` (ServerTuning, 2..200), Statistik `chisel_uses`, Auslöser `chisel`, Zähler `chisel_steps`.
- SB-eigene Ketten in den Meißel-Tabellen: End-Steinfamilien Astralit/Nihilit/Ender-Quarz (Enderit-Stufe), hohle entrindete Stämme
  (Holzwerk, Stein-Stufe); Overlay-Dateien für Simple End-Hölzer und rissiges Eis (Simple Blocks) liegen schon als Datenpaket-Dateien.
- Nutzer: SB-Tab „Tools“ (Zeile `chisels`, Icon Eisenmeißel), Advancements `building/root`, `chisel/*`, `tiers/*` (Kriterien),
  Loot-Injektion (Meißel + Fast-Chiseling-Bücher), Händler (Werkzeugschmied 3: 3 Meißel, Bibliothekar 3: Buch), In-World-Export/JEI
  (`InWorldTransformations.chisel`, `InWorldRecipeCatalog`), Handhinweis (`TransformTargets`), Testzentrale (`TestCentreSections`,
  `FeatureStations`), Guide, Discover, Wiki, Money-Preise, FTB-Quests, Versatility-Priorität, ≈30 Testklassen.
- Fremdmodule: simplevisuals (Tag `simplevisuals:frame_chisels`), simplehammers (liest Constructor's Touch per Id, eigene Kopie der
  Ausrichtung), simplemoney/simplelib-Tests (feste Id `simplebuilding:iron_chisel`/`diamond_chisel`).

## 2. Entscheidungen (selbst getroffen, begründet)
1. **Modul `simplechisels`** per `newmod.py --parent simplebuilding --bundle required --config-switch true --enderite`. Paket
   `com.simplechisels`. requires `simplelib`; optional `simplebuilding`, `simpleores` (Enderit-Rezept), `jei`. Kein Pflicht-Simple-Ores
   (wie Hämmer): ohne Simple Ores bleibt der Enderit-Meißel registriert, sein Schmiederezept lädt nur mit Simple Ores.
2. **Inhalt Modul:** 7 Meißel `simplechisels:<stufe>_chisel` (Werte wie bisher: Haltbarkeit, Abklingzeit, Verzauberbarkeit,
   Netherit/Enderit feuerfest), `ChiselItem`-Kopie ohne SB-Bezüge, eigene Tabellen-Klasse + Ladeliste (`chisel_transformations`
   aller Namensräume, `simplechisels` zuerst) + Client-Sync (eigenes Paket `simplechisels:tables`), Verzauberung
   `simplechisels:fast_chiseling` (JSON), Tag `simplechisels:chisels` (+ Vanilla `enchantable/durability|vanishing`), 12 Rezepte
   + Rezept-Advancements (Upgrade-Rezepte nutzen SBs `count_based_smithing` → Ladebedingung simplebuilding, wie Hämmer), Modelle,
   Item-Definitionen, Texturen (Kopie, SB behält seine für 26.2), Lang EN/DE, Tab „Simple Chisels“, Guide-Buch, Discover, Wiki,
   Preise, Testzentrale-Station, Config. Vanilla-Ketten = Standarddateien `data/simplechisels/chisel_transformations/<stufe>_<tabelle>.json`.
3. **Spatel entfallen im Modul:** IdMove `simplebuilding:<stufe>_spatula` → `simplechisels:<stufe>_chisel` (genau das, was
   `LegacySpatulaMigration` tat, jetzt auch in Kisten und ungeladenen Chunks). SB-Spatel-Felder auf 26.3 `null`, Migration aus.
4. **Bleibt SB (Integration):** Constructor's Touch (SB-Verzauberung, Modul liest per Id `simplebuilding:constructors_touch`; ohne
   SB hat kein Item sie), Range, Advancements `building/*`, `chisel/*`, `tiers/*` (Kriterien nennen die neuen Ids), Statistik/
   Auslöser/Zähler (über Hörer), Holzschnitzen (`PotteryCarving` als Haken), SB-Ketten als SB-Datenpaket-Dateien
   (`data/simplebuilding/chisel_transformations/stone_chisel.json` = hohle Stämme, `enderite_chisel.json` = End-Steinfamilien; auf 26.3
   schreibt die SB-Datagen nur noch Ketten mit Nicht-Vanilla-Blöcken), Loot-Injektion und Händler (Ids → simplechisels), Handhinweis-
   Animation, In-World-Export/JEI-Kategorie „Meißel“, Testzentrale-Abschnitt, Versatility-Priorität (Tag statt Klasse).
5. **Schnittstelle (Prinzip 3/6a):** neue inhaltsfreie SimpleLib-API `com.simplelib.api.ChiselActions`: Rechtsklick-Haken vor dem
   Umformen (SB: Holzschnitzen), Ereignis-Hörer (`chisel` → SB Statistik/Auslöser/Zähler), veröffentlichte Werkzeugtabellen
   (`Tool`: Item, vier Abbildungen, Abklingzeit, Spatel-Flag) für SBs Export/JEI/Testzentrale. Handhinweis über vorhandenes
   `GadgetActions.registerItemHint` (SB `TransformTargets` fragt es schon). Rechte: framework `Protection` + `HammerActions.mayChange`
   (SB meldet dort seine WorldPermissions, gilt für jedes Werkzeug) – Kopie von `HammerProtection`. SB importiert keine Modulklasse.
6. **26.2 unverändert** (`McVersion.CHISELS_IN_SB`: 26.2 `true`, 26.3 `false`; `CHISELS_NAMESPACE`). SB-Felder `*_CHISEL`/`*_SPATULA`
   auf 26.3 `null`, Zugriff per `util/ChiselRefs` (Muster HammerRefs). SB-Datagen überspringt auf 26.3 Meißel-Modelle, -Rezepte,
   `fast_chiseling`, Vanilla-Ketten, Spatel-Tags. `ModEnchantments.FAST_CHISELING` zeigt auf 26.3 auf `simplechisels:fast_chiseling`.
7. **IdMoves** `simplebuilding:` → `simplechisels:`: ITEM 7 Meißel + 6 Spatel (→ Meißel), ENCHANTMENT `fast_chiseling`, RECIPE +
   Rezept-Advancement der 12 Rezepte. Datenkomponente „letzter Zielblock“: eigene `simplechisels:last_target` (SBs
   `coordinates` gehört auch anderen Items, bleibt; alte Meißel verlieren nur die Tooltip-Zeile bis zum nächsten Klick).
8. **Config** `config/simplechisels-server.json`: `enabled`, `cooldownTicks.{stone,copper,iron,gold,diamond,netherite,enderite}`
   (2..200, Standard wie bisher), einmal aus `simplebuilding.json` → `server.tools.<stufe>ChiselCooldownTicks` übernommen;
   `/simplechisels config`. SB blendet die 7 Optionen auf 26.3 aus (`ConfigOptions.MOVED_TO_CHISELS`). Schalter „Enable Simple
   Chisels“ (newmod): aus = Tab leer, Meißel formen nicht um (bleiben registriert). `tools.enableChiselAnimation` (Client) bleibt SB.
9. **Ein Tab** „Simple Chisels“ (Meißel, Fast-Chiseling-Bücher, Guide) im Modul, Reihenfolge aus `creative_tab_order.json` (newmod);
   SB-Tab „Tools“ ohne Meißel-Zeile auf 26.3 (Icon → anderes SB-Werkzeug).
10. **Kein neues Optik-Asset** (Texturen kopiert); Mod-Icon = newmod-Platzhalter (Neuzeichnung offen, wie bei jedem neuen Modul).

## 3. Tests
- Modul (`module-simplechisels-fabric-263` + standalone): Umformen vor/zurück + Schaden, Spatel-/Meißel-IdMoves, Verzauberung-Id,
  Datenpaket-Tabellen (Reihenfolge, fremde Datei ergänzt, unbekannter Block übersprungen), Fast Chiseling verkürzt Abklingzeit,
  Config-Übernahme, Schalter aus, Tab-Inhalt, Enderit-Rezept nur mit Simple Ores, Smoke.
- SB auf 26.3: Meißel-Tests (`ChiselTests`, Spatel-/Migrationsteile) überspringen bzw. auf `ChiselRefs`; Querschnitte
  `simplebuilding:data_integrity*` (+ config_option, chisel, in_world_export, modpack gezielt).
- Lokal: javac-Typprüfung (`/root/sb-tools-b/b-guide01-javac.sh`) Modul + geänderte SB-Klassen, JSON-Prüfung, check_data.py.
- sbt (EIN Lauf): Compile `:mc26_3:fabric:compileJava :mc26_3:fabric:compileGametestJava :modules:simplechisels:fabric:compileJava`,
  `:mc26_3:fabric:runDatagen` + Diff-Ausgabe (Abgleich mit den Hand-Änderungen), Modul-Targets, Kern-Filter.

## 4. Risiken
- Ladereihenfolge: SB-Datagen/Tab lesen Modul-Items per Id → `initializeFirst("simplechisels")`.
- Hand-gepflegte 26.3-Generate (`mc26_3/generated`, `removed-on-26.3.txt`) weichen von der Datagen ab → Datagen im sbt-Lauf, Diff prüfen.
- Doppelte Wirkung: SB lädt/synct auf 26.3 keine Meißel-Tabellen mehr (`CHISELS_IN_SB`).

## 5. Stand / Abweichungen (07.11.)
- **Abweichung Felder:** statt `null` (Hämmer) zeigen die SB-Felder `*_CHISEL` auf 26.3 auf die Modul-Items (Muster Pads, Typ jetzt
  `Item`); so bleiben Tabs, Datagen, Loot, Advancements und die meisten Tests unverändert. Nur die Spatel-Felder sind auf 26.3 `null`.
- **Abweichung Tabellen-Standard:** das Modul liest seine eingebauten Ketten aus den eigenen Standarddateien im Jar (`BuiltInChains`),
  keine zweite Java-Kopie der Ketten.
- SB-Tests mit Meißel-Verhalten (`ChiselTests`, Constructor's Touch-/Fast-Chiseling-Tests, Spatel-Migration, Datenpaket-Tabellen)
  überspringen auf 26.3 wie bei Pads; ihr Inhalt läuft als Modultest (10 Tests, davon `with_simplebuilding` nur mit SB).
- Händler (SB-Bedingungen `simplebuilding:config`, `weighted_enchant`) bleiben SB: 26.3-Overlay-Dateien mit den neuen Ids;
  `in_enchanting_table` als Overlay ohne Fast Chiseling (das Modul trägt es selbst ein).
- Handbuch-Textur und Mod-Icon: Platzhalter (Generator-Farben), Besitzer-Wahl offen.
- Lokal (sb-agent): javac-Typprüfung grün bis auf Umgebungsfehler (Access-Widener/Datagen-API), precheck grün bis auf
  `wiki/generate.py` (lokales Pillow zu alt) → Wiki im sbt-Lauf auf sb-test.
- Nicht gemacht: NeoForge/Forge (Regel nur 26.3 Fabric; SB auf NeoForge/Forge 26.3 hätte ohne Modul-Port Luft-Felder), Client-Läufe.
