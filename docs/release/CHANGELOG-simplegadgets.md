# Simple Gadgets 0.9.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Gadgets** for Minecraft 26.3 on Fabric.

Gadgets you hold and use: the Gauge (speed dial, altimeter with fall-damage warning, auto-walk) and the Transposer (turns and swaps blocks in place). Moved from SimpleBuilding (ids migrate); more gadgets follow.

## Highlights (from the commit history)

- G4 Simple Gadgets - the Echo Sounder moves to the sub-mod (Fabric, part 1)
- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- Transposer poses armor stands (13 Bedrock poses), broken stand keeps pose
- Remove first pickup tips (), mirror chance by level, narrow pipe neck, gadget messages as sound/particles, dead Fabric tab hooks
- ..3 evil rarity, nether heart/gem recipes, mighty/evil conduits, creative tab policy
- G2b Simple Gadgets - the Resonance Rod moves to the sub-mod (Fabric)
- G2a Simple Gadgets - the Transposer moves to the sub-mod (Fabric)
- G1 Simple Gadgets - the Gauge moves to a required sub-mod (Fabric)

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplegadgets config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
