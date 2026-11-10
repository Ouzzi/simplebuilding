# Simple Interfaces 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Interfaces** for Minecraft 26.3 on Fabric.

Restyle of the Vanilla container screens (a coloured box per block, drawn in code), linked containers and Easy Shulkers / Ender Chests; a sub-mod of Simple QoL, also playable alone.

## Added

- **Style comparison switch** - In development, or when showStyleToggle is enabled, the button and unbound key toggle all SimpleLib-based screens between the Simple style and Vanilla.
- **Simple container style** - Chest, barrel, ender chest, shulker box, hopper, dispenser/dropper and crafter screens and the mount inventories (horse, donkey, mule, llama, camel, nautilus) get a box in the block's color around the container slots and a light box around your inventory, with sunk rounded slots.
- **Simple style for workstations** - Crafting table, furnace, blast furnace, smoker, brewing stand, beacon and enchanting table screens get the Simple style in the block's color.
- **Simple style for anvil, loom & co.** - Anvil, grindstone, stonecutter, loom, cartography table, smithing table, villager trading and your own inventory get the Simple style: a box in the block's color, the big result slot, engraved symbols (plus, arrow, grinding wheel, XP) instead of Vanilla's pictures, raised tiles for recipes, patterns and offers.
- **Linked containers** - Sneak + right-click a container with an empty main hand to mark it (a double chest counts as one); dust particles at your hand show the mark.
- **Easy shulker boxes and ender chests** - Right-click a shulker box (any color, also SimpleBuilding tier boxes) or an ender chest in the air, or right-click it on its own inventory slot, to open it without placing it.
- **Shulker box and ender chest tooltips** - Hold Shift over a shulker box to see its contents as a slot grid; the ender chest item shows your own ender chest.
- **Bundles show everything** - Bundle tooltips show every entry instead of Vanilla's twelve, and the mouse wheel selects any of them.
- **Spyglass and zoom** - The spyglass darkens only the screen edge, hides the HUD (crosshair stays) and zooms steplessly with the mouse wheel.
- **Clearer slot highlight** - The highlight of the hovered slot is drawn only behind the item, so the item stays fully visible.
- **Key binding page** - A page for all key bindings of Minecraft and every mod by mod, by function or as a 60/75/100 % keyboard, with search and conflict marks.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: Cloth Config, SimpleLib
- Optional integrations: simplebuilding, modmenu

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleinterfaces config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
