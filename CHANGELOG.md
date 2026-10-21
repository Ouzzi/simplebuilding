# Changelog

## SimpleBuilding 2.0.0 (Minecraft 26.3) – the module split

**Back up your world before you update.** 2.0.0 moves many blocks and items into their own mods. Existing worlds are
converted automatically when they load (items, blocks, block entities, entities, recipe book and advancements), and
every old id keeps an alias, but a backup is the only way back if anything goes wrong.

### What changed

SimpleBuilding is now a *super mod*: it bundles and requires a family of sub-mods, and each sub-mod also runs on its
own. The SimpleBuilding jar for 26.3 contains everything you need; when you install mods one by one, add these:

| Required sub-mod | What lives there now | New id namespace |
|---|---|---|
| SimpleLib 0.2.0 | Shared materials (cracked diamond, netherite nugget, enderite base, upgrade templates, polished end stone), id moves, in-world upgrades | `simplelib:` |
| Simple Ovens 1.0.0 | Reinforced / Netherite / Enderite furnaces, smokers, blast furnaces and their minecarts | `simpleovens:` |
| Simple Crucibles 1.0.0 | Crucibles, barrels, reinforced cauldron, soul lava and the bucket family | `simplecrucibles:` |
| Simple Octets 1.0.0 | Octet (eighth) blocks and the shape saw recipes | `simpleoctets:` |
| Simple Blocks 1.0.0 | Building blocks without a themed home | `simpleblocks:` |
| Simple Hammers 1.0.0 | The seven sledgehammers, hammer enchantments, in-world crafting | `simplehammers:` |
| Simple Storage 1.0.0 | Chests, tiered chests and shulker boxes, bundles, quivers, backpacks, storage enchantments, rare chest and shulker finds | `simplestorage:` |
| Simple Mobheads 1.0.0 | Mob heads | `simplemobheads:` |

Optional sub-mods (Simple Tweaks, Riding, Maps, Sandwiches, Money, Fun, Dimensions, Trims, Mobs, End, Respawn, Beacons,
Shops, Wanderers, Questionary) stay bundled and can each be switched off in the config tab "Simple Mods".

### Config

- Each sub-mod has its own config file (`config/<mod>-server.json` or `config/<mod>.json`). On the first start it takes
  over the values your `simplebuilding.json` had; after that its own file counts.
- These SimpleBuilding options do nothing on 26.3 any more and are no longer shown in the config screen, in
  `/simplebuilding config list` or in the wiki: `server.machines.*FurnaceSpeed` (now Simple Ovens),
  `server.tools.sledgehammerUpgradeSeconds`, `server.tools.*UpgradeDamagePerHit`, `server.tools.diamondBlockPebbles`
  (now Simple Hammers), `tools.invertBundleInteractions`, `server.features.backpack`, `server.loot.betterChestPercent`,
  `server.loot.*ShulkerPercent`, `server.loot.endermitesPerRareShulker` (now Simple Storage). Your old file still loads
  and keeps those values.
- The backpack key is now Simple Storage's (`key.simplestorage.open_backpack`); your old binding is taken over once.

### Other lines

Minecraft 26.2 and 1.21.11 keep the single-jar SimpleBuilding with `simplebuilding:` ids; they share the version number
2.0.0 but have no content change in this release.

### Downgrading

Going back to 1.3.x after a world was opened with 2.0.0 is not supported yet (the planned 1.3.x-bridge version with
reverse aliases is not built). Use your backup.
