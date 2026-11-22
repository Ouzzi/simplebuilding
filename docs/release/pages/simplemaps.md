# Simple Maps - store page draft

Status: **draft**

## Summary (Modrinth, max 150 characters)

Wayfinder maps: endless maps per dimension with zoom, waypoints in the locator bar and cartography table copies and merges.

## Release channel: Beta

- Version number: `0.1.0-beta.1`
- Modrinth "Version type": **Beta**; CurseForge "Release type": **Beta** (never Release for this first version)

## Description (Markdown)

# Simple Maps

> **Beta.** This mod is released as a beta: features, balance and config options can still change between versions, and bugs are possible. Back up your world before you install or update it, and report problems on the issue tracker.

Simple Maps adds endless maps per dimension with zoom, waypoints shown in the locator bar, framed views, and copying, extending and combining at the cartography table.

## Features

- **Wayfinder maps** - Endless maps, one per dimension: the plain map for the Overworld and other dimensions, a Nether map and an End map.
- **Mega wayfinder map**
- **Picture maps** - Turn a PNG into a wall of locked maps: /simplemaps picture opens a dialog, the picture is split into up to 4 x 4 Vanilla maps that you hang in item frames.
- **Map screen** - Use the map to open it: drag to move, mouse wheel or +/− to zoom (1, 4, 8, 16 and 64 blocks per pixel).
- **Claims layer** - With Simple Tweaks installed, the layers button of the map screen offers a Claims layer: claimed land as colored strips on explored terrain, your own land framed.
- **Waypoints** - Up to eight waypoints belong to the map itself; they show in the locator bar while the map is in the main or off hand.
- **Cartography table** - Copy, extend and combine wayfinder maps at the cartography table; each can be switched off on the server.
- **Maps in item frames** - A framed wayfinder map keeps its view until you save a new one: right-click opens the map, the frame's area is outlined, and saving asks once more.
- **Web map export** - Server admins can export the explored world as a static web map: picture tiles, markers.json and a small viewer page.
- **Creative tabs** - The module's items sit in its own creative tab only (no Vanilla tabs).
- **Commands** - /simplemaps config shows and changes every option of config/simplemaps-server.json (list [group], get, set, reset, help; Tab completes options and values).

## Vanilla use

Gives explorers endless maps per dimension with waypoints and picture maps; Vanilla maps keep working. The map screen and locator bar get client hooks; the web export is off by default.

## Dependencies

- **Required:** Fabric API (no other mods)
- **Included in this download:** SimpleLib
- **Optional:** Simple Money, Simple Ores

## Compatibility

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Java 25
- Fabric only for now.

## Part of the Simple family

**Standalone mod.** It is not part of a super mod; install it on its own with the required mods above.

## License

All Rights Reserved with source-available code. Contributions need the contributor license agreement (CLA). Earlier SimpleBuilding versions stay CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simplemaps
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Source: https://github.com/Ouzzi/simplebuilding

## Categories and tags (suggestion)

- Modrinth categories: utility, adventure
- Tags: maps, waypoints, exploration
- Environment: client and server

