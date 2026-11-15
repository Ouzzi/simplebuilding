# Simple Opt Server 1.0.0-beta.1 - changelog (Minecraft 26.3, Fabric, beta)

## Release notes

**Beta release.** This first version is published as a beta (release channel Beta on Modrinth and CurseForge): features, balance and config options can still change, and bugs are possible. Back up your world and report problems on the issue tracker. No performance figures are promised: the effect has not been measured across computers and mod packs yet.

First release of **Simple Opt Server** for Minecraft 26.3 on Fabric.

Server optimizations of Simple Optimization: view distance follows server load (MSPT) within the server.properties bounds; optional activation range for far mobs (off by default); steps back when ServerCore is installed.

## Added

- **View distance by server load** - On a dedicated server with players online, every stepSeconds (default 15) the average tick time is checked: above targetMspt (default 45 ms) the view distance drops by one down to minView (default 6), below 80 % of it the distance climbs back to the server.properties value.
- **Activation range (off by default)** - Changes gameplay, so it starts with mode off in config/simpleoptserver.json; set mode auto to use it (restart needed).

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleoptserver config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
