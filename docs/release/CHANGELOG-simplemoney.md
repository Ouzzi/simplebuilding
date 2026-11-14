# Simple Money 0.9.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Money** for Minecraft 26.3 on Fabric.

Currency production, villager trades and treasure loot.

## Added

- **Special Paper** - Crafted from two paper and one honeycomb; used with an iron ingot and resin fiber to smith a banknote blank.
- **Special Fiber** - Crafted from copper nuggets, gold nuggets, amethyst shards, and diamonds; used with a gold ingot to refine a banknote blank.
- **Resin Fiber** - Crafted from iron nuggets, resin clumps, honeycomb, and bone meal; used to smith banknote blanks.
- **Banknote Blank** - An uncommon smithing ingredient made from special paper, resin fiber, and an iron ingot.
- **Refined Banknote Blank** - An uncommon ingredient made by refining a banknote blank.
- **Raw Bill** - A rare crafting result.
- **Money Bill** - Currency for the added villager and wandering trader offers.
- **47 extra trade offers** - Villagers (armorer 8, cleric 2, farmer 7, fletcher 3, librarian 5, mason 8, toolsmith 7) and the wandering trader (7) sell and buy more goods for money bills. One rule prices all of them: one bill is worth 3 diamonds or 16 emeralds. Vanilla and SimpleBuilding offers stay in the pool.
- **Treasure currency** - One independent roll per chest.
- **Paper rocket recipe** - Additional source recipe: seven paper and two gunpowder produce one rocket.
- **Money Links: Add-on Trades** - Adds buy-only money offers for loaded mods without removing their original trades.
- **Linked Price Table: simplebuilding** - 148 item prices; loaded-mod condition: simplebuilding.
- **Linked Price Table: simplepads** - 37 item prices; loaded-mod condition: simplepads (moved from SimpleBuilding on 04.11.2026).
- **Linked Price Table: simplebuildingtools** - 24 item prices; loaded-mod condition: simplebuildingtools (moved from SimpleBuilding on 07.11.2026).
- **Linked Price Table: simplechisels** - Prices for the Simple Chisels items when Simple Money is installed.
- **Linked Price Table: simplefun** - 1 item prices; loaded-mod condition: simplefun.
- **Linked Price Table: simplemobheads** - 89 item prices; loaded-mod condition: simplemobheads.
- **Linked Price Table: simplemodels** - 0 item prices; loaded-mod condition: simplemodels.
- **Linked Price Table: simplequalityoflife** - 0 item prices; loaded-mod condition: simplequalityoflife.
- **Linked Price Table: simpleriding** - 0 item prices; loaded-mod condition: simpleriding.
- **Linked Price Table: simplesounds** - 0 item prices; loaded-mod condition: simplesounds.
- **Linked Price Table: simplecrucibles** - 2 item prices; loaded-mod condition: simplecrucibles.
- **Linked Price Table: simpletools** - 1 item prices; loaded-mod condition: simpletools.
- **Linked Price Table: simplehammers** - 7 item prices; loaded-mod condition: simplehammers.
- **Linked Price Table: simpleovens** - 9 item prices; loaded-mod condition: simpleovens.
- **Linked Price Table: simplemachines** - 4 item prices; loaded-mod condition: simplemachines (the Enderite Piston also needs simpleores).
- **Linked Price Table: simpletrims** - 3 item prices; loaded-mod condition: simpletrims.
- **Linked Price Table: simpletweaks** - 0 item prices; loaded-mod condition: simpletweaks.
- **Linked Price Table: simplevisuals** - 0 item prices; loaded-mod condition: simplevisuals.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib (Cloth Config optional: config screen only)
- Optional integrations: jei, modmenu, simplebuilding, simplefun, simplemodels, simplequalityoflife, simpleriding, simplesandwiches, simplesounds, simpletweaks, simplevisuals, wiringexample, simpleores

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplemoney config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
