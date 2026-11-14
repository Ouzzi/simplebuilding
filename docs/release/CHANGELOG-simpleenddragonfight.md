# Simple End Dragon Fight 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple End Dragon Fight** for Minecraft 26.3 on Fabric.

Ender Dragon fight: +1 health bar per player, a faster dragon, entry animations and Dragonmites.

## Added

- **One health bar per player** - Every player on the main End island adds one full dragon health bar: 200 health per player as in Vanilla (200, 400, 600 ...), or 300 per player with "raiseBaseHealth" on. Alone you fight a Vanilla dragon.
- **Faster dragon** - Alone the dragon flies at Vanilla speed; every further player adds 1.5 %, at most 35 % (setting speedMultiplier raises the base).
- **Harder with every summoning** - Optional, off by default: every further dragon summoned with End crystals is harder for good (summonScaling, e.g. 0.15 = +15 % per summoning, at most x3).
- **Dragon Chitin repairs Elytra** - at the anvil, like Phantom Membrane.
- **Reward without Simple Mobs** - from the 4th/10th dragon the Ender Heart/Gem (Simple Mobs); without it the fight pays experience instead (rewardXpFallback).
- **Entry animation** - Every player who enters a running fight gets a 3-second greeting: portal ring, the dragon rises and scents the newcomer, a growl with a shock ring, then sparks rain down.
- **Dragonmites** - From half health on, the dragon calls Dragonmites near the exit portal: a first wave at once, then one every 30 seconds.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API, SimpleLib
- Optional integrations: simplemobs

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleenddragonfight config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
