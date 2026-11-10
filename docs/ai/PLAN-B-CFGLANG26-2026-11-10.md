# Plan b-cfglang26 (10.11.2026): ST-20 Teil 2, Config-Lang-Schema auf text.autoconfig.*

Basis hub/b-bundle175-gen. PRINZIPIEN A11 Punkt 2. Betroffen: die 14 Module der LEGACY-Liste von `tools/check_module_style.py`
(simpledimensions, simpleend, simpleenddragonfight, simplemobheads, simplerespawn, simpletweaks, simplefarming, simpleinterfaces,
simpleloot, simplemobmanners, simplemovement, simplesounds, simpletools, simpleweather).

## Abbildung (Inhalte unveraendert, 1048 Lang-Zeilen, EN+DE paarig)
- (a) `<ns>.option.X` , (c) `config.<ns>.X` , (d) `<ns>.config.X` -> `text.autoconfig.<ns>.option.X`
- `.tooltip` -> `.@Tooltip`; `title` -> `text.autoconfig.<ns>.title`; `category.X` -> `text.autoconfig.<ns>.category.X`
- Zusatzschluessel (`.default`, `.tab`, `.light`, Reiter wie `access`) bleiben unter `option.<...>` (wie Simple Riding).
- Nicht angefasst: `ability.simplemobheads.*`, `config.jade.*`, Wiki-/Guide-Texte.

## Code mitgezogen
Screens (Farming/Tools/Interfaces/MobManners/Loot/Movement/Weather/Sounds/MobHeads/DimensionConfigScreen), Dimensions
`config-options.json` + Tests (DimensionTests, DimensionsGuideTests), `tools/check_data.py`/`gen_data.py` der Module.
Keine Cloth-Logik geaendert (b-cloth26 parallel: nur Key-Strings in simpledimensions).
LEGACY in `check_module_style.py` ist jetzt leer (darf nur schrumpfen).

## Verifikation
check_module_style (0 Alt-Module), check_lang_keys, check_tooltip_text, precheck --fast, module_guides --check, Modul-check_data.py,
sbt: Compile + `simplebuilding:config_option*` + module-<mod>-fabric-263 der 14 Module.
Nicht getestet: Client-Sicht der Config-Screens (Klick-Test im Bündel), NeoForge/Forge.

## Stand 10.11. abends
sbt-Lauf (Compile + config_option* + module-<mod>-fabric-263 der 14 Module) war eingereiht, Status: offen - sb-test hing (Stopp A 20:35). Nach Entwarnung per `/root/sbt b-cfglang26 ...` nachholen (nicht doppelt starten, falls der eingereihte Lauf noch durchläuft: Log /tmp/cfglang-sbt.log).
