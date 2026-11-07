# Plan b-qw09 (07.11.): Guide-IDs nach Sub-Mod-Umzug

Ist: GuideContent/GuideBooks nennen `simplebuilding:`-IDs von Dingen, die auf 26.3 in Sub-Mods liegen (Hammer-Verzauberungen break_through/radius/override u.a.).
Entscheidung: statt ~100 Strings einzeln umzuschreiben, wird jede Guide-ID zentral ueber SimpleLibs `IdMoves` auf die heutige ID aufgeloest
(`GuideContent.moved`, angewandt in `item()`, `enchantment()` und `GuideTabs.Tab.gateKeys()`). 26.2: keine Moves, unveraendert.
Test: `theEnchantmentsGuideCoversEveryModEnchantment` prueft jetzt auch Verzauberungen der Sub-Mod-Namensraeume (Hammers/Storage/BTools); Chapter-/Gate-Tests pruefen Items/Rezepte gegen die Registry.
Verifikation: compile, `simplebuilding:guide_book*`, `simplebuilding:data_integrity*` (fabric-263).
