# Simple Quality of Life — 26.3 port inventory

Source (read-only): `C:/Users/oussa/Downloads/Minecraft/Mine/custom created mods/simplequalityoflife`.
Source HEAD: `9272a4ee08211c99caa52419bfde9a336991fff5`; working tree already contains an uncommitted multiloader conversion. The active `common/fabric/neoforge` sources, not deleted old `src/main` or duplicated `legacy-src`, define the inventory. Version 1.0.6, CC0.

## Features

- **Manual crawling**: P or /crawl sets the synchronized swimming pose. Jump exits. Commands are limited to one change per 10 ticks; spectators and passengers are refused.
- **Auto-walk**: R toggles ordinary forward input only when the server enables qOL.enableAutowalk (default false). Opening a screen or leaving the world stops it. A click sound signals the change. It adds no speed or reach.
- **Climbing and sliding**: Climbing speed defaults to 0.4 blocks/tick (range 0.2–0.4). Slide speed defaults to 0.8 (range 0.15–0.8). CAMERA requires looking down and sneaking; ALWAYS requires looking down without sneaking. The server rejects excess vertical movement on climbable blocks with a bounded latency allowance.
- **Frost Walker on powder snow**: Frost Walker permits walking on powder snow. Enabled by frostWalkerWalkOnPowderSnow, default true.
- **Feather Falling farmland protection**: Feather Falling boots prevent trampling. This complements SimpleBuilding Breeze head protection. Enabled by qOL.preventFarmlandTrampleWithFeatherFalling, default true.
- **Hoe harvest and replant**: Main-hand hoes harvest mature wheat, carrots, potatoes, beetroot, nether wart, and cocoa. One seed is taken from the actual loot; survival costs one durability. Server reach, world border, spawn protection, build rights, loader break vetoes, and a four-tick action limit are enforced.
- **Furnace lava fill**: Either hand inserts one lava bucket into a furnace, blast furnace, or smoker with an empty fuel slot or one leftover empty bucket. Locked furnaces and sneaking are excluded. Survival consumes the lava bucket; an existing empty bucket is returned exactly once. SimpleBuilding furnace subclasses are supported through the Vanilla interface.
- **Sharpness cuts vegetation**: Sharpness III or higher on a sword or axe clears explicit small vegetation overlapping the attacked entity. Sneaking disables it. At most 27 positions within 4.5 blocks are considered; loader break vetoes and action cooldown apply. The matching vegetation outline is hidden.
- **Durability bonus**: Damageable held Vanilla and mod tools with at least 80% durability can receive an attack damage and mining speed bonus. The default multiplier is 1.0 (no bonus); item durability capacity and wear remain unchanged. Threshold is bounded to 0.8–1 and multiplier to 1–1.5. Server config controls gameplay.
- **Mute mobs**: qOL.mutedEntities selects entity IDs. Names ending in _mute or _shhh are silent by default. Lists accept at most 64 nonempty distinct entries of at most 64 characters. Existing Vanilla silent state remains effective.
- **Permanent babies**: Every 100 ticks, named ageable mobs ending in _baby or _small are set to age -24000. The source also makes named adults young again; this behavior is preserved. Removing the suffix allows aging again.
- **Piglin gold equipment**: Gold armor trims or a golden sword, spear, pickaxe, axe, shovel, or hoe in either hand count as safe equipment. The two switches default true. This does not prevent anger caused by attacking or opening containers.
- **Weather controls**: qOL.disableWeather (default false) clears rain and thunder and skips weather progression. qOL.clientRainParticleDensity is a local visual preference, default 20, bounded 0–100; it never changes server weather.
- **Vault cooldown**: A player may loot a vault again after qOL.vaultCooldownDays, default 100 Minecraft days, bounded 1–36500. The original SimpleBuildingLootTimes UUID-to-time map is retained. Missing timestamps on already rewarded entries start a cooldown rather than granting free repeat loot; backwards time does not unlock rewards.

## Configuration and compatibility

Config file `config/simplequalityoflife.json`. Original nesting and keys are retained. Gameplay configuration is owned by the server; config sync is S2C only (`simplequalityoflife:config_sync`, maximum JSON 32768 bytes). Rain density is cosmetic. Config UI has movement, interaction, mobs, weather, and vault tabs; every option has bilingual name and default-bearing tooltip. Client file edits take effect on server restart, not as a client gameplay request. Admin command changes persist immediately.

Additional server switches: `qOL.enableManualCrawl` and `qOL.enableVaultCooldown`, both default true. Disabling vault resets restores Vanilla one-time rewards without deleting saved timestamps. These are configuration controls, not additional QoL features.

| Key | Default |
|---|---|
| `frostWalkerWalkOnPowderSnow` | `true` |
| `qOL.enableAutowalk` | `false` |
| `qOL.mutedEntities` | `[]` |
| `qOL.nametagMuteSuffixes` | `["_mute", "_shhh"]` |
| `qOL.nametagBabySuffixes` | `["_baby", "_small"]` |
| `qOL.preventFarmlandTrampleWithFeatherFalling` | `true` |
| `qOL.sharpnessCutsGrass` | `true` |
| `qOL.enableHoeHarvest` | `true` |
| `qOL.enableFurnaceLavaFill` | `true` |
| `qOL.ladderClimbingSpeed` | `0.4` |
| `qOL.enableFastLadderSlide` | `true` |
| `qOL.ladderSlideSpeed` | `0.8` |
| `qOL.ladderSlideActivation` | `"CAMERA"` |
| `qOL.vaultCooldownDays` | `100` |
| `qOL.enableFullDurabilityBonus` | `true` |
| `qOL.fullDurabilityThreshold` | `0.8` |
| `qOL.fullDurabilityBonusMultiplier` | `1.0` |
| `qOL.piglinsIgnoreGoldTrims` | `true` |
| `qOL.piglinsIgnoreGoldTools` | `true` |
| `qOL.disableWeather` | `false` |
| `qOL.clientRainParticleDensity` | `20` |

Hard bounds: climb 0.2–0.4 blocks/tick; slide 0.15–0.8; bonus 1–1.5; threshold 0.8–1; vault days 1–36500; rain 0–100; lists 64 entries/64 characters. Nonfinite numbers use defaults. Auto-actions are limited to one per four ticks, crawl to one per ten ticks. Climb packets share a tick distance budget; position tolerance 0.05, latency allowance at most three ticks. Vanilla horizontal movement checks remain active.

## Registry and data inventory

No registered custom items, blocks, entities, enchantments, recipes, loot tables, tags, advancements, creative tabs, or menus exist in the active source. No token is shipped. Source language files contain obsolete names for `spawn_teleporter`, `launchpad`, `brick_snowball`, and `money_items`; these are unused strings, not world registry IDs, and are excluded. Old decorative PNGs are not copied as gameplay art. Existing Vanilla item models/textures/recipes remain Vanilla-owned. No source tests were found.

Network: `simplequalityoflife:config_sync` retains its JSON shape. Key category `simplequalityoflife:general`; key translations `key.simplequalityoflife.crawl` (P), `key.simplequalityoflife.autowalk` (R). Vault save/load uses the underlying field before a world is attached; this fixes a source getter returning null during load and dropping saved timestamps. The Vault map keeps the historically misleading key `SimpleBuildingLootTimes`; it belongs to the source mod, not SimpleBuilding's Astral Vault. Map values are UUID strings and game-time longs. No old registry aliases are needed because no source registrations are removed.

## Commands

Crawl synchronization adds the S2C-only `simplequalityoflife:crawl_state` UUID/boolean payload.
Only the server's validated command/jump path changes this state; the client cannot submit it.
Tracking observers receive the current state, so client pose updates cannot undo authorized crawling.

Players: `/crawl`. Owner permission: `/simplequalityoflife vaults cooldown <1..36500>`; `/simplequalityoflife tweaks ladderSpeed <0.2..0.4>`; boolean subcommands `fullDurabilityBonus`, `autowalk`, `farmlandProtect`, `frostWalkerSnow`, `hoeHarvest`, `furnaceLava`, `sharpnessCut`; list commands `muteSuffixes`/`babySuffixes` with `list`, `add`, `remove`, `clear`. Administrative command feedback is retained; gadget/key feedback uses sounds and poses.

## Overlap and collision review

- No registry or recipe collision with SimpleBuilding or already imported Simple Tweaks features. Feather Falling farmland protection complements the Breeze head hook on the same `FarmlandBlock.fallOn`; both use MixinExtras expression modification, plus a NeoForge event.
- Lava filling supports SimpleBuilding furnace subclasses through `AbstractFurnaceBlockEntity`, never internal module imports. Its lock and fuel slot are respected.
- Durability bonus also affects SimpleBuilding damageable tools. The default is 1.0; the 1.5 cap retains the existing maximum +50% combat/mining gain. Server-hook tests cover a Vanilla pickaxe, the public SimpleBuilding hammer ID and a component-defined third-party-like tool. This is a balance interaction, not a duplicated feature.
- SimpleBuilding movement/air-jump mixins also touch LivingEntity/Player. Climb checks are restricted to climbable blocks; normal movement retains Vanilla checks.
- Dev `mousetweaks` and `clientsort` cover inventory operations; this source has no sorting or transfer packets, so none are added. `clienttweaks` offers assorted optional client automation: potential key/input overlap with auto-walk, so it requires owner combination review. `sodium-extra` has cosmetic weather controls that may compound rain reduction. These dev mods default off; no claim of arbitrary modpack compatibility.
- R/P may collide with user or dev-mod bindings; Vanilla key remapping remains available. No automatic rebinding.

## Launch, tests, and deferred work

Manifest-driven Hub discovery and isolated integration instances; Fabric and NeoForge suites load SimpleBuilding. Module catalog includes launch, every gameplay family, bounds, permissions, cooldown, config/lang, and cross-mod probes. Client smoke uses the existing module-selected Fabric harness.

Forge 26.3 now has an experimental opt-in loader adapter; see the status below. 26.2/1.21.11/26.4 are deferred until release approval. No files in those lines are ported here. No existing source world is upgraded in place. The current module does not provide JEI recipes or Jade blocks because it registers neither; bilingual Wiki chapters document every feature/config/command.

## Experimental Forge 26.3

Enable `-Pforge263=true`. All 24 canonical server cases are registered with identical IDs/bodies. Forge supplies S2C-only config/crawl channels, login/tracking sync, commands, key bindings, client tick/logout, real break-event veto (including Result.DENY), lowest-priority interactions and farmland protection. The canonical config/main class is compiled through a generated platform adaptation; fields, defaults and normalization are never duplicated. Module-owned JSON load/save retains `config/simplequalityoflife.json`, 32768-byte input limit and administrative persistence. No cross-mod shim packages. Cloth GUI is excluded. The experimental Forge transport currently requires this module on both sides. Forge client rendering, physical keys, real multiplayer delivery and owner-world upgrades remain unverified.

Forge follow-up verification: 24/24 canonical cases passed in the combined run `2026-09-30T17-52-13Z-77a1`; **2828/2828, alles gruen** across existing Fabric/NeoForge/Forge 26.3, integration and all manifest module server suites. Explicit Forge compile without `forge_runs`: exit 0. Client and owner-world limits above remain open.

## Default bonus change (2026-09-30)

The original key, interaction tab and option name remain. Fresh configs and missing keys use
1.0. Existing files explicitly containing 1.5 keep that value: the old generated default
cannot be distinguished from an intentional owner choice. Set it to 1.0 to disable the
bonus in an existing world. No migration rewrites files automatically. NaN and either
infinity use 1.0; finite values below 1 or above 1.5 clamp to those bounds, including at
server runtime reads. The existing 1.5 cap prevents larger combat/mining amplification
while preserving accepted configurations. Both loaders consume the same shared EN/DE
resources; no separate language copies exist in the loader directories.

Verification: both complete QoL module server suites passed, **48/48, alles gruen**, run 2026-09-30T20-53-16Z-7aed (24 per loader). The durability case exercises actual server-player attack/mining hooks for a Vanilla pickaxe and sword, the SimpleBuilding hammer and a component-defined foreign-tool fixture; finite/nonfinite caps, worn tools and unchanged item durability are asserted. The first launch failed before tests because Java 8 was missing from the cold ForgeGradle configuration; a locally downloaded, checksum-verified JDK 8 resolved it. No Forge gameplay was run. Client rendering, real third-party mods and owner worlds remain unverified.
