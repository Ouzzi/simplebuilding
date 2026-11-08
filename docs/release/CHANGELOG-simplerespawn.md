# Simple Respawn 0.1.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Respawn** for Minecraft 26.3 on Fabric.

Downed instead of dead: a teammate can revive you before you bleed out (then 3 hearts, no food).

## Highlights (from the commit history)

- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- /<modid> config for every config option
- Mod icons with characteristic motif on background tile, smaller strong upgrade shard
- Item stats, death vault, downed screen, models tab on simplelib blocks; contrast lint (ST-30..35)
- Feat: Simple Respawn shows a red name tag with time left and a red locator-bar mark for downed players
- Recipe-book unlocks for every module, registry tests and first pickup tips

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplerespawn config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
