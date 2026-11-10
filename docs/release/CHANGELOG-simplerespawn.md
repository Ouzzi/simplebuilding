# Simple Respawn 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Respawn** for Minecraft 26.3 on Fabric.

Downed instead of dead: a teammate can revive you before you bleed out (then 3 hearts, no food).

## Added

- **Downed instead of dead** - Lethal damage knocks a player down instead of killing them, as long as another player is online who could help (config onlyWithOtherPlayers; alone it is the Vanilla death).
- **Downed screen** - An own screen in the style of the Vanilla death screen shows the time left and three buttons: Spectate, Inventory and Give up.
- **Reviving** - A teammate holds right-click on the downed player for 5 seconds (reviveSeconds); afterwards the player stands up with 3 hearts and an empty food bar.
- **Fair play** - Leaving the game while downed is death; the real death is always the Vanilla one, so drops and other mods behave as usual.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplerespawn config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
