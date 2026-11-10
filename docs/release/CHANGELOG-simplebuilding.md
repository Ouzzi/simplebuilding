# SimpleBuilding 2.0.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

Version 2.0.0 is the module split for Minecraft 26.3 on Fabric. Back up your world before you update.

Building tools, storage, machines and enchantments for Minecraft; the super mod of the Simple family, bundling its required sub-mods.

## Added

- New required sub-mods Simple Building Tools (building wands, octants, blueprints) and Simple Chisels (the seven chisels and Fast Chiseling) take over those tools from the core mod.
- Simple Blocks gains 42 new building blocks (mossy shapes, cracked prismarine, cracked polished deepslate, cracked blue nether bricks, and stairs, slabs and walls in the style of the Vanilla families), plus 9 new octet blocks.
- Cracked Astralit, Nihilit and Ender Quartz bricks, Nihilit End planks (Ashwood and Duskwood), and stairs, slabs and walls for Astralit and Nihilit blocks and the ender quartz wall.
- New book designs for chiseled bookshelves.
- The Echo Sounder now lives in Simple Gadgets, and the Strong Upgrade (template and shard) moved to SimpleLib.
- A new Simple Modpack guide (in SimpleLib) gives every Simple mod one shared handbook with a chapter per mod.
- New pressing recipes for Rose Gold.

## Changed

- SimpleBuilding is now a super mod: building tools, chisels, ovens, crucibles, octets, building blocks, hammers, storage, mob heads, pads, gadgets and more live in their own required sub-mods that also run on their own. Existing worlds convert automatically and every old id keeps an alias. Back up your world before updating.
- Each sub-mod has its own config file and takes over your old SimpleBuilding values on the first start.
- Blossom Gold is now Rose Gold; copper and iron tool and armor values were adjusted.
- The single Vanilla crosshair moves to the aim point and the grey extra crosshair is gone.
- Commands start with their mod id; the old names keep working as aliases.
- The mod license changed from CC0 to All Rights Reserved with source-available code and a contributor license agreement (CLA). Earlier versions stay CC0.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: Cloth Config, SimpleLib, Simple Ovens, Simple Mobheads, Simple Crucibles, Simple Octets, Simple Blocks, Simple Hammers, Simple Storage, Simple Lights, Simple Gadgets, Simple Ores, Simple Machines, Simple Arrows, Simple Mobs, Simple Resonance, Simple Enchanting, Simple Pads, Simple Building Tools, Simple Chisels
- Optional integrations: jei, roughlyenoughitems, modmenu, jade, appleskin, mousetweaks, trinkets_updated, curios, simplemaps, simplequestionary, simpleriding, simplesandwiches, simpletweaks, simplemoney, simplefun, simpledimensions, simpletrims, simpleend, simplerespawn, simplebeacons, simpleshops, simplewanderers, simpleanimals, simplemusic, simpleearth, simplechallenges, simpledinos

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplebuilding config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
