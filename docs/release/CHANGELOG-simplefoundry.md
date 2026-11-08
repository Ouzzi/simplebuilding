# Simple Foundry 0.1.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Foundry** for Minecraft 26.3 on Fabric.

A multiblock foundry: build a tank of melting chambers in a brick hull, throw in ore and fuel, and pour the melt by layer through spouts into casting trays. Gold, copper and iron, one ingot per ingot. Works on its own with Vanilla; a sub-mod of Simple Ovens.

## Highlights (from the commit history)

- Fuel tank with gauge, layered melt in chamber windows, desk window (FOUNDRY32-UI A)
- Desk tiers 3/6/9/12, layer arrow, gear melting, cast target interface
- New sub-mod simplefoundry of Simple Ovens - multiblock foundry stage 1

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Optional integrations: simpleovens

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplefoundry config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
