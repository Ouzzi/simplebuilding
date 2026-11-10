# Simple Earth 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Earth** for Minecraft 26.3 on Fabric.

The Overworld seen from orbit: far above the world it becomes a slightly rounded cube (27 display versions) whose faces show Vanilla map colours read from the world generator - no chunks generated. Flat sun, cube moon, stars; below everything stays Vanilla.

## Added

- **Orbit** - World 0 ~ 0 is the middle of the earth's top face (there is no activation height).
- **Terrain from the world generator** - The faces show Vanilla map colours (grass, leaves, sand, snow, water, terracotta ...) with map-style height shading.
- **27 versions** - Behind every edge lies new terrain: four faces north is 4 edge lengths away, never back at the start (tesseract).
- **Sky** - In orbit the sky is black with stars, the sun stays a flat square far away, the moon is a small rounded cube lit by the sun whose position follows Vanilla's moon phase, and a stepped atmosphere rim glows blue on the day side, indigo at night and orange at the light border.
- **Menu** - Key # (changeable in the controls; there is no button in the pause menu) opens a box at the right edge: the world stays visible, the movement keys keep working, the mouse works the menu.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleearth config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
