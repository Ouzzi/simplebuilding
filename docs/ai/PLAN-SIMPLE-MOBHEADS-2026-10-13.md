# Plan: Simple Mobheads umsetzen (2026-10-13)

Branch `claude-mobheads` (Basis `claude-wave1` ab05365fa). Grundlage: `KONZEPT-SIMPLE-MOBHEADS-2026-10-13.md` – Besitzer
13.10.: **alle Empfehlungen übernommen** (Fragen 1–12). Regeln: `PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`,
`PLAN-MODULAUFTEILUNG-2026-10-11.md` (§2.1 Pflicht-Sub-Mod, §3.2 `IdMoves`), AGENTS.md 3.7 (Mojang-Pixel nur beim Build).

## 1. Ist-Zustand (erhoben)

- 4 Köpfe in Simple Fun (`heads/*`, Loot, Fähigkeiten Kuh/Schaf/Huhn/Schwein, Gruß beim Schleichen, PvP-Spielerkopf,
  Konfig, Befehl `/simplefun pvp headDrops`, Tests `FunTests`/`FunSecurityTests`, Tab/Suchtab hinter dem Drachenkopf).
- 12 Köpfe im SB-Kern (`common/src/shared`, also auch 26.2!): `BlazeHeadType`, `ModHeads`, `HeadAbilities` + 6 Mixins,
  `ModSkullModels` + `SkullModelMixin`, `SkullBlockEntityTypeMixin`, Loot `ModLootTableModifications`, Datagen
  (Modelle, Loot, Tags, Rezepte), JEI `MobDropCatalog`, Tests `MobHeadTests`, Testzentrale.
- **26.3-API geprüft** (Quellen `/root/mcsrc263`, Vanilla-Jar per javap):
  - `Item.Properties#loweredMobVisibility` / Komponente `minecraft:mob_visibility` (Faktor 0,5) = die „Tarnung“ aus §3.5.1
    ist reine Vanilla-Komponente; SB wertet den Extra-Kopf-Slot dafür schon aus (`LivingEntityExtraHeadMixin`).
  - `SkullBlockRenderer.createModel` / `SKIN_BY_TYPE` / `submitSkull`, `SkullSpecialRenderer.Unbaked(kind, texture)`,
    `LivingEntityRenderer#extractRenderState` setzt `wornHeadType`, `CustomHeadLayer` nutzt `createModel`.
  - `SkullBlockEntity` speichert unbekannte Item-Komponenten in `BlockEntity.components` (gespeichert, beim Abbau per
    `copy_components` zurück); `getUpdateTag` = `saveCustomOnly` → Komponenten erreichen den Client **nicht** → kleiner Mixin.
    Client lädt Update/Chunk über `loadWithComponents` (javap: Vanilla + NeoForge).
  - Varianten der Mobs sind Komponenten (`cat/variant`, `wolf/variant`, `frog/variant`, `pig/variant`, `cow/variant`,
    `chicken/variant`, `axolotl/variant`, `fox/variant`, `parrot/variant`, `rabbit/variant`, `llama/variant`,
    `horse/variant`, `mooshroom/variant`, `villager/variant`, `sheep/color`, `shulker/color`, `tropical_fish/pattern`,
    `zombie_nautilus/variant`) und lesbar über `Entity#get(DataComponentType)` → generisch über den Codec zu Text.
  - Item-Modell `minecraft:select` + `minecraft:component` wählt nach einer eigenen String-Komponente.
  - `ModelLayerLocation(Identifier, layer)` ist ein Record → Mob-Modelle per Text (`minecraft:pig#main`) backbar, ohne Klassen.
  - Geladener Creeper: `Creeper#killedEntity` rollt `charged_creeper` einmal je Explosion, prüft nur `shouldDropLoot` des
    Creepers → Babys liefern den Kopf (Frage 5 „normaler Kopf“ = Standardverhalten, Test).
  - 26.3 hat neue Mobs: `sulfur_cube`, `parched`, `camel_husk`, `zombie_nautilus`, `nautilus`, `copper_golem`, `happy_ghast`.

## 2. Architektur (Entscheidungen)

1. **Modul `modules/simplemobheads`** (Namespace `simplemobheads`, „Simple Mobheads“ 1.0.0), angelegt mit `tools/newmod.py`,
   hart: simplelib (gebündelt, `IdMoves`), framework (gebündelt), Cloth Config (Konfig-Bildschirm wie Fun).
   `modules.json`: `parent: simplebuilding`, `bundle: required`; SB: `requires` + `depends`/`type="required"`/`mandatory`,
   Fabric `include`+`implementation`, NeoForge `jarJar`+Run-Mod, Forge `forgeBundleModules`.
2. **Eine Datenquelle** `modules/simplemobheads/data/mobs.json` → `tools/generate_heads.py` (`--check`) erzeugt:
   `shared/java/com/simplemobheads/data/HeadTable.java` (Registrierungs-/Render-Tabelle), Blockstates, Item-Modelle
   (Varianten per `select`), Block-Loot (mit `copy_components`), Tags (`minecraft:skulls`, `mineable/axe`,
   `simplemobheads:trial_chamber_heads`, `minecraft:freeze_immune_wearables`), Lang EN/DE (Köpfe, Varianten, Konfig),
   Advancements, Money-Preise, Fragentisch-Einträge. Neue Mobs = Eintrag in `mobs.json`.
3. **Variante als Daten (Weg B):** Item-Komponente `simplemobheads:variant` (String). Block bleibt Vanilla-`SkullBlock` +
   `SkullBlockEntity` (kein eigener BE-Typ nötig: Komponente liegt in `BlockEntity.components`); Mixin
   `SkullBlockEntity#getUpdateTag` schickt sie mit. Jede Variante = eigener **Render-Typ** (`SkullBlock.Type`,
   `simplemobheads:cat/siamese`), Block-Renderer und getragener Kopf tauschen den Typ nach der Komponente
   (Mixins auf `extractRenderState`), Item-Modell per `select`. Name: „Katzenkopf (Siam)“ (Frage 10).
4. **Geometrie generisch:** Kopf-Teil aus dem Vanilla-Modell (`ModelLayerLocation` aus `mobs.json`, Teilpfad, optional
   Drehung/Skalierung), Unterkante auf y=0, mittig, zu große Köpfe verkleinert; Zusatzschichten (Augen leuchtend,
   Kleidung, Wolle gefärbt, durchscheinend). Die 16 vorhandenen Köpfe behalten ihre abgenommene Geometrie
   (`ModSkullModels`/`AnimalSkullModels` ziehen als „custom“ um). Fehlt ein Modell (Mod-Mob ohne Mod): Würfel-Rückfall.
5. **Drops:** Pool je Kopf in `minecraft:charged_creeper` (Opfer = Mob; nur wenn der Entity-Typ registriert ist),
   Loot-Funktion `simplemobheads:copy_variant`, Bedingung `simplemobheads:head_drop` (Hauptschalter, Wither aus,
   Warden an, beim Würfeln geprüft). Spieler-Kill-Chance (Standard aus, 0 % + 1 %/Plünderung) und PvP-Spielerkopf
   im Todes-Ereignis der Loader. Vanilla-Köpfe bleiben unberührt.
6. **Migration:** `IdMoves` (ITEM, BLOCK, ADVANCEMENT) für `simplefun:{pig,cow,chicken,sheep}_{head,wall_head}` und
   `simplebuilding:{blaze_head,…,stray_skull,…}` (+Wand) → `simplemobheads:*`; Konfig-Übernahme aus `simplefun.json`
   (`framework ConfigFiles.adoptLegacy`); `/simplefun pvp headDrops` bleibt als Verweis-Alias.
   **SB 26.2 bleibt monolithisch** (Modulplan §5): die SB-Köpfe registriert nur noch 26.2 (`McVersion.MOBHEADS_MODULE`);
   auf 26.3 liefern `TweaksItems.head(type)`/`TweaksBlocks.head(type)` die Mobheads-Objekte per Registry-Id (lazy),
   damit Rezepte, Testzentrale, JEI und `MobHeadTests` unverändert weiterlaufen (dann als Integrationstests).
   SB-Fähigkeiten/-Loot/-Renderer schalten auf 26.3 ab (Mobheads macht es), Scarecrow-Ackerschutz bleibt.
   Tag `simplebuilding:trial_chamber_heads` bleibt (26.3: verweist auf `#simplemobheads:trial_chamber_heads`).
7. **Kopf-Slot:** framework 0.1.11 `WornHeads` (reiner Java-Vertrag: Anbieter liefert Zusatzköpfe einer Entität);
   SB meldet seinen Extra-Kopf-Slot an; Mobheads fragt Helm + Vertrag. Tarnung läuft schon über Vanilla + SB.
8. **Fähigkeiten:** 16 vorhandene unverändert (Konfig-Schalter je Fähigkeit), neu 12 aus §3.5: Biene (Stock-Ernte ohne
   Wut), Fuchs (Beerensträucher stechen nicht), Frosch (höher springen auf Seerosen/im Wasser), Ziege (Rammstoß ohne
   Schaden), Katze (Creeper meiden den Träger, 6 Blöcke), Pilzkuh (Schüssel → Pilzsuppe), Sniffer (Samen erschnüffeln,
   Abklingzeit), Allay (Items in der Nähe schweben leicht), Schildkröte (Eier zertrampeln nicht), Eisbär (Eis rutscht
   weniger), Wolf (wilde Wölfe greifen nicht an), Warden (die Hälfte der Schritte erreicht Sculk nicht). Dazu für jeden
   Kopf: Tarnung (Vanilla-Komponente), Notenblock-Klang, Gruß beim Schleichen (Klang + Noten, Abklingzeit, aus Fun).
9. **Tab:** eigener Tab „Mob Heads“ (Grundköpfe + alle Varianten), Suchtab/Functional Blocks hinter dem Drachenkopf
   (Grundköpfe, abschaltbar über `CreativeTabSettings`).
10. **Simple Fun 1.4.0** ohne Köpfe (Code, Daten, Konfig-Schlüssel, Tests, Wiki, Preise, Fragentisch); hängt nicht an Mobheads.

## 3. Stufen (je ein Commit, Fabric zuerst; Compile 3 Loader + 26.2 je Stufe)

1. framework `WornHeads` + Modulgerüst + `mobs.json` + Generator + Registrierung/Client/Drops/Konfig/Befehl (nur Mobheads).
2. Umzug: Fun-Köpfe + SB-Köpfe raus (26.3), `IdMoves`, Konfig-Übernahme, SB/Fun anpassen, Bündelung/Metadaten.
3. Fähigkeiten (16 + 12), Kopf-Slot-Vertrag.
4. Wiki/Guide/Fragentisch/Money/Testzentrale/discover; Tests (Mobheads-Suite + Standalone, SB/Fun angepasst).
5. Client-Screenshots aller Köpfe → `~/previews/mobheads/` + Übersichtsseite `/tmp/claude-0/sbx/mobheads.html`.

## 4. Tests

- Mobheads (`module_game_test_*`, alle Loader + Standalone): Registrierung + Vollständigkeit (jeder lebende Mob hat
  Kopf oder Ausschlussgrund), Klang je Kopf, Drop durch geladenen Creeper (Variante übernommen, Baby → Kopf, normaler
  Tod → keiner, Wither aus/Warden an, Spieler-Kill Standard aus), PvP-Spielerkopf, Platzieren/Abbauen behält Variante,
  Migration (alte Fun-/SB-Ids als Item, Block, platzierter Kopf in Chunk-NBT, Advancement), Konfig-Übernahme, Fähigkeiten
  (mit/ohne Kopf, Extra-Slot über `WornHeads`), Befehl + Alias, Tabs.
- SB: `MobHeadTests`, `PadOverhaul*`, `DataIntegrity*` grün (Mobheads geladen). Fun: Suite ohne Köpfe grün.
- Client: Screenshot-Lauf (Fabric) aller Köpfe inkl. Varianten. Keine NeoForge/Forge-Client-Abnahme (Besitzer).

## 5. Risiken / Grenzen

- Ladereihenfolge SB ↔ Mobheads: SB greift nur lazy per Id zu (nie im Static-Init).
- Generische Köpfe können schief aussehen → Screenshot-Abnahme, Korrekturen in `mobs.json` (Drehung/Teil/Skalierung).
- Tropenfisch: 12 Muster, Farben fest (Grundfarbe/Musterfarbe des Mobs nicht als Variante) – bewusst vereinfacht.
- Varianten-Klänge (Katze, Wolf, Schwein …) nutzen den Grundklang.
