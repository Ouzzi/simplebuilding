# Plan Bündel 83 (Sitzung B, b-bundle83, 31.10.)

Basis claude-wave1 = master 8d31b229d (Bündel 82). Branches: b-mob32, b-ig31v, b-trials32, b-ores32, b-fix32, b-small32,
b-mobs32, b-mach32, b-optik32, b-rel32, b-snow32 (nachgereicht). b-ores32-gen NICHT gemergt (fremde Dateien).

## Konflikte und Entscheidung (beide Seiten vereint)
- b-mob32 (simpleresonance + Besatz-Fähigkeiten): Modullisten (TcContext, ConfigOptionTests, enabled-mods.json,
  fabric.mod.json, neoforge.mods.toml, forge build.gradle, mod_icons.py, module_guides.py) = simpleores UND simpleresonance;
  modules.json JSON-3-Wege (simpleresonance neu, simplemobs requires/tests von b-mob32, SB optional +simpleresonance);
  MobsConfig: Trials-Felder (evilOmen/voidChain) + applyZones/zonesToJson; mixins: Trials- + TrimAbility-Mixins;
  MODS-UND-FEATURES Zeile Simple Mobs: Zonen-Text + Trials-Text, beide Plan-Dateien.
  Generiert, eine Seite: simplemobs Guide-Seiten in lang (ours), MobsDiscover (ours) – bprep erzeugt neu.
- b-trials32: MobsConfig (TR31-R-Felder vor applyZones), mixins vereint, MODS-UND-FEATURES Zeile wie oben + TR31-R.
- b-ores32: module_guides.py neuer Ores-Intro + Resonance-Eintrag.
- b-mobs32: AnimalsConfig/check_data Schlüssel hedgehogThorns + boarTruffles/boarTruffleChance.
- b-mach32 (simpleredstone aufgelöst): alle simpleredstone-Einträge raus (ConfigOptions, SimplebuildingConfig,
  ConfigOptionTests, TcContext, lang, modules.json, PRINZIPIEN), simpleresonance bleibt; ServerTuningConfig, ConfigOptionTests
  und lang: endgameEnchantLevels + doublePiston/signalDampener.
- b-fix32/b-optik32: nur generierte Dateien (bmerge, theirs). SearchTabPlacement (shared) ist weg, keine Referenzen mehr.

## Abweichungen / Befunde
- (a) Trüffel im Vanilla-Tab Nahrung: Simple Animals hat keinen eigenen Mod-Tab; CreativeTabPolicy lässt Mod-Items dann im
  Vanilla-Tab (wie Krabbe seit Bündel 82, Gate grün). Keine Änderung; data_integrity-Test im Testlauf prüft es.
- Tooltip-Prüfer: Doppelkolben/Signaldämpfer-Tooltips (jetzt SB-Lang) > 48 Zeichen → gekürzt.
- check_jei_coverage: simpleanimals-Mob-Beute ohne Rezeptanzeige – schon in master 8d31b229d so (kein Bündel-Fehler).
- modtree.py neu: MODSTRUKTUR.* + creative_tab_order.json (+simpleresonance).

## Verifikation
Lokale Python-Prüfer, bprep (Compile, Generatoren, check, -gen), gezielter Testlauf (Module simpleresonance/simplemobs/
simpleores/simpleanimals/simplemobheads/simplesandwiches/simpledinos/simpleblocks standalone + SB-Filter), check_client_mixins.
