# Simple Blocks 0.9.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Blocks** for Minecraft 26.3 on Fabric.

Missing building blocks: Hushed Deepslate Tiles that dampen vibrations like wool, Sculk Deepslate, the missing Vanilla stairs, slabs and walls and cracked/mossy brick variants. The Shapecutter cuts stone and wood into every stair, slab and shape. Works on its own with Vanilla.

## Added

- **Shapecutter** - The Vanilla stonecutter on a polished iron base with copper corner caps and a polished steel blade, for stone AND wood: it cuts everything the stonecutter cuts, plus planks, logs, wool and every other block whose stairs or slabs the stonecutter cannot make.
- **Hushed Deepslate Tiles** - Deepslate tiles that swallow vibrations like wool: a Sculk Sensor or Warden does not hear a vibration that passes through the block, and walking on it makes none.
- **Sculk Deepslate** - Deepslate with sculk veins.
- **Sculk Tiles** - Deepslate tiles in sculk colors: each tile a little lighter or darker, a few sculk sparks breathe slowly.
- **Missing Stairs, Slabs and Walls** - Stairs, slabs and walls Vanilla lacks: Calcite, Netherrack and Smooth Basalt (all three), Cut (Red) Sandstone stairs and walls, Smooth (Red) Sandstone walls, Smooth Stone walls.
- **Cracked and Mossy Bricks** - Aged variants of Vanilla bricks that have none: cracked Red Nether, Tuff, Mud, End Stone, Cinnabar and Sulfur bricks; mossy Tuff, Mud, End Stone, Cinnabar and Sulfur bricks, mossy Deepslate Bricks and Tiles.
- **Chiseled Blocks** - Chiseled Polished Andesite, Diorite and Granite, Chiseled Prismarine, Chiseled Dark Prismarine and Chiseled Red Nether Bricks: the carved cousins Vanilla lacks.
- **Sandstone Pillars** - Sandstone Pillar and Red Sandstone Pillar: the pillar Vanilla gives quartz and purpur, now for both sandstones.
- **Ice Building Blocks** - Polished ice and ice bricks of ice, packed ice and blue ice, chiseled ice, and stairs, slabs and walls of all three ices.
- **Blue Nether Bricks** - A blue counterpart to Red Nether Bricks, with stairs, slab and wall.
- **Natural Block Variants** - Slabs of dirt, grass, sand and gravel, chiseled packed and blue ice, cracked ice, a nautilus shell block and froglights in three more colors.
- **Suspended and Levitating Sand and Gravel** - Sand and gravel without gravity (suspended) and with reversed gravity (levitating).
- **Snowlogging** - Use a snow layer on a fence, a small flower, grass, a fern or a bush and the snow shares the block with it, like in Bedrock Edition: the plant or fence stays visible in the snow.
- **Frost Grass** - A grass block with snow on top slowly freezes to Frost Grass in cold biomes or with ice below it; without snow it slowly thaws back to grass.
- **Pale Blocks** - A pumpkin, carved pumpkin, jack o'lantern, melon or hay bale placed in the Pale Garden slowly fades like copper weathers: Faded, then Bleached, then Pale (the end stage).
- **Compressed, Dense and Collapsed blocks** - Stage 1 Compressed = 9 blocks, stage 2 Dense = 9 Compressed, stage 3 Collapsed = 9 Dense, for 19 common Vanilla building blocks (dirt, mud, cobblestone, stone, sand, red sand, gravel, clay, netherrack, cobbled deepslate, andesite, diorite, granite, tuff, sandstone, red sandstone, end stone, blackstone, soul sand).

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, jei

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleblocks config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
