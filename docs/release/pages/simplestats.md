# Simple Stats - store page draft

Status: **draft**

## Summary (Modrinth, max 150 characters)

Every tool, weapon and armor piece keeps its own statistics with an item-specific statistics screen.

## Description (Markdown)

# Simple Stats

Simple Stats records blocks mined, kills by mob and variant, damage absorbed, distances and catches per item and shows them in a statistics screen.

## Features

- **Item statistics** - Tools, weapons, shields, elytra, fishing rods, brushes, flint and steel and armor (also mount armor) keep their own statistics in the simplestats:item_stats component.
- **Blocks mined** - Every block broken with a tool counts for that tool, listed by block, including blocks with zero hardness.
- **Kills and damage**
- **Armor, shields and wearing time** - Damage that armor and its enchantments take away is split among the worn pieces in proportion to their armor points (an approximation); fall damage goes to the boots.
- **Distances and flights** - Boots count the walked, sprinted, sneaked and swum distance; the glider counts distance flown, flights, rocket boosts and the longest flight.
- **Special tools**
- **Tooltip and statistics screen** - The tooltip shows one headline value and the hint "Hold Shift for more"; with Shift it shows the most important values.
- **Size, inheritance and reset** - Lists are as complete as what was counted.
- **Who counts and who sees** - Everyone who sees an item sees its statistics: nothing is hidden and no owner is stored, so sharing an item shares its numbers.
- **Commands** - /simplestats reset clears the statistics of the held item, /simplestats show lists the raw values, /simplestats reload reads the settings again.

## Dependencies

- **Required:** Fabric API (no other mods)
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

- Modrinth categories: utility, game-mechanics
- Tags: statistics, tools, weapons, armor
- Environment: client and server

