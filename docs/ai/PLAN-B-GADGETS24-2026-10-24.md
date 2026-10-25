# Plan b-gadgets24 (24.10.2026): OW24-2 Teil Simple Gadgets – neuer Pflicht-Sub-Mod `simplegadgets`

Branch `b-gadgets24` (Basis claude-wave1 73c0d7be0), Sitzung B. Grundlage: `PLAN-MODULFAMILIE-2026-10-16.md` (Zeile
„Simple Gadgets (neu)“, Abgrenzung, Familienregel, Besitzer-Antworten F2/F5 vom 16.10.), Hauptplan
`PLAN-MODULAUFTEILUNG-2026-10-11.md`, Vorbild Umzug mit Id-Erhalt `PLAN-B-AUD5-2026-10-24.md` §5 (Simple Lights) und für
Items mit Code auf beiden Linien `PLAN-B-S8C1-STORAGE-2026-10-20.md` (`McVersion.*_IN_SB`, SB-Klassen bleiben für 26.2).
Nur 26.3 Fabric (Besitzer 22.10.).

## 1. Ist-Zustand: die Gadgets im Code (git grep, Stand 73c0d7be0)

Abgrenzung (Plan 16.10.): was man in der Hand hält und benutzt (Messen, Drehen, Anziehen, Orten) → Gadgets.
Hämmer (simplehammers), Meißel, Bauzauberstäbe, Blaupausen, Hängematten, Pads bleiben (F2). Simple Enderite ist „später“.

| Gadget (Anzeige EN/DE) | Id heute | Klasse / Quelle | Kopplung in SB (Auszug) | Stufe |
|---|---|---|---|---|
| Gauge / Messuhr | `simplebuilding:velocity_gauge` | `items/custom/VelocityGaugeItem` (162 Z.) | HUD `SpeedometerHudOverlay` + gemeinsames `HudPanel`/`HudFrame`/`ModHud` (Stapel Octant/Gauge/Rod, SB-Config `hudPosition*`/`hudScale`/`showModHud`), `client/GaugeAutowalk` + `KeyboardInputMixin`, Modell-Eigenschaft `GaugeNeedleModelProperty` (`simplebuilding:gauge_needle`), `RangeReach` (Range ohne Blockreichweite), Verzauberungen Range/Constructor's Touch (SB, per Schlüssel), Rezept mit `simplebuilding:copper_core`, Advancement `gadgets/speed_reader`, Tags `range_enchantable`/`constructors_touch_enchantable`, 16+1 Item-Modelle (Datagen), 18 Texturen (`mc26_3/overlay`) | **G1 (dieser Branch)** |
| Transposer / Rotator | `simplebuilding:rotator` | `RotatorItem` (606 Z.) + `util/RotatorSwap`, `util/TransformTargets` | `AnvilRechargeable` (SB-Amboss-Mixin, Enderperlen), `ServerTuning`, Config `tools.rotator*`, `ModTriggers.ROTATE`, In-World-Katalog (`InWorldTransformations`, JEI `InWorldCategory`), Testzentrale `FeatureStations` | G2 |
| Resonance Rod / Resonanzstab | `simplebuilding:amethyst_lens` | `tweaks/item/LaserPointerItem` (SB-internes Tweaks-Paket) | `AnvilRechargeable` (Redstone), HUD-Slot ROD, `LaserRenderer`, `TweaksClient` | G2 |
| Detector / Detektor | `simplebuilding:detector` | `OreDetectorItem` (870 Z.) + `util/PlacedDetectors` | abgelegte Geräte `PlacedTemplates`/`PlacedTemplateBlock(Entity)` (SB-Block), `ModBlocks.ASTRALIT_ORE/NIHILITH_ORE`, `ServerTuning.oreDetector*`, Verzauberungen Radius/Touch, Glint-Renderer | G3 |
| Attractor | `simplebuilding:magnet` | `MagnetItem` (332 Z.) + `util/PlacedAttractors`, `AttractorFilter` | wie Detektor (abgelegt), `ItemEntityMixin`/`ItemFrameEntityMixin`, `RangeReach`, Config `tools.magnetRangeMultiplier`. **„Attractor aus simpletweaks claims“ = dasselbe Item:** Claims (`simpletweaks/claims`, Stage4/ToolClaimTests) schützt nur gegen das SB-Item `magnet` (per Id); es gibt dort kein eigenes Gerät → beim Umzug nur Claims-Tests/Schutz auf die neue Id umstellen | G3 |
| Echo Sounder / Echolot | `simplebuilding:echo_sounder` | `tweaks/item/EchoCompassItem` | `TweaksConfig`, `EchoCompassFov`, `ModTriggers`, `ServerTuningInfo`, NeoForge/Forge-Events. Existiert schon („Echolot (neu)“ im Plan 16.10. ist inzwischen gebaut) | G4 |
| Color Brush / Farbpinsel | `simplebuilding:color_brush` | `ColorBrushItem` (292 Z.) | Tinte aus SB-Farbkästen (`PaintBoxItem`, `PaintBoxContents`, `PlacedPaintBoxBlock`, Enderit-Stufe), Verzauberung Color Palette, Anvil-Mixin, Brush-Ink-Modell-Eigenschaft. Farbkästen bleiben SB (Behälter, Enderit-Stufe); Pinsel liest sie per Komponente/Id | G4 |
| Octant / Oktant (+16 Farben) | `simplebuilding:octant`, `octant_<farbe>` | `OctantItem` + `OctantScreen`, 2 Pakete, `OctantShape/Surface/HammerArea/CauldronWash` | tief im Bau-Kern: Blaupausen (`BlueprintScanner/Code/ShapeFill`), Baustab-Vorschau, Hammer-Bereich, abgelegte Geräte, Testzentrale (`TcOp/TcCanvas`), Loot, Datafix, `MouseMixin`, Rangefinder-HUD | G5 |

Geprüft, ob Teile schon woanders liegen: **kein** Gadget liegt in `simpleoctets` (dort nur Achtelblöcke) oder einem anderen
Modul; `simpletools` hat keine Geräte-Items (Bürste `BiomeBrush` ist QoL-Verhalten der Vanilla-Bürste, bleibt). Eisenstab
(`iron_pole`) ist ein **Block** (Blitzableiter, Zutat für Rotator/Resonanzstab) → bleibt SB, Modul-Rezepte nennen ihn bedingt.

## 2. Zuschnitt und Reihenfolge

Der Gesamtumfang (7 Familien, ~3500 Zeilen Item-Code, 30+ SB-Kopplungsstellen, ~70 Testdateien) passt nicht in einen Branch.
Stufen, je eigener Branch/Gate, Reihenfolge nach Kopplung (klein → groß):

- **G1 (b-gadgets24): Modul-Gerüst + Messuhr.** Alles, was jede weitere Stufe braucht (Modul, Schalter, Tab, Handbuch,
  Discover, Teststation, Wiki, Money, Icon, modules.json, IdMoves, Migrationstest, SimpleLib-HUD-Vertrag) plus das
  kleinste Gadget mit allen Kopplungsarten (Client-HUD, Mixin, Modell-Eigenschaft, SB-Verzauberungen, bedingtes Rezept).
- G2: Rotator + Resonanzstab (gemeinsam `AnvilRechargeable` → SimpleLib, HUD-Slot ROD, In-World-Katalog per Id).
- G3: Detektor + Attractor (gemeinsam abgelegte Geräte: SimpleLib-Vertrag für `PlacedTemplates`, Claims auf neue Id).
- G4: Echolot + Farbpinsel.
- G5: Oktant (braucht SimpleLib-Vertrag „Bereichsauswahl“ für Blaupause/Baustab/Hammer; größter Teil).

## 3. Entscheidungen G1 (selbst getroffen)

- **E1 Nur 26.3 Fabric** (wie Simple Lights E1): `simplegadgets` 0.9.0, `loaders: ["fabric"]`, gebündelt von SB 26.3 Fabric
  (`depends simplegadgets >=0.9.0`, `include` + `implementation`). SB NeoForge/Forge 26.3 verlieren die Messuhr bis zum Port-Run
  (SB erreicht sie nur per Id → kein Compile-Bruch). Offen notiert.
- **E2 26.2 bleibt (Muster 8c-1):** SB-Klassen (`VelocityGaugeItem`, `GaugeAutowalk`, `GaugeNeedleModelProperty`,
  `SpeedometerHudOverlay`) bleiben in `common/src/shared`; neuer Schalter `McVersion.GADGETS_IN_SB` (26.2 true, 26.3 false) +
  `McVersion.GADGETS_NAMESPACE`. Auf 26.3 registriert SB das Item nicht (`ModItems.VELOCITY_GAUGE = null`), keine
  Modell-Eigenschaft, kein Autowalk-Haken, kein Gauge-HUD. `mc1_21_11` unberührt.
- **E3 Modul-Code** `com.simplegadgets.*` (Kopien ohne SB-Import): `gauge/VelocityGaugeItem`, `client/GaugeAutowalk`,
  `client/GaugeNeedleModelProperty` (`simplegadgets:gauge_needle`), `client/GaugeHud` (Inhalt wie `SpeedometerHudOverlay`),
  Mixin `KeyboardInputMixin` (eigener Name, nur Messuhr-Autowalk), `GadgetEnchantments` (Schlüssel `simplebuilding:range` und
  `simplebuilding:constructors_touch`, ohne SB Stufe 0).
- **E4 SimpleLib `com.simplelib.api.client.GadgetHud`** (neu, Regel 6a): die gemeinsame Kasten-Optik (Rahmen 26.3 wie SBs
  `HudFrame`, Füllung/Farben/Kontrast wie `HudPanel`), Stapel-Slots per Name + Reihenfolge + Aktiv-Prüfung, Lage/Größe/Sichtbar
  über einen Einstellungs-Lieferanten (Standard: sichtbar, x 0 %, y 50 %, 100 %). SB 26.3 meldet über `LibClientServices`
  seine Slots OCTANT (0) und ROD (2) und seine HUD-Config als Lieferanten an; `HudPanel.stackOffset` fragt auf 26.3 SimpleLib.
  Das Modul meldet GAUGE (1) an. So stapeln Oktant und Messuhr weiter wie bisher, mit und ohne SB.
- **E5 Ids (SimpleLib `IdMoves`):** ITEM `velocity_gauge`, RECIPE `velocity_gauge`, ADVANCEMENT `recipes/tools/velocity_gauge`
  `simplebuilding:` → `simplegadgets:`. Das Advancement `simplebuilding:gadgets/speed_reader` bleibt SBs (SB-Baum), sein
  Item-Prädikat per Id.
- **E6 Daten im Modul:** Texturen (+`.layers.json`) per `git mv` aus `mc26_3/overlay`; Item-Definition + 17 Modelle aus SBs
  Datagen kopiert, Namensraum umgeschrieben (26.2 behält SBs Datagen-Ausgabe; 26.3-Datagen von SB lässt sie weg →
  `removed-on-26.3.txt`). Rezept mit SB: `simplebuilding:copper_core` (Bedingung `simplebuilding` geladen); ohne SB:
  `velocity_gauge_without_simplebuilding` mit Kupferbarren statt Kern (Axt-Prinzip/Vanilla-Weg). Verzauberbarkeit: Modul-Tag-Dateien
  `data/simplebuilding/tags/item/{range,constructors_touch}_enchantable.json` mit `{"id": "simplegadgets:velocity_gauge",
  "required": false}` (Tags additiv; ohne SB wirkungslos). Lang EN/DE ins Modul (`item.simplegadgets.velocity_gauge`, `tooltip.simplegadgets.velocity_gauge.*`,
  `hud.simplegadgets.velocity_gauge.*`), SB-Schlüssel bleiben (26.2).
- **E7 SB 26.3 per Id (`util/GadgetRefs`):** Tab-Zeile, Suchtab (Gadget-Reihe), `InfoTooltips` (Kupferkern-Verwendung),
  `RangeReach.keepsReach`, `HudPanel.active(GAUGE)`, `RecipelessJeiInfo`, `GuideBookTests`, Datagen (Tags, Advancements),
  `craftable.json`-Overlay, DataIntegrity-Listen. `ClientSpeedometerMixin` (Altlast, schreibt nur CustomModelData) wird auf 26.3
  durch `stack.is(null)` = false still → unverändert.
- **E8 Schalter + Tab:** „Enable Simple Gadgets“ (`enableSimpleGadgets`, framework `SubMods`); eigener Tab `simplegadgets:gadgets`
  (Messuhr + Handbuch, später alle Gadgets); Vanilla-Tab „Werkzeuge“ nur ohne SB (SB sortiert per Id).
- **E9 Weiteres wie Simple Lights E8:** Handbuch (`module_guides.py`), Discover, Testzentrale-Station, Wiki
  `modules/simplegadgets/wiki/manual.json` (SB-Notiz zur Messuhr zieht um), Money `prices.json` (Messuhr-Preis zieht mit),
  Icon (`mod_icons.py`, Motiv Messuhr), Handbuch-Textur, `integration/enabled-mods.json`, `TcContext.FAMILY`,
  `docs/MODS-UND-FEATURES.md`, Prinzipien-Tabelle, Config-Schalter in `ConfigOptions`/`SimplebuildingConfig`/`ConfigOptionTests`.
- **E10 Tests:** Gauge-Tests aus `TweaksTests` (Rezept, Autowalk unter Bildschirmen, Pfad-Folgen, Höhenmesser/Range/Aufprall/
  Nadel) → `com.simplegadgets.test.GadgetsTests` (+ `registered`, `sub_mod_switch`, `recipe_without_simplebuilding`,
  `migration_from_simplebuilding`: alte Item-/Rezept-/Freischaltungs-Id, alter Stapel aus NBT über `ItemStack.CODEC`, alter Stapel
  in einem Container (Truhen-BE aus NBT), Rahmen-Item); SB-Tests der Messuhr auf 26.3 mit `GADGETS_IN_SB`-Wächter, Katalog-Einträge
  bleiben (Wächter `succeed`). Fabric-Adapter `ModuleGameTest`.

## 4. Risiken

- HUD-Vertrag ändert SBs Oktant-/Stab-HUD-Stapel auf 26.3 → `ui_text_contrast`/HUD-Tests müssen grün bleiben; Client-Sicht nicht
  prüfbar (keine Client-Läufe) → offen notiert.
- Datagen 26.3 auf sb-test: nur eigene Dateien übernehmen (Diff prüfen).
- Andere B-Agenten in gemeinsamen Dateien (Lang, QUEUE, modules.json, ModItems) → nur eigene Zeilen.

## 5. Prüfung (sb-test, über /root/sbt)

Compile `:mc26_3:fabric` + `:modules:simplegadgets:fabric` (+ `:compileJava` für 26.2-Teil von `check`);
`module-simplegadgets-fabric-263`, `module-simplegadgets-standalone-fabric-263`; `fabric-263` gefiltert (`*gauge*`,
`*test_centre*`, `*data_integrity*`, `*config_option*`, `*tweaks*`); `integration-263`; Generatoren (Datagen 26.3, Guides, Wiki,
Discover); `./gradlew check`. Nicht: NeoForge/Forge, Client-Sicht.

## 6. Stand und Abweichungen

G1 umgesetzt wie geplant: Modul `modules/simplegadgets` 0.9.0 (Fabric 26.3; `GadgetsRegistry`, `VelocityGaugeItem`, `GaugeHud`,
`GaugeAutowalk`, `GaugeNeedleModelProperty`, Mixin `GaugeKeyboardInputMixin`, `GadgetEnchantments`, IdMoves Item/Rezept/
Freischaltung), SimpleLib `com.simplelib.api.client.GadgetHud`, SB `McVersion.GADGETS_IN_SB`/`GADGETS_NAMESPACE`, `util/GadgetRefs`,
`LibClientServices` meldet Oktant/Stab-Slots + HUD-Config an, Schalter „Simple Gadgets aktivieren“, Texturen per `git mv`, Modelle/
Rezept/Freischaltung umgeschrieben, Lang/Fragentisch/Wiki/Money ins Modul. Tests: `GadgetsTests` (10, davon 4 Messuhr-Tests aus
`TweaksTests` portiert, neu Registrierung, Schalter, Migration mit altem Stapel inkl. Komponente und alter Truhe aus NBT, Rezept
mit/ohne SB, Discover, Handbuch); SB-Messuhr-Tests auf 26.3 per `GADGETS_IN_SB`-Wächter.

Abweichungen: (a) Kein Block, daher kein Paletten-Test (`NbtUtils`/`BlockState.CODEC` entfällt für G1; Pflicht ab G3, wenn
abgelegte Geräte/`PlacedTemplateBlock` umziehen). (b) `RecipelessJeiInfo` nennt die Messuhr auf 26.3 nicht mehr (Modul hat kein JEI-Plugin, das Item hat
ein Rezept). (c) `ClientSpeedometerMixin` (Altlast) unverändert, auf 26.3 wirkungslos.

Belege (sb-test): Compile `:mc26_3:fabric` (+gametest), `:modules:simplegadgets:fabric`, `:modules:simplelib:fabric`;
module-simplegadgets-fabric-263 10/10, standalone 10/10; fabric-263 `*gauge*` 4/4, `*test_centre*` 21/21, `*config_option*` 20/20,
`*guide*` 16/16, `*core_tooltips*` 1/1, `*renamed_item_ids*` 1/1, `*data_integrity*` 51/51; integration-263 4/4; Datagen 26.3, Guides,
Wiki, Discover, Money-Links, Ebenen erzeugt; `./gradlew check` grün (f50633c90). (d) Abweichung: die SB-Lang-Schlüssel der Messuhr bleiben im
26.3-Overlay (`check_tooltip_text`: das Overlay muss jeden gemeinsamen Schlüssel halten); nur die JEI-Info entfällt dort nicht, ist aber ungenutzt.

## 7. Stufe G2 (Branch `b-gadgets25`, Basis `b-gadgets24`): Transposer + Resonanzstab

Nach dem Muster von G1 (§3, E1-E10): SB-Klassen bleiben für 26.2 (`McVersion.GADGETS_IN_SB`), auf 26.3 sind `ModItems.ROTATOR` und
`TweaksItems.LASER_POINTER` `null`, SB erreicht beide per Id (`util/GadgetRefs`). Zwei Commits: **G2a Transposer**, **G2b Resonanzstab**
(der Stab hängt an Netzwerk, Renderer, drei Client-Mixins und eigener Config, deshalb getrennt prüfbar).

- **E11 Gemeinsame Verträge in SimpleLib (Regel 6a):** `api/AnvilRechargeable` + `mixin/AnvilRechargeMixin` (Aufladen im Amboss ohne Stufen,
  dasselbe Verhalten wie SBs Mixin, andere Schnittstelle, damit SB (26.2) und Modul sich nie doppeln); `api/GadgetActions`: Ereignisse
  (`ROTATE`, `LENS_BEAM`, `LASER_TNT`; SB hört in `LibServices` und feuert seine Freischaltungen), Werte-Tabelle (`fact`) für SBs In-World-Katalog,
  Hand-Hinweis (`registerItemHint`/`itemHint` für SBs `TransformTargets`, `registerHandHint` Client: SB beantwortet für das Modell-Prädikat
  mit seiner vollen Prüfung). Berechtigungen: Modul `GadgetProtection` (framework `Protection` + `HammerActions.mayChange`, Muster `HammerProtection`).
- **E12 Config `config/simplegadgets-server.json`** (`GadgetsConfig`, Muster `HammerConfig`, Übernahme der SB-Werte beim ersten Start über
  `ConfigFiles.adoptLegacy`: `tools.rotator*`, `server.charges.{rotator,lens}MaxCharge`, später `server.laser.*`, `tweaks.laserPointer.*`). SBs Schlüssel
  bleiben (Stufe 9: ausblenden). Höchstladung wird beim Registrieren gelesen (Neustart nötig, wie vorher).
- **E13 Ids:** ITEM `rotator`/`amethyst_lens`, RECIPE gleich, ADVANCEMENT `recipes/tools/<id>` `simplebuilding:` -> `simplegadgets:` (IdMoves). Kein Block,
  keine BlockEntity -> kein Paletten-Test (Pflicht erst G3). `ModTriggers.ROTATE/LENS_BEAM/LASER_TNT` und die SB-Freischaltungen (`gadgets/spin_doctor`,
  `burning_focus`, `remote_detonation`) bleiben SBs, Symbol per Id.
- **E14 Rezepte:** mit SB `simplebuilding:iron_core` + `iron_pole`; ohne SB (`*_without_simplebuilding`): Eisenbarren statt Kern/Stab (Muster Messuhr).
- **E15 Hand-Hinweis des Transposers:** Modell-Eigenschaft `simplegadgets:transform_hint` (statt `simplebuilding:transform_hint`); mit SB gilt dessen volle Prüfung.
- **E16 Tests:** `RotatorTests` -> `com.simplegadgets.test.RotatorTests` (1:1 portiert), Stab-Tests aus `TweaksTests`/`CombatTests`/`ModpackTests`/`ServerTuningTests`/
  `ConfigOptionTests`/`AdvancementTriggerTests` -> `GadgetsTests` bzw. Modul-Tests; SB-Tests per `GADGETS_IN_SB`-Wächter bzw. `GadgetRefs`. Jeder neue Test mit Fabric-Adapter (`ModuleGameTest`).

### Stand G2 und Abweichungen (25.10.)

Umgesetzt wie in §7 geplant: Modul `simplegadgets` trägt jetzt Messuhr, Transponierer (`com.simplegadgets.rotator.RotatorItem`/`RotatorSwap`,
`TransformHintModelProperty`) und Resonanzstab (`rod.ResonanceRodItem`/`RodBeam`/`LensMeasurement`, `network.LaserPayload`/`RodNetwork`,
`client.RodClient`/`RodRenderer`, Mixins `RodCrosshair`/`RodSteadyView`/`RodHandTilt`/`FireIgniteOddsInvoker`, Komponente
`simplegadgets:lens_measurement`), dazu `GadgetsConfig` (`config/simplegadgets-server.json`, Übernahme der SB-Werte) und `GadgetProtection`
(framework `Protection` + loader-Break-Ereignis über einen vom Fabric-Einstieg gesetzten `BuildGuard`). SimpleLib: `AnvilRechargeable` +
`AnvilRechargeMixin`, `XpRepairRefusalMixin` (Tag `simplelib:xp_repair_incompatible`), `GadgetActions`. SB: `McVersion.GADGETS_IN_SB` setzt
`ModItems.ROTATOR`, `TweaksItems.LASER_POINTER`, `ModDataComponentTypes.LENS_MEASUREMENT` auf 26.3 auf `null`; `GadgetRefs.rotator()/rod()`;
`TransformTargets` fragt `GadgetActions.itemHint`, `LibServices` übersetzt die Ereignisse in `ModTriggers`, `LibClientServices` beantwortet den
Hand-Hinweis und lässt den ROD-Slot weg; Datagen (Rezepte, Modelle, Tags, Freischaltungen per Id), Handbuch-Kapitel per Id, Tests per
`GADGETS_IN_SB`-Wächter bzw. `GadgetRefs`. Tests im Modul: `RotatorTests` (15, aus SB portiert), `RodTests` (21, aus `TweaksTests`/`CombatTests`/
`ServerTuningTests`/`ModpackTests` portiert), Migration von altem Transponierer-/Resonanzstab-Stapel (Schaden, Messungs-Komponente, Truhe),
Registrierung, Rezepte mit/ohne SB.

Abweichungen: (a) Kein Block/keine BlockEntity: kein Paletten-Test (Pflicht erst G3). (b) Werte sind server-lokal wie bei den Hämmern: der
Strahl-Client rechnet mit seiner eigenen Datei (kein Sync-Paket mehr; der Server prüft Reichweite und Schalter weiter selbst). (c) SBs Config-
Schlüssel (`tools.rotator*`, `server.charges.*`, `server.laser.*`, `tweaks.laserPointer.*`) bleiben stehen (Stufe 9: ausblenden); SBs
`RecipeFilter`-Schalter `laserPointer.enable` blendet das Modul-Rezept nicht mehr aus (der Schalter `rodEnabled` sperrt nur den Strahl).
(d) JEI-Infoseiten des Resonanzstabs (`TweaksJeiInfo`) entfallen auf 26.3 wie die der Messuhr. (e) Der Discover-Tipp „Transponierer“ ist jetzt der
des Moduls; SBs 26.2-Linie verliert ihn. (f) Gemeinsamer Verzauberungs-Mixin: Modul-Transponierer nimmt Unbreaking, kein Mending auch allein
(SimpleLib-Tag statt SB-Tag). (g) Client-Tests (`GadgetsClientTest`) nicht angepasst/gelaufen (keine Client-Läufe).
