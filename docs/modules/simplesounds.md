# Simple Sounds (26.3, 0.1.0)

Additive Fabric/NeoForge module, scaffolded with tools/newmod.py. No source repository,
items, blocks, recipes, trades, entities, audio files or pixel art. SimpleBuilding is untouched.
JEI recipes, Jade block providers, creative rows and item advancements do not apply to this
zero-content cosmetic module. Discoverability is through bilingual wiki and tabbed config,
Fabric Mod Menu / NeoForge Mods. All tooltips state their defaults.

## Effect contract

The stable IDs and categories mirror Simple Visuals' effects.json exactly. The module-owned
assets/simplesounds/effects.json is the runtime mapping; the data gate compares both registries
when Simple Visuals is present. No implementation-class imports or dependency on its runtime.
Both mods independently observe the local synchronized player and loaded adjacent blocks.
Simple Sounds deliberately observes only the local player: no remote health/location tracking,
no extra server packets, no chunk loads and no gameplay authority delegated to the client.
Simple Visuals is a soft dependency. Automatic following of its live intensity is not implemented;
the independent global default is SUBTLE, with explicit INHERIT/OFF/four intensity overrides.

| Effect ID | Vanilla sound | Minimum interval |
|---|---|---|
| footstep_dust | block.sand.step | 40 ticks |
| cold_breath | entity.player.breath | 40 ticks |
| fireflies | block.grass.step | 40 ticks |
| pollen | block.azalea_leaves.step | 40 ticks |
| fire_sparks | block.fire.ambient | 40 ticks |
| water_ripples | entity.generic.swim | 40 ticks |
| water_droplets | block.pointed_dripstone.drip_water | 40 ticks |
| leaf_fall | block.cherry_leaves.step | 40 ticks |
| enchanted_items | block.amethyst_block.chime | 40 ticks |
| beacon_aura | block.beacon.ambient | 40 ticks |
| damage_feedback | entity.player.hurt | 40 ticks |
| healing_feedback | block.amethyst_block.chime | 40 ticks |

No exclusions. Firefly/pollen sounds are soft rustling interpretations rather than new recordings.
Vanilla audio and its available subtitles are reused; no sounds.json or custom subtitles needed.
Pitch varies 0.95?1.05. Ambient base volume .12, reactions .22; gains OFF/SUBTLE/NORMAL/
STRONG/MAXIMUM are 0/.35/.6/.8/1. Vanilla Ambient/Environment and Master sliders still apply.
No gain exceeds 1 and final volume never exceeds .25. Existing Vanilla feedback remains audible.

## Configuration and security

Local config/simplesounds.json, maximum 64 KiB read; malformed input uses defaults. Tab General:
globalLevel SUBTLE, volumeCap .25 (0?.25), soundsPerTick 2 (0?4), soundsPerPlayer 1 (0?2),
cooldownTicks 40 (20?1200). Other tabs follow the six effect categories plus reactive feedback.
Each effect defaults to INHERIT; OFF always silences it. Nonfinite volume uses default; unknown
IDs/null overrides are dropped. Local cosmetics have no server gameplay settings to synchronize.

Every playback passes a bounded budget. Excess requests are dropped, never queued. Maximum
four sounds/tick, two/player/tick, eight budgeted player IDs and 96 cooldown keys; runtime
observes only one player. Repeating one effect waits the greater of config cooldown and mapping
interval. Effect priority rotates, history resets on world or player changes (including respawn).
Global OFF, per-effect OFF, volume zero and either count zero are independent spam switches.
No C2S receiver, inventory mutation, movement/reach amplification, world/claim modification,
chunk/entity creation, trading or duplication path exists. Six loaded neighbor samples per
condition, no radius scan. Fire conditions may inspect three such sets. Health changes are
only the local synchronized values. These are sound counterparts, not gameplay notifications.

## Data and plugin integration

Manifest supplies both projects, optional dependencies, paths, module test catalogues and
client smoke entrypoint. Launch Hub discovers them without shared wiring edits. Scaffold also
updates integration/enabled-mods.json. Fabric module tests load SimpleBuilding; NeoForge run
includes SimpleBuilding via public registry/Vanilla test contracts. Own generated resources
path remains empty because there is no gameplay datagen. balance/simplesounds reserves history;
authoritative tuning is the mapping JSON plus named Java config/budget constants.
Wiki manual and both language files are module-owned. Generated wiki data and the manifest-driven index were refreshed by the generator;
no hand-edited generated data or shared implementation changes. Forge is now declared as experimental opt-in support; see the later Forge status.

Tests: launch/zero-item integrity, hostile config bounds, flood/player budgets, cooldown/reset,
SimpleBuilding coexistence and unchanged inventory, each of twelve Vanilla sound IDs and
intensity/override/volume policy; bilingual/options/mapping/client-isolation data gate.
Client smoke covers boot, a real world, tick playback and config screen. Results are appended
below after execution. Subjective sound quality and NeoForge UI require owner acceptance.
Other Minecraft lines are deferred; shared 26.2 compatibility is checked by the final gate.

## Verification (2026-09-30)

- Module server suites: 34/34, alles gruen, run 2026-09-30T16-34-09Z-c2c1;
  17 per loader, SimpleBuilding loaded in each runtime. Initial compile/API and mixin-package
  failures were corrected before these final passing runs; no final module tests skipped.
- Fabric client: 3/3, alles gruen, run 2026-09-30T16-35-28Z-342e; title/world/config,
  real tick-hook reactive playback budget and global Off assertions. World/config screenshots
  visually inspected and saved in modules/simplesounds/previews. Screenshots do not prove
  subjective audio quality or every environmental trigger.
- Test centres rebuilt in both separate 26.3 GameTest worlds: 10/10, alles gruen,
  run 2026-09-30T16-36-44Z-b753, single filter simplebuilding:*test_centre*;
  plan, stations and full existing item/block coverage. Owner world untouched.
- Module data hook, wiki generation/default --check, module generation/--check passed.
  Final gradlew.bat check -q --no-daemon exit 0 includes --all wiki check, balance/module data,
  18 wiki unit tests and shared 26.2 compilation. No deferred-line source edits.
- Not verified: NeoForge client/UI, German UI rendering, subjective listening, every ambient
  condition in gameplay, long-duration/performance and arbitrary modpacks, owner world,
  complete existing SimpleBuilding server/client suites. This earlier port run did not verify Forge or deferred-line runtime.
- Decisions for owner acceptance: Vanilla-only audio, local-player-only observation, independent
  intensity settings (no automatic live following of Simple Visuals). Fireflies/pollen use soft
  rustling. No blocking owner question, no push/merge.

## Experimental Forge 26.3

Opt-in with `-Pforge263=true`; same 17 server catalogue cases and client-only sound tick mixin. JSON settings and sound budgets are unchanged. Cloth GUI/client harness sources are excluded because a compatible Forge artifact is unavailable. Forge client audio, UI and owner-world behavior remain unverified.

Forge follow-up verification: 17/17 canonical cases passed in the combined run `2026-09-30T17-52-13Z-77a1`; **2828/2828, alles gruen** across existing Fabric/NeoForge/Forge 26.3, integration and all manifest module server suites. Explicit Forge compile without `forge_runs`: exit 0. Client and owner-world limits above remain open.
