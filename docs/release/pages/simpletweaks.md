# Simple Tweaks - store page draft

Status: **draft**

## Summary (Modrinth, max 150 characters)

Server claims, admin tools (block log, backups, anti-X-ray) and player tweaks, on by default; older worlds keep loading.

## Release channel: Beta

- Version number: `0.9.0-beta.1`
- Modrinth "Version type": **Beta**; CurseForge "Release type": **Beta** (never Release for this first version)

## Description (Markdown)

# Simple Tweaks

> **Beta.** This mod is released as a beta: features, balance and config options can still change between versions, and bugs are possible. Back up your world before you install or update it, and report problems on the issue tracker.

Simple Tweaks keeps older worlds compatible and gives servers three groups of tools, each switchable and almost all on by default: claims that protect builds, admin tools (block and container log, inventory backups, world backups, vanish, freeze, restart announcements, lag finder, anti-X-ray) and player tweaks (death hint, partial keep inventory, newcomer protection, AFK handling, votes, server events, and welcome messages that are off by default).

## Features

- **Legacy World Compatibility** - An exact whitelist resolves old item, block, block entity, and component names to existing SimpleBuilding registries.
- **Claims (On by Default)**
- **Claim Deed** - Craft paper, an ink sac and honeycomb into an empty Claim Deed.
- **Old Claim Table** - The claim table is gone: claims are seen on the wayfinder map (Simple Maps) and in the deed.
- **Claims on the map**
- **Experimental Forge 26.3** - Opt-in Forge compatibility adapter, with SimpleBuilding required.
- **Block and container log (on by default)** - Logs blocks players break or place, items taken from or put into containers, sign edits, explosions and fire (hoppers optional), with actor, time, position and dimension.
- **Inventory backups on death (on by default)** - Before a player drops anything on death, their inventory, armor, offhand, XP, time, cause and position are saved; the last keep deaths (default 5, 1–50) per player stay.
- **/simpletweaks invsee and /simpletweaks endersee (on by default)** - Own admin screen in the SimpleLib box style: the target's armor, offhand, inventory and hotbar (or the ender chest) above your own inventory.
- **Vanish (on by default)**
- **Freeze (on by default)** - /simpletweaks freeze <player> holds a player in place: every move is set back, and breaking, using, placing, attacking and the drop key are refused.
- **Restart announcements (on by default)**

## Vanilla use

Keeps older worlds loading and adds server claims that are on by default; the claim rules change who may build where on a server. Also on by default: partial keep inventory (hotbar and armor stay on death), a death hint, AFK players not counting for sleeping, newcomer protection and a dragon egg after every dragon fight; each can be switched off.

## Dependencies

- **Required:** Fabric API (no other mods)
- **Included in this download:** SimpleLib
- **Optional:** Simple Dimensions, SimpleBuilding, Simple Money, Simple Weather, Simple Maps

## Compatibility

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Java 25
- Fabric only for now.

## Part of the Simple family

**Standalone mod.** It is not part of a super mod; install it on its own with the required mods above.

## License

All Rights Reserved with source-available code. Contributions need the contributor license agreement (CLA). Earlier SimpleBuilding versions stay CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simpletweaks
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Source: https://github.com/Ouzzi/simplebuilding

## Categories and tags (suggestion)

- Modrinth categories: utility, management
- Tags: claims, compatibility, server, admin
- Environment: client and server

