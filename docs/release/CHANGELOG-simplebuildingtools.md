# Simple Building Tools 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Building Tools** for Minecraft 26.3 on Fabric.

Building tools of the SimpleBuilding family: Building Wands (copper to Enderite and creative), the Octant in 17 colors and the Blueprint with editor, picture import and cartography table copies. Simple Ores adds the Enderite wand, SimpleBuilding the backpack supply and the placed octants.

## Added

- **Simple Building Tools: a sub-mod of SimpleBuilding** - The Building Wands, the Octant and the Blueprint have their own mod since 07.11.2026; SimpleBuilding bundles it as a required sub-mod, and it runs on its own with only SimpleLib.
- **Building Wand: whole areas in one click** - The building wand is the mod's area tool: you click a block face with it and it fills a square plane in front of that face with blocks from your inventory - ring by ring from the center outwards, each block oriented the way you would place it yourself.
- **Octant: Measuring and Planning Shapes** - The Octant is SimpleBuilding's tape measure and planning tool.
- **Blueprint: Writing, Scanning and Building Structures** - A Blueprint stores a whole structure as a short, readable build code.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simpleores, simplestorage, simplehammers, simpleoctets

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplebuildingtools config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
