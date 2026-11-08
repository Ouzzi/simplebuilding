# Simple Storage 0.9.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Storage** for Minecraft 26.3 on Fabric.

Crates for every wood (open food boxes for nine stacks, hoppers and comparators work), the Storage Crafting Table that keeps its grid, and tiered chests, trapped chests and tier shulker boxes with more slots and bigger stacks, moved from SimpleBuilding (ids migrate); bundles and quivers in three tiers with the storage enchantments Drawer, Deep Pockets and Funnel, bundles set down on a block, and the four backpacks (worn, placed, Deep Pockets and Funnel; ids migrate).

## Highlights (from the commit history)

- Interface fits chests and barrels, pipes without stubs, mod barrels like Vanilla
- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- Lead colour back, pipe window padding, A groove + 5x5 neck, A conduit items, furnace boats for every furnace tier
- Output neck 3x3 -> 4x4 (owner 02.11.)
- Enderite variants only with Simple Ores, simpleores recommended
- Compass window, in/out/off per side, round robin, NBT migration
- Remove first pickup tips (), mirror chance by level, narrow pipe neck, gadget messages as sound/particles, dead Fabric tab hooks
- Owner picks 31.10. - strong gunpowder/TNT, conduit icons+tints, pipe 6-3 neck, braided cable

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simpleend, trinkets_updated, curios, simpleores, simplecrucibles

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplestorage config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
