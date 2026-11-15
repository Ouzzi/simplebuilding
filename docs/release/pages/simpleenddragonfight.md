# Simple End Dragon Fight - store page draft

Status: **draft**

## Summary (Modrinth, max 150 characters)

Tougher Ender Dragon fight: more health bars per player, a faster dragon, entry animations and Dragonmites.

## Release channel: Beta

- Version number: `0.1.0-beta.1`
- Modrinth "Version type": **Beta**; CurseForge "Release type": **Beta** (never Release for this first version)

## Description (Markdown)

# Simple End Dragon Fight

> **Beta.** This mod is released as a beta: features, balance and config options can still change between versions, and bugs are possible. Back up your world before you install or update it, and report problems on the issue tracker.

Simple End Dragon Fight adds one health bar per player on the island, a faster dragon, entry animations and Dragonmites from half health.

## Features

- **One health bar per player** - Every player on the main End island adds one full dragon health bar: 200 health per player as in Vanilla (200, 400, 600 ...), or 300 per player with "raiseBaseHealth" on.
- **Faster dragon** - Alone the dragon flies at Vanilla speed; every further player adds 1.5 %, at most 35 % (setting speedMultiplier raises the base, e.g.
- **Harder with every summoning** - Optional (off by default so that farming dragon breath is not punished): every further dragon summoned with End crystals is harder for good, e.g.
- **Entry animation** - Every player who enters a running fight gets a 3-second greeting: portal ring, the dragon rises and scents the newcomer, a growl with a shock ring, then sparks rain down.
- **Dragonmites** - From half health on, the dragon calls Dragonmites near the exit portal: a first wave at once, then one every 30 seconds.
- **Config** - Server file config/simpleenddragonfight.json; with SimpleBuilding the module is also switched by "Enable Simple End Dragon Fight".
- **Commands** - /simpleenddragonfight config shows and changes every option of config/simpleenddragonfight.json (list [group], get, set, reset, help; Tab completes options and values).

## Vanilla use

Makes the Vanilla dragon fight tougher and fairer for groups (a health bar per player, a faster dragon, extra minions); it changes the fight, not the End itself.

## Dependencies

- **Required:** Fabric API, SimpleLib
- **Optional:** Simple Mobs

## Compatibility

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Java 25
- Fabric only for now.

## Part of the Simple family

**Standalone mod.** It works on its own; other Simple mods add optional extras when installed together.

## License

All Rights Reserved with source-available code. Contributions need the contributor license agreement (CLA). Earlier SimpleBuilding versions stay CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simpleenddragonfight
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Source: https://github.com/Ouzzi/simplebuilding

## Categories and tags (suggestion)

- Modrinth categories: adventure, mobs
- Tags: ender-dragon, boss, end
- Environment: client and server

