# Simple Storage 0.9.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Storage** for Minecraft 26.3 on Fabric.

Crates for every wood (open food boxes for nine stacks, hoppers and comparators work), the Storage Crafting Table that keeps its grid, and tiered chests, trapped chests and tier shulker boxes with more slots and bigger stacks, moved from SimpleBuilding (ids migrate); bundles and quivers in three tiers with the storage enchantments Drawer, Deep Pockets and Funnel, bundles set down on a block, and the four backpacks (worn, placed, Deep Pockets and Funnel; ids migrate).

## Added

- **Crates** - Crates are open wooden boxes shaped like the composter that store up to 9 stacks of food in at most 9 different kinds; each kind is a layer, the newest on top, seen through the slats.
- **Storage Crafting Table** - The Storage Crafting Table is a crafting table that keeps its 3x3 grid: closing the screen leaves the items where they are, and they lie flat on the table's top.
- **Chest tiers** - Copper chest -> Reinforced Chest -> Netherite Chest -> Enderite Chest: more slots per tier (27/36/45/54, double chests twice that), and from netherite on bigger stacks per slot (x2, enderite x4).
- **Shulker box tiers** - Shulker box -> Reinforced -> Netherite -> Enderite Shulker Box: the slots and stack sizes of the chest tiers (36/45/54 slots, x2 and x4 stacks from netherite on) in a box that keeps everything when broken.
- **Rare structure finds** - In newly generated structures a loot chest is now and then a better chest with double loot, and end city shulkers are sometimes reinforced, netherite or enderite.
- **Bundles: Reinforced, Netherite, Enderite** - Simple Storage extends the vanilla bundle into a three-tier family: the Reinforced Bundle holds 1.5x as much (96 items), the Netherite Bundle twice that (192) and the Enderite Bundle three times as much (288).
- **Quivers** - Quivers are arrow bundles: they take only arrows and supply a bow automatically as soon as they sit in the off-hand, in the chest slot, in the hotbar or - with SimpleBuilding's Constructor's Touch - anywhere in the inventory.
- **Placed bundles** - Sneak + right-click on a block with a bundle or quiver to set it down as a 3D bundle - on the floor (falls like sand) or hung on a wall.
- **Backpacks** - Backpacks are storage you wear in the chest slot: four tiers from 9 to 50 slots, opened with their own key (default B) as your normal inventory plus the backpack's slots.
- **Storage enchantments: Deep Pockets, Drawer, Funnel** - Three enchantments for the bundles, quivers and backpacks: Deep Pockets gives them more room, Drawer much more room for one kind of item, Funnel picks items up off the ground.
- **Pipes** - A pipe is a hopper with one slot that looks like a cable.
- **Chest network (prototype)** - Copper cables connect chests, each with a chest interface on its side, to one controller chest.
- **Barrels** - Copper, Reinforced, Netherite and Enderite barrels: bigger than Vanilla's, upgraded in place, and the docking barrel of the Simple Crucibles.

## Changed

- Content that used to live in SimpleBuilding now lives in this mod. Old worlds convert automatically and every old id keeps an alias; back up your world before updating.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simpleend, trinkets_updated, curios, simpleores, simplecrucibles

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplestorage config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
