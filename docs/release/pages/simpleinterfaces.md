# Simple Interfaces - store page draft

Status: **draft**

## Summary (Modrinth, max 150 characters)

Restyled Vanilla container screens, linked containers and easy Shulker and Ender Chest access.

## Description (Markdown)

# Simple Interfaces

Simple Interfaces restyles the Vanilla container screens with a colored box per block, adds linked containers and easy Shulker and Ender Chest handling. A sub-mod of Simple Quality of Life that also runs alone.

## Features

- **Style comparison switch** - In development, or when showStyleToggle is enabled, the button and unbound key toggle all SimpleLib-based screens between the Simple style and Vanilla.
- **Simple container style**
- **Simple style for workstations** - Crafting table, furnace, blast furnace, smoker, brewing stand, beacon and enchanting table screens get the Simple style in the block's color.
- **Simple style for anvil, loom & co.**
- **Linked containers** - Sneak + right-click a container with an empty main hand to mark it (a double chest counts as one); dust particles at your hand show the mark.
- **Easy shulker boxes and ender chests**
- **Shulker box and ender chest tooltips** - Hold Shift over a shulker box to see its contents as a slot grid; the ender chest item shows your own ender chest.
- **Bundles show everything** - Bundle tooltips show every entry instead of Vanilla's twelve, and the mouse wheel selects any of them.
- **Sub-mod of Simple QoL** - Playable alone.
- **Commands** - /simpleinterfaces config shows and changes every option of config/simpleinterfaces-server.json (list [group], get, set, reset, help; Tab completes options and values).
- **Spyglass and zoom** - The spyglass darkens only the screen edge, hides the HUD (crosshair stays) and zooms steplessly with the mouse wheel.
- **Clearer slot highlight** - The highlight of the hovered slot is drawn only behind the item, so the item stays fully visible.

## Dependencies

- **Required:** Fabric API, Cloth Config, SimpleLib
- **Optional:** SimpleBuilding, Mod Menu
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

- Modrinth categories: utility, storage
- Tags: gui, containers, inventory, quality-of-life
- Environment: client and server

