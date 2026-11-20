# Simple Livemap 1.0.0-beta.1 - changelog (Minecraft 26.3, Fabric, beta)

## Release notes

**Beta release.** This first version is published as a beta (release channel Beta on Modrinth and CurseForge): features, balance and config options can still change, and bugs are possible. Back up your world and report problems on the issue tracker.

First release of **Simple Livemap** for Minecraft 26.3 on Fabric.

Live web map of the world as the players really explored it (visited chunks and held maps): top-down tiles with map relief, player positions on request, built-in web server (on out of the box, only on this computer, private with a key; own website possible).

## Added

- **Explored chunks** - The map only shows what players really explored: every chunk a player (not a spectator) walked into, plus record.radius chunks around it (default 2 = 5 x 5), and the drawn area of a filled Vanilla map held in a hand (record.maps, default on).
- **Map tiles** - Explored chunks are drawn from above with the Vanilla map colours of their blocks and the Vanilla map relief (higher ground brighter, lower darker, water by depth; render.shading, default on) – the 2.5D look.
- **Web map** - A built-in web server shows the map in the browser (drag, zoom, dimensions).
- **Private or public** - A map is private by default (web.visibility): the page needs the link from /simplelivemap link, which carries a random key after # (browsers never send that part, so it stays out of logs). /simplelivemap visibility public opens it for everyone who reaches the port.
- **Open Livemap in chat** - In single player and on a LAN world the host gets one clickable chat line per session (only the host, can be turned off).
- **Busy port** - When port 8125 is taken, the map uses the next free port up to 8134 for the session and says so in the log.
- **Own website** - Guide with nginx and Caddy examples and a static-copy script (docs/deploy/LIVEMAP-SELF-HOST.md).
- **Only visible players** - Player positions (web map and hosted map) never include sneaking players, invisible players (potion or otherwise) or spectators; each player can also hide with /simplelivemap hide.
- **Map styles** - Three looks in the view settings of the web map: Blocks (default, block edges when zoomed in), Shaded relief (hillshade and water depth from a height layer) and Cartographer (paper map with contour lines); the choice is kept in the browser.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Optional integrations: simplemaps

## Notes

- The hosted map on livemap.simplemods.de (`/simplelivemap connect <code>`) opens later; until then the command cannot connect. Nothing is uploaded unless a world is connected.
- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplelivemap config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
