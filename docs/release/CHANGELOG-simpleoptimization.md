# Simple Optimization 1.0.0-beta.1 - changelog (Minecraft 26.3, Fabric, beta)

## Release notes

**Beta release.** This first version is published as a beta (release channel Beta on Modrinth and CurseForge): features, balance and config options can still change, and bugs are possible. Back up your world and report problems on the issue tracker. No performance figures are promised: the effect has not been measured across computers and mod packs yet.

First release of **Simple Optimization** for Minecraft 26.3 on Fabric.

Super-mod: bundles Simple Opt Render, Simple Opt Memory and Simple Opt Server, with one on/off switch each.

## Added

- **Super-mod for optimizations** - Simple Optimization has no content of its own: it bundles Simple Opt Render, Simple Opt Memory and Simple Opt Server. config/simpleoptimization.json holds one switch each (enableSimpleOptRender, enableSimpleOptMemory, enableSimpleOptServer, default true); off makes that sub-mod inert, its own settings stay in its own config.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: Simple Opt Render, Simple Opt Memory, Simple Opt Server

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleoptimization config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
