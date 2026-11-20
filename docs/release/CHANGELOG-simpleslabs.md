# Simple Slabs 0.1.0-beta.1 - changelog (Minecraft 26.3, Fabric, beta)

## Release notes

**Beta release.** This first version is published as a beta (release channel Beta on Modrinth and CurseForge): features, balance and config options can still change, and bugs are possible. Back up your world and report problems on the issue tracker.

First release of **Simple Slabs** for Minecraft 26.3 on Fabric.

Half slabs (4 pixels, half a slab) of every block with a slab, freely stacked in the four layers of a block cell, and upright against a wall as side half slab or side slab.

## Added

- **Half slabs: 4-pixel layers of every block with a slab** - 170 half slabs (Vanilla, SimpleBuilding and its sub-mods, also dirt, grass, sand, red sand and gravel) with the textures of their slab. Any of the four layers of a cell can be filled, in any combination: placed at the clicked height, the same half slab fills the free layer at the click.
- **Side half slab and side slab** - Every material also has a side half slab item: right-click on the side of a block stands it upright against it; two layers are a side slab, four fill the cell. One half slab crafts into one side half slab and back.
- **Slab and half slab in one block** - A half slab on its own slab adds a layer.
- **Breaking and water** - Every filled layer drops one half slab (side forms: one side half slab); half slabs hold water like a slab.
- **Sand, gravel and grass** - Sand, red sand and gravel half slabs fall and settle; the grass half slab takes the biome color.
- **Copper half slabs age** - Unwaxed copper half slabs age like Vanilla copper; honeycomb waxes, an axe scrapes.
- **Shapecutter and back** - 2 half slabs or side half slabs from a slab, 4 from the full block (Shapecutter of Simple Blocks, without it the stonecutter); 2 half slabs craft back into the slab.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simpleblocks, simpleend, simpleores, simpleenchanting, simplechess, jei

## Notes

- Half slabs of another mod's blocks exist only while that mod is loaded.
