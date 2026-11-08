# Simple Challenges 0.1.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Challenges** for Minecraft 26.3 on Fabric.

Game modes for a world of your choice: unlock the world cell by cell (chunks, blocks or three chunks), paid with items, credits or levels. Without a chosen challenge the mod changes nothing.

## Highlights (from the commit history)

- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- S5 lucky block, S6 skyline, S7 easter eggs, S8 16 presets
- S3 world creation (type, row, select screen, area editor, crosshair line) + S4 skyblock family; spawn 0,0, mob spawn lock, edge lean, per-player credits
- S0-S2 module - safety lock, preset loader, unlock grid, enforcement, price engine, credits, deadlock protection

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplechallenges config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
