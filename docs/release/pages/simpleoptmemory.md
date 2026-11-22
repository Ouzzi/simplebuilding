# Simple Opt Memory - store page draft

Status: **draft**

## Summary (Modrinth, max 150 characters)

Memory optimization for block states: shares their property and face tables, with switch, config and mod detection.

## Release channel: Beta

- Version number: `1.0.0-beta.1`
- Modrinth "Version type": **Beta**; CurseForge "Release type": **Beta** (never Release for this first version)

## Description (Markdown)

# Simple Opt Memory

> **Beta.** This mod is released as a beta: features, balance and config options can still change between versions, and bugs are possible. Back up your world before you install or update it, and report problems on the issue tracker.

> No performance promises: the effect has not been measured across computers and mod packs yet, so this page names no FPS, memory or tick-time figures. Results depend on your hardware and the other mods you run.

Simple Opt Memory is the home for memory optimizations. It shares the property, key and face-support tables of block states that hold the same values, and provides a switch, a config and detection of other optimization mods (it steps aside for FerriteCore).

## Features

- **Steps back for known mods** - config/simpleoptmemory.json has a mode per area (auto, ours, theirs, off).
- **Shared block-state tables**
- **Commands** - /simpleoptmemory config shows and changes every option of config/simpleoptmemory.json (list [group], get, set, reset, help; Tab completes options and values).

## Dependencies

- **Required:** Fabric API (no other mods)
- **Included in this download:** SimpleLib
- **Optional:** none

## Compatibility

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Java 25
- Fabric only for now.

## Part of the Simple family

**Sub-mod** of Simple Optimization: included in that download, and it also runs on its own with the required mods above.

## License

All Rights Reserved with source-available code. Contributions need the contributor license agreement (CLA). Earlier SimpleBuilding versions stay CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simpleoptmemory
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Source: https://github.com/Ouzzi/simplebuilding

## Categories and tags (suggestion)

- Modrinth categories: optimization
- Tags: performance, memory
- Environment: client and server

