# Simple Movement 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Movement** for Minecraft 26.3 on Fabric.

Manual crawling, server-allowed autowalk, faster ladders and Frost Walker on powder snow; a sub-mod of Simple QoL, also playable alone.

## Added

- **Manual crawling** - C (default) or /simplemovement crawl sets the synchronized swimming pose.
- **Auto-walk** - The Auto Walk key (unbound by default) toggles ordinary forward input only when the server enables enableAutowalk (default false).
- **Climbing and sliding** - Climbing speed defaults to 0.4 blocks/tick (range 0.2–0.4).
- **Frost Walker on powder snow** - Frost Walker permits walking on powder snow.
- **Jump Boost steps up** - While Jump Boost is active (potion, beacon or command), the step_height attribute is topped up to at least 1 block, so you walk up a full block without jumping.
- **Step Height** - Treasure enchantment for boots, levels I-II: the step_height attribute rises by 0.4 / 0.9, so you step up 1 block (I) or 1.5 blocks (II, fences) without jumping.
- **Sprint** - Treasure enchantment for leggings, levels I-III: while sprinting, movement speed rises by 10 / 20 / 30 % of the base speed (so +10/20/30 % over normal sprint speed); walking and sneaking are unchanged.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: none (Cloth Config optional: config screen only)
- Optional integrations: simplequalityoflife, modmenu

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplemovement config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
