# Simple Music 0.1.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Music** for Minecraft 26.3 on Fabric.

Sheet music as a text note code (editor with test listening, .txt/.nbs import), pressed onto blank discs in the Disc Press, and uploaded mono Ogg discs with quota and OP approval, played in the Vanilla jukebox and the Pocket Jukebox.

## Highlights (from the commit history)

- Commands start with their mod id, old names stay as aliases
- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- Vanilla-Form discs, softer sheet lines/jukebox, Duskwood log softer + signs B with lilac chains, inverted trim stage logic (1 px outline, armor-shaded fill)
- Remove first pickup tips (), mirror chance by level, narrow pipe neck, gadget messages as sound/particles, dead Fabric tab hooks
- ..3 evil rarity, nether heart/gem recipes, mighty/evil conduits, creative tab policy
- Feat: chisel on every framed item (), example button in sheet music editor (), stackable cheese/butter layers
- Info pages (structure discs), fletching table entry, money JEI entrypoint, copper chest by id
- Recipe viewer entries per module, also standalone

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simplemobheads

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplemusic config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
