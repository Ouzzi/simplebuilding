# Simple Ovens 0.9.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Ovens** for Minecraft 26.3 on Fabric.

Reinforced, Netherite and Enderite furnaces, smokers and blast furnaces (2x/4x/8x speed, double experience from Netherite) and furnace minecarts, moved from SimpleBuilding (ids migrate). Upgrades in the world with the axe on its own, with the sledgehammer of Simple Hammers.

## Highlights (from the commit history)

- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- Lead colour back, pipe window padding, A groove + 5x5 neck, A conduit items, furnace boats for every furnace tier
- Furnace boats (): boat/raft per Vanilla wood with a furnace, fuel push, derived item overlay, tests
- Enderite variants only with Simple Ores, simpleores recommended
- Player potions, ZO9 Mobs/Resonance required sub-mods, Crucibles sub-mod of Ovens (plan B-)
- Remove first pickup tips (), mirror chance by level, narrow pipe neck, gadget messages as sound/particles, dead Fabric tab hooks
- New sub-mod simplefoundry of Simple Ovens - multiblock foundry stage 1
- ..3 evil rarity, nether heart/gem recipes, mighty/evil conduits, creative tab policy

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simplefoundry, simplecrucibles, simpleores

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleovens config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
