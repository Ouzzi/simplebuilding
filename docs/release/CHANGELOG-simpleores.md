# Simple Ores 0.1.0 - changelog draft (Minecraft 26.3, Fabric)

_Auto-drafted from the git history: the highlight list is raw and needs hand editing before upload. Nothing here is published._

## Release notes

First release of **Simple Ores** for Minecraft 26.3 on Fabric.

Silver ore in the Overworld and the End, lead (blocks that muffle sound and stop the Sculk), the silver mirror, and rose gold: two alloys of gold, copper and silver (hard Rose Copper, soft Rose Gold), pressed into pressings, melted in the blast furnace only, with tools and armor of their own. Works on its own with Vanilla.

## Highlights (from the commit history)

- Pressling blocks, netherite/enderite scrap blocks, sage orb block, rose carrots and nuggets
- Blossom Gold -> Rose Gold (IdMoves), Rose Copper display name, new pressing recipes, tool/armor values (copper/iron durability, faster)
- Silver sheet (1 px plate, 12 per 3 ingots), mirror = glass pane + sheet, lead arrow tip (gravity x1.5, +0.5 damage)
- One clean creative tab per mod, family order
- Simple Modpack guide in SimpleLib - top bookmarks per super-mod, chapters per mod as data
- Simple Arrows sub-mod - Silver Arrow moved from Simple Ores, Light Arrow; Fletching Table potion slot (), engraving A
- Rose/blossom apples without fire resistance (final values), effect tooltip; apple texts complete; netherite/enderite apple textures built from the jar
- Lead plate, lead octets with full soundproofing weight

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simplehammers

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleores config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
