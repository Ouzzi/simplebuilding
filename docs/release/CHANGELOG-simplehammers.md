# Simple Hammers 0.9.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Hammers** for Minecraft 26.3 on Fabric.

Seven sledgehammers (Stone to Enderite) that mine a 3x3 area, reshape blocks into stairs and slabs, carve octets, crush diamond and quartz blocks and upgrade blocks in the world strike by strike, with the enchantments Break Through, Override and Radius, moved from SimpleBuilding (ids migrate). With a hammer mod the in-world ways of the other Simple mods take the hammer instead of the axe.

## Added

- **Sledgehammers: mining a whole area** - Seven sledgehammers from Stone to Enderite.
- **Carving and reshaping** - Sneak and hold use on a block: after a short charge the hammer knocks the aimed octet (an eighth) off it.
- **Crushing diamond and quartz blocks** - Right-click a diamond block or a Cracked Diamond Block nine times with an iron-tier hammer or better: it breaks into 81 Diamond Pebbles (config diamondBlockPebbles).
- **Crushing stone: Stone, Cobblestone, Gravel, Sand** - Right-click (not sneaking) Stone, Cobblestone or Gravel five times with any sledgehammer: Stone becomes Cobblestone, Cobblestone becomes Gravel, Gravel becomes Sand.
- **Strike tempo by hammer tier** - Every conversion with a hammer strikes at the tempo of the tier: Netherite every 20 ticks (the old tempo), Enderite 19, Diamond 21, Gold 22, Iron 23, Copper 24, Stone 25.
- **In-world upgrades with the hammer** - Hammer in the main hand, the upgrade material in the off hand, hold use on an upgradable block: one strike per tick-gap of the hammer tier (about one per second, 5 by default), the last one turns it into the next tier with its contents.
- **Hammer enchantments** - Three enchantments for sledgehammers: Radius makes the area 5x5, Break Through (I-II) takes one more layer behind the face per level, Override (I-II) takes other blocks along - level I every pickaxe block, level II everything breakable, and the hammer then also harvests axe, shovel and hoe blocks at its full speed.
- **A tool in the crafting grid** - A shapeless recipe with a sledgehammer gives the hammer back with 1 durability less instead of using it up; a hammer that breaks doing so is gone.
- **Sledgehammers at the anvil** - Two sledgehammers combine for 1 level: both durabilities plus 12 % of the maximum, enchantments merged like vanilla (incompatible ones from the right hammer are dropped).

## Changed

- Content that used to live in SimpleBuilding now lives in this mod. Old worlds convert automatically and every old id keeps an alias; back up your world before updating.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simpleoctets, simplecrucibles, simpleores, jei

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplehammers config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
