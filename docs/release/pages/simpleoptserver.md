# Simple Opt Server - store page draft

Status: **draft**

## Summary (Modrinth, max 150 characters)

Server optimization: view distance follows server load within server.properties bounds.

## Description (Markdown)

# Simple Opt Server

Simple Opt Server adapts view distance to the server's tick time (MSPT) within the bounds of server.properties, and steps back for ServerCore.

## Features

- **View distance by server load**
- **Activation range (off by default)** - Changes gameplay, so it starts with mode off in config/simpleoptserver.json; set mode auto to use it (restart needed).
- **Commands** - /simpleoptserver config shows and changes every option of config/simpleoptserver.json (list [group], get, set, reset, help; Tab completes options and values).

## Dependencies

- **Required:** Fabric API (no other mods)
- **Optional:** none

## Compatibility

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Java 25
- Fabric only for now.

## Part of the Simple family

**Sub-mod** of Simple Optimization. It is a required part of the super mod, and it loads without it as long as its own required mods above are installed.

## License

All Rights Reserved with source-available code. Contributions need the contributor license agreement (CLA). Earlier SimpleBuilding versions stay CC0.

## Links

- Wiki: <WIKI_URL>
- Issues: <ISSUES_URL>
- Source: <SOURCE_URL>

## Categories and tags (suggestion)

- Modrinth categories: optimization
- Tags: performance, server, view-distance
- Environment: client and server

