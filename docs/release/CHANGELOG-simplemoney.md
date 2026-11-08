# Simple Money 0.9.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Money** for Minecraft 26.3 on Fabric.

Currency production, villager trades and treasure loot.

## Highlights (from the commit history)

- Simple Building Tools as its own required sub-mod - building wands, octants and blueprints move out of the SimpleBuilding core
- Simple Chisels as its own required sub-mod - the seven chisels, their datapack tables and Fast Chiseling move out of the SimpleBuilding core
- Strong Upgrade (template + shard) moves to SimpleLib
- G4 Simple Gadgets - the Echo Sounder moves to the sub-mod (Fabric, part 1)
- Cracked Astralit/Nihilit/Ender Quartz bricks, Nihilit End planks (Ashwood/Duskwood), book designs for chiseled shelves (WU 06.11. 7-9)
- Pressling blocks, netherite/enderite scrap blocks, sage orb block, rose carrots and nuggets
- Compressed rename, Collapsed stage with Slowness I-III and SimpleLib SlotAlert
- Compact/Dense blocks for 19 Vanilla bases

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: Cloth Config
- Optional integrations: jei, modmenu, simplebuilding, simplefun, simplemodels, simplequalityoflife, simpleriding, simplesandwiches, simplesounds, simpletweaks, simplevisuals, wiringexample, simpleores

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplemoney config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
