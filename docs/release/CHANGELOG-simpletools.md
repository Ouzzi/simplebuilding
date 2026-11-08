# Simple Tools 0.1.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Tools** for Minecraft 26.3 on Fabric.

Full-durability bonus, the Thrift and Soulbound enchantments, the Death Vault, the Biome Brush, anvil repairs without extra cost and lava buckets into furnaces; a sub-mod of Simple QoL, also playable alone.

## Highlights (from the commit history)

- One clean creative tab per mod, family order
- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- Remove first pickup tips (), mirror chance by level, narrow pipe neck, gadget messages as sound/particles, dead Fabric tab hooks
- /<modid> config for every config option
- W23-5 frames flat from above (variant A), chisel tilts in 6 x 15 degree steps (synced frame datum)
- Death vault legend as icon cards, Simple Music windows in jukebox brown (owner 23.10.)
- Death vault legend in groups with count column, crafting storage items 1 px (owner 22.10.)
- Death vault in box style, crafting storage items 1.5 px high

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: Cloth Config
- Optional integrations: simplebuilding, simplequalityoflife, modmenu

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpletools config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
