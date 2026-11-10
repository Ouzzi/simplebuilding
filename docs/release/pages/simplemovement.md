# Simple Movement - store page draft

Status: **draft**

## Summary (Modrinth, max 150 characters)

Manual crawling, server-allowed autowalk, faster ladders and Frost Walker on powder snow.

## Description (Markdown)

# Simple Movement

Simple Movement adds crawling on demand, an autowalk the server can allow, faster ladders and Frost Walker that works on powder snow.

## Features

- **Manual crawling** - C (default) or /simplemovement crawl sets the synchronized swimming pose.
- **Auto-walk** - The Auto Walk key (unbound by default) toggles ordinary forward input only when the server enables enableAutowalk (default false).
- **Climbing and sliding** - Climbing speed defaults to 0.4 blocks/tick (range 0.2–0.4).
- **Frost Walker on powder snow** - Frost Walker permits walking on powder snow.
- **Jump Boost steps up** - While Jump Boost is active (potion, beacon or command), the step_height attribute is topped up to at least 1 block, so you walk up a full block without jumping.
- **Commands** - /simplemovement crawl is available to players.
- **Sub-mod of Simple QoL** - Playable alone (config/simplemovement.json).
- **Step Height** - Treasure enchantment for boots, levels I-II: the step_height attribute rises by 0.4 / 0.9, so you step up 1 block (I) or 1.5 blocks (II, fences) without jumping.
- **Sprint**

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

- Modrinth categories: game-mechanics, utility
- Tags: movement, crawl, ladders, quality-of-life
- Environment: client and server

