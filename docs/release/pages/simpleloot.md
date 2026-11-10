# Simple Loot - store page draft

Status: **draft**

## Summary (Modrinth, max 150 characters)

Vault cooldown measured in days instead of once per player; a sub-mod of Simple Quality of Life.

## Description (Markdown)

# Simple Loot

Simple Loot lets a player loot a vault again after a configurable number of Minecraft days (default 100) instead of only once.

## Features

- **Vault cooldown** - A player may loot a vault again after vaultCooldownDays, default 100 Minecraft days, bounded 1–36500.
- **Commands** - Permission level 2: /simpleloot vaults cooldown <days> and /simpleloot vaults repeat <true|false>; changes are saved to config/simpleloot.json.
- **Sub-mod of Simple QoL** - Playable alone (config/simpleloot.json).

## Dependencies

- **Required:** Fabric API, Cloth Config
- **Optional:** Simple Quality of Life, Mod Menu
<!-- Cloth Config moves from required to optional once the optional-Cloth change lands; adjust then. -->
## Compatibility

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Java 25
- Fabric only for now.

## Part of the Simple family

**Sub-mod** of Simple Quality of Life. It is a required part of the super mod, and it loads without it as long as its own required mods above are installed.

## License

All Rights Reserved with source-available code. Contributions need the contributor license agreement (CLA). Earlier SimpleBuilding versions stay CC0.

## Links

- Wiki: <WIKI_URL>
- Issues: <ISSUES_URL>
- Source: <SOURCE_URL>

## Categories and tags (suggestion)

- Modrinth categories: game-mechanics
- Tags: vaults, loot, cooldown
- Environment: client and server

