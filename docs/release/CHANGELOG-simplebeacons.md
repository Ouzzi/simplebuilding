# Simple Beacons 0.1.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Beacons** for Minecraft 26.3 on Fabric.

Strong Beacon (6 pyramid levels, 10 more effects, range by pyramid material), Strong Conduit (7x7x7 frame, 216 blocks under water) and the Strong Upgrade template. Works on its own with the axe; a hammer mod adds the sledgehammer.

## Highlights (from the commit history)

- Strong Upgrade (template + shard) moves to SimpleLib
- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- Lead colour back, pipe window padding, A groove + 5x5 neck, A conduit items, furnace boats for every furnace tier
- Enderite variants only with Simple Ores, simpleores recommended
- Remove first pickup tips (), mirror chance by level, narrow pipe neck, gadget messages as sound/particles, dead Fabric tab hooks
- Owner picks 31.10. - strong gunpowder/TNT, conduit icons+tints, pipe 6-3 neck, braided cable
- ..3 evil rarity, nether heart/gem recipes, mighty/evil conduits, creative tab policy
- Feat: 1.5 wide doors, double piston redraw, final shops look, cracked packed/blue ice, beacon icon B

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simpleores

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplebeacons config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
