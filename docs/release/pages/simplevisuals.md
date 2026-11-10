# Simple Visuals - store page draft

Status: **draft**

## Summary (Modrinth, max 150 characters)

Client HUD, tooltip and anvil formatting plus Vanilla-style immersion particles, all bounded and optional.

## Description (Markdown)

# Simple Visuals

Simple Visuals tweaks how the game looks on the client: HUD, tooltips, anvil text and immersion particles built from Vanilla assets.

## Features

- **Speed lines** - Bounded radial lines at high speed or acceleration; cosmetic only, first-person view.
- **Pickup notifications** - Server pickup animations create up to eight short-lived item or XP rows, with four layouts, scale, side, opacity and rarity colors.
- **Elytra pitch helper** - Two target lines while gliding; defaults -40 and 40 degrees, tolerance 10, sensitivity 4.
- **Status effect duration bars** - HUD and inventory bars compare the current duration with the greatest observed duration of that active effect.
- **Locator player heads** - Uses only server-supplied Vanilla waypoints; replaces eligible icons with player heads and keeps Vanilla angles, visibility and elevation arrows.
- **Chat player heads** - Faces beside recognizable player messages use cached Vanilla skins.
- **Local death coordinates** - Disabled, appended, or separate local coordinate line for your own death message.
- **Map preview tooltips** - A 128-pixel map preview, center coordinates and dimension id from map data already available to this client.
- **Held item information** - Durability and up to eight enchantment lines accompany the Vanilla held-item name.
- **Damage indicators** - Up to 32 short-lived numbers for observed health decreases of nearby visible living entities.
- **Biome notifications** - Optional biome name on transition, with display duration, offset and re-entry cooldown.
- **Anvil formatting and uses** - Valid & or section-sign color/style codes are accepted only under server formatting policy.

## Dependencies

- **Required:** Fabric API, Cloth Config
- **Optional:** SimpleBuilding, Simple Models, Mod Menu
<!-- Cloth Config moves from required to optional once the optional-Cloth change lands; adjust then. -->
## Compatibility

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Java 25
- Fabric only for now.

## Part of the Simple family

**Sub-mod** of Simple Aesthetics. It is a required part of the super mod, and it loads without it as long as its own required mods above are installed.

## License

All Rights Reserved with source-available code. Contributions need the contributor license agreement (CLA). Earlier SimpleBuilding versions stay CC0.

## Links

- Wiki: <WIKI_URL>
- Issues: <ISSUES_URL>
- Source: <SOURCE_URL>

## Categories and tags (suggestion)

- Modrinth categories: decoration, utility
- Tags: client, hud, tooltips, particles
- Environment: client and server

