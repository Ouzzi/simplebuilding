# Simple Hammers - store page draft

Status: **draft**

## Summary (Modrinth, max 150 characters)

Seven sledgehammers from Stone to Enderite (with Simple Ores) for 3x3 mining, reshaping blocks and in-world upgrades.

## Release channel: Beta

- Version number: `0.9.0-beta.1`
- Modrinth "Version type": **Beta**; CurseForge "Release type": **Beta** (never Release for this first version)

## Description (Markdown)

# Simple Hammers

> **Beta.** This mod is released as a beta: features, balance and config options can still change between versions, and bugs are possible. Back up your world before you install or update it, and report problems on the issue tracker.

Simple Hammers adds seven sledgehammers (Enderite with Simple Ores) that mine a 3x3 area, reshape blocks into stairs and slabs, carve octets (with Simple Octets), crush diamond and quartz blocks, and upgrade blocks in the world. Enchantments: Break Through, Override and Radius.

## Features

- **Sledgehammers: mining a whole area** - Seven sledgehammers from Stone to Enderite.
- **Carving and reshaping** - Sneak and hold use on a block: after a short charge the hammer knocks the aimed octet (an eighth) off it.
- **Crushing diamond and quartz blocks** - Right-click a diamond block or a Cracked Diamond Block nine times with an iron-tier hammer or better: it breaks into 81 Diamond Pebbles (config diamondBlockPebbles).
- **Crushing stone: Stone, Cobblestone, Gravel, Sand** - Right-click (not sneaking) Stone, Cobblestone or Gravel five times with any sledgehammer: Stone becomes Cobblestone, Cobblestone becomes Gravel, Gravel becomes Sand.
- **Strike tempo by hammer tier** - Every conversion with a hammer strikes at the tempo of the tier: Netherite every 20 ticks, Enderite 19, Diamond 21, Gold 22, Iron 23, Copper 24, Stone 25.
- **In-world upgrades with the hammer**
- **Hammer enchantments**
- **A tool in the crafting grid** - A shapeless recipe with a sledgehammer gives the hammer back with 1 durability less instead of using it up; a hammer that breaks doing so is gone.
- **Sledgehammers at the anvil** - Two sledgehammers combine for 1 level: both durabilities plus 12 % of the maximum, enchantments merged like vanilla (incompatible ones from the right hammer are dropped).
- **Older worlds** - Sledgehammers from worlds of earlier SimpleBuilding versions keep working as Simple Hammers' hammers, with their enchantments and begun upgrades.
- **Creative tabs** - The module's items sit in its own creative tab only (no Vanilla tabs).
- **Commands** - /simplehammers config shows and changes every option of config/simplehammers-server.json (list [group], get, set, reset, help; Tab completes options and values).

## Vanilla use

Adds sledgehammers that mine 3x3, reshape and upgrade blocks; they are tools and enchantments on top of Vanilla, with hooks on mining speed, anvil and a few recipes.

## Dependencies

- **Required:** Fabric API (no other mods)
- **Included in this download:** SimpleLib
- **Optional:** SimpleBuilding, Simple Octets, Simple Crucibles, Simple Ores, JEI

## Compatibility

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Java 25
- Fabric only for now.

## Part of the Simple family

**Sub-mod** of SimpleBuilding: included in that download, and it also runs on its own with the required mods above.

## License

All Rights Reserved with source-available code. Contributions need the contributor license agreement (CLA). Earlier SimpleBuilding versions stay CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simplehammers
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Source: https://github.com/Ouzzi/simplebuilding

## Categories and tags (suggestion)

- Modrinth categories: equipment, utility
- Tags: hammer, mining, tools
- Environment: client and server

