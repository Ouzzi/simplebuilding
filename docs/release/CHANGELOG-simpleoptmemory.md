# Simple Opt Memory 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Opt Memory** for Minecraft 26.3 on Fabric.

Memory optimizations of Simple Optimization: block states share their read-only property and face tables (steps back for FerriteCore); model baking later.

## Added

- **Shared block-state tables** - Block states that hold the same property values (for example every stair block facing east) share one value array instead of one each, blocks with the same properties share one key array, and states with the same face-support pattern share one table.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleoptmemory config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
