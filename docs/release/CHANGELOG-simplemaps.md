# Simple Maps 0.1.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Maps** for Minecraft 26.3 on Fabric.

Wayfinder maps: endless maps per dimension with zoom, waypoints in the locator bar, framed views and cartography table upgrades.

## Highlights (from the commit history)

- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- Enderite variants only with Simple Ores, simpleores recommended
- ..3 evil rarity, nether heart/gem recipes, mighty/evil conduits, creative tab policy
- Key binding page (list + keyboard 60/75/100, conflicts), blueprint and picture map on UiBoxes (WP-25/)
- /<modid> config for every config option
- Feat: inverse trim rim as subtraction, 16 tinted display cases (variant C), pictureMap default on
- Picture maps (AUD-6 stage 3): PNG to Vanilla map wall, /simplemaps picture, server checks
- Mega wayfinder map with dimension buttons, anvil unlocks and enderite recipes

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplemoney, simpleores

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplemaps config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
