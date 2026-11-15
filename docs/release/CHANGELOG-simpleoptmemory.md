# Simple Opt Memory 1.0.0-beta.1 - changelog (Minecraft 26.3, Fabric, beta)

## Release notes

**Beta release.** This first version is published as a beta (release channel Beta on Modrinth and CurseForge): features, balance and config options can still change, and bugs are possible. Back up your world and report problems on the issue tracker. No performance figures are promised: the effect has not been measured across computers and mod packs yet.

First release of **Simple Opt Memory** for Minecraft 26.3 on Fabric.

Memory optimizations of Simple Optimization: block states share their read-only property and face tables (steps back for FerriteCore); model baking later.

## Added

- **Shared block-state tables** - Block states that hold the same property values (for example every stair block facing east) share one value array instead of one each, blocks with the same properties share one key array, and states with the same face-support pattern share one table.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleoptmemory config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
