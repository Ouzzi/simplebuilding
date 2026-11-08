# Simple Shops 0.1.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Shops** for Minecraft 26.3 on Fabric.

Strong Beacon (6 pyramid levels, 6 more effects), Strong Conduit (7x7x7 frame, 144 blocks under water) and the Strong Upgrade template. Works on its own with the axe; a hammer mod adds the sledgehammer.

## Highlights (from the commit history)

- Commands start with their mod id, old names stay as aliases
- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- ..3 evil rarity, nether heart/gem recipes, mighty/evil conduits, creative tab policy
- Feat: 1.5 wide doors, double piston redraw, final shops look, cracked packed/blue ice, beacon icon B
- /<modid> config for every config option
- Feat: inverse trim rim as subtraction, 16 tinted display cases (variant C), pictureMap default on
- Terminal needs silk touch, new shop recipes, red display tint example
- Sales storage attaches to its sales block, free display cases, glass lid

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simplemoney, simpletweaks

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleshops config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
