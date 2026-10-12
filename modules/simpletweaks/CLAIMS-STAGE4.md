# Claims stage 4 continuation

## State and scope
Continue existing `codex-next-claims` at `7422a3ab`. Retain and review all draft
automation hooks and tests. Stages 1–3 are committed; stages 5/6 belong to the
separate access worker. Claims stays disabled and is not ready to enable.

## Files and work
1. Review installed 26.3 Minecraft sources and existing automation tests.
2. Fix natural-damage classification and full multipart explosion/fire targets in
   module Claims helpers/mixins; add real positive/negative/off regressions.
3. Trace SimpleBuilding reinforced/breaker/Enderite pistons, hoppers and placed
   attractors. Extend the generic framework/26.3 bridge only where required and
   exercise real entrypoints through registry IDs. Preserve inactive behavior.
4. Document conservative owner-boundary rules, dispenser limitations and remaining
   unsupported paths in module docs/wiki. Append QUEUE/HANDOFF only.

## Risks
Null damage source does not imply an attack. Neighbor updates can destroy another
part of a block. Custom automation can bypass Vanilla hooks. Disabled hooks must
return before world/data reads; enabled malformed stores fail closed. Test fixtures
must isolate whole claim chunks, preserve positive controls and clean up actors.

## Verification
Run relevant module Fabric/NeoForge filters (one pattern per invocation), including
existing disabled/no-provider cases and new actual automation regressions. Read
`alles gruen`, record counts/run IDs. Use bounded Gradle workers/memory in this
worktree, no clients, no parallel full suites. Generate/check wiki and run final
`gradlew.bat check -q`. Commit stage 4 only, without transient orchestration notes,
push or merge; report remaining gaps honestly.

## Review findings resolved
- Natural damage no longer routes every null source through environmental denial.
  Actual fall and drowning ticks, starvation/other damage, a visiting player,
  projectiles, cloud ticks and indirect item damage retain positive controls.
- Explosion and fire mutation checks include known bed/chest counterparts. Actual
  red-bed and straw-bed explosions cover boundary denial and wilderness/off cases.
- Retraction preview now masks only its own head while resolving. Vanilla removes
  that head before its resolver; previewing the unmodified world could fail and
  previously skip connected-branch checks. The scoped view does not mutate blocks.
- Framework automation checks cover custom breaker/fuel operations, custom hopper
  output (including its local loader-API target) and placed-attractor pulls. Fabric
  machine break events are permission probes after the automation check, so the
  fake player's tool listener cannot incorrectly refuse legitimate same-owner use.
  Other loader listeners still receive the event and may veto it.
- The installed-source audit now includes mutable final damage overrides, omitted
  by the original draft's `public boolean hurtServer` search. Container footprint
  traversal is bounded to 256 locations and eight nested containers, linear checks.

## Verification so far
- `2026-09-30T23-26-22Z-376e`: initial resumed run was red (test compile errors on
  Fabric; 27/32 NeoForge). It exposed the retraction bypass and fixture problems:
  mock-player load acknowledgement, out-of-range inactive entity chunks, and stone
  using the 26.3 sulfur-cube dispenser behavior rather than plain item ejection.
- `2026-09-30T23-41-04Z-9706`: 63/64; real Fabric same-owner breaker refusal found
  in its fake-player event, then corrected through the generic probing contract.
- `2026-09-30T23-45-13Z-23b2`: **64/64, alles gruen**, 32/32 per loader. Existing
  claims cases retained. Seven new cases each exercise enabled, disabled and absent
  providers, using synchronous fixtures with block/entity cleanup. The legacy
  stage-4 cases remain, with corrected natural-magic and dispenser expectations.
- All-module wiki generation/check and default wiki check passed. Module data and
  bilingual key/default checks passed. Final worktree gate/baseline checks follow.

## Remaining activation blockers
The follow-up below guards copper-golem direct inventory transfers, crafter
fallback ejection and lightning ignition/copper cleaning. Golems remain unusable
on claimed land until trusted ownership/provenance exists. Lightning rod redstone
and entity transformations remain unguarded; lightning damage currently follows
conservative entity-source denial.
Unknown positionless containers are refused while enabled;
advanced dispenser behaviors are refused near claims, with no proof of arbitrary
mod behavior range. Remote loader storage endpoints, direct mutations by other
mods, all physical secondary effects and exhaustive fire/fluid variants remain
outside verified coverage. Claims stays OFF and is not ready to enable. These are
explicit stage-4 gaps, not work silently assigned to stages 5/6.

Installed-source basis: Minecraft 26.3, NeoForge 26.3.0.16-beta; cached NeoForm
decompilation `decompile_207a421f359c3aca6f053020c72b1f96f373abcf_output.jar`.
Reviewed ServerExplosion, FireBlock, LavaFluid, FlowingFluid, LiquidBlock,
PistonBaseBlock/PistonStructureResolver, HopperBlockEntity, DispenserBlock,
DropperBlock, Projectile, LivingEntity, AreaEffectCloud and mutable damage
overrides. Follow-up gap audit also inspected CrafterBlock,
TransportItemsBetweenContainers and LightningBolt. No Minecraft source is vendored.

## Final baseline verification
All four runs used Fabric + NeoForge 26.3, one filter per invocation, with no
active claim provider. Every listed runner output explicitly said `alles gruen`.

| Filter | Passed | Run ID |
| --- | ---: | --- |
| `simplebuilding:*piston*` | 56/56 | `2026-09-30T23-48-11Z-d4a1` |
| `simplebuilding:*hopper*` | 52/52 | `2026-09-30T23-49-34Z-8de1` |
| `simplebuilding:*attractor*` | 6/6 | `2026-09-30T23-51-32Z-e9b7` |
| `simplebuilding:*test_centre*` | 10/10 | `2026-09-30T23-53-07Z-ac63` |

The last run rebuilt both isolated test centers and passed complete SimpleBuilding
item/block coverage. No owner world or Minecraft client was opened. Gradle is
limited to two workers, 2 GiB heaps and two active processors per Java process.

## Final worktree gate
On 2026-10-01, the full worktree `gradlew.bat check -q` passed with
`GRADLE_EXIT=0`. It ran without parallel projects, with two workers and 2 GiB
heaps, including the existing shared/26.2 and 1.21.11 compilation checks. No runtime
ports or client runs were performed. Log: `scratchpad/claims/stage4-final-check.log`.
Wiki/default/all-module checks, balance/module validation and the standard checks
completed successfully. Existing compiler deprecation/annotation warnings remain.

## Exact changed-file scope
The following 44 files comprise this stage-4 commit. Transient orchestration notes
and `.serena/` are preserved locally and excluded.

```text
.claude/QUEUE.md
common/src/shared/java/com/simplebuilding/api/WorldPermissions.java
common/src/shared/java/com/simplebuilding/blocks/custom/NetheriteBreakerPistonBlock.java
common/src/shared/java/com/simplebuilding/blocks/custom/ReinforcedPistonBlock.java
common/src/shared/java/com/simplebuilding/blocks/entity/custom/ModHopperBlockEntity.java
common/src/shared/java/com/simplebuilding/platform/PlatformServices.java
common/src/shared/java/com/simplebuilding/util/BuildPermissions.java
common/src/shared/java/com/simplebuilding/util/PlacedAttractors.java
docs/HANDOFF.md
docs/modules/simpletweaks.md
framework/src/main/java/com/simplebuilding/framework/api/Protection.java
mc26_3/framework/java/com/simplebuilding/api/FrameworkProtection.java
modules/simpletweaks/CLAIMS-PLAN.md
modules/simpletweaks/CLAIMS-STAGE4.md
modules/simpletweaks/audit/claims-damage-targets.txt
modules/simpletweaks/fabric/src/main/java/com/simplebuilding/modules/simpletweaks/ClaimsGameTest.java
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/ClaimAutomation.java
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/ClaimTests.java
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/Claims.java
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/EnvironmentClaimTests.java
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/Stage4ClaimTests.java
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimCompoundAccessor.java
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimDamageMixin.java
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimDispenserMixin.java
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimDropperMixin.java
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimEffectMixin.java
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimExplosionMixin.java
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimFireMixin.java
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimFluidMixin.java
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimHopperMixin.java
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimLavaFireMixin.java
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimLiquidMixin.java
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimPickupMixin.java
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimPistonInvoker.java
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimPistonMixin.java
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimPistonResolverMixin.java
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimProjectileMixin.java
modules/simpletweaks/shared/resources/assets/simpletweaks/lang/de_de.json
modules/simpletweaks/shared/resources/assets/simpletweaks/lang/en_us.json
modules/simpletweaks/shared/resources/simpletweaks.claims.mixins.json
modules/simpletweaks/wiki/manual.json
wiki/data/modules.js
wiki/data/simpletweaks.js
wiki/data/simpletweaks.json
```

## Follow-up plan (2026-10-01)
Trace the installed 26.3 sources before closing the known Vanilla gaps. Guard
Crafter dispenseFrom before recipes consume ingredients, checking the facing
cell and the complete resolved destination container. Guard copper-golem direct
pickup and putdown before any slot mutation. Because golems carry items across
land and have no trusted owner, refuse claimed-container interaction rather than
inferring permission from the golem's current position. Wilderness remains usable.
For lightning, check each fire/copper mutation target, including random copper
walk steps; do not skip an entire lightning entity merely because one target is
protected. Add real entrypoint controls for enabled denial, wilderness, disabled
and absent-provider behavior. Keep default OFF and all unverified mod/secondary
paths documented. No client runs or main-checkout builds.

### Follow-up implementation and evidence

- `ClaimCrafterMixin` checks the destination cell and resolved container footprint
  at `dispenseFrom` entry, before recipe output, remainders or input consumption.
- `ClaimTransportMixin` guards real pickup/putdown entrypoints. Both the golem
  footprint and every resolved container half must be unclaimed. Standing on an
  owner's land does not grant a roaming golem that owner's permissions.
- `ClaimLightningMixin` rejects individual ignition candidates before Vanilla
  increments its fire count and rejects copper targets before direct/random-walk
  changes. A refused random candidate also emits no cleaning particles.
- Three new GameTests exercise those actual Vanilla methods, enabled/wilderness/
  disabled/no-provider controls, item conservation, double-container boundaries
  and unknown container refusal. Tests leave claims disabled by default.
- First compile run `2026-10-01T03-08-14Z-2b3d` was red because old copper constants
  no longer exist in 26.3; tests now resolve those blocks by registry ID.
- `2026-10-01T03-11-07Z-b279`: NeoForge 3/3, Fabric 2/3. The Fabric crafter's
  positive dropped-item query failed in a newly loaded neighboring chunk.
  Targeted retry `2026-10-01T03-13-19Z-7b4f` passed 1/1. The test now preloads both
  chunks and waits five server ticks before its synchronous fixtures; full
  regression below must establish the final result.
- All-module wiki generation/check and `modules/simpletweaks/tools/check_data.py`
  passed. No client or owner world was opened. Parent owns the final combined gate.
- Final complete Simple Tweaks suites: `2026-10-01T03-23-37Z-f1a5`, Fabric
  **50/50**, NeoForge **50/50**, **alles gruen: 100/100 bestanden, 0 rot**.
  This includes the new full-container cases and the chunk-warmed crafter fixture,
  all existing Claims, portal, compatibility and module tests. Two workers,
  2 GiB heaps, two active processors. Shared final gate remains with the parent.

## Completion (2026-10-12, branch claude-claims)

Plan: close the remaining stage-4 gaps without changing the default (OFF), keep every
existing regression, add a real-entrypoint GameTest for each new protection path, and
run the module suites on Fabric, NeoForge and Forge 26.3 plus the standalone targets.

Decisions (taken autonomously, documented for the owner):

- **Damage policy.** The previous rule denied *every* non-player attack on claimed
  targets, which made claims mob-immune zones (zombies could not hurt a player standing
  in a claim). Now: a responsible player (resolved through projectiles, clouds and
  lightning causes) needs rights at the whole target footprint; mob attacks and natural
  damage stay Vanilla; unattributed explosions spare claimed non-player entities only
  (players still take explosion damage). PvP against a player standing on someone
  else's claim needs trust there.
- **Mob griefing.** `Level.destroyBlock` with a breaker (Wither, Ravager incl. crop
  trampling, Silverfish wake-up, villager harvest, boat lily pads, modded breakers)
  plus explicit hooks for Enderman take/place, door-breaking Zombies/Vindicators,
  `RemoveBlockGoal`, Silverfish infestation, the Ender Dragon's wall check, farmland
  trampling and turtle eggs. Hostile (`Enemy`) or unattributed causes never change
  claimed land; other mobs only act within the land they stand on (same-owner rule as
  automation), so villagers still farm and sheep still graze inside their owner's claim.
- **Vehicles.** Boats (placed through `use`, not `useOn`) need rights at the boat
  footprint; minecarts already go through the checked `useOn`. Vehicles are entities:
  attacking, riding, leashing and explosion damage were already covered.
- **Copper golem provenance.** Golems carry a persistent entity tag naming the land a
  load came from and may only put it down on that same land. They work inside a claim,
  never carry items out of or into one. Untagged (pre-existing) loads may only be put
  down on the land the golem stands on. Replaces the blanket refusal.
- **Positioned wrappers.** Containers from `WorldlyContainerHolder` blocks (composter,
  modded holders) are remembered with their block position when the hopper resolves
  them, so same-owner hopper/composter automation works instead of failing closed.

Still outside verified coverage (not blockers for the owner's release decision, all
fail closed or are cosmetic): arbitrary mod machines mutating the world directly,
remote capability endpoints, non-plain dispenser behaviors near claims (conservatively
refused), snow golem trails and wither roses (cosmetic additions), lightning
rod redstone from natural lightning, burning entities melting powder snow.
