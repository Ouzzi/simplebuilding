# Simple Resonance 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Resonance** for Minecraft 26.3 on Fabric.

How experienced a player is and how dangerous the land is: a player value from playing time and survival, danger zones that grow softly with the distance from the spawn, the owner of a monster spawn and a newcomer protection. An API for other mods; allows Simple Mobs to give new monsters status effects instead of more health.

## Added

- **Player value** - A number from 0 to 1 for how experienced a player is: W = 0.7 x long-term value + 0.3 x the share since the last death.
- **Danger zones** - The land gets more dangerous the farther it is from the world spawn: level 0.5 up to 512 blocks, rising softly to 5 at 6000 blocks (smoothstep, no hard edges).
- **Spawn owner and spawn level** - Every monster spawn attempt belongs to exactly one player nearby, chosen with weight 1/distance (no average).
- **Newcomer protection** - A spawn is at most one level above the level of its owner, and in the first 6 played hours monsters stay at level 1.
- **Interface for other mods** - Other mods ask Simple Resonance through the framework contract ResonanceContracts (pure Java, no class import): player value, level, zone level, spawn owner, spawn level, award points, newcomer flag.
- **Takeover from SimpleBuilding** - Nothing is lost: on a player's first tick Simple Resonance reads the old tracker block SimpleBuildingData (baselines since the last death, active time, kill totals) and starts the long-term points at 1 per 30 old active minutes.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Optional integrations: simplebuilding, simplemobs

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleresonance config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
