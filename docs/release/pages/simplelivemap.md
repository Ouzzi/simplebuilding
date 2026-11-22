# Simple Livemap - store page draft

Status: **draft**

## Summary (Modrinth, max 150 characters)

Live web map of your world as players explored it, with a built-in web server that works out of the box, only on your own computer.

## Release channel: Beta

- Version number: `1.0.0-beta.1`
- Modrinth "Version type": **Beta**; CurseForge "Release type": **Beta** (never Release for this first version)

## Description (Markdown)

# Simple Livemap

> **Beta.** This mod is released as a beta: features, balance and config options can still change between versions, and bugs are possible. Back up your world before you install or update it, and report problems on the issue tracker.

Simple Livemap draws a web map from the chunks players visited and the maps they held, with relief and optional player positions. The built-in web server works out of the box on your own computer (private, with a key), and a guide shows how to put the map on your own website.

## Features

- **Explored chunks**
- **Map tiles**
- **Web map** - A built-in web server shows the map in the browser (drag, zoom, dimensions).
- **Private or public**
- **Open Livemap in chat** - In single player and on a LAN world the host gets one chat line per session with a clickable [Open Livemap].
- **Own website** - You can show the map on your own website: a reverse proxy (nginx or Caddy) in front of the built-in server, or a static copy of the tiles uploaded to any web space.
- **One map per installation** - Self-hosting serves exactly one live map per installation and server: one web server, one world.
- **Hosted map (livemap.simplemods.de)** - Without an own website: make a one-time code on livemap.simplemods.de (valid 15 minutes) and type /simplelivemap connect <code> as game master or single-player owner.
- **Map viewer**

## Dependencies

- **Required:** Fabric API (no other mods)
- **Included in this download:** SimpleLib
- **Optional:** Simple Maps

## Compatibility

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Java 25
- Fabric only for now.

## Part of the Simple family

**Standalone mod.** It is not part of a super mod; install it on its own with the required mods above.

## License

All Rights Reserved with source-available code. Contributions need the contributor license agreement (CLA). Earlier SimpleBuilding versions stay CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simplelivemap
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Source: https://github.com/Ouzzi/simplebuilding

## Categories and tags (suggestion)

- Modrinth categories: utility, management
- Tags: map, web, server, exploration
- Environment: client and server

