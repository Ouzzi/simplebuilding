# Simple Crucibles 0.9.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Crucibles** for Minecraft 26.3 on Fabric.

Iron, Reinforced, Netherite and Enderite crucibles (a cooking station without fuel; the barrels that dock onto them are Simple Storage's), the reinforced cauldron and village field kitchens, moved from SimpleLib and SimpleBuilding (ids migrate). Upgrades in the world with the axe on its own, with the sledgehammer of Simple Hammers.

## Added

- **Crucibles** - A cooking station without fuel that cooks every slot at once, heated by the block below.
- **Village field kitchens** - Villages of all five types can have a small field kitchen: an iron crucible on an unlit campfire with cooked food, a barrel of raw meat and a seat.
- **Reinforced cauldron** - A cauldron for water, lava, powder snow and soul lava; lava in it heats a crucible like a lava source.
- **Soul lava** - A rare Nether fluid and the only extreme heat source: it flows short and slow, is never replaced, and gives Seelenbrand (soul burn).
- **Copper and soul lava buckets** - Buckets for the crucible's fluids: the iron bucket breaks on soul lava, copper oxidizes.

## Changed

- Content that used to live in SimpleBuilding now lives in this mod. Old worlds convert automatically and every old id keeps an alias; back up your world before updating.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, jei, jade, simplestorage, simpleores

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplecrucibles config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
