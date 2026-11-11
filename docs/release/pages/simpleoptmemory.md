# Simple Opt Memory - store page draft

Status: **draft**

## Summary (Modrinth, max 150 characters)

Memory optimizations for block states and models; currently holds the switch, config and mod detection.

## Description (Markdown)

# Simple Opt Memory

Simple Opt Memory is the home for memory optimizations. At the moment it provides the switch, the config and detection of other optimization mods.

## Features

- **Steps back for known mods** - config/simpleoptmemory.json has a mode per area (auto, ours, theirs, off).
- **Shared block-state tables**
- **Commands** - /simpleoptmemory config shows and changes every option of config/simpleoptmemory.json (list [group], get, set, reset, help; Tab completes options and values).

## Dependencies

- **Required:** Fabric API (no other mods)
- **Optional:** none

## Compatibility

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Java 25
- Fabric only for now.

## Part of the Simple family

**Sub-mod** of Simple Optimization. It is a required part of the super mod, and it loads without it as long as its own required mods above are installed.

## License

All Rights Reserved with source-available code. Contributions need the contributor license agreement (CLA). Earlier SimpleBuilding versions stay CC0.

## Links

- Wiki: <WIKI_URL>
- Issues: <ISSUES_URL>
- Source: <SOURCE_URL>

## Categories and tags (suggestion)

- Modrinth categories: optimization
- Tags: performance, memory
- Environment: client and server

