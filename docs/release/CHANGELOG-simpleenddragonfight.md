# Simple End Dragon Fight 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple End Dragon Fight** for Minecraft 26.3 on Fabric.

Ender Dragon fight: +1 health bar per player, a faster dragon, entry animations and Dragonmites.

## Added

- **One health bar per player** - Every player on the main End island adds one full dragon health bar: 300 health per player (300, 600, 900 ...), or the Vanilla 200 per player with "raiseBaseHealth" off.
- **Faster dragon** - The dragon flies and turns 20 % faster, plus 1.5 % per further player, at most 35 %.
- **Harder with every summoning** - Every further dragon summoned with End crystals is harder for good: +15 % health, damage and speed per summoning, at most x3.
- **Entry animation** - Every player who enters a running fight gets a 3-second greeting: portal ring, the dragon rises and scents the newcomer, a growl with a shock ring, then sparks rain down.
- **Dragonmites** - From half health on, the dragon calls Dragonmites near the exit portal: a first wave at once, then one every 30 seconds.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Optional integrations: simplemobs

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleenddragonfight config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
