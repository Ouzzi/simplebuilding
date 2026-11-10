# Simple Chisels 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Chisels** for Minecraft 26.3 on Fabric.

Seven chisels (Stone to Enderite) that move a block one step along its family - and back with sneak - with the enchantment Fast Chiseling; other mods add chains through data files.

## Added

- **Chisels: the next block of the family** - Seven chisels from Stone to Enderite.
- **Fast Chiseling** - The chisel enchantment (levels I and II, also at the enchanting table): the cooldown after a step drops to 70 % (I) or 40 % (II), never below one tick, and the chisel mines faster (like Efficiency II or IV).
- **Chisel tables as data** - Every chain is a datapack file data/<namespace>/chisel_transformations/<name>.json with a tier (stone, iron, diamond, netherite, enderite), a table (chisel, or touch for Constructor's Touch), chains of block ids (optionally cyclic) and an optional remove list.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simpleores, jei

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplechisels config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
