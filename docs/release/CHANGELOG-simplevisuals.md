# Simple Visuals 0.9.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Visuals** for Minecraft 26.3 on Fabric.

Bounded client HUD, tooltips, anvil formatting, and Vanilla immersion particles.

## Added

- **Speed lines** - Bounded radial lines at high speed or acceleration; cosmetic only, first-person view.
- **Pickup notifications** - Server pickup animations create up to eight short-lived item or XP rows, with four layouts, scale, side, opacity and rarity colors.
- **Elytra pitch helper** - Two target lines while gliding; defaults -40 and 40 degrees, tolerance 10, sensitivity 4.
- **Status effect duration bars** - HUD and inventory bars compare the current duration with the greatest observed duration of that active effect.
- **Locator player heads** - Uses only server-supplied Vanilla waypoints; replaces eligible icons with player heads and keeps Vanilla angles, visibility and elevation arrows.
- **Chat player heads** - Faces beside recognizable player messages use cached Vanilla skins.
- **Local death coordinates** - Disabled, appended, or separate local coordinate line for your own death message.
- **Map preview tooltips** - A 128-pixel map preview, center coordinates and dimension id from map data already available to this client.
- **Held item information** - Durability and up to eight enchantment lines accompany the Vanilla held-item name.
- **Damage indicators** - Up to 32 short-lived numbers for observed health decreases of nearby visible living entities.
- **Biome notifications** - Optional biome name on transition, with display duration, offset and re-entry cooldown.
- **Anvil formatting and uses** - Valid & or section-sign color/style codes are accepted only under server formatting policy.
- **Footstep dust** - Moving on the ground outside water.
- **Cold breath** - Breath outside water in biomes cold enough for snow.
- **Fireflies** - Nighttime above blocks in the Vanilla dirt tag.
- **Pollen** - Daytime beside a flower.
- **Fire sparks** - Adjacent fire, lava or campfire.
- **Water ripples** - Movement at the water surface.
- **Water droplets** - Movement in water.
- **Falling leaves** - Leaves next to a sampled position two blocks above the player.
- **Enchanted item hints** - Enchanted main-hand or off-hand item.
- **Beacon aura** - Adjacent beacon.
- **Damage feedback** - Observed decrease in nearby entity health.
- **Healing feedback** - Observed increase in nearby entity health.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: none (Cloth Config optional: config screen only)
- Optional integrations: simplebuilding, simplemodels, modmenu

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplevisuals config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
