# Simple Quality of Life 0.9.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Quality of Life** for Minecraft 26.3 on Fabric.

Super-mod: bundles Simple Weather, Simple Interfaces, Simple Loot, Simple Mob Manners, Simple Farming, Simple Tools and Simple Movement with one switch each; guide book and start quest.

## Highlights (from the commit history)

- Commands start with their mod id, old names stay as aliases
- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- /<modid> config for every config option
- Mod icons with characteristic motif on background tile, smaller strong upgrade shard
- Feat: misc 21.10. (enderite repair tag, creative tab names, modules 0.9.0, claim land costs)
- ST-40 guide books + FTB start quests for maps/tools/enddragonfight, ST-41 questionary texts
- New Simple Blocks sub-mod - module skeleton, 33 blocks, tab, loot hook, tests
- Marked container HUD banner, Escape cancel, auto-end after visit, creative guard

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: Cloth Config, Simple Weather, Simple Interfaces, Simple Loot, Simple Mob Manners, Simple Farming, Simple Tools, Simple Movement
- Optional integrations: simplebuilding, modmenu

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplequalityoflife config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
