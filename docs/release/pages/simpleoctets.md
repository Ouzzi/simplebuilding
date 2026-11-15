# Simple Octets - store page draft

Status: **draft**

## Summary (Modrinth, max 150 characters)

Octets: half-block cubes of every wood and block with stairs and slabs that mix in a single cell.

## Release channel: Beta

- Version number: `0.9.0-beta.1`
- Modrinth "Version type": **Beta**; CurseForge "Release type": **Beta** (never Release for this first version)

## Description (Markdown)

# Simple Octets

> **Beta.** This mod is released as a beta: features, balance and config options can still change between versions, and bugs are possible. Back up your world before you install or update it, and report problems on the issue tracker.

Simple Octets adds 0.5 x 0.5 x 0.5 cubes you cut from a full block in the Shapecutter of Simple Blocks (in the stonecutter without it) and craft back into the block. Different octets mix in one cell; a sledgehammer breaks one corner off a full block.

## Features

- **Octets: half-block cubes of every material** - An octet is a cube of 0.5 x 0.5 x 0.5 blocks.
- **Shapecutter and back** - The Shapecutter of Simple Blocks cuts 8 octets from a full block, and 8 octets of one material craft back into it.
- **Mixed cells** - Octets of different materials share one cell.
- **Melon octets** - Sneak + right-click a block with a melon slice to place it as a melon octet (without sneaking it is eaten).
- **Breaking a corner off a block** - Sneaking with a sledgehammer, you break only the looked-at corner off a full block that has octets: a cell of 7 octets stays and one octet drops.
- **Octets of other mods' blocks** - Octets of another mod's blocks exist only while that mod is loaded, with their recipes; without it neither the cells nor the recipes are there.
- **Worlds from SimpleBuilding** - Octets from worlds of older SimpleBuilding versions keep working.

## Vanilla use

Adds eighth-size blocks cut from their full block so builders get finer detail; the Vanilla blocks stay as they are. Two changes to Vanilla inputs - sneak-placing a melon slice and corner breaking with a sledgehammer - can each be switched off in the server config.

## Dependencies

- **Required:** Fabric API, SimpleLib
- **Optional:** SimpleBuilding, Simple End, Simple Ores, Simple Enchanting, JEI

## Compatibility

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Java 25
- Fabric only for now.

## Part of the Simple family

**Sub-mod** of SimpleBuilding. It is a required part of the super mod, and it loads without it as long as its own required mods above are installed.

## License

All Rights Reserved with source-available code. Contributions need the contributor license agreement (CLA). Earlier SimpleBuilding versions stay CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simpleoctets
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Source: https://github.com/Ouzzi/simplebuilding

## Categories and tags (suggestion)

- Modrinth categories: decoration
- Tags: building-blocks, octets, micro-blocks
- Environment: client and server

