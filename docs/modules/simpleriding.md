# Simple Riding 26.3

Source (read-only, clean commit `ff83701`): `C:/Users/oussa/Downloads/Minecraft/Mine/custom created mods/simpleriding`, Fabric/Yarn Minecraft 1.21.11, version 1.0.5, CC0. The source has 22 Java files, no automated tests, no own items/blocks/entities/recipes/commands/keybindings/advancements. Empty datagen/effect classes register nothing. This port is Fabric + NeoForge 26.3 only; Forge and older/snapshot lines are deferred.

## Complete feature inventory

- Tailwind I–III on saddles and 16 harness colors. Player-controlled horses/camels: +30% movement speed per level; pigs/striders/other saddled mounts: +20%; Happy Ghasts: +85% movement/flying speed. Named server constants cap speed at +100% per level and level at III. Bonuses use transient ADD_MULTIPLIED_BASE modifiers; removing equipment/enchantment or rider removes them.
- Leaping I–III on horse armor: +20% jump strength per level while player-controlled; cap +50% per level. Six vanilla horse armor materials plus optional `simplebuilding:enderite_horse_armor`. No nautilus extension.
- Horse armor utility whitelist: Protection, Fire Protection, Blast Protection, Projectile Protection, Feather Falling, Leaping. The source's anvil/table whitelist excludes Unbreaking/Mending/Thorns despite a looser global predicate. Effective table enchantability 15; riding enchantments themselves are book-only. Vanilla protection types retain exclusivity. Feather Falling uses the source's 12% per level computed-fall-damage reduction, at most IV.
- Creative tab `simpleriding:riding_items`: Tailwind III and Leaping III books, saddle icon.
- Persistent `simpleriding:coordinates` BlockPos component is unused but retained for old stacks; synchronized codec added.
- Exploration loot and librarian offers below; two independent worldGen toggles.
- Config screen through Fabric Mod Menu and NeoForge's mod config button. JSON `config/simpleriding.json`, server owned, defaults and bounds visible in both languages. Changes saved by the screen take effect on restart.

## Registry/data IDs

Preserved enchantments: `simpleriding:tailwind`, `simpleriding:leaping`; component `simpleriding:coordinates`; creative tab `simpleriding:riding_items`; item tags `simpleriding:saddle_enchantable`, `simpleriding:horse_armor_enchantable`; attribute IDs `simpleriding:tailwind_boost`, `simpleriding:leaping_boost`. New internal data codecs: `simpleriding:weighted_enchant`, `simpleriding:trades_enabled`. Trade IDs: `simpleriding:librarian/{2,3,4}/riding_book`. No item/block/entity IDs to migrate. No recipes: vanilla equipment retains its own sources; books come from loot, librarians, or creative tab.

## Config (all source keys preserved)

| JSON path | Default | Range / timing |
|---|---|---|
| worldGen.enableVillagerTrades | true | Server data loading |
| worldGen.enableLootTableChanges | true | Server loot loading |
| enchantments.swiftRide.ghastSpeedMultiplier | 0.85 | 0–1 per level |
| enchantments.swiftRide.horseSpeedMultiplier | 0.3 | 0–1 per level |
| enchantments.swiftRide.otherSpeedMultiplier | 0.2 | 0–1 per level |
| enchantments.horseJump.jumpStrengthMultiplier | 0.2 | 0–0.5 per level |

Missing/null sections are defaulted, nonfinite numbers use defaults, negative numbers clamp to zero. Invalid JSON fails startup clearly rather than silently discarding a server config. Named constants in RidingConfig and RidingLoot, trade JSONs and `balance/simpleriding/options.json` expose tunables to infrastructure without importing SimpleBuilding implementation classes.

## Loot and trades (source code, not README promises)

Bastion treasure/other: 0–2 rolls, Tailwind II/III 20/10, Leaping II/III 10/5, empty 30. Nether bridge: 0–1 roll, Tailwind II/III 5/10, empty 20. Trial chambers reward_common/reward_rare: 0–2 rolls, Leaping II/III 10/3, Tailwind II/III 10/3, Protection IV 10, empty 60.

Librarian level 2: riding I weights 30/20, 10–29 emeralds, 2 uses, 25 XP, discount 0.5. Level 3: riding II weights 20/30, 15–34 emeralds, 1 use, 50 XP, discount 1.0, 15% second-enchantment attempt. Level 4: riding III weights 20/20, 15–49 emeralds, 1 use, 80 XP, discount 1.0, 35% second attempt. Second selection retries at most 10 times; same-enchantment selection is discarded. No wandering trader or level-5 offer exists in source. Vanilla 26.3 data-driven trades replace Fabric's old TradeOfferHelper.

## Collisions, duplicates, deliberate corrections

- SimpleBuilding ships Enderite horse/nautilus armor and armor recipes, but neither Tailwind nor Leaping nor these riding utilities. No duplicate registry item/recipe is created. Public optional Enderite horse armor tag entry enables synergy; nautilus armor is unchanged.
- Both mods inject LivingEntity.tick and Enchantment.canEnchant/isSupportedItem. SimpleBuilding's armor trim attributes use distinct IDs; its XP repair restriction targets a different item tag. Their normal predicates do not overlap. Module tests run with SimpleBuilding loaded and check real anvil/storage behavior.
- The source injects AnvilMenu/createResult and EnchantmentHelper and globally broadens armor/foot tags. The port keeps the narrower effective whitelist via canEnchant/isSupportedItem; an extra source anvil interception is unnecessary. Foot/armor tags remain additive; primary-item checks reject player-only boot effects.
- Source horse Protection applied an extra 4% per level after vanilla BODY protection. The port uses vanilla exactly once (avoids double protection); Feather Falling still needs its horse-specific calculation hook because its vanilla slot is FEET.
- Source Happy Ghast fallback granted Tailwind I without an enchanted item and scanned fields reflectively. The port reads the BODY harness and requires actual Tailwind, without reflection or debug-log spam.
- Source could leave speed attached after replacing an enchanted saddle while still riding. The port clears zero-level modifiers each tick.
- No config-name/keybind/command/recipe collision; module config and language prefixes remain `simpleriding`. SimpleTweaks takeover adds no duplicate riding enchantments.

## Tests and launch

Own `simpleriding:riding_game_test_*` catalogue, shared bodies and thin loader adapters. Run `python tools/testrunner/run.py --targets module-simpleriding-fabric-263,module-simpleriding-neoforge-263 --filter 'simpleriding:*'`. Fabric uses the selected integration instance (requires SimpleBuilding + Simple Riding + Cloth Config); NeoForge uses `integration/run-neoforge-263` with both source mods loaded. Existing integration-263 remains a separate wiring test. Launch Hub discovers the module via manifest; Fabric integration client/server buttons use its selection.

Tests cover registry/data launch, all six anvil enchants/table utility, horse/pig/strider/camel speed and equipment cleanup, Ghast harness, Leaping and dismount, actual armor damage, loaded trade pools and generated offers/prices/levels, loot book generation/toggle, config bounds/lang/tooltips/defaults, Enderite armor, and foreign enchantment/component roundtrip through a SimpleBuilding hopper. No custom item needs a new model/texture or test-centre item station. Source mod has no tests to port.

Fabric client smoke: `python tools/testrunner/run.py --targets module-simpleriding-client-263`. It proves title-to-world, both mods loaded, synchronized enchantments/tab, normal-world resolved librarian pools (without experimental Trade Rebalance), and opening the config screen. Three screenshot checkpoints are under `integration/run-fabric-263/screenshots/`. Only this module's client test runs; SimpleBuilding's client suites are excluded. The Hub queues both module server targets after its separate wiring test, so Fabric's integration directory is never used by those two tests concurrently.

Verified server run `2026-09-30T13-31-20Z-5d9f`: existing Fabric/NeoForge 1554/1554 plus module 26/26, all green. Separate integration `2026-09-30T13-34-31Z-962c`: 1/1. Client `2026-09-30T13-28-14Z-ead5`: 3/3 screenshot checkpoints; config screenshot visually inspected. Launch Hub: 35 unit tests green. Test-centre build and item/block coverage are included in the full existing suites. Wiki generator/check, module static data gate, books (0 problems), and textures are green.

Final `gradlew.bat check -q --no-daemon`: explicit `GRADLE_EXIT=0`, including shared 26.2 compilation and module/client-harness compilation. Not verified: NeoForge client rendering/config opening, a real upgraded source-mod world, owner-world test-centre rebuild, and Forge/deferred-line ports. Source repository remains clean; no push or merge.

## Deferred ports and limitations

Forge 26.3: add loader metadata/entrypoint, registry/config-condition/loot and gametest adapters using shared code. 26.2/1.21.11/26.4: separate release port after owner approval; no edits made to those project trees. Other mods' mount inventories are supported only through public equipment slots. Source's explicit Final whitelist is retained. Server gameplay values are authoritative; client configuration does not change a remote server. Owner test world is never altered by these test instances. Verification results are recorded in HANDOFF.

Experimental Trade Rebalance limitation: Vanilla replaces librarian tags when that experimental pack is enabled (also enabled automatically in GameTest worlds). This hides both SimpleBuilding and Simple Riding offers in those resolved pools. Tests verify the shipped additive links and generate real offers directly; the normal client-world smoke checks the resolved pools with Trade Rebalance off. No feature is falsely reported as available in an experimental pool.
