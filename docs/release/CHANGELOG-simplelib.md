# SimpleLib 0.4.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **SimpleLib** for Minecraft 26.3 on Fabric.

Shared library of the Simple mods: common materials and upgrade templates, id migrations, config commands, UI building blocks, heat sources and warm food. Bundled by the Simple mods that need it.

## Added

- **Simple Modpack guide** - One guide for every Simple mod, there with any of them installed: craft it from a book and a crafting table (shapeless), or get it from the first FTB Quests quest.
- **Warm food** - Cooked food comes out of a crucible warm and is eaten 15% faster while warm.
- **Shared materials** - Base materials several Simple mods use: diamond pebble, cracked diamond and its block, netherite nugget, the basic upgrade template, polished end stone (shared building block of the family) and the six building cores.
- **Cursed and Mythic rarity** - Two tiers above Epic for every Simple mod: Cursed (radiant red name with a black shadow; in the tooltip the letters burn, orange and gold flames rise into them and run through the name) and Mythic (an opal of aquamarine, azure and lavender moving through the letters with a pearl gloss, a white sheen sweeping across in the tooltip, enchantment glint).
- **Legendary moments** - The hardest advancements and the hardest enchantments play a deep choir and a particle show at the player.
- **Hang watchdog** - When the server tick stands still, Simple Lib writes a list of all threads to the logs folder, so a frozen game can be diagnosed.
- **Free Look** - While Free Look is on, the mouse turns only the camera, not the player; walking and all other input stay normal.

## Changed

- Shared materials, upgrade templates and in-world upgrades moved here from SimpleBuilding; old worlds load them under their new ids.
- Enderite now belongs to Simple Ores.
- The mod license changed from CC0 to All Rights Reserved with source-available code and a contributor license agreement (CLA). Earlier versions stay CC0.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Optional integrations: jei

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplelib config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
