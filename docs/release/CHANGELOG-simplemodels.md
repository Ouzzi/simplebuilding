# Simple Models 0.1.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Models** for Minecraft 26.3 on Fabric.

Server-approved custom item models, anvil assignment, and a searchable model browser.

## Highlights (from the commit history)

- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- /<modid> config for every config option
- A-f - setting switches, wheel paging, base-item filter + sort, tooltip model preview, creative tab, tab clear of the recipe book; plan
- Mod icons with characteristic motif on background tile, smaller strong upgrade shard
- Item stats, death vault, downed screen, models tab on simplelib blocks; contrast lint (ST-30..35)
- New Simple Blocks sub-mod - module skeleton, 33 blocks, tab, loot hook, tests
- Recipe-book unlocks for every module, registry tests and first pickup tips
- Texts for all simplemodels items (EN+DE, lore/use/secret)

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Optional integrations: simplebuilding, modmenu

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplemodels config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
