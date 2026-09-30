# SimpleBuilding server caps (26.3)

All defaults, option paths, labels and tabs stay unchanged. EN/DE tooltips in both
resource locations include the valid range. `TweaksConfig.validate()` clamps existing
values in memory and logs a warning; it does not request a save. Nonfinite floating
values use the existing default. Integer command arguments reject NaN and infinity.
The existing first-join legacy migration remains separate from cap validation.

| Option | Valid range | Default | Reason for the maximum |
| --- | --- | --- | --- |
| spawn.boostStrength | 0.1–1.2 | 0.6 | Twice the normal impulse, with an independent speed ceiling. |
| optimization.xpClumpRadius | 0–8 blocks | 2 | Local orb merging; bounds the spatial scan to a small neighborhood. |
| padTuning.launchpadStrengthMultiplier | 0–2 | 1 | At most twice the existing tier thrust; existing tier capacities remain unchanged. |
| commands.killCommandRadius | 1–256 blocks | 100 | Allows a larger local cleanup without a dimension-wide entity search. |
| spawn.spawnElytraRadius | 1–256 blocks | 25 | A bounded spawn region rather than world-wide free flight/refilling. |
| padTuning.teleporterTier1WarmupTicks | 1–12000 ticks | 1000 | Ten minutes is ample for a deliberate warm-up without extreme counters. |
| padTuning.teleporterTier2WarmupTicks | 1–12000 ticks | 400 | Same ceiling, unchanged tier default. |
| padTuning.teleporterTier3WarmupTicks | 1–12000 ticks | 100 | Same ceiling, unchanged tier default. |
| padTuning.potionPadChargeStepTicks | 1–1200 ticks | 20 | One minute per step; the three-step counter stays small. |
| padTuning.potionPadCooldownFactor | 0–10 | 2 | At most ten effect durations, without unbounded multiplication. |
| balancing.echoSounderJumpCooldownTicks | 0–12000 ticks | 480 | At most ten minutes, with zero still disabling the cooldown. |
| balancing.echoSounderAttemptLockTicks | 0–12000 ticks | 100 | Same bounded timer policy. |
| laserPointer.range | 1–1024 blocks | 512 | Twice the cosmetic default; relay recipients remain limited to 128 blocks and gameplay to server view distance. |

The boost handler keeps the equipment, gliding and charge checks. It refuses
nonfinite charge data, guards strength even if code bypasses load validation, and
caps the resulting total velocity at **3 blocks/tick (60 blocks/second)**. Nonfinite
resulting motion is reset to zero. Each server player can spend at most **three
boosts per 100 server ticks (five seconds)**. The window belongs to the player,
not to the chest item or charge component, so spawn/pad refills and equipment swaps
do not reset it. Wrong-state/empty-charge packets do not spend the budget.

Runtime readers independently cap XP scans, pad tuning, spawn radius, kill radius,
Echo Sounder timers and laser range. `RecipeFilter` recognizes the current
`amethyst_lens` recipe and retains the old `laser_pointer` match for legacy datapacks.

`HardenTests` adds one server case per changed option (excessive value, nonfinite
command refusal and floating-value load fallback), plus default validation,
actual boost-handler spam/refill/window recovery, nonfinite strength/wrong-state,
actual invalid boost commands, XP/launch runtime guards and the recipe regression.
Existing config tests pin every shipped default and name/tooltip/tab/default text.
The Balancing extractor understands the named caps and has regression assertions
for all 13 changed fields. The wiki config table is generated from these tooltips.

26.2 only receives shared-code compilation through the requested `check` gate;
runtime verification is limited to Fabric/NeoForge 26.3 and integration-263.
