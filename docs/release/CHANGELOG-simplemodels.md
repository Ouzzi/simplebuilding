# Simple Models 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Models** for Minecraft 26.3 on Fabric.

Server-approved custom item models, anvil assignment, and a searchable model browser.

## Added

- **Anvil Assignment** - Put an approved base item in the left anvil slot, leave the second slot empty, choose Models, then Assign and take the result.
- **Model Browser** - The Models bookmark on the inventory, the creative inventory and the anvil opens the browser; while the recipe book is open it sits on the top edge.
- **Folder Catalog and Resource Packs** - Add or remove JSON definitions in config/simplemodels/catalog on the server, then run /simplemodels reload.
- **Example Models** - Six example models come built in and need no resource pack: Stool (oak slab), Table Lantern (lantern), Mailbox (barrel), Birdhouse (oak fence), Weather Vane (lightning rod) and Crystal Cluster (amethyst cluster).
- **Model Editor (preview)** - The Editor button in the model browser opens the selected model (otherwise the stool example) in a Blockbench-like editor: cube list with show/hide, a 3D view (right drag turns, Shift or middle button pans, wheel zooms, number pad 1/3/7 front/right/top), and a gizmo to move (G), rotate (R) or scale (S, Ctrl both sides) the selected cube on a grid preset (1 px · 22.5°, 0.5 · 15°, 0.25 · 5° or free; Alt: free, Shift: 1°).

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Optional integrations: simplebuilding, modmenu

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplemodels config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
