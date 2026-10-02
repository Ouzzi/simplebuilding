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

## Implemented additions (proposal recorded before implementation in 631ee38e)

Eight cosmetic delights, each individually switchable, server-triggered and cooldown-limited: flower sniff (hold flower while sneaking), cookie crumbs (eat cookie), apple sparkle (eat apple), carrot crunch (eat carrot), melon splash (eat melon), honey bubbles (drink honey), bread crumbs (eat bread), berry blush (eat berries). Use existing particles and quiet sounds; no buffs, item rewards, terrain changes or text.

Charged-creeper-only pig/cow/chicken/sheep heads: standing, wall, ground-item and worn rendering with vanilla mob textures. The `simplefun:fun` tab (`itemgroup.simplefun.fun`) holds only the mod items - brick snowball, then the four heads; the vanilla ingredients stay in their vanilla tabs. On all three loaders the brick snowball also follows the snowball in Combat and the heads follow the dragon head in Functional Blocks, so the search tab lists them there (test `creative_tabs`). Secret abilities are cosmetic animal greetings while sneaking, each with a switch and cooldown. No recipes, combat/movement bonuses or free resources. Further witty details: heads play their animal's sound above note blocks; delightful particles never spawn entities or load chunks.

## Stages and verification status

Inventory committed as `631ee38e`; playable registry, gameplay, client, additions and initial tests committed as `bd511228`. Final quality stage adds rendering regression checks, real merchant offers, command permissions, indirect-damage checks, finite projectiles, legacy-config bounds and LAN protection. There are 32 module GameTests on each loader and an isolated Fabric client smoke with four screenshot checkpoints.

The complete 26.3 run passed **1627/1627**, including 1562 existing tests, the integration harness and 64 module tests (`2026-09-30T15-47-00Z-8618`). The final private/LAN/dedicated glass guard passed **2/2** separately (`2026-09-30T15-56-02Z-6ca2`). Test centres were rebuilt in the existing separate Fabric/NeoForge GameTest worlds; their coverage/stations remained green. Module namespace registration, models, all five items and eight skull blocks have their own coverage/data checks. The owner's world was not opened.

Fabric title-to-world, module configuration and standing/wall/ground/worn head rendering passed **4/4** (`2026-09-30T15-41-55Z-f8d3`). All four screenshots were viewed; no missing head model/texture errors remained. Evidence: `modules/simplefun/docs/previews/`. A first screenshot inspection exposed incorrectly nested special item models and Windows text decoding; both were repaired. No new pixel art was made: original source assets are preserved, new heads reference actual Vanilla textures.

The module provides 26 server settings in three tabs, with bilingual names/tooltips/defaults. Original nine config keys remain intact. Added switches: higher knockback, Brick Snowball use, No Damage trades, animal-head loot, four independent head greetings and eight independent delights. `fun.maxKnockback` is bounded to 0–4; disabling higher knockback caps the enchantment contribution at two. Worn heads offer no attributes or movement/combat bonuses. Delights use distinct Vanilla particles with a shared 100-tick cooldown, six-particle limit and quiet sound.

Glass breaking is deliberately restricted to **private single-player**. Published LAN and dedicated servers refuse block destruction even when the old config key is true. Only ordinary/stained glass and panes are eligible; tinted glass and ice remain intact. The private-world route also checks interaction protection and the world border. Throws have one shared server cooldown across item types, consume exactly one item, store only one render item and expire after 200 ticks. No Damage affects direct melee only; its bounded knockback works even though zero-damage hits do not use Vanilla's ordinary hurt knockback path.

No duplicate SimpleBuilding items/features were added. The intentional `minecraft:knockback` override is a datapack collision; custom IDs all remain in `simplefun`. JEI supplies acquisition/use pages; Vanilla skull names provide Jade's ordinary block identification. Quiet advancements give inventory acquisition hints. Framework/public Vanilla registries are used for cross-mod storage tests, without importing SimpleBuilding internals. No module-specific shared build/runner/Hub wiring was added. Scaffold integration selection and generated wiki outputs are the only shared additions alongside the module's own manifest entry.

Manual wiki chapters cover content, original commands, all config and all additions. Balance producer metadata is `balance/simplefun/options.json`; hard caps live in named Java constants. The wiki inventory export is written from the **booted registries**, using `SIMPLEFUN_EXPORT_ROOT=<absolute modules/simplefun path>` during module GameTests. Data remain inside the module's generated resource tree.

Configuration screens edit a local file for the next process restart; clients cannot submit server gameplay settings. Remote admins use the original level-four commands or the server file. Head loot/trade switches apply at server data loading, so changing the file requires restart. The normal librarian pool supports the No Damage offer; Minecraft's experimental Trade Rebalance may replace the normal pool, as with the existing modules.

Not verified: NeoForge client/UI, an actual upgraded 1.21.11 world, the owner's modpack/world, interactive JEI/Jade clicks, German UI screenshots, LAN networking with two real clients, and subjective audio. The private/LAN/dedicated permission matrix is covered by server tests. Source repo is clean and read-only. No push or merge.

Forge 26.3 needs its own entrypoint, trade/command/client/test adapters. Minecraft 26.2, 1.21.11 and 26.4 are deferred release ports. Source repository and existing SimpleBuilding directories remain untouched.
`nFinal full Gradle check: **GRADLE_EXIT=0**, including shared 26.2 compilation, balance, module data, atlas/Jade checks and all-module wiki validation. Default and all-module wiki generation/checks passed. Final namespace wiki generation/check also passed after LAN wording was synchronized.

## Experimental Forge 26.3
Opt-in `-Pforge263=true` adapter: shared bounded mechanics, registries, conditional trades, loot, JSON configuration, and server tests. Same catalogue IDs and shared bodies; module-owned loader hooks and isolated test world. No speculative optional Forge dependencies. Cloth GUI unavailable; existing server JSON settings retained. Forge client, real multiplayer and optional integrations remain unverified.

Forge catalogue: **32/32, alles gruen**, `2026-09-30T17-05-12Z-3196`. Registry, recipes, conditional trades/loot, config/security bounds, Piggy and cross-mod tests passed. Piggy uses tracked entity data on Forge; remote-player rendering still needs real multiplayer/client acceptance. Build generates the canonical config minus inert Cloth annotations, avoiding duplicate shim packages; field values/normalization remain canonical. JEI plugin and Cloth GUI are excluded because no Forge 26.3 integration is declared.
