# Simple Crucibles 0.9.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Crucibles** for Minecraft 26.3 on Fabric.

Iron, Reinforced, Netherite and Enderite crucibles (a cooking station without fuel; the barrels that dock onto them are Simple Storage's), the reinforced cauldron and village field kitchens, moved from SimpleLib and SimpleBuilding (ids migrate). Upgrades in the world with the axe on its own, with the sledgehammer of Simple Hammers.

## Highlights (from the commit history)

- Interface fits chests and barrels, pipes without stubs, mod barrels like Vanilla
- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- Enderite variants only with Simple Ores, simpleores recommended
- Remove first pickup tips (), mirror chance by level, narrow pipe neck, gadget messages as sound/particles, dead Fabric tab hooks
- ..3 evil rarity, nether heart/gem recipes, mighty/evil conduits, creative tab policy
- Village kitchen one block lower, 10 % magma; dev mods DH/Voxy/Sodium
- Recipe viewer entries per module, also standalone
- /<modid> config for every config option

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, jei, jade, simplestorage, simpleores

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplecrucibles config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
