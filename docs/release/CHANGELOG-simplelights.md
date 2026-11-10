# Simple Lights 0.9.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Lights** for Minecraft 26.3 on Fabric.

Light Panes: flat 1 px lamps on any block side - plain, transparent, sixteen colors, a redstone pane whose color follows the signal strength and copper panes that toggle like the Copper Bulb. Moved from SimpleBuilding (ids migrate).

## Added

- **Light Panes** - A flat lamp panel, 1 px thick like an item frame, that sticks to any side of a block: plain, transparent, in the 16 dye colors, a redstone version whose color follows the signal strength, and a copper version that toggles with every redstone pulse.

## Changed

- Content that used to live in SimpleBuilding now lives in this mod. Old worlds convert automatically and every old id keeps an alias; back up your world before updating.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplelights config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
