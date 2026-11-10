# Simple Maps 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Maps** for Minecraft 26.3 on Fabric.

Wayfinder maps: endless maps per dimension with zoom, waypoints in the locator bar, framed views and cartography table upgrades.

## Added

- **Wayfinder maps** - Endless maps, one per dimension: the plain map for the Overworld and other dimensions, a Nether map and an End map.
- **Mega wayfinder map** - One map for every dimension: buttons in the map screen's title bar switch between the Overworld (grass block), the Nether (netherrack), the End (end stone) and further dimensions; the map remembers the last one.
- **Map screen** - Use the map to open it: drag to move, mouse wheel or +/− to zoom (1, 4, 8, 16 and 64 blocks per pixel).
- **Claims layer** - With Simple Tweaks installed, the layers button of the map screen offers a Claims layer: claimed land as colored strips on explored terrain, your own land framed.
- **Waypoints** - Up to eight waypoints belong to the map itself; they show in the locator bar while the map is in the main or off hand.
- **Cartography table** - Copy, extend and combine wayfinder maps at the cartography table; each can be switched off on the server.
- **Maps in item frames** - A framed wayfinder map keeps its view until you save a new one: right-click opens the map, the frame's area is outlined, and saving asks once more.
- **Web map export** - Server admins can export the explored world as a static web map: picture tiles, markers.json and a small viewer page.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplemoney, simpleores

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplemaps config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
