# Simple Livemap 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Livemap** for Minecraft 26.3 on Fabric.

Live web map of the world as the players really explored it (visited chunks and held maps): top-down tiles with map relief, player positions on request, optional built-in web server (off by default, local only, private with a key).

## Added

- **Explored chunks** - The map only shows what players really explored: every chunk a player (not a spectator) walked into, plus record.radius chunks around it (default 2 = 5 x 5), and the drawn area of a filled Vanilla map held in a hand (record.maps, default on).
- **Map tiles** - Explored chunks are drawn from above with the Vanilla map colours of their blocks and the Vanilla map relief (higher ground brighter, lower darker, water by depth; render.shading, default on) – the 2.5D look.
- **Web map** - An optional built-in web server shows the map in the browser (drag, zoom, dimensions).
- **Private or public** - A map is private by default (web.visibility): the page needs the link from /simplelivemap link, which carries a random key after # (browsers never send that part, so it stays out of logs). /simplelivemap visibility public opens it for everyone who reaches the port.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Optional integrations: simplemaps

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplelivemap config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
