# Simple Challenges 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Challenges** for Minecraft 26.3 on Fabric.

Game modes for a world of your choice: unlock the world cell by cell (chunks, blocks or three chunks), paid with items, credits or levels. Without a chosen challenge the mod changes nothing.

## Added

- **Safety lock** - Without a chosen challenge Simple Challenges changes nothing: no state file is written, the collision and block hooks return at once and no tick work runs.
- **Unlock grid** - The Overworld is cut into square columns of 1, 2, 4, 8, 16 (a chunk), 32 or 48 blocks (three chunks) over the full height.
- **Border line** - Clients with the mod get the free cells from the server (everything at join, one cell per unlock) and see the edge of the free area as a line of red dust near them. /simplechallenges border switches the line on and off on your client.
- **Prices** - Every cell next to the free area has a price, worked out once from the world seed, the cell and what the world offers there, and then kept in the world state.
- **Credits from advancements** - With credits as currency, every advancement that shows a toast and is not a root or recipe advancement pays credits once for the whole team (or, with the server option perPlayerCredits, once for each player into a pot of their own): task 1, goal 2, challenge 5 (changeable in the preset).
- **Deadlock protection** - When a challenge starts and after every unlock the mod checks that at least one cell next to the free area can be paid with something the free area offers.
- **Presets** - A challenge is a preset file: data/<namespace>/simplechallenges/preset/<id>.json in a mod or data pack, or config/simplechallenges/presets/<id>.json of your own.
- **World creation** - The world type button of the Create New World screen offers Simple Challenges next to Superflat.
- **Skyblock family** - Three world modes start in the void and can be combined with the Unlock mode.
- **Spawn, edges and the crosshair line** - A challenge chosen for a brand-new world fixes the world spawn at block 0,0 without the Vanilla scatter, so the start always lies inside the start area (a world that was already played keeps its spawn).
- **Lucky Block and Random Drops** - In a Lucky challenge a block you break drops another block instead of itself.
- **Skyline Block** - The world is void and single blocks float in the air with one block of air between them, in a fixed order per dimension: the Overworld line starts with grass, dirt and wood and ends with Obsidian (the portal block), the Nether line holds all Nether blocks, the End line all End blocks.
- **Easter eggs of the Skyline** - In the linear and the grid Skyline (never the ring) a loot structure waits in -X, where no line block stands: in the End a small ship with an Elytra in a frame, in the Nether a mini bastion whose chest always holds a Netherite upgrade smithing template, in the Overworld a vault that gives a Heavy Core and a frame with a trial key.
- **The 16 presets** - Shipped challenges: Vanilla Unlock (a chunk per cell), Block by Block (a single block per cell), Quick Steps (three chunks per cell), Trophy Land (advancements give credits), Island Unlock (Skyblock with unlockable cells), Classic Skyblock, Sky One Block, Sky Grid, Lucky Strike (broken blocks drop other blocks), Lucky Land (Lucky with Unlock), Lucky One Block, Random Drops (every block always drops the same other block), Skyline Block (one line), Skyline Grid, Skyline Ring and One Line (a one chunk wide corridor to unlock along).

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplechallenges config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
