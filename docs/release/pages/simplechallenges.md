# Simple Challenges - store page draft

Status: **draft**

## Summary (Modrinth, max 150 characters)

Game modes for a world of your choice, starting with the Unlock mode: priced cells you must free to enter.

## Description (Markdown)

# Simple Challenges

Simple Challenges adds the Unlock mode: the world is cut into cells (a block column, a chunk or three chunks) and only freed cells can be entered. Each cell has a price from the world, with deadlock protection and an emergency unlock. Without a chosen challenge the mod changes nothing.

## Features

- **Safety lock** - Without a chosen challenge Simple Challenges changes nothing: no state file is written, the collision and block hooks return at once and no tick work runs.
- **Unlock grid** - The Overworld is cut into square columns of 1, 2, 4, 8, 16 (a chunk), 32 or 48 blocks (three chunks) over the full height.
- **Border line** - Clients with the mod get the free cells from the server (everything at join, one cell per unlock) and see the edge of the free area as a line of red dust near them.
- **Prices** - Every cell next to the free area has a price, worked out once from the world seed, the cell and what the world offers there, and then kept in the world state.
- **Credits from advancements**
- **Deadlock protection** - When a challenge starts and after every unlock the mod checks that at least one cell next to the free area can be paid with something the free area offers.
- **Presets** - A challenge is a preset file: data/<namespace>/simplechallenges/preset/<id>.json in a mod or data pack, or config/simplechallenges/presets/<id>.json of your own.
- **Commands** - /simplechallenges (status) shows the challenge, free cells, credits and the price of the cell you look at.
- **World creation** - The world type button of the Create New World screen offers Simple Challenges next to Superflat.
- **Skyblock family** - Three world modes start in the void and can be combined with the Unlock mode.
- **Spawn, edges and the crosshair line**
- **Lucky Block and Random Drops** - In a Lucky challenge a block you break drops another block instead of itself.

## Vanilla use

Offers an optional world mode with free and locked areas for players who want a harder start; without a chosen challenge it changes nothing.

## Dependencies

- **Required:** Fabric API, SimpleLib
- **Optional:** SimpleBuilding

## Compatibility

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Java 25
- Fabric only for now.

## Part of the Simple family

**Standalone mod.** It works on its own; other Simple mods add optional extras when installed together.

## License

All Rights Reserved with source-available code. Contributions need the contributor license agreement (CLA). Earlier SimpleBuilding versions stay CC0.

## Links

- Wiki: <WIKI_URL>
- Issues: <ISSUES_URL>
- Source: <SOURCE_URL>

## Categories and tags (suggestion)

- Modrinth categories: game-mechanics, adventure
- Tags: challenge, game-mode, survival
- Environment: client and server

