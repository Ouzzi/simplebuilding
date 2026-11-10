# Simple Fun 0.9.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Fun** for Minecraft 26.3 on Fabric.

Bounded playful mechanics, throwable bricks and cosmetic transformations (the animal heads moved to Simple Mobheads in 1.4.0).

## Added

- **Sneak Throw** - Sneak while dropping items.
- **Throwable Bricks** - Brick, nether brick and resin brick: throw with use.
- **Piggy Transformation** - Eating raw or cooked pork grants a cosmetic pig head for five minutes; milk removes the effect.
- **Damageless** - Feather melee and Damageless I deal zero direct melee damage, with knockback.
- **Knockback V** - Vanilla Knockback accepts feathers/sticks and levels up to five; this replaces vanilla enchantment data and can collide with datapacks.
- **Admin Commands** - Permission level 2: /simplefun tweaks yeet toggle/strength; /simplefun tweaks bricks enable/breakGlass/damage/snowballDamage.
- **Brick Snowball** - Four snowballs around one brick craft one.
- **Flower Sniff** - Cosmetic delight: hold a flower and sneak.
- **Cookie Crumbs** - Cosmetic delight: eat a cookie.
- **Apple Sparkle** - Cosmetic delight: eat an apple.
- **Carrot Crunch** - Cosmetic delight: eat a carrot.
- **Melon Splash** - Cosmetic delight: eat melon.
- **Honey Bubbles** - Cosmetic delight: drink honey.
- **Bread Crumbs** - Cosmetic delight: eat bread.
- **Berry Blush** - Cosmetic delight: eat sweet or glow berries.
- **Party Popper** - Paper, string and a dye craft two.
- **Dice and Coin** - Dice (two bone meal + black dye) roll 1-6, sneaking two dice; the lucky coin (copper ingot + gold nugget) shows heads or tails.
- **High Five** - Sneak with an empty hand and click another player: you raise your hand for 3 s (they see a hint).
- **Petting** - The owner sneaks with an empty hand and clicks an own tamed wolf, cat, parrot or horse: hearts and its contented sound, once every 10 s per animal; Pets given statistic.
- **Skipping Stones** - One cobblestone, tuff, andesite or cobbled deepslate makes four flat stones.
- **Water Balloon** - Leather, string and a water bucket make four (the bucket stays).
- **Bubble Wand** - Stick, string and a water bucket (the bucket stays).
- **Paper Plane** - One paper folds a plane.
- **Rubber Duck** - Yellow dye, a slimeball and a gold nugget.
- **Snow Angel** - Sneak still for 3 s on a snow layer, snow block or powder snow, then jump: a snow angel print of particles stays for 30 s (larger in a snowstorm) with a soft crunch.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: Cloth Config
- Optional integrations: modmenu, jei, jade

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplefun config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
