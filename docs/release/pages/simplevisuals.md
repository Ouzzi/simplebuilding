# Simple Visuals - store page draft

Status: **draft**

## Summary (Modrinth, max 150 characters)

Client HUD, tooltip and anvil formatting plus Vanilla-style immersion particles, all bounded and optional.

## Release channel: Beta

- Version number: `1.0.0-beta.1`
- Modrinth "Version type": **Beta**; CurseForge "Release type": **Beta** (never Release for this first version)

## Description (Markdown)

# Simple Visuals

> **Beta.** This mod is released as a beta: features, balance and config options can still change between versions, and bugs are possible. Back up your world before you install or update it, and report problems on the issue tracker.

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
- **Damage indicators** - Up to 32 short-lived numbers for observed health decreases of nearby visible living entities, on by default.
- **Biome notifications** - Optional biome name on transition, with display duration, offset and re-entry cooldown.
- **Anvil formatting and uses** - Valid & or section-sign color/style codes are accepted only under server formatting policy.

## Good to know

- **Needed on the client too:** chiseled (tilted) item frames send an extra value that a game without Simple Visuals does not understand. The chisels come from Simple Chisels, which is needed on the client anyway, so this only means: install Simple Visuals there as well.

## Dependencies

- **Required:** Fabric API (no other mods)
- **Included in this download:** SimpleLib
- **Optional:** SimpleBuilding, Simple Models, Mod Menu, Cloth Config

## Compatibility

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Java 25
- Fabric only for now.

## Part of the Simple family

**Sub-mod** of Simple Aesthetics: included in that download, and it also runs on its own with the required mods above.

## License

All Rights Reserved with source-available code. Contributions need the contributor license agreement (CLA). Earlier SimpleBuilding versions stay CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simplevisuals
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Source: https://github.com/Ouzzi/simplebuilding

## Categories and tags (suggestion)

- Modrinth categories: decoration, utility
- Tags: client, hud, tooltips, particles
- Environment: client and server

