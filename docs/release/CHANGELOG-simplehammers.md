# Simple Hammers 0.9.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Hammers** for Minecraft 26.3 on Fabric.

Seven sledgehammers (Stone to Enderite) that mine a 3x3 area, reshape blocks into stairs and slabs, carve octets, crush diamond and quartz blocks and upgrade blocks in the world strike by strike, with the enchantments Break Through, Override and Radius, moved from SimpleBuilding (ids migrate). With a hammer mod the in-world ways of the other Simple mods take the hammer instead of the axe.

## Highlights (from the commit history)

- Blossom Gold -> Rose Gold (IdMoves), Rose Copper display name, new pressing recipes, tool/armor values (copper/iron durability, faster)
- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- Enderite variants only with Simple Ores, simpleores recommended
- Remove first pickup tips (), mirror chance by level, narrow pipe neck, gadget messages as sound/particles, dead Fabric tab hooks
- ..3 evil rarity, nether heart/gem recipes, mighty/evil conduits, creative tab policy
- New sub-mod simpleores - silver, rose gold (Rose Bronze/Blossom Gold), tools/armor, optional rose gold hammers
- Static JEI coverage check, hammer rank comparator
- Recipe viewer entries per module, also standalone

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simpleoctets, simplecrucibles, simpleores, jei

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplehammers config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
