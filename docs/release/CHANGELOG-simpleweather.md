# Simple Weather 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Weather** for Minecraft 26.3 on Fabric.

Weather switch (server) and rain density (client); a sub-mod of Simple QoL, also playable alone.

## Added

- **Disable weather** - disableWeather (default false, server) clears rain and thunder and skips weather progression.
- **Rain and thunder frequency** - rainFrequencyFactor and thunderFrequencyFactor (default 1 = Vanilla, bounded 0.25–4, server) speed up or slow down the countdown between two rains or storms: 2 = the dry spell is half as long, 0.25 = four times as long.
- **Rain density** - clientRainParticleDensity is a local visual preference, default 20, bounded 0–100: thins rain in the air, ground particles and rain sounds; it never changes server weather.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: none (Cloth Config optional: config screen only)
- Optional integrations: simplequalityoflife, modmenu

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleweather config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
