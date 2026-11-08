# Simple Opt Memory 0.1.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Opt Memory** for Minecraft 26.3 on Fabric.

Memory optimizations of Simple Optimization: block states share their read-only property and face tables (steps back for FerriteCore); model baking later.

## Highlights (from the commit history)

- /<modid> config for the remaining modules, simplelib bundled
- Mod icons with characteristic motif on background tile, smaller strong upgrade shard
- Stage 4 - O3 more culling, O9 detail switches, O4 shared state tables, S5b activation range (off), target check
- Simple Optimization super-mod, sub-mods render/memory/server, compat engine (framework 0.1.12), O1/O7/M1/M2/S5a

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleoptmemory config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
