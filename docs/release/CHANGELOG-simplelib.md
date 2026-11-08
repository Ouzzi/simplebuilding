# SimpleLib 0.4.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **SimpleLib** for Minecraft 26.3 on Fabric.

Shared library of the Simple mods: common materials and upgrade templates, id migrations, config commands, UI building blocks, heat sources and warm food. Bundled by the Simple mods that need it.

## Highlights (from the commit history)

- Simple Chisels as its own required sub-mod - the seven chisels, their datapack tables and Fast Chiseling move out of the SimpleBuilding core
- Strong Upgrade (template + shard) moves to SimpleLib
- G4 Simple Gadgets - the Echo Sounder moves to the sub-mod (Fabric, part 1)
- Pressling blocks, netherite/enderite scrap blocks, sage orb block, rose carrots and nuggets
- Compressed rename, Collapsed stage with Slowness I-III and SimpleLib SlotAlert
- Single vanilla crosshair moved to aim point, grey extra crosshair removed
- Commands start with their mod id, old names stay as aliases
- Simple Pads as its own required sub-mod - pads, pressure plates and the Spawn Elytra move out of the SimpleBuilding core

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Optional integrations: jei

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplelib config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
