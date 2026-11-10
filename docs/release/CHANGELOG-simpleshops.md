# Simple Shops 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Shops** for Minecraft 26.3 on Fabric.

Strong Beacon (6 pyramid levels, 6 more effects), Strong Conduit (7x7x7 frame, 144 blocks under water) and the Strong Upgrade template. Works on its own with the axe; a hammer mod adds the sledgehammer.

## Added

- **Display Case** - A glass case with five goods slots that shows what is for sale; the lid is glass too, so the goods are visible from above.
- **Sales Block** - The cash register of a shop: a wooden block under the Display Case.
- **Sales Storage** - Chest-sized extra store for the earnings.
- **Buying** - Click the Display Case or the Sales Block: pick a goods stack, put the price items into the pay slots (same positions as the price row) and press Trade.
- **Server shop** - An unlimited shop that only operators (permission level 2) create and configure with /simpleshops servershop.
- **Ender shops** - Like an ender chest: the goods, prices and earnings of every shop live in the world data, the blocks are only windows onto them.
- **Trade Terminal** - Remote access to shops: gold ingots, an eye of ender, bookshelves, a lectern and planks.
- **Protection** - A placed shop belongs to its owner (the player who placed it, saved by UUID on the server).
- **Claims and Simple Money** - Standalone, a shop works everywhere.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simplemoney, simpletweaks

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleshops config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
