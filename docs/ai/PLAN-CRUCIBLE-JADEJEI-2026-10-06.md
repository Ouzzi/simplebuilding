# Plan: Jade + JEI fuer Milchkessel / verstaerkten Kessel / Tiegel-Bau (2026-10-06)

Branch `claude-crucible4-gpt`, nur committen. Auftrag: `gpt-jadejei.md` (Scratchpad).

## Ist-Zustand
- Jade-Plugin `common/src/jade/.../SimplebuildingJadePlugin`; Daten `compat/BlockInfo` (Topics, Server/State). `mc1_21_11/shared` hat eine
  EIGENE BlockInfo ohne `crucible`-Topic -> neue Topics im Plugin nur ueber `topic.id().equals(..)` ansprechen.
- JEI: `CrucibleCategory` (widget-only), Katalog ohne JEI-Klassen in `compat/`. `McVersion.CRUCIBLE` nur 26.3 (26.2 = false).
- Kessel -> verstaerkter Kessel: steht in `SledgehammerUpgrades.crucibleUpgrades`, taucht aber NICHT in `machine_upgrade` auf
  (`InWorldTransformations.sledgehammerUpgrade` iteriert nur Kupfertruhe, Shulkerkiste, Mod-Bloecke) -> eigener Eintrag noetig.
- Tiegel-Werte nur ueber `CrucibleCompat` (einzige SimpleLib-Bruecke, `SimpleLibApi.buildStrikes/attachStrikes`).

## Aenderungen
1. `BlockInfo`: Topic `CAULDRON("cauldron", false)`; `stateLines` erkennt `simplesandwiches:milk_cauldron` und
   `simplelib:reinforced_cauldron` per Registry-ID, liest `content`/`stage`/`level` per Name (kein Klassenbezug).
   Reinforced ohne `level`-Property: wasser/pulverschnee = volle Fuellung 3/3 (der Block nimmt immer einen vollen Eimer).
2. Jade-Plugin: Client-Komponente fuer `AbstractCauldronBlock` (nur Topic-Id `cauldron`), `addTooltipCollectedCallback`
   entfernt `JadeIds.UNIVERSAL_FLUID_STORAGE` bei den zwei Block-IDs. 1.21.11-Linie: kein Topic -> nur Callback (API vorhanden).
3. `CrucibleCompat` (26.2 + 26.3): `buildStrikes()` / `attachStrikes()`.
4. `CauldronWorldCatalog` (shared, JEI-frei, `McVersion.CRUCIBLE`, Items per ID, fehlende uebersprungen) + `CauldronWorldCategory`
   (`simplebuilding:cauldron_world`) + Registrierung/Katalysatoren im JEI-Plugin. Eintraege: iron_crucible_build, barrel_attach,
   cauldron_reinforce, milk_butter, milk_cheese. Zeiten des Milchkessels nicht angezeigt (SandwichConfig unbekannt).
5. Lang EN/DE in beiden Lang-Baeumen (nach `jei.simplebuilding.crucible.warming`).
6. GameTests: `BlockInfoTests.cauldronsShowContentRipenessAndFill`, `CrucibleTests.cauldronWorldCatalogMatchesTheRules`
   (+ Fabric-Adapter + Spec-Eintraege, sortiert).

## Verifikation
Gates laut Auftrag: check, 26.2-Compile, Forge-26.3-Compile, mc1_21_11 Jade-Compile (falls Task), testrunner fabric-263/neoforge-263.
