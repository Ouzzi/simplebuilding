# Simple Machines - store page draft

Status: **draft**

## Summary (Modrinth, max 150 characters)

Double Piston, Signal Dampener, upgraded pistons, faster filter hoppers and carts, a Smither and an Auto Crafter.

## Release channel: Beta

- Version number: `0.1.0-beta.1`
- Modrinth "Version type": **Beta**; CurseForge "Release type": **Beta** (never Release for this first version)

## Description (Markdown)

# Simple Machines

> **Beta.** This mod is released as a beta: features, balance and config options can still change between versions, and bugs are possible. Back up your world before you install or update it, and report problems on the issue tracker.

Simple Machines adds the Double Piston, the Signal Dampener, Reinforced and Reinforced Sticky Pistons, Netherite and Enderite Pistons that break blocks, Reinforced, Netherite and Enderite hoppers (2x/4x/8x faster, with an item filter) and hopper minecarts, the Smither (smithing by redstone) and the Auto Crafter (crafts by itself into the hopper below).

## Features

- **Reinforced, Netherite and Enderite Hoppers**
- **Hopper Minecarts** - A hopper minecart per hopper tier: it takes in 2, 4 or 8 items per tick (the speed of its hopper; Vanilla's cart 1) and filters like the hopper block, with the same menu.
- **Piston upgrades**
- **Double Piston**
- **Signal Dampener** - A flat redstone part like the Repeater that takes 0 to 3 off the signal and passes it on one redstone tick later.
- **Smither (Auto Smither)** - The Smither runs the smithing table like the Crafter: one smithing per redstone pulse from its template, base and addition.
- **Auto Crafter (Autonomous Crafter)** - The Auto Crafter is a Crafter that crafts its grid by itself every 4 ticks as long as a hopper stands below it.
- **Creative tabs** - The module's items sit in its own creative tab only (no Vanilla tabs).
- **Commands** - /simplemachines config [list | get | set | reset | help] (operators).

## Vanilla use

Gives redstone players stronger pistons, faster hoppers, a double piston, a signal dampener, a Smither and an Auto Crafter that needs no redstone clock (the Vanilla Crafter makes one item per pulse); Vanilla pistons, hoppers and the Crafter stay as they are. The Reinforced Piston moves one unbreakable block only when paid with a Redstone Block, the breaker pistons wear out and ask claim mods first; the upgrades extend the piston logic by mixin.

## Dependencies

- **Required:** Fabric API, SimpleLib
- **Optional:** Jade, SimpleBuilding, Simple Ores, JEI

## Compatibility

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Java 25
- Fabric only for now.

## Part of the Simple family

**Sub-mod** of SimpleBuilding. It is a required part of the super mod, and it loads without it as long as its own required mods above are installed.

## License

All Rights Reserved with source-available code. Contributions need the contributor license agreement (CLA). Earlier SimpleBuilding versions stay CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simplemachines
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Source: https://github.com/Ouzzi/simplebuilding

## Categories and tags (suggestion)

- Modrinth categories: technology, utility
- Tags: redstone, pistons, hoppers, machines
- Environment: client and server

