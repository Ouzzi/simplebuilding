# Plan b-s9-release (21.10.) – Modulaufteilung Stufe 9 „Release/Aufräumen“

Branch `b-s9-release` (von `claude-wave1` ccc43f28a). Grundlage: `PLAN-MODULAUFTEILUNG-2026-10-11.md` §3.4, §4 Zeile 9, §5
(Release/Versionierung) und die „Stufe 9“-Vermerke der Stufen 3c, 4, 7, 8 (QUEUE.md:87/89/721, PLAN-S7-HAMMERS:80/142,
PLAN-B-S8B3:41/102, PLAN-B-S8C1:128, PLAN-B-S8C2:105, Memory uebergabe-A.md:124/242, uebergabe-B.md:326/341/352).

## 1. Ist-Zustand (gesammelte Stufe-9-Reste)

| # | Rest | Befund |
|---|---|---|
| R1 | SB-Config Ofentempo `server.machines.{reinforced,netherite,enderite}FurnaceSpeed` | 26.3: einziger Leser `ServerTuning.furnaceExtraTicks` → SB-Öfen/Ofen-Loren, die auf 26.3 nicht registriert sind (`OVENS_IN_SB=false`); Simple Ovens übernimmt die Werte einmal aus `simplebuilding.json` |
| R2 | Hammer-Schlüssel `server.tools.{sledgehammerUpgradeSeconds, reinforced/netherite/enderiteUpgradeDamagePerHit, diamondBlockPebbles}` | Leser nur im 26.2-Zwilling (`common/src/mc26_2`), auf 26.3 `HammerConfig` (Erst-Übernahme) |
| R3 | `tools.invertBundleInteractions`, `server.features.backpack`, `server.loot.{betterChestPercent, reinforced/netherite/enderiteShulkerPercent, endermitesPerRareShulker}` | Bündel/Köcher/Rucksack/seltene Funde sind auf 26.3 Simple Storage (`STORAGE_IN_SB=false`); die Modul-Config gilt |
| R4 | `InWorldTransformations.diamondCrush` nennt SBs Kieselzahl | 26.3-Katalog (Wiki/JEI-Export) muss Simple Hammers' `HammerConfig.diamondBlockPebbles` zeigen (gleicher Standard 81) |
| R5 | SB-Tabs/Suchtab/Guide/Testzentrale nur über IDs/Verträge | Audit (grep `com.simple{storage,ovens,hammers,crucibles,octets,blocks,mobheads}` in SB-Code): Tabs, Suchtab, Guide und Testzentrale importieren **keine** Modulklasse; Modul-Items stehen dort per Id (`*Refs`, `McVersion.*_NAMESPACE`), Testzentrale liest `framework TestCentreStations` (`dev/testcentre/ModuleStations`). Modulklassen nutzt SB nur für Pflicht-Sub-Mod-Technik (Hammer-Engine, Octet-Zellen, Tiegel-API) – außerhalb dieses Punkts. → erledigt, nur Beleg |
| R6 | `GuideChapters`-Leser (3c) | Vertrag in framework, **kein** Modul meldet Kapitel an; jedes Modul hat sein eigenes Buch (`tools/guides/module_guides.py`) |
| R7 | Wiki-Umzug | SB-`wiki/manual.json` hat noch Kapitel zu umgezogenen Inhalten (`backpacks`, `bundles`, `quivers`, `placed_bundles`, `crucible_parts`, Teile von `enchant_storage_player`, `loot_balance`); dieselbe Datei beschreibt aber auch 26.2 (dort SB-Inhalt) |
| R8 | Versionen | `gradle.properties mod_version=1.3.1` (eine Nummer für alle SB-Linien, Jar `simplebuilding-<mc>-<v>`); neue Sub-Mods schon 1.0.0 (`ext.moduleVersion`), SB verlangt `>=1.0.0` |
| R9 | Changelog | gibt es nicht (Plan §3.4/§5: Migrationshinweis + Backup-Empfehlung) |
| R10 | Brücken-Version 1.3.x-bridge | Rück-Aliase `simple<sub>:*` → `simplebuilding:*` in einem SB 1.3.x vor der Aufteilung (Downgrade-Pfad) |

## 2. Entscheidungen

- **E1 Ausblenden (R1–R3)**: neue Liste `ConfigOptions.MOVED_TO_SUB_MODS` (Pfad → Flag), `Option.hidden()` = Flag
  der Linie sagt „nicht mehr in SB“ (`OVENS_IN_SB`/`HAMMERS_IN_SB`/`STORAGE_IN_SB`). Felder bleiben unverändert
  (Name, Typ, Standard, Lang-Schlüssel) → alte `simplebuilding.json` lädt fehlerfrei, Werte bleiben beim Speichern erhalten,
  Erst-Übernahme der Module liest sie weiter. Ausgeblendet in: AutoConfig-Bildschirm (Fabric über
  `AutoConfigClient.getGuiRegistry`, NeoForge im eigenen Registry-Aufbau; Prädikat-Transformer → keine Zeile, kein
  Tooltip), Forge-Bildschirm (`ForgeConfigScreen` überspringt, schreibt die Entwurfswerte unverändert zurück),
  `/simplebuilding config list` + Vorschläge, Wiki-Config-Liste (26.3). `config get/set/reset` mit vollem Pfad bleiben
  (Skripte/Kompatibilität, harmlos). 26.2 blendet nichts aus (Flags `true`).
- **E2 Kieselzahl (R4)**: statisches `SledgehammerItem.catalogDiamondPebbles()` in beiden Zwillingen (26.2:
  `ServerTuning`, 26.3-Overlay: `HammerConfig.diamondBlockPebbles` – Pflicht-Sub-Mod, Overlay importiert Simple Hammers
  bereits); `InWorldTransformations` ruft es.
- **E3 Audit (R5)**: kein Code nötig; der Beleg in §1 reicht (kein zusätzlicher Wächter, YAGNI).
- **E4 GuideChapters (R6)**: **nicht** bauen. Grund: die Modulbücher sind eigenständig (Besitzer-Entscheid „Buch je
  Modul“); ein Leser in SBs Mega-Guide hätte keinen Anmelder und würde Inhalte doppeln. Der Vertrag bleibt (framework-API,
  harmlos). Folgepunkt nur, wenn ein Modul Kapitel in SBs Buch liefern soll.
- **E5 Versionen (R8)**: `mod_version` 1.3.1 → **2.0.0** (Plan §5: Strukturbruch der 26.3-Linie). Das Schema hat nur eine
  SB-Nummer; die Jars tragen die MC-Version vorn (`simplebuilding-26.2-2.0.0`), 26.2/1.21.11 sind inhaltlich unverändert
  (im Changelog vermerkt). Sub-Mods bleiben, wie ihre Stufen sie gesetzt haben. Kein Tag, nichts veröffentlicht.
- **E6 Changelog (R9)**: `CHANGELOG.md` (EN, Modrinth/CurseForge-tauglich) für SB 2.0.0: Aufteilung, Pflicht-Sub-Mods,
  ID-Umzüge mit automatischer Migration, **Welt sichern**, verschobene Config-Schlüssel, 26.2 unverändert.
- **E7 Zurückgestellt (Teil 2, konkret in Queue/Memory)**: R7 Wiki-Umzug (braucht Linien-Trennung im Wiki: 26.2-Kapitel
  bleiben SB, 26.3 verweist auf Modul-Wiki), R10 Brücken-Version (eigener Build aus einem Stand vor Stufe 3d +
  Rück-Alias-Tabelle; erst sinnvoll, wenn 2.0.0 veröffentlicht wird – Besitzer-Freigabe), Backup-Marker
  `world/simplebuilding-split-backup.marker` (§3.4, noch nicht vorhanden).

## 3. Dateien

- `common/src/shared/java/com/simplebuilding/config/ConfigOptions.java` (Liste, `hidden()`, `visible()`)
- `ConfigOptions.hiddenFields()` + Prädikat-Transformer direkt in den Fabric-/NeoForge-Client-Klassen (Forge 26.3 hat kein volles cloth `gui.registry` im Klassenpfad)
- `src/main/java/com/simplebuilding/SimplebuildingClient.java`, `neoforge/.../SimplebuildingNeoForgeClient.java`,
  `mc26_3/forge/.../ForgeConfigScreen.java`, `common/.../command/SimplebuildingCommand.java`
- `common/src/mc26_2/.../SledgehammerItem.java`, `mc26_3/overlay/.../SledgehammerItem.java`, `InWorldTransformations.java`
- `wiki/config_metadata.py`, `wiki/generate.py` (26.3-Liste ohne ausgeblendete Optionen)
- Tests: `ConfigOptionTests` (neu `movedOptionsAreHiddenButStillLoad`, `list tools.` zählt sichtbare) + Katalog + Fabric-Adapter
- `gradle.properties`, `CHANGELOG.md`, `readme.md` (Versionszeile), `docs/MODS-UND-FEATURES.md` falls Config erwähnt

## 4. Verifikation

Compile 3 Loader 26.3 + 26.2 (`:compileJava`), fabric-263/neoforge-263 `simplebuilding:config_option*` und
`simplebuilding:inworld*`/`sledgehammer*`, Standalone `module-simplehammers-standalone-fabric-263` (unverändert, Probe),
gen (Guides, Wiki --all, Discover) + `./gradlew check`. Nicht: Client-Läufe (verboten) → Sicht des Config-Bildschirms offen.

## 5. Ergebnis

**Teil 1 umgesetzt (21.10.):** E1, E2, E5, E6 wie geplant, E3 belegt, E4 entschieden.
Abweichung: der AutoConfig-Transformer steht direkt in `SimplebuildingClient` (Fabric) und `SimplebuildingNeoForgeClient`
(`ConfigOptions.hiddenFields()`), nicht in einer gemeinsamen Klasse – Forge 26.3 hat kein cloth `gui.registry` im Klassenpfad.
Belege: Compile `:mc26_3:{fabric,neoforge,forge}:compileJava :compileJava :neoforge:compileJava` Exit 0; fabric-263 und
neoforge-263 `simplebuilding:config_option*` je 19/19 (inkl. neuem `config_option_game_test_moved_options_are_hidden_but_still_load`);
Standalone Simple Ovens/Hammers/Storage (fabric) 18+14+113 = 145/145; Guides/Wiki --all/Discover erzeugt (Wiki-Config-Liste
26.3 −15 Optionen, `modules.js` 2.0.0); `./gradlew check` siehe Queue-Beleg.
Nicht geprüft: Client-Sicht der Config-Bildschirme (Client-Läufe verboten), 26.2-Gametests (nur Compile), Forge-263-Gametests.

**Offen (Teil 2):** R7 Wiki-Umzug, R10 Brücken-Version 1.3.x-bridge, Backup-Marker (§3.4), SimpleLib-Version
(0.2.0 seit Stufe 3a trotz neuer APIs → vor Release 0.3.0 und `simplelib >=0.3.0` in allen Modul-Metadaten),
volles Gate + alle Standalone + alle Migrations-Targets (Hauptsitzung).
