# Simple Dimensions 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Dimensions** for Minecraft 26.3 on Fabric.

Configurable dimensions, light arches, and safe linked portals.

## Added

- **Default Dimensions** - Three dimensions exist from the first start, no setup needed: Skyblock (a void with a safe island to build on), Mining (a deep stone resource world) and Travel (a flat plane for fast travel).
- **Skyblock** - Build one of six glowstone arches and place a separate emitting block within three blocks of the interior.
- **Mining Dimension** - Portal rows: DXXD / D..D / D..D / D..D / DEED. D is deepslate bricks, X diamond ore, E emerald ore.
- **Travel Dimension** - Portal rows: KRRK / O..O / O..O / O..O / SSSS. K is a wither skeleton skull, R resin bricks, O dark oak logs, S soul sand.
- **Portal Shapes** - The six default matrices are AGA/G.G/G.G; AGGA/G..G/G..G/G..G; AGGGA/G...G/G...G/G...G; AAGGAA/AG..GA/G....G/G....G/G....G; AAGGGAA/AG...GA/G.....G/G.....G/G.....G; AAGGGGAA/AG....GA/G......G/G......G/G......G. A is checked air, G glowstone, and dots become portal cells.
- **Safe Travel and Return** - Only unmounted players travel.
- **Server Claim Permissions** - Enabled Simple Tweaks claims are checked through the public framework API. Activation checks every frame and interior cell; travel checks the source, linked destination, landing, and every generated platform cell.
- **Getting the Guide** - This guide is easy to get: craft the Simple Modpack guide from a book and a crafting table, run /simpledimension guide (any player) to receive it with this chapter open, or travel to a dimension for the first time and it is handed to you once.
- **Inventory and Rights** - Your inventory stays with you: only unmounted players travel, items lying around, mobs and mounts stay behind.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: Cloth Config
- Optional integrations: simplebuilding, modmenu, flan, ftbchunks, openpartiesandclaims, griefdefender, claimchunk

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpledimensions config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
