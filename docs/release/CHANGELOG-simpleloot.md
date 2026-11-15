# Simple Loot 1.0.0-beta.1 - changelog (Minecraft 26.3, Fabric, beta)

## Release notes

**Beta release.** This first version is published as a beta (release channel Beta on Modrinth and CurseForge): features, balance and config options can still change, and bugs are possible. Back up your world and report problems on the issue tracker.

First release of **Simple Loot** for Minecraft 26.3 on Fabric.

Vault cooldown in days instead of once per player; a sub-mod of Simple QoL, also playable alone.

## Added

- **Vault cooldown** - A player may loot a vault again after vaultCooldownDays, default 100 Minecraft days, bounded 1–36500.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: none (Cloth Config optional: config screen only)
- Optional integrations: simplequalityoflife, modmenu

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleloot config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
