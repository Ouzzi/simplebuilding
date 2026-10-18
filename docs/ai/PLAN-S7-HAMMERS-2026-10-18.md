# Plan Stufe 7: Hämmer nach `simplehammers` (2026-10-18)

Branch `claude-s7-hammers` (von `claude-wave1` 740161a7c), Sitzung A. Grundlage: `PLAN-MODULAUFTEILUNG-2026-10-11.md`
(§1.2 Hammers, §1.4 Zyklen, §2.6 Axt/Hammer-Prinzip, Stufe 7), `PLAN-MODULFAMILIE-2026-10-16.md` (§5 F3), Vorbilder
`PLAN-S6-OCTETS-2026-10-17.md` / `PLAN-S5B-2026-10-16.md`, `PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`, AGENTS.md 15-19.

## 1. Ist-Zustand (git grep, Stand 740161a7c)

- **Kern (SB `common/src/shared`, 26.2 + 26.3):** `items/custom/SledgehammerItem` (751 Z.: Flächenabbau-Liste, Umformen
  Block→Treppe→Stufe und zurück, Achtel schnitzen, Diamant-/Quarzblock zerschlagen, Haltbarkeit), `util/SledgehammerUtils`
  (Abbautempo/-auswahl, Oktant-Auswahl), `util/SledgehammerUpgrades` (924 Z.: In-World-Aufwertung mit Schlägen, Aufträge je
  Seite, Animation-Phasen, Tooltips, eingebaute SB-Tabelle), `util/SledgehammerProgress` (SavedData Schlagfortschritt),
  `util/SledgehammerUsageEvent` (Flächenabbau beim Abbau, Loader-Events), `util/SledgehammerCrafting` (+`ShapelessRecipeMixin`:
  Hammer bleibt im Raster), `util/HammerCorners` (+`CarvedStairMixin`: Eigenschaft `carved` an Vanilla-Treppen),
  `data/SledgehammerUpgradeData` (Dateien `sledgehammer_upgrades`/`in_world_upgrades`), Mixins `BlockStateBaseMixin`
  (Abbautempo-Teiler, gemischt mit Strip Miner), Client `MultiPlayerGameModeHammerProgressMixin` (Exploit-Fix N65),
  `HeldItemRendererMixin`/`AvatarRendererMixin` (Ausholen), `ItemMixin` (Aufwertungs-Tooltips), `BlockHighlightRenderer`/
  `MultiBlockBreakingSupport` (Flächen-Umriss/-Risse, gemischt mit Stäben/Strip-/Vein-Miner).
- **Registrierung:** `ModItems` 642-654 (7 `registerSledgehammer`), Tag `simplebuilding:sledgehammer_tools`
  (`ModTags.Items.SLEDGEHAMMER_ENCHANTABLE`, auch in `#minecraft:enchantable/mining`), `#simplelib:hammers` (26.3 Overlay),
  Verzauberungen `break_through`, `override`, `radius` (`ModEnchantments`; `radius` auch Erzdetektor über `RADIUS_ENCHANTABLE`),
  Rezepte 7 + 3 Upgrade-Rezepte + 2 Schmiede-Rezepte (+Rezept-Advancements), Item-Modelle/-Definitionen, 7 Texturen
  (Besitzer-Kunst, eingecheckt), Lang 8 Item-/Tooltip-Schlüssel + Verzauberungen, Dorfbewohner-Trades (Werkzeugschmied 2),
  Money-Preise/-Links (7), Discover-Tipps, Guide-Seiten, Testzentrale (`FeatureStations`), FTB-Quest `stone_sledgehammer`.
- **Kopplung (≈110 Dateien, 282 `ModItems.*_SLEDGEHAMMER`, ≈40 `instanceof SledgehammerItem`):** SB-Blöcke mit Menü
  (`TieredChest`, `TieredShulkerBox`, Trichter, Öfen-Reste, `PlacedTemplate`, `PlacedSmallParts`, `DimensionalScrap`),
  `MiningUtils`, Strip/Vein/Versatility-Miner, `NihilVaultStorage`, `ChunkLoaderRegistry`, Astral-Tisch, Blaupause,
  `FlatToggles`, `DiscFlips`, `PlacedTemplates` (Besatz), `SledgehammerChips` (Splitter aus platzierten Teilen),
  `InWorldTransformations`/`TransformTargets` (In-World-Rezepte/Handhinweise, JEI `InWorldCategory`/`CauldronWorldCatalog`),
  `SledgehammerEntityInteraction` (Besatz-Aufwertung, 26.3 in Simple Trims), Tabs/Suchtab, Tooltips, Datagen
  (Rezepte, Modelle, Tags, Advancements `hammer/*`, Loot, Datentabellen), ≈45 Gametest-Klassen, 4 Client-Tests.
- **Fremdmodule:** simplecrucibles (Bau/Andocken über SB `CrucibleCompat.hammerUse`, gestufte Tiegel-Aufwertung N51),
  simpleovens/simplebeacons (In-World-Aufwertung über `#simplelib:hammers`), simpleblocks (`SawUpgrade.hammerMode =
  isModLoaded("simplebuilding")`), simpletrims (Tag `template_hammers` → `simplebuilding:sledgehammer_tools`), simpletweaks
  (Protect-/Claims-Tests mit `simplebuilding:*_sledgehammer`; Flächenabbau läuft serverseitig, wird nie gemeldet),
  simplelib/simpleovens/simplesandwiches-Tests (feste Id `simplebuilding:iron_sledgehammer`), simplemoney (Preise, Links,
  `MoneyDiscover`). Config: `server.tools.{sledgehammerUpgradeSeconds, reinforced|netherite|enderiteUpgradeDamagePerHit,
  diamondBlockPebbles}` (SB `ServerTuning`, an Clients gesendet).

## 2. Entscheidungen (selbst getroffen)

- **E1 Umfang Modul:** 7 Hämmer (`simplehammers:<tier>_sledgehammer`), Klasse + Motor: Flächenabbau, Abbautempo,
  Umformen, Achtel schnitzen (über SimpleLib `OctetCells`, nur mit Simple Octets), Diamant-/Quarzblock zerschlagen,
  In-World-Aufwertung (Aufträge, Schläge, Fortschritt, Material, Haltbarkeit) auf SimpleLibs `in_world_upgrades`, Hammer im
  Werkbankraster, `carved`-Treppen, Exploit-Fix-Mixin, die drei Hammer-Verzauberungen (`break_through`, `override`, `radius`),
  Rezepte, Tags (`#simplehammers:sledgehammers`, Mitglied von `#simplelib:hammers`, `#minecraft:enchantable/mining`,
  `#simplehammers:radius_enchantable`), Texturen (git mv), Modelle, Lang, Trades, eigener Tab, Guide-Buch, JEI-Infoseiten,
  Discover, Testzentrale-Station, Wiki, Money-Tabelle, Config. Hammers schaltet beim Laden die Axt-Wege ab (§2.6,
  `disableAxeWays` wandert aus `LibServices.hammersReplaceAxes`).
- **E2 Bleibt SB (Integration, SB-Blöcke):** Besatz (`PlacedTemplates`), Splitter (`SledgehammerChips`), Platten-Drehen
  (`FlatToggles`/`DiscFlips`), In-World-Rezeptkatalog + JEI-Kategorien (`InWorldCategory`, `CauldronWorldCatalog` inkl.
  Milchkessel), Handhinweise (`TransformTargets`), eingebaute SB-Upgrade-Tabelle (Datagen der SB-Dateien, Truhen/Shulker/
  Trichter/Kolben/Astral), Aufwertungs-Tooltips der SB-Maschinen, Advancements `hammer/*` (SB-Baum, Trigger über Hörer),
  Hand-/Avatar-Animation, Flächen-Umriss und -Risse (Client-Renderer gemischt mit Stäben/Minern), Oktant-Auswahl.
  Offen notiert: Umriss/Risse/Ausholen fehlen dem Modul allein (spielbar, nur Optik).
- **E3 Schnittstelle (Prinzip 3/6a):** neue SimpleLib-API `com.simplelib.api.HammerActions` (inhaltsfrei): Rechtsklick-
  Haken vor/nach der Rechteprüfung, Abbau-Veto (Besatz-Haltung), Flächen-Ersatz (Oktant), Rechteprüfung (SB
  `WorldPermissions`), Ereignis-Hörer (`reshape`, `diamond_crush`, `area_block`, `upgrade_netherite|enderite`), gestufte
  Aufwertungen (Tiegel N51, meldet simplecrucibles), Mehrblock-Ziele (Doppeltruhe, meldet SB), Zustand nach Aufwertung
  (Brecher-Kolben-Schaden), `areaBreaking()` für andere Module (Tweaks). Andere Module kennen nur diese API und Tags,
  nie `com.simplehammers`. simplecrucibles übernimmt Bau/Andocken mit dem Hammer und die Menü-Weitergabe (bisher SB
  `CrucibleCompat`), Advancement „Tiegel gebaut“ über einen Hörer der Crucibles-API.
- **E4 26.2 / gemeinsamer Code (Muster Stufe 6):** die verschobenen Kernklassen ziehen per `git mv` nach
  `common/src/mc26_2/java` (26.2 unverändert, SB registriert dort weiter `simplebuilding:*`), im 26.3-Overlay liegen
  **dünne Unterklassen gleichen Namens** (`SledgehammerItem extends com.simplehammers.item.SledgehammerItem`, ebenso
  `SledgehammerUtils`, `SledgehammerUpgrades`, `SledgehammerProgress`, `HammerCorners`, `SledgehammerCrafting`,
  `SledgehammerUsageEvent`), damit SB-Code und SB-Tests über die geerbten Statics den Modulmotor nutzen. `instanceof
  SledgehammerItem` in gemeinsamem Code → `SledgehammerItem.isHammer(...)`; `ModItems.*_SLEDGEHAMMER` → `HammerRefs`
  (Ids, Muster `OvenRefs`; Felder auf 26.3 `null`, `McVersion.HAMMERS_IN_SB`/`HAMMERS_NAMESPACE`). SB-Mixins mit Hammer-Teil
  (`BlockStateBaseMixin`, `CarvedStairMixin`, `ShapelessRecipeMixin`, `MultiPlayerGameModeHammerProgressMixin`) und SB-Loader-
  Events wirken auf 26.3 nicht mehr (Modul hat eigene), die SB-Klassen bleiben für 26.2.
- **E5 Ids/Migration:** ITEM 7 Hämmer, ENCHANTMENT 3, RECIPE + Rezept-Advancements (12 Rezepte) `simplebuilding:` →
  `simplehammers:` per `IdMoves` (Forge-Aliase automatisch). SB-Advancements `hammer/*` bleiben `simplebuilding:` (Kriterien
  nennen die neuen Item-Ids). Tag `simplebuilding:sledgehammer_tools` bleibt als Alias-Datei (SB, enthält
  `#simplehammers:sledgehammers`, ≥ 2 Versionen; simpletrims/simplelib zeigen auf den neuen Tag). SavedData-Name des
  Schlagfortschritts bleibt (alte Welten behalten angefangene Schläge).
- **E6 Config:** `config/simplehammers-server.json` (`enabled`, `upgradeSeconds`, `reinforcedDamagePerHit`,
  `netheriteDamagePerHit`, `enderiteDamagePerHit`, `diamondBlockPebbles`; gleiche Grenzen), einmalige Übernahme aus
  `simplebuilding.json` → `server.tools`. Serverlokal wie Ovens; Client rechnet mit dem eigenen Wert (Server beendet früher,
  Client lässt am getauschten Block los – vorhandener Mechanismus). SB-Schlüssel bleiben bis Stufe 9 (wie Ovens) sichtbar,
  wirken auf 26.3 nicht mehr (offen notiert). Schalter „Enable Simple Hammers“ in SB (`enableSimpleHammers`, `SubMods`):
  aus = Hämmer bleiben registriert, Tab ohne Guide, Rechtsklick-Wege/Flächenabbau aus (Abbau wie Spitzhacke).
- **E7 Enderit (F3):** Enderit-Hammer bleibt registriert und herstellbar (Schmiede-Rezept mit `#c:ingots/enderite` +
  Enderit-Vorlage, Bedingung `simplelib:material_requested` enderite); Flag `SimpleHammers.ENDERITE_NEEDS_SIMPLE_ENDERITE =
  false` + TODO: mit Simple Enderite nur dort freischalten. Werkzeugmaterial Enderit als eigene Konstante (`HammerTiers`).
- **E8 Abhängigkeiten:** `simplehammers` = Pflicht-Sub-Mod von SB 26.3 (gebündelt, Fabric/NeoForge/Forge, SB kompiliert gegen
  Overlay-Unterklassen), Pflicht `simplelib`; optional `simplebuilding`, `simpleoctets`, `simplecrucibles`, `jei`. simpleblocks
  Säge: Hammer-Weg, wenn `simplehammers` geladen ist (nicht mehr SB). Simple Tweaks: Tests auf `#simplelib:hammers`.

## 3. Umsetzung (Commits)

1. Plan (dieses Dokument).
2. SimpleLib `HammerActions` + Tiegel-Haken in simplecrucibles (Bau/Andocken/Menü/gestuft, Hörer), Tests unverändert grün.
3. Modul-Gerüst `modules/simplehammers/{shared,fabric,neoforge,forge,clienttest,wiki,tools}` nach `simpleoctets`:
   `SimpleHammers` (init, IdMoves, Config, Material-Anfragen, Axt aus, Teststation), `registry/HammerRegistry`, `HammerTiers`,
   Motor-Klassen (`item/SledgehammerItem`, `engine/*`), Mixins (Abbautempo, `carved`, Werkbank, Client-Fortschritt),
   Loader-Einstiege (Abbau-Event, Server-Tick, Abmelden), Ressourcen (Texturen git mv, Modelle, Rezepte, Tags,
   Verzauberungen, Lang EN/DE, Trades), Tab, Guide, Discover, JEI-Info, Tests (`HammerTests`), `modules.json`, Money.
4. SB: Overlay-Unterklassen + 26.2-Zwillinge, `HammerRefs`, `McVersion.HAMMERS_IN_SB`, Haken-Anmeldung (`LibServices`),
   Aufrufer umstellen, Datagen ohne Hämmer (26.3), Lang/Tabs/Suchtab/Testzentrale/Tooltips/Discover/Wiki-Quellen,
   Advancement-Kriterien, Gradle-Bündel + `depends`, Schalter, Tests auf Ids.
5. Fremdmodule: simpleblocks, simpletrims-Tag, simplelib/simpleovens/simplesandwiches/simpletweaks-Tests, simplemoney.
6. Erzeugen auf sb-test: Datagen 26.3, `discover.py`, `module_guides.py`, `generate_links.py`, `wiki/generate.py --all`.

## 4. Risiken

- Doppelte Wirkung (SB- und Modul-Mixin/-Event zugleich) → SB-Teile hinter `McVersion.HAMMERS_IN_SB`.
- `carved`-Eigenschaft an Vanilla-Treppen: genau ein Mixin darf sie anlegen (sonst doppelte Property, Absturz).
- Registrierreihenfolge NeoForge/Forge: SB-Code liest Hämmer nur über `HammerRefs` nach der Registrierung.
- Verzauberungs-Umzug: gespeicherte Verzauberungen per `IdMoves`; Datagen-Ausgabe 26.3 darf die SB-Dateien nicht mehr liefern.
- Masse der SB-Tests (≈45 Klassen) → mechanisch über `HammerRefs`, Compile-Fehler auf sb-test iterativ.

## 5. Prüfung (sb-test)

Compile 26.3 fabric/neoforge/forge + 26.2 `:compileJava`; SB-Filter `sledgehammer`, `sledgehammer_upgrade`,
`sledgehammer_octet`, `migration`, `data_integrity`, `crucible`; `module-simplehammers-{fabric,neoforge}-263`, Standalone
simplehammers fabric+neoforge, simpleblocks `saw`, simpletweaks `protect`, simplecrucibles, simplelib; Checks
`checkModuleGuides checkModuleData checkWiki checkDiscover checkMultimod`. Nicht geprüft (bewusst): Client-Sicht.
