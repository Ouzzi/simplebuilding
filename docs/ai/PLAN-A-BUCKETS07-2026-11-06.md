# Plan a-buckets07 (WH26-7): Enderit- und Keramik-Eimer nach Simple Tools (06.11.2026)

Besitzer 06.11.: "ja", Eimer ziehen standardmaessig nach Simple Tools. Vorlage: a-barrels06 (1979cd512, BarrelDock-Vertrag in SimpleLib).
Status: NUR PLAN, nicht umgesetzt (Kopplung unten; Umsetzung braucht mehrere Gradle-Laeufe mit Iteration).

## Ist-Zustand
- Alle Eimer sind Simple Crucibles (`simplecrucibles:*`, vorher `simplebuilding:*`, IdMoves in SimpleCrucibles.BUCKETS_FROM_SB).
- EINE Klasse `fluid/CrucibleBucketItem` (Kind COPPER/ENDERITE/IRON/CERAMIC) liest die Item-Felder direkt aus `CrucibleFluids.*`
  (empty/filled/halfEnderite/ceramic-Stufen); dazu `EnderiteMilkBucketItem`, `BucketDispensing`, `CauldronBuckets`,
  `ReinforcedCauldronBuckets` (Crucibles-API fuer den Verstaerkten Kessel), Komponenten `ceramic_uses`/`oxidation`/`waxed`/`wax_worn` (CrucibleComponents).
- Enderit-Seelenlava-Eimer haengen an Seelenlava (Fluid, Fuel-Config `SoulLava.fuelTicks()`), also Crucibles.
- Verweise (grep enderite_bucket|ceramic_bucket|clay_bucket): Crucibles (Java, Lang, Items/Models/Texturen, Rezepte+Advancements, manual.json, Discover, BucketTests, ModuleGameTest),
  SimpleBuilding (BucketRefs, ModItemGroupsContent, GuideContent, DataIntegrityTests, WoodworkTests, ModAdvancementProvider, questionary, tags enderite_ingot_tier/enderite_items,
  advancement bottomless_bucket, EnderiteBucketClientTest, integration CrossModGameTest), Simple Money (prices, Trader-Link, MoneyDiscover, manual), Wiki-Daten, tools/textures/*.py.

## Zielbild
1. SimpleLib `com.simplelib.bucket`: `CrucibleBucketItem`, `EnderiteMilkBucketItem`, `BucketDispensing` und ein neutraler Halter `BucketFamily`
   (statische Item-/Komponenten-Felder, die jedes Modul fuer seinen Teil befuellt; Seelenlava per Registry-Id lazy, Fuel-Ticks per Supplier).
   Die `CrucibleFluids.X`-Zugriffe in diesen Klassen werden `BucketFamily.X`.
2. Simple Tools registriert (ids `simpletools:*`): enderite_bucket, enderite_water/lava(+_full), enderite_milk(+_full), raw_ceramic_bucket,
   ceramic_/chipped_/cracked_/brittle_ (empty, water, lava), Komponente `ceramic_uses`; Enderit-Seelenlava-Eimer (+_full) nur wenn `simplecrucibles` geladen ist.
   Rezepte enderite_bucket, raw_ceramic_bucket, ceramic_bucket_from_smelting_*, Advancements, Lang EN/DE, Items/Models/Texturen, Tags, Tab, Guide, Wiki-Notizen.
3. Simple Crucibles behaelt: Kupfer-Eimer, Seelenlava-Eimer, Seelenlava, Kessel-Anbindung (`ReinforcedCauldronBuckets` erkennt die Klasse aus SimpleLib, nicht Items).
   Ohne Simple Tools: keine Enderit-/Keramik-Eimer, Crucibles voll spielbar (nur bedingt nutzen, Lookup per Registry-Id).
4. IdMoves (Tools.registerMoves): Items `simplecrucibles:` UND `simplebuilding:` -> `simpletools:` fuer alle obigen Ids, Rezepte+Advancements `recipes/tools/*`, Komponente `ceramic_uses`.
   Crucibles.BUCKETS_FROM_SB/BUCKET_RECIPES_FROM_SB um die verschobenen Ids kuerzen (Kette sb -> tools muss in Tools stehen, sonst bricht die zweistufige Aufloesung alter Welten).
5. Simple Money: Preise `money/simplecrucibles/prices.json` -> `money/simpletools/prices.json`, Trader-Link enderite_bucket, MoneyDiscover, manual.json.
6. Tests: ModpackTests-Upgrade-Zaehler und DataIntegrity (Gate 116 scheiterte an genau so etwas), BucketRefs.NAMESPACE je Eimer (Crucibles `simplecrucibles` / Tools `simpletools`),
   BucketTests Enderit/Keramik-Teile in Tools-Modul verschieben (Katalog-Eintrag + @GameTest-Adapter), Crucibles-Tests auf Kupfer/Seelenlava kuerzen, Combo crucibles+tools fuer Kessel-Pfade.

## Risiken
- Statische Reihenfolge: Items mit `craftRemainder(ENDERITE_BUCKET)` brauchen den Eimer zuerst; Seelenlava-Fluid wird erst von Crucibles registriert -> lazy per Id.
- Fuel-Multiplikator der Seelenlava (Crucibles-Config) fuer den Enderit-Seelenlava-Eimer: Registrierungs-Wert aus Tools nicht lesbar; Supplier + Standard 1.
- Mixins (Kupfer) bleiben in Crucibles, importieren nur die SimpleLib-Klasse.
- Forge/NeoForge nicht Teil (nur 26.3 Fabric), Loader-Klassen CruciblesNeoSoulLava/CruciblesForgeSoulLava bleiben spaeter zu pruefen.

## Verifikation
`/root/sbt a-buckets07 "compile + module-simpletools + module-simplecrucibles (fabric + standalone) + simplebuilding:data_integrity* + simplebuilding:modpack*"`;
grep `enderite_bucket|ceramic_bucket|clay_bucket` ohne Treffer in alten Besitzern ausser IdMoves.

## Rueckfragen (selbst entschieden)
- Enderit-Seelenlava-Eimer: Tools (nur mit Crucibles), nicht Crucibles, wegen craftRemainder-Reihenfolge.

## Plan-Pruefung vor der Umsetzung (06.11., a-buckets07)
Vereinfacht / korrigiert:
- SimpleLib `com.simplelib.bucket`: `ModBucketItem` (= alte CrucibleBucketItem, unveraendert bis auf Feldzugriffe), `EnderiteMilkBucketItem`,
  `CauldronBuckets`, `BucketDispensing`, Halter `Buckets` (Items, Komponenten, Registrier-Helfer). Simple Tools bettet SimpleLib schon ein.
- `CauldronBucketMixin` (Vanilla-Kessel) zieht mit nach SimpleLib, sonst fehlt Tools allein das Kessel-Verhalten (im Plan vergessen).
- Enderit-Seelenlava-Eimer: `Buckets.registerEnderiteSoulLava()` rufen BEIDE Module nach ihren Items; registriert wird beim zweiten Aufruf,
  wenn Enderit-Eimer und Seelenlava da sind (keine Lazy-Fluid-Konstruktion, Ladereihenfolge egal). Id `simpletools:*`, Brenndauer aus der Crucibles-Config.
- Komponenten: oxidation/waxed/wax_worn bleiben Crucibles (Kupfer), `ceramic_uses` -> `simpletools:ceramic_uses`; der Halter haelt die Typen.
- JEI-Infoseiten bleiben in Crucibles (nur vorhandene Items); Tools allein hat keine JEI-Integration (offen, nicht Teil).
- Tests: reine Enderit/Keramik-Tests -> Tools-Modul (`ToolsBucketTests`, auch standalone); gemischte (Seelenlava, verstaerkter Kessel) bleiben in
  Crucibles mit `partnerMissing("simpletools")` + neue Combo crucibles+simpletools.
- Forge/NeoForge-Loader nur mechanisch nachgezogen, nicht kompiliert (NUR 26.3 Fabric).
