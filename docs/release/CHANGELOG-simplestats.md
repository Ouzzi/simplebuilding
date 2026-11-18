# Simple Stats 1.0.0-beta.1 - changelog (Minecraft 26.3, Fabric, beta)

## Release notes

**Beta release.** This first version is published as a beta (release channel Beta on Modrinth and CurseForge): features, balance and config options can still change, and bugs are possible. Back up your world and report problems on the issue tracker.

First release of **Simple Stats** for Minecraft 26.3 on Fabric.

Every tool, weapon and piece of armor keeps its own statistics: blocks mined, kills by mob and variant, damage absorbed, distances, catches and more, with an item-specific statistics screen.

## Added

- **Item statistics** - Tools, weapons, shields, elytra, fishing rods, brushes, flint and steel and armor (also mount armor) keep their own statistics in the simplestats:item_stats component.
- **Blocks mined** - Every block broken with a tool counts for that tool, listed by block, including blocks with zero hardness.
- **Kills and damage** - A kill counts for the weapon that caused it, by mob and by mob variant (cat type, sheep color, baby, charged creeper, slime size, villager profession and the vanilla variant components).
- **Armor, shields and wearing time** - Damage that armor and its enchantments take away is split among the worn pieces in proportion to their armor points (an approximation); fall damage goes to the boots.
- **Distances and flights** - Boots count the walked, sprinted, sneaked and swum distance; the glider counts distance flown, flights, rocket boosts and the longest flight.
- **Special tools** - Axes count stripped logs, hoes tilled soil, shears sheared mobs by kind and color, brushes finished suspicious blocks, flint and steel its fires, and fishing rods casts and catches (by item and as fish, treasure or junk).
- **Tooltip and statistics screen** - The tooltip shows one headline value with the top entry as an icon; Shift shows the top three with icon, name and amount.
- **Size, inheritance and reset** - Lists are as complete as what was counted.
- **Who counts and who sees** - Everyone who sees an item sees its statistics: nothing is hidden and no owner is stored, so sharing an item shares its numbers.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Optional integrations: simplebuilding

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplestats config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
