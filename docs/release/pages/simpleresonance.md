# Simple Resonance - store page draft

Status: **draft**

## Summary (Modrinth, max 150 characters)

Player value, danger zones and spawn owners: an API and gameplay layer for experience-aware spawning.

## Description (Markdown)

# Simple Resonance

Simple Resonance tracks how experienced a player is, how dangerous the land is, which player a monster spawn belongs to, and adds newcomer protection. Other mods such as Simple Mobs use it as an API.

## Features

- **Player value** - A number from 0 to 1 for how experienced a player is: W = 0.7 x long-term value + 0.3 x the share since the last death.
- **Danger zones** - The land gets more dangerous the farther it is from the world spawn: level 0.5 up to 512 blocks, rising softly to 5 at 6000 blocks (smoothstep, no hard edges).
- **Spawn owner and spawn level** - Every monster spawn attempt belongs to exactly one player nearby, chosen with weight 1/distance (no average).
- **Newcomer protection** - A spawn is at most one level above the level of its owner, and in the first 6 played hours monsters stay at level 1.
- **Interface for other mods**
- **Takeover from SimpleBuilding**
- **Commands** - /simpleresonance shows your value, the level of the land where you stand and your newcomer protection.

## Dependencies

- **Required:** Fabric API (no other mods)
- **Optional:** SimpleBuilding, Simple Mobs

## Compatibility

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Java 25
- Fabric only for now.

## Part of the Simple family

**Sub-mod** of SimpleBuilding, Simple Mobs. It is a required part of the super mod, and it loads without it as long as its own required mods above are installed.

## License

All Rights Reserved with source-available code. Contributions need the contributor license agreement (CLA). Earlier SimpleBuilding versions stay CC0.

## Links

- Wiki: <WIKI_URL>
- Issues: <ISSUES_URL>
- Source: <SOURCE_URL>

## Categories and tags (suggestion)

- Modrinth categories: game-mechanics, library
- Tags: spawning, difficulty, api
- Environment: client and server

