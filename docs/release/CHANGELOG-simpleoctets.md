# Simple Octets 0.9.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Octets** for Minecraft 26.3 on Fabric.

Octets: half-block cubes (0.5 x 0.5 x 0.5) of every wood, every block with stairs and slabs, and the melon, which mix in one cell, moved from SimpleBuilding (ids migrate). Cut in the stonecutter, crafted back into the block; the sledgehammer of Simple Hammers carves them.

## Highlights (from the commit history)

- Palette stage 2 A-D - 42 Simple Blocks members (mossy shapes, cracked prismarine/polished deepslate/blue nether, Vanilla-family walls/stairs/slabs) + 9 octets
- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- Remove first pickup tips (), mirror chance by level, narrow pipe neck, gadget messages as sound/particles, dead Fabric tab hooks
- Lead plate, lead octets with full soundproofing weight
- Texts for Simple Hammers, Simple Octets and Simple Music (ST-41 follow-up)
- Eight octets merge into the full block, sneaking breaks a corner off (); head right-click into the extra head slot
- Mod icons with characteristic motif on background tile, smaller strong upgrade shard
- Feat: misc 21.10. (enderite repair tag, creative tab names, modules 0.9.0, claim land costs)

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simpleend, simpleores, jei

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleoctets config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
