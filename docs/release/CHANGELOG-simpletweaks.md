# Simple Tweaks 0.9.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Tweaks** for Minecraft 26.3 on Fabric.

Legacy compatibility and server-authoritative claims, on by default.

## Highlights (from the commit history)

- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- Simple Machines stage 2 - piston upgrades leave the core
- ..3 evil rarity, nether heart/gem recipes, mighty/evil conduits, creative tab policy
- Strong Beacon weakened, Mighty/Evil/Mighty Evil beacons, level icons, range cap 340 (WP-08)
- /<modid> config for the remaining modules, simplelib bundled
- Recipe viewer entries per module, also standalone
- Simplelib PlayerState leave/stop hook, NetherRoof/TrimLight/Fear/Haunt state dropped through it
- Deeds at the cartography table, old claim table becomes a cartography table

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simpledimensions, simplebuilding, simplemoney, simpleweather, simplemaps

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpletweaks config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
