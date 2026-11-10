# Simple Octets 0.9.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Octets** for Minecraft 26.3 on Fabric.

Octets: half-block cubes (0.5 x 0.5 x 0.5) of every wood, every block with stairs and slabs, and the melon, which mix in one cell, moved from SimpleBuilding (ids migrate). Cut in the stonecutter, crafted back into the block; the sledgehammer of Simple Hammers carves them.

## Added

- **Octets: half-block cubes of every material** - An octet is a cube of 0.5 x 0.5 x 0.5 blocks.
- **Shapecutter and back** - The Shapecutter of Simple Blocks cuts 8 octets from the full block of their material (wood octets from the planks); with Simple Blocks the Vanilla stonecutter has no octet recipes.
- **Mixed cells** - Octets of different materials share one cell: the cell becomes the mixed octet cell, which keeps the material of each octet in its block entity, and turns back into a pure cell as soon as one material is left.
- **Melon octets** - Sneak + right-click a block with a melon slice to place it as a melon octet (without sneaking it is eaten).
- **Octets of other mods' blocks** - Octets of another mod's blocks exist only while that mod is loaded, with their recipes; without it neither the cells nor the recipes are there.

## Changed

- Content that used to live in SimpleBuilding now lives in this mod. Old worlds convert automatically and every old id keeps an alias; back up your world before updating.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simpleend, simpleores, jei

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleoctets config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
