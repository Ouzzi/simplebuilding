# Simple Machines 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Machines** for Minecraft 26.3 on Fabric.

The machine parts of the Simple Machines tab: Double Piston (one block, two stages), Signal Dampener (takes 0-3 off the signal), and the piston upgrades - Reinforced (Sticky) Piston (pushes 18, one paid unbreakable), Netherite and Enderite Piston (break the block in front, with durability, repaired with their nugget), Reinforced, Netherite and Enderite Hopper (2/4/8 times as fast, item filter) and their hopper minecarts. Works on its own; the Enderite parts need Simple Ores.

## Added

- **Reinforced, Netherite and Enderite Hoppers** - Three faster hoppers: the Reinforced Hopper (4 ticks between transfers), the Netherite Hopper (2) and the Enderite Hopper (1; Vanilla 8), each with an item filter in its menu.
- **Hopper Minecarts** - A hopper minecart per hopper tier: it takes in 2, 4 or 8 items per tick (the speed of its hopper; Vanilla's cart 1) and filters like the hopper block, with the same menu.
- **Piston upgrades** - Four stronger pistons: the Reinforced Piston and its sticky twin push 18 blocks and, paid with a Redstone Block, one unbreakable block; the Netherite and Enderite Pistons break blocks by signal strength, wear out like tools and are repaired with their nugget.
- **Double Piston** - One block that pushes in two stages: a signal of 1 to 7 brings the head out one block like a Vanilla piston, 8 to 15 slides an arm out of it and moves the head a second block - three blocks long in all.
- **Signal Dampener** - A flat redstone part like the Repeater that takes 0 to 3 off the signal and passes it on one redstone tick later.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simpleores

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplemachines config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
