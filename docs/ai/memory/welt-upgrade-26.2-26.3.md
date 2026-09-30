---
name: welt-upgrade-26-2-26-3
description: "Welt-Upgrade 26.2->26.3: Vanilla-DataFixer ignoriert Mod-Ids; ModDataFixer-Nachlauf, Fixtures, Fallen"
metadata:
  node_type: memory
  type: project
  modified: 2026-09-25T20:00:00.000Z
---

Stand 2026-09-25 (Branch worktree-agent-aa4660d54f117d6fc, Commits 6f177f0d/e1f47dd7, nicht gemergt).
Datenversion 26.2 = 4903, 26.3 = 5023. Doku: docs/UPGRADE-26.2-26.3.md (Audit-Tabelle).

**Kern:** Vanilla-DFU fixt nichts unter Mod-Ids (Mod-BE-Items, Mod-Entities, Inhalt von
simplebuilding:backpack_contents). `DataFixTypesMixin` haengt an `DataFixTypes#update(DataFixer,Dynamic,int,int)`
(alle Welt-Pfade laufen da durch) und ruft `ModDataFixer.afterVanilla`: Mod-Trichter/-Oefen als
minecraft:hopper/furnace/..., levitating_block als falling_block, Rucksack-Eintraege/GhostItems als ITEM_STACK.

**Fakten 26.2->26.3:** 26.2-Entdeckerkarte = `filled_map` mit map_decorations "+" (ExplorerMapItemFix 5008
macht daraus ocean_explorer_map, 5012 benennt in ocean_monument_map um) - `minecraft:ocean_explorer_map` gibt es
auf 26.2 NICHT. BlockState-Codec 26.3 liest nur id/properties (FallingBlock faellt sonst auf Sand zurueck).
pot_decorations Liste->Karte, map_color entfernt.

**Tests:** WorldUpgradeTests (8), Fixtures testing/fixtures/upgrade-26.2/*.snbt; neu schreiben mit
Env `SIMPLEBUILDING_WRITE_UPGRADE_FIXTURES=1` auf :runGametest (Gradle reicht Env durch, kein build.gradle noetig).
Orakel = Vanilla-Truhe mit denselben Stapeln im selben Fixture-Chunk. Falle: ModHopperBlockEntity#setItem
kopiert bei aktivem Filtermodus Stapel in leere Geisterslots.
