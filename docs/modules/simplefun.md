# Simple Fun — 26.3 takeover

Source (read-only): `C:/Users/oussa/Downloads/Minecraft/Mine/custom created mods/simplefun`, clean commit `b778d9bb9559aa17af7ef9a840fe80bb83e1a02a`, version 1.2.0, CC0. There are 37 tracked Java sources, not 188; build output is excluded. Fabric and NeoForge use Mojang mappings. No source tests exist.

## Source inventory

* Sneak-drop (Shift+Q) multiplies item velocity, adds a 20-tick pickup delay. Config `fun.enableYeet=true`, `fun.yeetStrength=3`.
* Vanilla brick, nether brick and resin brick become throwable projectiles. `fun.enableThrowableBricks=true`, `fun.throwableBricksBreakBlocks=false`, `fun.brickDamage=2`. Glass-sounding blocks except tinted glass can break in the original implementation; this is too broad and bypasses claims.
* `simplefun:brick_snowball`: stack size 16, recipe four snowballs around one brick, one result; `fun.brickSnowballDamage=2`. Projectile `simplefun:brick_projectile`; thrown-item renderer and mixed snow/brick impact particles. Original item bypasses the throwable-brick switch.
* Eating raw/cooked pork grants `simplefun:piggy_effect` for 6000 ticks. `fun.enablePiggyEffect=true`. Cosmetic pig head, synced entity data on Fabric and attachment on NeoForge. Neutral effects are still removed by milk: the source comment claiming otherwise is incorrect.
* PvP death produces a vanilla player head with the victim's resolved profile. `fun.playerHeadDrops=true`.
* Feather melee and `simplefun:no_damage` suppress damage; `fun.enableNoDamage=true`. Source damage hook also suppresses indirect player-owned damage, an unintended limitation to correct.
* No Damage I supports feathers, sticks and weapon tags. Novice librarian listing: 25 emeralds, three uses, 15 XP, price multiplier 0.3.
* Vanilla `minecraft:knockback` data override raises maximum level to five and supports `simplefun:knockback_allowed`; feather/stick enchantability is 10. Tags include swords, axes, mace, trident, bow, crossbow and lunge.
* Admin permission level 4: `/simplefun pvp headDrops <bool>`; `/simplefun tweaks yeet toggle <bool>` and `strength <float>`; `/simplefun tweaks bricks enable <bool>`, `breakGlass <bool>`, `damage <float>`, `snowballDamage <float>`. Original floats have no upper bounds. No keybinds, custom blocks or additional mobs.
* Data: one recipe and its recipe-unlock advancement, two enchantment definitions, one item tag, bilingual lang, brick-snowball model/item definition/texture and pig texture.

## Overlaps and port decisions

No existing SimpleBuilding content is duplicated. New farm-animal head IDs remain in `simplefun`, distinct from SimpleBuilding's twelve heads. Shared injection targets (skull renderer/type validity, living damage, item use) use cooperative injections and only this module's types. No imports of SimpleBuilding implementation classes are allowed.

The vanilla Knockback override is an explicit global datapack collision, preserved for world compatibility and documented. Server bounds must cap effective knockback, including forged enchantment levels. Glass destruction must respect server interaction protection and world borders; safest default remains disabled. Projectile lifetime, throw cooldown and damage must be bounded. No Damage must affect direct melee only. Head farming cannot produce more than one head per charged creeper. Config screens edit server configuration for restart; multiplayer clients cannot submit gameplay values.

## Proposed additions (before implementation)

Eight cosmetic delights, each individually switchable, server-triggered and cooldown-limited: flower sniff (hold flower while sneaking), cookie crumbs (eat cookie), apple sparkle (eat apple), carrot crunch (eat carrot), melon splash (eat melon), honey bubbles (drink honey), bread crumbs (eat bread), berry blush (eat berries). Use existing particles and quiet sounds; no buffs, item rewards, terrain changes or text.

Charged-creeper-only pig/cow/chicken/sheep heads: standing, wall, ground-item and worn rendering with vanilla mob textures; one creative-tab row. Secret abilities are cosmetic animal greetings while sneaking, each with a switch and cooldown. No recipes, combat/movement bonuses or free resources. Further witty details: heads play their animal's sound above note blocks; delightful particles never spawn entities or load chunks.

## Stages and verification status

1. Inventory (this document). 2. Registry/content. 3. Gameplay/security. 4. Client. 5. Additions. 6. Tests, data and final gates. No completed port or passing runtime verification is claimed by this inventory.

Forge 26.3 needs its own entrypoint, trade/command/client/test adapters. Minecraft 26.2, 1.21.11 and 26.4 are deferred release ports. Source repository and existing SimpleBuilding directories remain untouched.
