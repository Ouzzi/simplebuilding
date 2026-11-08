# Simple Earth 0.1.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Earth** for Minecraft 26.3 on Fabric.

The Overworld seen from orbit: far above the world it becomes a slightly rounded cube (27 display versions) whose faces show Vanilla map colours read from the world generator - no chunks generated. Flat sun, cube moon, stars; below everything stays Vanilla.

## Highlights (from the commit history)

- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- Free Look hand stays with body + grey real crosshair, default keys, Simple Keybinds fold all/sub-groups
- Earth menu only by key O, 1.5 double door (shared stub column) + door edge textures, only two pressling recipes
- E30 - centre 0~0 (no activation height), key settings first, detail = n pixels per edge, folding world V25-V27 with eased phases, menu: V25-V27 direct, rest in drop-down
- E29 - shared settings, smooth V12, wider limits, free look (simplelib), favourites row + drop-down, orbit versions V13-V24, ideas doc
- /17 display config, detail levels, biome colour table, surface detail, V10-V12
- Config commands /simpleearth config + /simpleclient simpleearth
- 2x2 biome majority per sample from 128 blocks/px, shallow dips stay land (fewer speckles)

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleearth config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
