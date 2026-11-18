# Simple Chess 0.1.0-beta.1 - changelog (Minecraft 26.3, Fabric, beta)

## Release notes

**Beta release.** This first version is published as a beta (release channel Beta on Modrinth and CurseForge): features, balance and config options can still change, and bugs are possible. Back up your world and report problems on the issue tracker.

First release of **Simple Chess** for Minecraft 26.3 on Fabric.

Chess moves out of SimpleBuilding into its own optional mod: 156 pieces (pawn, rook, knight, bishop, queen and king in 13 colors, each as a 3D figure and as a flat token) that stand up to four on a block, and 12 quartz checker boards with stairs and slabs. Works on its own with Vanilla and SimpleLib.

## Added

- **Chess pieces** - Right-click places a piece on the clicked quarter of a block with a full top, facing where you look; an empty hand turns it, sneak + empty hand picks it up, sneak + right-click with a piece swaps it. A 4 x 4 area of checkers is a whole chessboard.
- **Quartz checkers** - Twelve quartz checker pillar blocks (purpur, lapis, blackstone, resin, nether bricks, red nether bricks and the six End palette checkers) with stairs and slabs; no mob spawns on them.
- **Checker walls** - A wall of every quartz checker.
- **Chess octets (with Simple Octets)** - Every color gets an octet that mixes with Simple Octets' material octets in one cell; without Simple Octets the stonecutter cuts the pieces straight from the checkers.

## Changed

- Chess used to live in SimpleBuilding. Old worlds convert automatically: the old ids of the pieces, checkers, octets and their recipes turn into the new ones when a world loads. Back up your world before updating.
- The Astralit, Nihilit and Ender Quartz checkers and their pieces need SimpleBuilding's End palettes and only exist while SimpleBuilding is installed.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simpleoctets

## Notes

- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
