# Simple Ovens 0.9.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Ovens** for Minecraft 26.3 on Fabric.

Reinforced, Netherite and Enderite furnaces, smokers and blast furnaces (2x/4x/8x speed, double experience from Netherite) and furnace minecarts, moved from SimpleBuilding (ids migrate). Upgrades in the world with the axe on its own, with the sledgehammer of Simple Hammers.

## Added

- **Faster Furnaces: Reinforced, Netherite and Enderite** - Three upgraded tiers of the furnace, the smoker and the blast furnace.
- **In-World Upgrade** - A placed Reinforced machine becomes the Netherite one with a Netherite Nugget in the off hand, a Netherite machine the Enderite one with an Enderite Nugget.
- **Furnace Minecarts** - Reinforced, Netherite and Enderite furnace minecarts: one fuel item burns 2, 4 or 8 times as long as in Vanilla's furnace cart (the speed of the furnace of the tier), and the cart runs at 5/8, 3/4 or all of a plain minecart's top speed.
- **Furnace Boats** - A Vanilla boat with a furnace where the chest boat has its chest: you steer, the furnace pushes.

## Changed

- Content that used to live in SimpleBuilding now lives in this mod. Old worlds convert automatically and every old id keeps an alias; back up your world before updating.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simplefoundry, simplecrucibles, simpleores

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleovens config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
