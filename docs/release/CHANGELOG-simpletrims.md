# Simple Trims 0.1.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Trims** for Minecraft 26.3 on Fabric.

Glowing, Emitting and Pulsating armor trim templates, placeable trim templates and trim upgrades. Works on its own with the axe; SimpleBuilding adds the sledgehammer.

## Highlights (from the commit history)

- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- Vanilla-Form discs, softer sheet lines/jukebox, Duskwood log softer + signs B with lilac chains, inverted trim stage logic (1 px outline, armor-shaded fill)
- ..3 evil rarity, nether heart/gem recipes, mighty/evil conduits, creative tab policy
- /<modid> config for every config option
- Feat: inverse trim rim as subtraction, 16 tinted display cases (variant C), pictureMap default on
- Inverse trim in 3 stages (HARD, RIM, SOFT), stage up per smithing round
- Simplelib PlayerState leave/stop hook, NetherRoof/TrimLight/Fear/Haunt state dropped through it
- Inverting trim upgrade, shared JEI in-world upgrade tab in SimpleLib

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, jei

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpletrims config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
