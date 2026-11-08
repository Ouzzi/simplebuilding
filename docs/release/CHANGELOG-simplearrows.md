# Simple Arrows 0.1.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Arrows** for Minecraft 26.3 on Fabric.

Arrows: the Fletching Table (tip, shaft and fletching, an optional potion: 4 arrows with their own damage and flight), the Light Arrow, which lights the spot where it sticks until it despawns like a normal arrow, and the Silver Arrow, which hits undead harder (crafted with Simple Ores' silver). Works on its own.

## Highlights (from the commit history)

- Silver sheet (1 px plate, 12 per 3 ingots), mirror = glass pane + sheet, lead arrow tip (gravity x1.5, +0.5 damage)
- Simple Arrows stage 2 - Fletching Table and all SimpleBuilding arrows move into the module
- Simple Arrows sub-mod - Silver Arrow moved from Simple Ores, Light Arrow; Fletching Table potion slot (), engraving A

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simpleores

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplearrows config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
