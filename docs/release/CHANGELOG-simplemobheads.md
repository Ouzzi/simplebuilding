# Simple Mobheads 0.9.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Mobheads** for Minecraft 26.3 on Fabric.

A head for every mob: dropped when a charged creeper's explosion kills it, worn for a small secret, placed, or played on a note block.

## Added

- **A Head for Every Mob** - 97 heads: every mob of 26.3 that has no Vanilla head, the Simple mods' mobs, and the heads that moved in from Simple Fun and SimpleBuilding, plus four secret heads.
- **Charged Creeper Drops** - Only a charged creeper's explosion drops a mob's head, like Vanilla's creeper, zombie and skeleton heads: one head per explosion.
- **Variants** - A head keeps its mob's variant: a Siamese cat drops a Siamese cat head, a red sheep a red sheep head.
- **Wearing, Placing, Note Blocks** - Worn on the head, a mob's head makes that mob see you only half as far (like Vanilla's zombie head).
- **Head Abilities** - 28 heads hide a small, harmless ability while worn, each with its own server switch.
- **Greeting** - Sneaking with a mob head on plays its mob's voice quietly with a few notes, at most every five seconds (server switch).
- **Player Heads** - A player killed by another player drops their own head with their skin (server switch, default on; moved in from Simple Fun).
- **Server Commands** - Permission level 2: /simplemobheads pvp headDrops, drops creeper|wither|warden|playerKill, drops playerKillChance|lootingBonus, ability <id>, greeting, enabled.
- **Moved In** - Simple Fun's four animal heads and SimpleBuilding's twelve mob heads are Simple Mobheads heads now; old worlds keep them.
- **Simple Mods' Mobs** - Heads of the Simple mods' mobs (Deceiver, Hollow Howler, Dread, Hollow Watcher, Shellker, Soulfire Blaze, Blighted and Abyssal Wither, Nibblemite, Dragonmite) are always registered; they drop only when that mod is there.
- **He Was Never Removed** - A secret head that no mob drops.
- **Technoblade Never Dies** - A tribute head no mob drops: the Crowned Pig Head.
- **Killer Bunny** - A very rare rabbit is a killer bunny; its head is the rabbit head's Killer Bunny variant.
- **Giants** - Two giant heads, one and a half blocks big: the Giant Head and the Giant Cow Head.
- **Helmets on Heads** - Right-click a placed head with a helmet and it sits on the head, drawn with the helmet's own armor model.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib, Cloth Config
- Optional integrations: simplebuilding, simplemobs, simpleend, simpleenddragonfight, jei, modmenu

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplemobheads config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
