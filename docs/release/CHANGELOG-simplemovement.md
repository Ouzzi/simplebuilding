# Simple Movement 0.1.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Movement** for Minecraft 26.3 on Fabric.

Manual crawling, server-allowed autowalk, faster ladders and Frost Walker on powder snow; a sub-mod of Simple QoL, also playable alone.

## Highlights (from the commit history)

- Commands start with their mod id, old names stay as aliases
- Free Look hand stays with body + grey real crosshair, default keys, Simple Keybinds fold all/sub-groups
- /<modid> config for the remaining modules, simplelib bundled
- /<modid> config for every config option
- Mod icons with characteristic motif on background tile, smaller strong upgrade shard
- Jump Boost steps up one block (jumpBoostStepUp, max with Step Height)
- Step Height and Sprint treasure enchantments
- Simple Movement sub-mod split out of Simple QoL (stage 8)

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: Cloth Config
- Optional integrations: simplequalityoflife, modmenu

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplemovement config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
