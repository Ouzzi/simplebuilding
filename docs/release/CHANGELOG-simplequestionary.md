# Simple Questionary 0.1.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Questionary** for Minecraft 26.3 on Fabric.

The Questionary Table: put any item in and read its lore, its use and a secret in an open book.

## Highlights (from the commit history)

- One clean creative tab per mod, family order
- /<modid> config for every config option
- W23-6 book grows with the screen (double-chest margin), thick fold, binding bulge, shadow behind the boxes
- Table screen - column in the middle, book behind it; text page with hierarchy, picture plate, recipe grid + find hint, magnifier
- Mod icons with characteristic motif on background tile, smaller strong upgrade shard
- Layer sources for generators (snap/save), sync, coverage check, Vanilla-nearness audit tool
- Questionary table needs obsidian, template tooltips, Watcher blinks away after a hit, High Gravity fall damage, resonance button tidy (cap line gone, 8 hearts redrawn, button look)
- Recipe-book unlocks for every module, registry tests and first pickup tips

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplequestionary config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
