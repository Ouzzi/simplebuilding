# Simple Opt Render - store page draft

Status: **draft**

## Summary (Modrinth, max 150 characters)

Client optimizations: leaf culling, light-map cache, fast IP ping and telemetry off, stepping back for other mods.

## Description (Markdown)

# Simple Opt Render

Simple Opt Render adds client optimizations. Each area steps back automatically when a known optimization mod already covers it.

## Features

- **Steps back for known mods** - Each area has a mode in config/simpleoptrender.json: auto (default), ours, theirs or off.
- **Leaf culling** - Leaves no longer draw the faces they share with other leaves, so forests build smaller chunk meshes while the outside keeps the fancy cutout look.
- **Light map cache**
- **Fast IP ping** - A server address typed as a plain IP (IPv4, or IPv6 in brackets) is used as it is: no reverse DNS lookup and no SRV query, so the server list fills faster.
- **Telemetry off** - The client never starts a telemetry session: every telemetry event goes to the Vanilla disabled sender, nothing is sent.
- **More culling**
- **Detail switches**
- **Commands** - /simpleclient simpleoptrender shows and changes every option of config/simpleoptrender.json (list [group], get, set, reset, help; Tab completes options and values).

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
- Tags: performance, client, rendering
- Environment: client and server

