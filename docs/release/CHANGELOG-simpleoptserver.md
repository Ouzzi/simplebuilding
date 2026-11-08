# Simple Opt Server 0.1.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Opt Server** for Minecraft 26.3 on Fabric.

Server optimizations of Simple Optimization: view distance follows server load (MSPT) within the server.properties bounds; optional activation range for far mobs (off by default); steps back when ServerCore is installed.

## Highlights (from the commit history)

- Commands start with their mod id, old names stay as aliases
- /<modid> config for the remaining modules, simplelib bundled
- S1a/S5b headless measurements (a-opt45b plan)
- Mod icons with characteristic motif on background tile, smaller strong upgrade shard
- Stage 4 - O3 more culling, O9 detail switches, O4 shared state tables, S5b activation range (off), target check
- Simple Optimization super-mod, sub-mods render/memory/server, compat engine (framework 0.1.12), O1/O7/M1/M2/S5a

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleoptserver config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
