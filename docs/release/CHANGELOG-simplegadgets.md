# Simple Gadgets 0.9.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Gadgets** for Minecraft 26.3 on Fabric.

Gadgets you hold and use: the Gauge (speed dial, altimeter with fall-damage warning, auto-walk) and the Transposer (turns and swaps blocks in place). Moved from SimpleBuilding (ids migrate); more gadgets follow.

## Added

- **Gauge** - The Gauge shows speed while held in either hand, with an animated item needle and a HUD dial.
- **Transposer** - The Transposer is a handy tool for turning orientable blocks right where they are in the world, without breaking and replacing them.
- **Detector** - The Detector is sonar for mining: hold it in your hand and once every 20 ticks it looks for the nearest ore in a sphere around you, turns its amethyst needle toward the find like a compass toward a lodestone, and plays a sound with a faint particle trail from your eyes to the ore; which of the twenty ticks that falls on depends on the player, so that the detectors of several players do not all scan in the same tick.
- **Attractor: Collecting Items From a Distance** - The Attractor pulls loose items while held in either hand.
- **Readout box** - A held Gauge shows its readout in a small dark box next to the crosshair: the item's name as the title, a speed dial with the number in blocks per second, and further lines.

## Changed

- Content that used to live in SimpleBuilding now lives in this mod. Old worlds convert automatically and every old id keeps an alias; back up your world before updating.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplegadgets config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
