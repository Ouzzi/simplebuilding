# Simple Dimensions 0.1.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Dimensions** for Minecraft 26.3 on Fabric.

Configurable dimensions, light arches, and safe linked portals.

## Highlights (from the commit history)

- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- /<modid> config for the remaining modules, simplelib bundled
- Mod icons with characteristic motif on background tile, smaller strong upgrade shard
- ST-40 guide books + FTB start quests for maps/tools/enddragonfight, ST-41 questionary texts
- New Simple Blocks sub-mod - module skeleton, 33 blocks, tab, loot hook, tests
- Main switch on by default (owner release), /claim create, held attractor respects claims
- Recipe-book unlocks for every module, registry tests and first pickup tips
- Texts for all simpledimensions items (EN+DE, lore/use/secret)

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: Cloth Config
- Optional integrations: simplebuilding, modmenu, flan, ftbchunks, openpartiesandclaims, griefdefender, claimchunk

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpledimensions config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
