# Simple End 0.1.0-beta.1 - changelog (Minecraft 26.3, Fabric, beta)

## Release notes

**Beta release.** This first version is published as a beta (release channel Beta on Modrinth and CurseForge): features, balance and config options can still change, and bugs are possible. Back up your world and report problems on the issue tracker.

First release of **Simple End** for Minecraft 26.3 on Fabric.

End wood (Ashwood, Duskwood), End grass and the Ash Valley and Dawnlands biomes; bundles Simple End Dragon Fight.

## Added

- **Ashwood and Duskwood** - Two End woods with the full Vanilla wood set: log, wood, stripped log and wood, planks, stairs, slab, fence, fence gate, door, trapdoor, button, pressure plate, sign, hanging sign, shelf, leaves, boat and boat with chest.
- **Buds, gigantic End trees and stick trees** - Ash Bud and Dusk Bud grow on End stone or dirt.
- **Leaves, Drift Fruit and Spoiled Drift Fruit** - End leaves are no full block: see-through tendril fronds around a small crystal, tinted by the biome.
- **Void-safe wood** - Dropped End-wood items (logs, planks, everything of the sets, buds, End grass, Drift Fruit) are not lost in the void: below the world they float back up and hover just above the bottom.
- **End grass and End bushes** - Wiry End grass and twiggy End bushes, like Vanilla dry grass and dead bushes, standing on End stone and wherever those stand (sand, terracotta, dirt, grass, moss, mud, farmland).
- **Ash Valley and Dawnlands** - Two large End biomes on the outer islands (never on the dragon island): rare patches 28-44 chunks (450-700 blocks) across with a wavy rim, spanning several islands, replace the End highlands/midlands/barrens.
- **Nibblemite** - A small, passive Endermite kin that lives in Ash Valley and Dawnlands in groups of 3-5, colored after its biome (pale gray with lavender, taupe with rose).
- **End paintings** - 46 animated End pictures; the End Painting is placed like a Vanilla Painting, each picture plays one of three short animations about once a minute.
- **Simple End and its sub-mod** - SimpleBuilding bundles Simple End ("Enable Simple End"); Simple End bundles Simple End Dragon Fight ("Enable Simple End Dragon Fight" in config/simpleend.json).

## Changed

- End leaves break by hand in about 2 seconds (the hoe is faster); the cobweb-like brake is unchanged.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API, SimpleLib
- Optional integrations: simpleenddragonfight

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleend config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
