# Simple Sounds 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Sounds** for Minecraft 26.3 on Fabric.

Bounded local Vanilla sound counterparts for the twelve Simple Visuals immersion effects.

## Added

- **Footstep dust** - Local Vanilla sound minecraft:block.powder_snow.step; at least 20 ticks apart and never sooner than the effect cooldown, base volume 0.05, pitch 1.35–1.45.
- **Cold breath** - Local Vanilla sound minecraft:entity.player.breath; at least 160 ticks apart and never sooner than the effect cooldown, base volume 0.06, pitch 0.85–0.95.
- **Fireflies** - Local Vanilla sound minecraft:block.firefly_bush.idle; at least 100 ticks apart and never sooner than the effect cooldown, base volume 0.10, pitch 0.95–1.05.
- **Pollen** - Local Vanilla sound minecraft:block.flowering_azalea.step; at least 120 ticks apart and never sooner than the effect cooldown, base volume 0.05, pitch 1.35–1.45.
- **Fire sparks** - Local Vanilla sound minecraft:block.campfire.crackle; at least 60 ticks apart and never sooner than the effect cooldown, base volume 0.08, pitch 0.95–1.05.
- **Water ripples** - Local Vanilla sound minecraft:entity.generic.swim; at least 40 ticks apart and never sooner than the effect cooldown, base volume 0.05, pitch 1.25–1.35.
- **Water droplets** - Local Vanilla sound minecraft:block.pointed_dripstone.drip_water; at least 60 ticks apart and never sooner than the effect cooldown, base volume 0.08, pitch 1.05–1.15.
- **Leaf fall** - Local Vanilla sound minecraft:block.leaf_litter.step; at least 100 ticks apart and never sooner than the effect cooldown, base volume 0.06, pitch 1.15–1.25.
- **Enchanted items** - Local Vanilla sound minecraft:block.amethyst_block.chime; at least 200 ticks apart and never sooner than the effect cooldown, base volume 0.05, pitch 1.45–1.55.
- **Beacon aura** - Local Vanilla sound minecraft:block.beacon.ambient; at least 200 ticks apart and never sooner than the effect cooldown, base volume 0.06, pitch 0.95–1.05.
- **Damage feedback** - Local Vanilla sound minecraft:entity.player.hurt; at least 40 ticks apart and never sooner than the effect cooldown, base volume 0.10, pitch 0.75–0.85.
- **Healing feedback** - Local Vanilla sound minecraft:block.amethyst_block.resonate; at least 40 ticks apart and never sooner than the effect cooldown, base volume 0.12, pitch 1.55–1.65.
- **Local cosmetic safety** - No packets, server gameplay, entities, inventory changes or remote player scans.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: none (Cloth Config optional: config screen only)
- Optional integrations: simplevisuals, simplebuilding, modmenu

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplesounds config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
