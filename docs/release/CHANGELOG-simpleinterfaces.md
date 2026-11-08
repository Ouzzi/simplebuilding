# Simple Interfaces 0.1.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Interfaces** for Minecraft 26.3 on Fabric.

Restyle of the Vanilla container screens (a coloured box per block, drawn in code), linked containers and Easy Shulkers / Ender Chests; a sub-mod of Simple QoL, also playable alone.

## Highlights (from the commit history)

- Shellker/Deceiver eggs back to pre-, Keys screens back to transparent style, Free Look wheel zoom
- Variant A box for key pages and Shapecutter
- Keyboard layouts US/DE/UK/FR, own layout editor (b-keys34)
- Free Look hand stays with body + grey real crosshair, default keys, Simple Keybinds fold all/sub-groups
- Free look keys/rules/reset (), hopper padding (), Vanilla-widget key page with function view
- Key binding page (list + keyboard 60/75/100, conflicts), blueprint and picture map on UiBoxes (WP-25/)
- Dev variants panel and registry, scope frame A/B as first variant
- Spyglass overlay as inverted UI window frame, flat/headless mob heads (horse family, warden feelers, camel/llama, ghast, mites, cod, creaking, elder guardian, glow squid)

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: Cloth Config, SimpleLib
- Optional integrations: simplebuilding, modmenu

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleinterfaces config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
