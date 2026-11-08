# Simple Machines 0.1.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Machines** for Minecraft 26.3 on Fabric.

The machine parts of the Simple Machines tab: Double Piston (one block, two stages), Signal Dampener (takes 0-3 off the signal), and the piston upgrades - Reinforced (Sticky) Piston (pushes 18, one paid unbreakable), Netherite and Enderite Piston (break the block in front, with durability, repaired with their nugget), Reinforced, Netherite and Enderite Hopper (2/4/8 times as fast, item filter) and their hopper minecarts. Works on its own; the Enderite parts need Simple Ores.

## Highlights (from the commit history)

- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- Simple Machines stage 3 - mod hoppers and hopper carts leave the core
- Feat: Jade/JEI show Simple Machines breaker pistons, admin book option, Simple Mobs trimStrength, mutations paths
- Simple Machines stage 2 - piston upgrades leave the core
- Frost grass looks like Vanilla snowy grass, stage-0 Double Piston pushable
- Pushable signal dampener without lever, double piston seam pair A, zebra/zorse compact model B, crab legs point down
- Simple Machines sub-mod stage 1 - Double Piston and Signal Dampener leave the core

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simpleores

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplemachines config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
