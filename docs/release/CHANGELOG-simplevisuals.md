# Simple Visuals 0.9.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Visuals** for Minecraft 26.3 on Fabric.

Bounded client HUD, tooltips, anvil formatting, and Vanilla immersion particles.

## Highlights (from the commit history)

- Simple Chisels as its own required sub-mod - the seven chisels, their datapack tables and Fast Chiseling move out of the SimpleBuilding core
- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- Owner choices 31.10. built in
- Feat: chisel on every framed item (), example button in sheet music editor (), stackable cheese/butter layers
- /<modid> config for every config option
- Simple Lights - the 37 Light Panes move to a required sub-mod (Fabric)
- W23-5 frames flat from above (variant A), chisel tilts in 6 x 15 degree steps (synced frame datum)
- Flat blocks and panes lie flat in item frames, seen from above (tilted 15 deg)

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: Cloth Config
- Optional integrations: simplebuilding, simplemodels, modmenu

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplevisuals config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
