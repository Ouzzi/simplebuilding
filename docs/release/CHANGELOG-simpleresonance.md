# Simple Resonance 0.1.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Resonance** for Minecraft 26.3 on Fabric.

How experienced a player is and how dangerous the land is: a player value from playing time and survival, danger zones that grow softly with the distance from the spawn, the owner of a monster spawn and a newcomer protection. An API for other mods; allows Simple Mobs to give new monsters status effects instead of more health.

## Highlights (from the commit history)

- Commands start with their mod id, old names stay as aliases
- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- Simple Resonance sub-mod, danger zones for new monsters, endgame enchant levels (WP-22, /3/4)

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Optional integrations: simplebuilding, simplemobs

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleresonance config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
