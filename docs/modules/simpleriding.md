# Simple Riding 26.3

Source (read-only, clean commit `ff83701`): `C:/Users/oussa/Downloads/Minecraft/Mine/custom created mods/simpleriding`, Fabric/Yarn Minecraft 1.21.11, version 1.0.5, CC0. The source has 22 Java files, no automated tests, no own items/blocks/entities/recipes/commands/keybindings/advancements. This follow-up adds an informational advancement. Empty datagen/effect classes register nothing. This port is Fabric + NeoForge 26.3 only; Forge and older/snapshot lines are deferred.

## Complete feature inventory

- Tailwind I–III on saddles and 16 harness colors. Player-controlled horses/camels: +30% movement speed per level; pigs/striders/other saddled mounts: +20%; Happy Ghasts: +85% actual flight speed per level. Flying attributes use square-root scaling because Vanilla uses that attribute in both ridden input and acceleration, avoiding quadratic amplification beyond the server cap. Server bounds cap speed at +100% per level, +300% total, and level at III. Nautiluses (including zombie nautiluses) use their own +20% default. Bonuses use transient ADD_MULTIPLIED_BASE modifiers; removing equipment/enchantment or rider removes them.
- Leaping I–III on horse armor: +20% jump strength per level while player-controlled; cap +50% per level. Six vanilla horse armor materials plus optional `simplebuilding:enderite_horse_armor`. All five vanilla nautilus armor materials plus optional `simplebuilding:enderite_nautilus_armor` accept Leaping for +20% dash strength per level. The server executes the dash, synchronizes the impulse, and preserves the vanilla 40-tick cooldown. Dash and Tailwind combine to at most 4x base speed, with an independent +150% jump/dash bonus cap.
- Horse armor utility whitelist: Protection, Fire Protection, Blast Protection, Projectile Protection, Feather Falling, Leaping. Nautilus armor accepts the same protection types and Leaping, but rejects Feather Falling. The source's anvil/table whitelist excludes Unbreaking/Mending/Thorns despite a looser global predicate. Effective table enchantability 15; riding enchantments themselves are book-only. Vanilla protection types retain exclusivity. Feather Falling uses the source's 12% per level computed-fall-damage reduction, at most IV.
- Creative tab `simpleriding:riding_items`: Tailwind III and Leaping III books, saddle icon.
- Persistent `simpleriding:coordinates` BlockPos component is unused but retained for old stacks; synchronized codec added.
- Exploration loot and librarian offers below; two independent worldGen toggles.
- Config screen through Fabric Mod Menu and NeoForge's mod config button. JSON `config/simpleriding.json`, server owned, defaults and bounds visible in both languages. Changes saved by the screen take effect on restart.

## Registry/data IDs

Preserved enchantments: `simpleriding:tailwind`, `simpleriding:leaping`; component `simpleriding:coordinates`; creative tab `simpleriding:riding_items`; item tags `simpleriding:saddle_enchantable`, `simpleriding:horse_armor_enchantable`; attribute IDs `simpleriding:tailwind_boost`, `simpleriding:leaping_boost`. New internal data codecs: `simpleriding:weighted_enchant`, `simpleriding:trades_enabled`. Trade IDs: `simpleriding:librarian/{2,3,4}/riding_book`. New additive tags: `simpleriding:nautilus_armor_enchantable` and `simpleriding:mount_armor_enchantable`; legacy horse tag remains horse-only. No item/block/entity IDs to migrate. No recipes: vanilla equipment retains its own sources; books come from loot, librarians, or creative tab.

## Config (all legacy paths preserved)

All 18 leaf options are catalogued in `RidingOptions`, shown in three config tabs, and exported in
`balance/simpleriding/options.json`. Every name, tooltip, tab and default is covered in en_us/de_de.
The local config screen writes a file for the next server restart; it never updates a running server
or sends config packets to a remote server.

| JSON path | Default | Hard range |
|---|---|---|
| worldGen.enableVillagerTrades | true | boolean |
| worldGen.enableLootTableChanges | true | boolean |
| enchantments.swiftRide.ghastSpeedMultiplier | 0.85 | 0–1 |
| enchantments.swiftRide.horseSpeedMultiplier | 0.3 | 0–1 |
| enchantments.swiftRide.otherSpeedMultiplier | 0.2 | 0–1 |
| enchantments.horseJump.jumpStrengthMultiplier | 0.2 | 0–0.5 |
| safety.enableTailwind | true | boolean |
| safety.enableLeaping | true | boolean |
| safety.enableArmorUtilities | true | boolean |
| safety.enableNautilus | true | boolean |
| safety.maximumSpeedBonus | 3.0 | 0–3 |
| safety.maximumJumpBonus | 1.5 | 0–1.5 |
| safety.movementDistancePerTick | 4.0 | 0.5–4 |
| safety.movementPacketsPerTick | 20 | 1–20 |
| enchantments.swiftRide.nautilusSpeedMultiplier | 0.2 | 0–1 |
| enchantments.horseJump.nautilusDashMultiplier | 0.2 | 0–0.5 |
| enchantments.horseJump.featherFallingReduction | 0.12 | 0–0.12 |
| enchantments.horseJump.armorEnchantability | 15 | 0–15 |

Missing/null sections are defaulted; nonfinite values use defaults; finite values are clamped.
Gameplay checks repeat bounds at use, including forged enchantment levels up to 255. Invalid JSON
fails startup clearly. Utility disabling prevents new utility enchants and the horse-specific
Feather Falling effect; existing protection still uses Vanilla's BODY pipeline. Setting armor
enchantability to zero disables table utility enchanting without changing anvil use.

## Server security and UX decisions

- The packet mixin runs after Vanilla's server-thread handoff. Only the controlling living rider
  may issue jump commands, with their own player ID and charge 1–100; cooldown and same-tick replay
  checks reject abuse. Nautilus impulses are computed from server equipment/config and sent to the rider. The resulting total motion (including existing momentum) is bounded to the configured tick movement limit and at most 3.9 blocks/tick for Vanilla motion synchronization; nonfinite motion is cleared.
- Supported mounts share a per-connection budget: up to 4 blocks cumulative movement and 20 packets
  per server tick, plus a horizontal envelope derived from server attributes and accepted Nautilus
  dash (including its decay). Camel dash envelopes include their Vanilla impulse, and the Vanilla 55-tick cooldown is retained on the server to reject replay. Flying/swimming mounts use that envelope for all axes. A horse ascent
  above the small tolerance requires an accepted grounded jump or a grounded step; jump height is bounded.
- NaN/infinity, impossible steering pitch, border-crossing bounding boxes and unloaded/out-of-height
  targets are refused before Vanilla moves anything. Dismount/remount or changing mounts never
  refills a same-tick budget. Ground flags for landing/fall checks are derived from server collision instead of trusting client claims. Vanilla collision, teleport, floating and move checks remain in place.
- Feature switches allow disabling Tailwind, Leaping, armor utilities or Nautilus bonuses. Security
  checks stay enabled even when bonuses are disabled. Rejected floods produce at most one position correction per tick. No new teleport, spawn, chunk ticket, inventory
  transfer or claim-permission path exists. External claim mods still enforce their own permissions;
  this is not a replacement for their protections or a full Vanilla anti-cheat.
- Existing mount sounds, bubbles and dash state provide feedback. No gadget chat/action-bar text,
  duplicate armor, new pixel art, custom HUD, recipe or item model is introduced. Existing enchantment
  tooltips, creative books, Vanilla anvil/table and equipment UI, wiki and the Nautilus advancement
  provide usage hints; JEI/Jade continue to show the existing equipment and attributes.
- Existing defaults remain intact, including Ghast +85% per level. The owner should assess the 4x
  aggregate speed ceiling and tight movement envelopes on real terrain, underwater dashes, falls,
  latency and third-party mount/physics mods. Lower distance limits can correct legitimate movement.

## Loot and trades (source code, not README promises)

Bastion treasure/other: 0–2 rolls, Tailwind II/III 20/10, Leaping II/III 10/5, empty 30. Nether bridge: 0–1 roll, Tailwind II/III 5/10, empty 20. Trial chambers reward_common/reward_rare: 0–2 rolls, Leaping II/III 10/3, Tailwind II/III 10/3, Protection IV 10, empty 60.

Librarian level 2: riding I weights 30/20, 10–29 emeralds, 2 uses, 25 XP, discount 0.5. Level 3: riding II weights 20/30, 15–34 emeralds, 1 use, 50 XP, discount 1.0, 15% second-enchantment attempt. Level 4: riding III weights 20/20, 15–49 emeralds, 1 use, 80 XP, discount 1.0, 35% second attempt. Second selection retries at most 10 times; same-enchantment selection is discarded. No wandering trader or level-5 offer exists in source. Vanilla 26.3 data-driven trades replace Fabric's old TradeOfferHelper.

## Collisions, duplicates, deliberate corrections

- SimpleBuilding ships Enderite horse/nautilus armor and armor recipes, but neither Tailwind nor Leaping. No duplicate registry item/recipe is created. Public optional Enderite horse armor tag entry enables synergy; optional Enderite nautilus armor tag enables the same protection and dash synergy without importing SimpleBuilding implementation classes.
- Both mods inject LivingEntity.tick and Enchantment.canEnchant/isSupportedItem. SimpleBuilding's armor trim attributes use distinct IDs; its XP repair restriction targets a different item tag. Their normal predicates do not overlap. Module tests run with SimpleBuilding loaded and check real anvil/storage behavior.
- The source injects AnvilMenu/createResult and EnchantmentHelper and globally broadens armor/foot tags. The port keeps the narrower effective whitelist via canEnchant/isSupportedItem; an extra source anvil interception is unnecessary. Foot/armor tags remain additive; primary-item checks reject player-only boot effects.
- Source horse Protection applied an extra 4% per level after vanilla BODY protection. The port uses vanilla exactly once (avoids double protection); Feather Falling still needs its horse-specific calculation hook because its vanilla slot is FEET.
- Source Happy Ghast fallback granted Tailwind I without an enchanted item and scanned fields reflectively. The port reads the BODY harness and requires actual Tailwind, without reflection or debug-log spam.
- Source could leave speed attached after replacing an enchanted saddle while still riding. The port clears zero-level modifiers each tick.
- No config-name/keybind/command/recipe collision; module config and language prefixes remain `simpleriding`. SimpleTweaks takeover adds no duplicate riding enchantments.

## Tests and launch

Own `simpleriding:riding_game_test_*` catalogue, shared bodies and thin loader adapters. Run `python tools/testrunner/run.py --targets module-simpleriding-fabric-263,module-simpleriding-neoforge-263 --filter 'simpleriding:*'`. Fabric uses the selected integration instance (requires SimpleBuilding + Simple Riding + Cloth Config); NeoForge uses `integration/run-neoforge-263` with both source mods loaded. Existing integration-263 remains a separate wiring test. Launch Hub discovers the module via manifest; Fabric integration client/server buttons use its selection.

Tests cover registry/data launch, all six anvil enchants/table utility, horse/pig/strider/camel speed and equipment cleanup, Ghast harness, Leaping and dismount, actual armor damage, loaded trade pools and generated offers/prices/levels, loot book generation/toggle, config bounds/lang/tooltips/defaults, Enderite armor, and foreign enchantment/component roundtrip through a SimpleBuilding hopper. No custom item needs a new model/texture or test-centre item station. An inventory advancement for nautilus armor explains applying Tailwind and Leaping at an anvil; it has no chat announcement or rewards. Source mod has no tests to port.

Fabric client smoke: `python tools/testrunner/run.py --targets module-simpleriding-client-263`. It proves title-to-world, both mods loaded, synchronized enchantments/tab, normal-world resolved librarian pools (without experimental Trade Rebalance), and opening the config screen. Three screenshot checkpoints are under `integration/run-fabric-263/screenshots/`. Only this module's client test runs; SimpleBuilding's client suites are excluded. The Hub queues both module server targets after its separate wiring test, so Fabric's integration directory is never used by those two tests concurrently.

Verified server run `2026-09-30T13-31-20Z-5d9f`: existing Fabric/NeoForge 1554/1554 plus module 26/26, all green. Separate integration `2026-09-30T13-34-31Z-962c`: 1/1. Client `2026-09-30T13-28-14Z-ead5`: 3/3 screenshot checkpoints; config screenshot visually inspected. Launch Hub: 35 unit tests green. Test-centre build and item/block coverage are included in the full existing suites. Wiki generator/check, module static data gate, books (0 problems), and textures are green.

Final `gradlew.bat check -q --no-daemon`: explicit `GRADLE_EXIT=0`, including shared 26.2 compilation and module/client-harness compilation. Not verified: NeoForge client rendering/config opening, a real upgraded source-mod world, owner-world test-centre rebuild, and Forge/deferred-line ports. Source repository remains clean; no push or merge.

## Deferred ports and limitations

Forge 26.3: add loader metadata/entrypoint, registry/config-condition/loot and gametest adapters using shared code. 26.2/1.21.11/26.4: separate release port after owner approval; no edits made to those project trees. Third-party mounts are not given new speed bonuses; the security guard covers Vanilla supported mounts. Source's explicit Final whitelist is retained. Server gameplay values are authoritative; client configuration does not change a remote server. Owner test world is never altered by these test instances. Verification results are recorded in HANDOFF.

Experimental Trade Rebalance limitation: Vanilla replaces librarian tags when that experimental pack is enabled (also enabled automatically in GameTest worlds). This hides both SimpleBuilding and Simple Riding offers in those resolved pools. Tests verify the shipped additive links and generate real offers directly; the normal client-world smoke checks the resolved pools with Trade Rebalance off. No feature is falsely reported as available in an experimental pool.

## Follow-up verification (2026-09-30, codex-riding-followup)

The expanded catalogue has 26 tests per loader, including actual packet dispatch with a teleport
acknowledgment, valid movement control, out-of-range/replayed commands, all mount speed caps,
combined Nautilus dash caps, feature toggles, every config leaf/default/bound, language completeness,
Enderite armor, and bounded weighted loot/trade codecs. Run outcomes are appended to HANDOFF.
Historical verification above belongs to the initial port, not this follow-up.

Final continuation results: full Fabric/NeoForge 26.3 suites **1562/1562, alles gruen**
(`2026-09-30T17-29-33Z-786a`); Riding plus integration **53/53, alles gruen**
(`2026-09-30T17-31-41Z-3c3b`); Fabric client smoke **3/3, alles gruen**
(`2026-09-30T17-33-00Z-5b61`). The client exercises server-owned Nautilus dash,
invalid charge rejection, ordinary and combined ascending/steering Ghast flight, and all
18 config entries in three tabs; the config screenshot was inspected. Full suites rebuild
the test centre and verify complete item/block coverage in both isolated test worlds.
`python wiki/generate.py --all`, `--all --check`, and final `gradlew.bat check -q`
passed; the final gate reran `--all --check` after the last German prose correction and
reported **GRADLE_EXIT=0**. No unrelated generated wiki content changed.
Not verified: NeoForge client rendering, real upgraded worlds, owner-world rebuild,
latency/third-party physics and real-terrain acceptance, Forge or deferred runtime lines.
No push or merge; no source changes in mc1_21_11 or mc26_4.
