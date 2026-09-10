#!/usr/bin/env python3
"""
The counter-check for a sharpened test: break the mod on purpose and require red.

A test that was sharpened to catch a defect has proved nothing until the defect has
been put in and the test has gone red on it. This runs that proof for the client
tests, where a round costs six to eight minutes and therefore has to be planned:

  * every mutation names the file, the exact text to replace, the replacement, the
    client SCRIPT it lands in and the message the sharpened step has to fail with;
  * one round applies at most one mutation per script - two in the same script would
    hide each other, because a script stops at its first failing step - and as many
    scripts in parallel as have a mutation left;
  * after the run every expected message has to be in the log, and NO script may
    have failed that had no mutation - a mutation whose damage spreads further than
    its test claims is reported, not tolerated;
  * the files are restored from git afterwards, which is why the working tree has
    to be clean before this starts.

Usage
    python tools/testrunner/mutations.py --list
    python tools/testrunner/mutations.py --plan                 # rounds, nothing run
    python tools/testrunner/mutations.py --run                  # all rounds
    python tools/testrunner/mutations.py --run --only hopper-glyph-swap,bundle-scale
    python tools/testrunner/mutations.py --run --target client-fabric-262
    python tools/testrunner/mutations.py --p6 --run             # the P6 core-area round (server)
    python tools/testrunner/mutations.py --p6 --run --line 1.21.11   # the same round on the 1.21.11 copy
    python tools/testrunner/mutations.py --p6b --run            # the remaining areas (server)

Results land in testing/mutations/<timestamp>.json and are summarised on stdout.
"""

from __future__ import annotations

import argparse
import datetime as dt
import io
import json
import re
import subprocess
import sys
from dataclasses import dataclass, asdict
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
SHARED = "common/src/shared/java/com/simplebuilding"
CLIENT_MAIN = "src/main/java/com/simplebuilding"


@dataclass(frozen=True)
class Mutation:
    id: str
    file: str
    old: str
    new: str
    script: str
    expect: str
    claim: str
    #: "client" runs a client target and reads its log; "server" runs one server target with the
    #: catalogue id in ``script`` as the filter and reads the JUnit report.
    kind: str = "client"


#: Every entry is one of the 21 client side false greens from testing/audit_falsegreens.json,
#: with the mutation the audit named - not a softer one - and the message the sharpened step
#: now fails with. The order inside a script does not matter; the planner spreads them.
MUTATIONS: list[Mutation] = [
    Mutation("hopper-button-geometry",
             f"{SHARED}/client/gui/NetheriteHopperScreen.java",
             "}).bounds(buttonX, buttonY, 18, 18).build());",
             "}).bounds(this.leftPos - 40, this.topPos - 40, 30, 30).build());",
             "hud-and-tooltip", "The hopper filter button is 30x30",
             "the filter button is 18x18 right of the five slots"),
    Mutation("hopper-glyph-swap",
             f"{SHARED}/client/gui/NetheriteHopperScreen.java",
             'String text = (mode == HopperFilterMode.WHITELIST) ? "✔" : "T";',
             'String text = (mode == HopperFilterMode.WHITELIST) ? "T" : "✔";',
             "hud-and-tooltip", "filter button draws [T]",
             "whitelist draws the check mark, type match the T"),
    Mutation("hopper-overlay-colour",
             f"{SHARED}/client/gui/NetheriteHopperScreen.java",
             "context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x60FFAA00);",
             "context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x60FF00FF);",
             "hud-and-tooltip", "has no 16x16 overlay in 0x60FFAA00",
             "a filtered slot carries the orange overlay"),
    Mutation("hopper-ghost-in-occupied-slot",
             f"{SHARED}/client/gui/NetheriteHopperScreen.java",
             "                    if (slot.getItem().isEmpty()) {\n                        context.item(ghostStack, slotX, slotY);",
             "                    if (true) {\n                        context.item(ghostStack, slotX, slotY);",
             "hud-and-tooltip", "painted over a real item",
             "the ghost icon stays out of an occupied slot"),
    Mutation("hopper-ghost-count",
             f"{SHARED}/blocks/entity/custom/ModHopperBlockEntity.java",
             "                ItemStack copy = stack.copy();\n                copy.setCount(1);\n                ghostItems.set(slot, copy);\n            }\n        }\n    }\n}",
             "                ItemStack copy = stack.copy();\n                ghostItems.set(slot, copy);\n            }\n        }\n    }\n}",
             "hud-and-tooltip", "instead of exactly one diamond",
             "the client side ghost item is clamped to one"),
    Mutation("hopper-immediate-feedback",
             f"{SHARED}/client/gui/NetheriteHopperScreen.java",
             "                if (this.menu.getBlockEntity() instanceof ModHopperBlockEntity be) {\n                    be.setGhostItemClient(hoveredSlot.getContainerSlot(), cursorStack);\n                }",
             "",
             "hud-and-tooltip", "still has no ghost item in slot 0",
             "the screen writes the ghost item locally, in the same tick"),
    Mutation("rangefinder-volume",
             f"{SHARED}/client/gui/RangefinderHudOverlay.java",
             "            int dx = Math.abs(pos1.getX() - pos2.getX()) + 1;\n            int dy = Math.abs(pos1.getY() - pos2.getY()) + 1;\n            int dz = Math.abs(pos1.getZ() - pos2.getZ()) + 1;",
             "            int dx = Math.abs(pos1.getX() - pos2.getX());\n            int dy = Math.abs(pos1.getY() - pos2.getY());\n            int dz = Math.abs(pos1.getZ() - pos2.getZ());",
             "hud-and-tooltip", "The rangefinder HUD does not say [Volume: 80",
             "both ends of the selection count"),
    Mutation("rangefinder-offhand",
             f"{SHARED}/client/gui/RangefinderHudOverlay.java",
             "        if (!hasOctant) {\n            stack = client.player.getOffhandItem();\n            if (stack.getItem() instanceof OctantItem) {\n                hasOctant = true;\n            }\n        }",
             "",
             "hud-and-tooltip", "With the octant in the OFF hand the rangefinder HUD draws nothing",
             "an off hand octant shows the HUD too"),
    Mutation("bundle-scale",
             f"{CLIENT_MAIN}/SimplebuildingClient.java",
             "float scale = (float) reinforcedData.maxCapacity() / 64.0f;",
             "float scale = (float) reinforcedData.maxCapacity() / 32.0f;",
             "hud-and-tooltip", "does not draw like a component scaled by maxCapacity / 64",
             "the tooltip bar scale is the capacity in stacks"),
    Mutation("wand-needs-block-hit",
             f"{SHARED}/client/render/BuildingWandPreviewRenderer.java",
             "if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK)",
             "if (!(hit instanceof BlockHitResult blockHit))",
             "building-wand-preview", "must not preview anything while the crosshair finds no block",
             "a miss is a BlockHitResult too, and draws nothing"),
    Mutation("wand-ghost-centred",
             f"{SHARED}/client/render/BuildingWandPreviewRenderer.java",
             "REPLACED_BY_PATCH_FUNCTION",
             "",
             "building-wand-preview", "The ghost preview is not centred",
             "each ghost shrinks around its block centre"),
    Mutation("settings-key-offhand",
             f"{CLIENT_MAIN}/SimplebuildingClient.java",
             "ItemStack stack = client.player.getMainHandItem();\n                    if (stack.getItem() instanceof OctantItem) {",
             "ItemStack stack = client.player.getMainHandItem().isEmpty() ? client.player.getOffhandItem() : client.player.getMainHandItem();\n                    if (stack.getItem() instanceof OctantItem) {",
             "client-bootstrap", "for an enchanted building wand in the off hand",
             "the settings key reads the main hand only"),
    Mutation("settings-key-netherite-only",
             f"{CLIENT_MAIN}/SimplebuildingClient.java",
             "} else if (stack.getItem() instanceof BuildingWandItem) {",
             "} else if (stack.is(com.simplebuilding.items.ModItems.NETHERITE_BUILDING_WAND)) {",
             "client-bootstrap", "The building wand settings for the copper wand never opened",
             "every wand tier opens the screen"),
    Mutation("octant-scroll-any-item",
             f"{SHARED}/mixin/client/MouseMixin.java",
             "if (client.player.getMainHandItem().getItem() instanceof OctantItem) {",
             "if (true) {",
             "client-bootstrap", "Scrolling with Control while holding STONE",
             "the wheel is only intercepted for an octant"),
    Mutation("octant-plain-only",
             f"{CLIENT_MAIN}/SimplebuildingClient.java",
             "                    if (stack.getItem() instanceof OctantItem) {",
             "                    if (stack.is(com.simplebuilding.items.ModItems.OCTANT)) {",
             "client-bootstrap", "The octant manager for a coloured octant never opened",
             "every octant colour opens the manager"),
    Mutation("pick-creative-guard",
             f"{SHARED}/mixin/client/MinecraftClientMixin.java",
             "        if (this.player.isCreative()) return;\n",
             "",
             "client-bootstrap", "In creative the pick took stone out of the Master Builder bundle",
             "the pick stays out of it in creative"),
    Mutation("space-key-every-tick",
             f"{CLIENT_MAIN}/SimplebuildingClient.java",
             "                if (isJumpPressed != wasJumpPressed) {\n                    ClientNetworking.send(new SpaceKeyPayload(isJumpPressed));\n                    wasJumpPressed = isJumpPressed;\n                }",
             "                ClientNetworking.send(new SpaceKeyPayload(isJumpPressed));\n                wasJumpPressed = isJumpPressed;",
             "client-bootstrap", "SpaceKeyPayloads instead of one",
             "the key state is sent only when it changes"),
    Mutation("hammer-should-break",
             f"{SHARED}/client/render/BlockHighlightRenderer.java",
             "REPLACED_BY_PATCH_FUNCTION",
             "",
             "block-highlight", "did not lose a box when one neighbour became oak planks",
             "only blocks the hammer would break get a box"),
    Mutation("strip-miner-correct-tool",
             f"{SHARED}/client/render/MultiBlockBreakingSupport.java",
             "} else if (stack.getItem().isCorrectToolForDrops(stack, mainState) && sneaking) {",
             "} else if (sneaking) {",
             "multi-block-breaking", "stone Vein Miner pickaxe produced a breaking crack",
             # Named for the Vein Miner case since 2026-09-10: the Strip Miner case the audit
             # pointed at stays green under this mutation, because getStripMinerBlocks stops at
             # the first block the tool cannot mine - an equivalent mutant there, not a weak test.
             "sneaking alone is not enough; the tool has to fit"),
    Mutation("hopper-ordinal-bounds",
             f"{SHARED}/screen/NetheriteHopperScreenHandler.java",
             "        if (ordinal >= 0 && ordinal < HopperFilterMode.values().length) {\n            return HopperFilterMode.values()[ordinal];\n        }\n        return HopperFilterMode.NONE;",
             "        return HopperFilterMode.values()[ordinal];",
             "hopper_game_test_the_mode_delegate_reads_and_writes_the_filter_mode",
             # The JUnit report carries the exception's message, not its class: "Index 3 out of
             # bounds for length 3" is what an ArrayIndexOutOfBoundsException says there.
             "out of bounds for length",
             "an ordinal off the wire falls back to Disabled instead of crashing the screen",
             kind="server"),
    Mutation("hopper-pickup-only",
             f"{SHARED}/screen/ModHopperScreenHandler.java",
             "                if (actionType == ContainerInput.PICKUP) {\n                    blockEntity.setGhostItem(slotIndex, cursor.isEmpty() ? ItemStack.EMPTY : cursor);\n                    // Abbrechen, damit Item nicht wirklich reingelegt wird\n                    return; \n                }",
             "                blockEntity.setGhostItem(slotIndex, cursor.isEmpty() ? ItemStack.EMPTY : cursor);\n                return;",
             "hopper_game_test_hopper_menu_opens_on_use_and_filter_clicks_never_store_the_item",
             "rewrote filter slot 0",
             "only a plain pickup click sets the filter; swaps and drags fall through",
             kind="server"),
    Mutation("survival-zero-fields",
             f"{SHARED}/mixin/SurvivalTracerMixin.java",
             "new SurvivalSyncPayload(currentDist, currentTime, totalHostileKills, totalPassiveKills, currentDamage)",
             "new SurvivalSyncPayload(0, currentTime, totalPassiveKills, totalHostileKills, 0)",
             "smoke", "SurvivalSyncPayload carries the wrong numbers",
             "every field of the sync carries its own number"),
]

#: P6 (testing/PLAN.md): the systematic round for the core areas - mining enchantments, the
#: tools, the gravity blocks and the trim effects. These are not answers to a known false green;
#: each is the kind of one-line slip a refactoring makes (a constant nudged, a guard dropped, a
#: branch flattened), placed in the mod's own source, with the one server test whose sentence
#: has to name it. What P6 asks is not "does the suite have a test for this" but "would the
#: suite NOTICE": a mutation that stays green here is a finding, and the runner reports it as
#: one instead of forgiving it.
#:
#: Proved on the 26.2 line only. The 1.21.11 test bodies are the translated copy of these
#: (port_tests_to_1_21_11.py --drift keeps them within twelve normalised lines, or within an
#: explained difference for the three classes in its DRIFT_EXPLAINED), so the same sentence is
#: there; what differs is the Minecraft underneath, which a mutation of the mod's source does not
#: touch.
P6_MUTATIONS: list[Mutation] = [
    # --- mining enchantments -------------------------------------------------------------
    Mutation("strip-depth-table",
             f"{SHARED}/util/MiningUtils.java",
             "return (level == 3) ? 4 : level;",
             "return level;",
             "vein_and_strip_miner_game_test_strip_miner_tunnels_along_the_facing_and_refunds_durability_through_the_block_break_event",
             "Expected block Air: got Stone",
             "Strip Miner III digs four deep, not three - the one depth table",
             kind="server"),
    Mutation("strip-refund-off",
             f"{SHARED}/util/StripMinerUsageEvent.java",
             "int damageRefund = (brokenBlocks + 1) / 3;",
             "int damageRefund = 0;",
             "vein_and_strip_miner_game_test_strip_miner_tunnels_along_the_facing_and_refunds_durability_through_the_block_break_event",
             "Strip Miner durability refund for the level II tunnel",
             "the tunnel refunds one point of durability per three blocks",
             kind="server"),
    Mutation("strip-sneak-gate",
             f"{SHARED}/util/StripMinerUsageEvent.java",
             "        if (!player.isShiftKeyDown()) {\n            return true;\n        }\n",
             "",
             "vein_and_strip_miner_game_test_strip_miner_tunnels_along_the_facing_and_refunds_durability_through_the_block_break_event",
             "Strip Miner fired without the player sneaking",
             "Strip Miner only fires while sneaking",
             kind="server"),
    Mutation("strip-speed-divisor-two",
             f"{SHARED}/mixin/PlayerEntityMixin.java",
             "case 2 -> divisor = 3.0f;",
             "case 2 -> divisor = 2.0f;",
             "mining_enchantment_game_test_strip_miner_divides_the_player_destroy_speed_per_level",
             "Strip Miner 2 left the destroy speed at",
             "Strip Miner II divides the destroy speed by three",
             kind="server"),
    Mutation("mining-direction-threshold",
             f"{SHARED}/util/MiningUtils.java",
             "if (pitch > 60) return Direction.DOWN;",
             "if (pitch > 70) return Direction.DOWN;",
             "vein_and_strip_miner_game_test_strip_miner_tunnels_along_the_facing_and_refunds_durability_through_the_block_break_event",
             "MiningUtils stopped digging downwards just past pitch 60",
             "the downward threshold sits at pitch 60 exactly",
             kind="server"),
    Mutation("vein-budget-level-one",
             f"{SHARED}/util/MiningUtils.java",
             "case 1 -> 3;",
             "case 1 -> 6;",
             "vein_and_strip_miner_game_test_vein_miner_breaks_the_whole_vein_through_the_block_break_event",
             "Vein Miner block budget at level I",
             "Vein Miner I has a budget of three blocks including the origin",
             kind="server"),
    Mutation("vein-ore-list-emerald",
             f"{SHARED}/util/MiningUtils.java",
             "state.is(BlockItemTags.DIAMOND_ORES.block()) ||\n                state.is(BlockItemTags.EMERALD_ORES.block());",
             "state.is(BlockItemTags.DIAMOND_ORES.block());",
             "vein_and_strip_miner_game_test_vein_miner_refuses_non_ores_and_too_weak_pickaxes_and_diverges_from_the_highlight_on_quartz",
             "Vein Miner refused a vein of Emerald Ore",
             "every one of the eight ore tags is in the one ore list",
             kind="server"),
    Mutation("vein-ore-gate",
             f"{SHARED}/util/MiningUtils.java",
             "        if (isPickaxe && !isOre(targetState)) return Collections.emptyList();\n",
             "",
             "vein_and_strip_miner_game_test_vein_miner_refuses_non_ores_and_too_weak_pickaxes_and_diverges_from_the_highlight_on_quartz",
             # The hook asks MiningUtils.isOre on its own before it asks for the list, so the stone
             # case stays intact; what sees the list's gate is the preview parity check on quartz.
             "the crack preview outlined a quartz vein the hook does not mine",
             "a pickaxe vein mines ores only, never plain stone",
             kind="server"),
    Mutation("versatility-hammer-step",
             f"{SHARED}/util/VersatilityUsageEvent.java",
             "score += 2000f;",
             "score += 1000f;",
             "mining_enchantment_game_test_versatility_prefers_the_hammer_and_ranks_the_chisel_last",
             "Versatility did not put the sledgehammer first",
             "the sledgehammer outranks every pickaxe regardless of speed",
             kind="server"),
    # --- tools -----------------------------------------------------------------------------
    Mutation("hammer-bedrock-guard",
             f"{SHARED}/util/SledgehammerUtils.java",
             "if (targetState.isAir() || targetState.getDestroySpeed(world, pos) < 0.0F) {",
             "if (targetState.isAir()) {",
             "sledgehammer_game_test_sledgehammer_field_skips_air_gaps_and_unbreakable_blocks",
             "Expected block Bedrock: got Air",
             "an Override II hammer still leaves bedrock standing",
             kind="server"),
    Mutation("hammer-air-gap-billed",
             f"{SHARED}/util/SledgehammerUtils.java",
             "if (targetState.isAir() || targetState.getDestroySpeed(world, pos) < 0.0F) {",
             "if (targetState.getDestroySpeed(world, pos) < 0.0F) {",
             "sledgehammer_game_test_sledgehammer_field_skips_air_gaps_and_unbreakable_blocks",
             "was billed as if it had been mined",
             "a hole in the face costs no durability",
             kind="server"),
    Mutation("hammer-flat-cost",
             f"{SHARED}/util/SledgehammerUsageEvent.java",
             "int damageAmount = isSuitable ? 1 : 2;",
             "int damageAmount = 1;",
             "sledgehammer_game_test_sledgehammer_bills_one_durability_per_block_and_two_for_the_wrong_tool",
             "durability the mod charged for an Override II hammer on glass",
             "the wrong tool class costs two points per block",
             kind="server"),
    Mutation("hammer-charge-clamp",
             f"{SHARED}/items/custom/SledgehammerItem.java",
             "return Math.clamp(time, 4, 40);",
             "return Math.clamp(time, 4, 50);",
             "sledgehammer_game_test_sledgehammer_charge_time_shortens_with_material_and_efficiency",
             "charge time in ticks of the stone",
             "the charge time is held at 40 ticks at the top",
             kind="server"),
    Mutation("hammer-efficiency-factor",
             f"{SHARED}/items/custom/SledgehammerItem.java",
             "float factor = speed + (efficiencyLevel * 5.0f);",
             "float factor = speed + (efficiencyLevel * 4.0f);",
             "sledgehammer_game_test_sledgehammer_charge_time_shortens_with_material_and_efficiency",
             "charge time of a diamond hammer with Efficiency V",
             "every Efficiency level adds five to the speed factor",
             kind="server"),
    Mutation("radius-ignores-sneak",
             f"{SHARED}/items/custom/SledgehammerItem.java",
             "int range = baseRange + ((!isPlayerSneaking && radiusKey.isPresent())",
             "int range = baseRange + ((radiusKey.isPresent())",
             "enchantment_effect_game_test_radius_widens_the_sledgehammer_face_and_sneaking_suppresses_it",
             "Expected block Stone: got Air",
             "sneaking suppresses the Radius widening",
             kind="server"),
    Mutation("rotator-rim-margin",
             f"{SHARED}/items/custom/RotatorItem.java",
             "getRimDirection(context, 0.125)",
             "getRimDirection(context, 0.124)",
             "rotator_game_test_rim_is_the_outer_eighth_of_every_face_and_nowhere_inside",
             # The part both lines say: on 26.2 the 0.124 probe is the first to see the margin, on
             # 1.21.11 the click at x = 0.124 arrives a hair below 0.124 (float on the way) and the
             # 0.124999 probe is the first. Same boundary, same wrong axis, different probe.
             "inside the rim: a log along y should have ended up along x, but it lies along z",
             "the rim is exactly the outer eighth",
             kind="server"),
    Mutation("chisel-half-speed",
             f"{SHARED}/items/custom/ChiselItem.java",
             "return (materialSpeed + efficiencyBonus) * 0.5f;",
             "return (materialSpeed + efficiencyBonus) * 0.6f;",
             "chisel_game_test_chisel_mines_at_half_material_speed",
             "stone chisel on stone: mining speed is",
             "the chisel mines at exactly half its material speed",
             kind="server"),
    Mutation("chisel-fast-chiseling-factor",
             f"{SHARED}/items/custom/ChiselItem.java",
             "(1.0f - (fastChiselingLevel * 0.3f))",
             "(1.0f - (fastChiselingLevel * 0.25f))",
             "building_enchantment_game_test_fast_chiseling_shortens_the_cooldown_and_speeds_up_mining",
             "Fast Chiseling I did not take 30% off the cooldown",
             "Fast Chiseling takes 30 percent per level off the cooldown",
             kind="server"),
    Mutation("wand-radius-cap",
             f"{SHARED}/items/custom/BuildingWandItem.java",
             "int maxTierRadius = (this.maxDiameter - 1) / 2;\n        int userRadius = nbt.contains(\"SettingsRadius\") ? nbt.getIntOr(\"SettingsRadius\", maxTierRadius) : maxTierRadius;\n        if (userRadius > maxTierRadius) userRadius = maxTierRadius;",
             "int maxTierRadius = (this.maxDiameter - 1) / 2;\n        int userRadius = nbt.contains(\"SettingsRadius\") ? nbt.getIntOr(\"SettingsRadius\", maxTierRadius) : maxTierRadius;\n        if (userRadius > maxTierRadius + 1) userRadius = maxTierRadius + 1;",
             "building_wand_game_test_wand_tier_caps_the_radius_setting_and_sizes_the_plane",
             "the copper wand did not build the square its own maximum diameter of",
             "the tier's diameter caps the radius the player asked for",
             kind="server"),
    # --- gravity blocks and pistons --------------------------------------------------------
    Mutation("levitate-delay",
             f"{SHARED}/blocks/custom/LevitatingBlock.java",
             "private static final int DELAY_AFTER_PLACE = 2;",
             "private static final int DELAY_AFTER_PLACE = 3;",
             "gravity_block_game_test_levitating_sand_leaves_on_vanillas_schedule_and_rises_on_its_curve",
             "the tick levitating sand lifts off",
             "levitating sand leaves on vanilla's two tick schedule",
             kind="server"),
    Mutation("levitate-drag",
             f"{SHARED}/entity/LevitatingBlockEntity.java",
             "private static final double AIR_DRAG = 0.98D;",
             "private static final double AIR_DRAG = 0.97D;",
             "gravity_block_game_test_levitating_sand_leaves_on_vanillas_schedule_and_rises_on_its_curve",
             "the rise should be the exact mirror of vanilla sand falling",
             "the rise mirrors vanilla's fall curve exactly",
             kind="server"),
    Mutation("piston-limit-seventeen",
             f"{SHARED}/mixin/PistonHandlerMixin.java",
             "return 18; // Das neue Limit",
             "return 17; // Das neue Limit",
             "gravity_block_game_test_reinforced_piston_moves_eighteen_blocks_while_the_netherite_one_keeps_vanillas_twelve",
             "Expected property extended to be true",
             "the reinforced piston moves eighteen blocks, not seventeen",
             kind="server"),
    Mutation("piston-limit-any-mod-piston",
             f"{SHARED}/mixin/PistonHandlerMixin.java",
             "if (state.is(ModBlocks.REINFORCED_PISTON)) {",
             "if (state.is(ModBlocks.REINFORCED_PISTON) || state.is(ModBlocks.NETHERITE_PISTON)) {",
             "gravity_block_game_test_reinforced_piston_moves_eighteen_blocks_while_the_netherite_one_keeps_vanillas_twelve",
             "Expected property extended to be false",
             "the netherite piston keeps vanilla's twelve",
             kind="server"),
    Mutation("piston-break-factor",
             f"{SHARED}/blocks/custom/NetheriteBreakerPistonBlock.java",
             "float breakThreshold = (power / 15.0f) * 50.0f;",
             "float breakThreshold = (power / 15.0f) * 60.0f;",
             "gravity_block_game_test_netherite_piston_breaks_only_what_the_signal_strength_can_afford",
             "Expected block Block of Netherite: got",
             "signal 14 cannot afford a netherite block (hardness 50)",
             kind="server"),
    # --- trim effects ----------------------------------------------------------------------
    Mutation("trim-damage-floor",
             f"{SHARED}/util/TrimEffectUtil.java",
             "if (multiplier < 0.1f) multiplier = 0.1f;",
             "if (multiplier < 0.0f) multiplier = 0.0f;",
             "trim_effect_game_test_damage_reduction_follows_the_pattern_and_keeps_its_floor",
             "the 10% damage floor is gone",
             "no trim set reduces damage below ten percent",
             kind="server"),
    Mutation("trim-ward-conditional",
             f"{SHARED}/util/TrimEffectUtil.java",
             "        multiplier -= calculateReduction(entity, \"ward\", 0.03f, progressMult);",
             "        if (source.is(DamageTypeTags.IS_FIRE)) multiplier -= calculateReduction(entity, \"ward\", 0.03f, progressMult);",
             "trim_effect_game_test_damage_reduction_follows_the_pattern_and_keeps_its_floor",
             "a full ward set against a generic hit",
             "ward is the unconditional reduction",
             kind="server"),
    Mutation("trim-jump-threshold",
             f"{SHARED}/util/TrimEffectUtil.java",
             "if (jumpScore >= 8.0) amplifier = 1;",
             "if (jumpScore >= 7.0) amplifier = 1;",
             "trim_effect_game_test_astralit_jump_boost_crosses_its_thresholds_on_tick",
             "wrong Jump Boost level just below the second threshold",
             "Jump Boost II starts at a score of 8.0 exactly",
             kind="server"),
]

#: P6b (2026-09-11): the same question for the areas P6 left out - hoppers, furnaces, containers,
#: trim effects and wiring, trades/loot/migration, the measuring tools, dynamic light, item
#: frames, the void rescue, the wand's internals. Designed by one reviewer per area and each
#: checked by a second against anchor, non-equivalence and the sentence the named test produces;
#: what the run then says about them is in testing/mutations/.
P6B_MUTATIONS: list[Mutation] = [
    # --- hoppers -----------------------------------------------------------------------------
    Mutation('hopper-enabled-guard-off',
             'common/src/shared/java/com/simplebuilding/blocks/entity/custom/ModHopperBlockEntity.java',
             '        if (!blockEntity.needsCooldown() && state.getValue(HopperBlock.ENABLED)) {',
             '        if (!blockEntity.needsCooldown()) {',
             'hopper_game_test_redstone_power_stops_every_hopper_transfer',
             'items a powered hopper pushed into the chest below it',
             'a redstone powered hopper moves nothing - the ENABLED check in insertAndExtract is the only lock the mod hopper has',
             kind="server"),
    Mutation('hopper-exact-match-flattened-to-type',
             'common/src/shared/java/com/simplebuilding/blocks/entity/custom/ModHopperBlockEntity.java',
             '                return ItemStack.isSameItemSameComponents(stack, ghost);',
             '                return stack.is(ghost.getItem());',
             'hopper_and_trim_game_test_hopper_filter_modes_gate_what_may_enter',
             'Exact Match ignored the components and accepted a renamed stone',
             'Exact Match compares item and components; Type Match compares the item only - the two branches of canPlaceItem are not the same rule',
             kind="server"),
    Mutation('hopper-learns-with-filter-off',
             'common/src/shared/java/com/simplebuilding/blocks/entity/custom/ModHopperBlockEntity.java',
             '        if (!stack.isEmpty() && currentFilterMode != HopperFilterMode.NONE) {',
             '        if (!stack.isEmpty()) {',
             'hopper_game_test_the_filter_learns_its_ghost_from_the_first_item_that_is_placed',
             'a hopper with its filter disabled learned the filter item',
             'a slot only teaches itself a filter item while a filter mode is on; a plain hopper never starts filtering because something entered it',
             kind="server"),
    Mutation('hopper-learned-ghost-keeps-count',
             'common/src/shared/java/com/simplebuilding/blocks/entity/custom/ModHopperBlockEntity.java',
             '                ItemStack ghost = stack.copy();\n                ghost.setCount(1);\n                ghostItems.set(slot, ghost);',
             '                ItemStack ghost = stack.copy();\n                ghostItems.set(slot, ghost);',
             'hopper_game_test_the_filter_learns_its_ghost_from_the_first_item_that_is_placed',
             'count of a filter item the hopper taught itself from a stack of 12',
             'a filter item is a one-item placeholder in every copy of the rule - setGhostItemInternal, setGhostItemClient and the learning path in setItem all clamp to one',
             kind="server"),
    Mutation('hopper-menu-slot-range-off-by-one',
             'common/src/shared/java/com/simplebuilding/screen/ModHopperScreenHandler.java',
             '        if (slotIndex >= 0 && slotIndex < 5 && blockEntity != null) {',
             '        if (slotIndex >= 0 && slotIndex <= 5 && blockEntity != null) {',
             'hopper_game_test_hopper_menu_opens_on_use_and_filter_clicks_never_store_the_item',
             'the click on a player inventory slot was swallowed by the filter',
             "the filter click only intercepts the five hopper slots; slot 5 is the first player inventory slot and belongs to vanilla - the menu's literal 5 and the block entity's own 0..4 guard have to agree",
             kind="server"),
    Mutation('hopper-stray-slot-broadcast',
             'common/src/shared/java/com/simplebuilding/blocks/entity/custom/ModHopperBlockEntity.java',
             '        if (!setGhostItemInternal(slot, stack)) {\n            return;\n        }',
             '        setGhostItemInternal(slot, stack);',
             'hopper_game_test_filter_items_are_stored_as_single_count_copies_and_can_be_cleared',
             'which is outside the five it can store',
             "a filter slot the hopper refused to write is not announced to the tracking clients - the broadcast stays behind the store's own result",
             kind="server"),
    # --- furnaces ----------------------------------------------------------------------------
    Mutation('furnace-netherite-step-four',
             'common/src/shared/java/com/simplebuilding/blocks/entity/custom/ModFurnaceBlockEntity.java',
             '            if (state.is(ModBlocks.NETHERITE_FURNACE)) {\n                extraTicks = 3;',
             '            if (state.is(ModBlocks.NETHERITE_FURNACE)) {\n                extraTicks = 4;',
             'furnace_game_test_one_coal_feeds_several_netherite_smelts_where_vanilla_manages_one',
             'ingots the netherite furnace finished in 230 ticks on a single piece',
             "the netherite furnace adds exactly three points of cooking progress per tick on top of vanilla's one - not more",
             kind="server"),
    Mutation('furnace-cap-lands-on-total',
             'common/src/shared/java/com/simplebuilding/blocks/entity/custom/ModFurnaceBlockEntity.java',
             '                    newCookTime = totalTime - 1;',
             '                    newCookTime = totalTime;',
             'furnace_game_test_boost_never_pushes_cooking_progress_to_the_full_cook_time',
             'the boost has to stop one tick short of the total',
             'the boost caps cooking progress at totalTime - 1 and leaves the last step onto the total to vanilla',
             kind="server"),
    Mutation('furnace-boost-without-fire-or-cook',
             'common/src/shared/java/com/simplebuilding/blocks/entity/custom/ModFurnaceBlockEntity.java',
             '        if (isBurning && cookTime > 0 && totalTime > 0) {',
             '        if (totalTime > 0) {',
             'furnace_game_test_boost_only_runs_while_the_furnace_burns_and_cooks',
             'highest cooking progress of a reinforced furnace without any fuel',
             'an unlit furnace with nothing cooking never gains progress from the boost',
             kind="server"),
    Mutation('smoker-boost-ignores-fire',
             'common/src/shared/java/com/simplebuilding/blocks/entity/custom/ModSmokerBlockEntity.java',
             '        if (isBurning && cookTime > 0 && totalTime > 0) {',
             '        if (cookTime > 0 && totalTime > 0) {',
             'furnace_game_test_progress_cools_down_at_the_vanilla_rate_once_the_fuel_is_spent',
             'ModSmokerBlockEntity#tick is still adding progress to a device that is not burning',
             "a cold smoker gets no boost - its progress decays at vanilla's two per tick, in the smoker's own copy of the guard",
             kind="server"),
    Mutation('blast-furnace-title-checks-furnace-tier',
             'common/src/shared/java/com/simplebuilding/blocks/entity/custom/ModBlastFurnaceBlockEntity.java',
             '        return Component.translatable(this.getBlockState().is(ModBlocks.NETHERITE_BLAST_FURNACE)',
             '        return Component.translatable(this.getBlockState().is(ModBlocks.NETHERITE_FURNACE)',
             'furnace_game_test_every_tier_opens_the_menu_of_its_vanilla_counterpart',
             "the netherite blast furnace's container title is container.simplebuilding.reinforced_blast_furnace instead of container.simplebuilding.netherite_blast_furnace",
             "each of the three block entities tells its tiers apart by its OWN family's netherite block - a copy-paste of the furnace's check into the blast furnace names every netherite blast furnace after its reinforced sibling",
             kind="server"),
    Mutation('smoker-block-opens-furnace-entities-only',
             'common/src/shared/java/com/simplebuilding/blocks/custom/ModSmokerBlock.java',
             '        if (blockEntity instanceof ModSmokerBlockEntity) {',
             '        if (blockEntity instanceof com.simplebuilding.blocks.entity.custom.ModFurnaceBlockEntity) {',
             'furnace_game_test_every_tier_opens_the_menu_of_its_vanilla_counterpart',
             'using a reinforced smoker opened no menu at all',
             'a smoker block opens the menu of its own block entity - the instanceof in openContainer is per family, not copied from the furnace block',
             kind="server"),
    # --- bundles-quiver ----------------------------------------------------------------------
    Mutation('drawer-visuals-offset',
             'common/src/shared/java/com/simplebuilding/items/custom/ReinforcedBundleItem.java',
             '                        // Gleiche Formel wie oben: (16 + level) / 8\n                        Fraction drawerBonus = Fraction.getFraction(16 + level, 8);',
             '                        // Gleiche Formel wie oben: (16 + level) / 8\n                        Fraction drawerBonus = Fraction.getFraction(8 + level, 8);',
             'reinforced_bundle_game_test_bar_and_tooltip_read_the_same_capacity_the_filling_uses',
             'bar width of a Drawer I bundle holding',
             'the bar and tooltip formula (getMaxCapacityForVisuals) uses the same Drawer offset as the filling formula (getMaxCapacity)',
             kind="server"),
    Mutation('quiver-base-keeps-bundle-bonus',
             'common/src/shared/java/com/simplebuilding/items/custom/QuiverItem.java',
             '        return getTierCapacityMultiplier(item);',
             '        return getTierCapacityMultiplier(item).multiplyBy(Fraction.getFraction(3, 2));',
             'quiver_game_test_capacity_drops_the_bundle_bonus_and_follows_tier_and_enchantments',
             'arrows a plain quiver takes',
             "a quiver does not get the bundle's 1.5x - it holds exactly one vanilla stack per tier",
             kind="server"),
    Mutation('quiver-consume-hotbar-short',
             'common/src/shared/java/com/simplebuilding/items/custom/QuiverItem.java',
             '        for (int i = 0; i < 9; i++) {\n            if (tryConsumeArrow(player.getInventory().getItem(i))) return;',
             '        for (int i = 0; i < 8; i++) {\n            if (tryConsumeArrow(player.getInventory().getItem(i))) return;',
             'quiver_game_test_bow_consumes_one_arrow_from_the_quiver_that_supplied_it',
             'arrows left in the hotbar quiver after it paid from hotbar slot 8',
             'the consuming walk covers the whole hotbar 0..8, the same range the finding walk covers',
             kind="server"),
    Mutation('quiver-filter-pinned-to-primary',
             'common/src/shared/java/com/simplebuilding/items/custom/QuiverItem.java',
             '        if (clickAction == getInsertClick() && !cursorStack.isEmpty()) {',
             '        if (clickAction == ClickAction.PRIMARY && !cursorStack.isEmpty()) {',
             'quiver_game_test_arrow_filter_holds_for_clicks_and_the_inverted_binding_slips_past_it',
             'the same from the other side - stone on the cursor, right clicked onto the quiver with',
             "the quiver's arrow filter on the cursor click path follows the configured insert click, not a fixed PRIMARY",
             kind="server"),
    Mutation('funnel-one-takes-foreign',
             'common/src/shared/java/com/simplebuilding/items/custom/ReinforcedBundleItem.java',
             '            return false; // Nicht im Bundle, liegen lassen',
             '            return true; // Nicht im Bundle, liegen lassen',
             'storage_enchantment_game_test_funnel_filter_decides_what_the_touch_sweeps_up',
             'a Funnel I bundle holding only stone swallowed a dirt drop; the filter is gone',
             'Funnel I only sweeps up kinds the bundle already holds; a kind it does not hold is left to vanilla',
             kind="server"),
    Mutation('bow-creative-pays',
             'common/src/shared/java/com/simplebuilding/mixin/BowItemMixin.java',
             '        if (!cir.getReturnValue() || !(user instanceof Player player) || player.getAbilities().instabuild) {',
             '        if (!cir.getReturnValue() || !(user instanceof Player player)) {',
             'quiver_game_test_bow_shoots_from_the_quiver_and_bills_it_outside_creative_only',
             'arrows left in the quiver after a creative shot - creative must not pay',
             'a creative player shoots out of the quiver without the quiver being billed',
             kind="server"),
    # --- trims -------------------------------------------------------------------------------
    Mutation('trim-material-gate-copy',
             'common/src/shared/java/com/simplebuilding/util/TrimEffectUtil.java',
             '        if (entity instanceof TrimBenefitUser user && !user.simplebuilding$areTrimBenefitsEnabled()) {\n            return 0;\n        }\n        int count = 0;',
             '        int count = 0;',
             'trim_effect_game_test_benefit_gate_switches_every_trim_effect_off',
             'materials were still counted with the trim benefits switched off',
             'the benefit switch gates material counting as well as pattern counting - getMaterialCount carries its own copy of the gate',
             kind="server"),
    Mutation('trim-snout-narrowed',
             'common/src/shared/java/com/simplebuilding/util/TrimEffectUtil.java',
             '        if (source.is(DamageTypeTags.IS_FIRE)) multiplier -= calculateReduction(entity, "snout", 0.05f, progressMult);',
             '        if (source.is(DamageTypes.ON_FIRE)) multiplier -= calculateReduction(entity, "snout", 0.05f, progressMult);',
             'trim_bonus_game_test_tag_keyed_patterns_cover_the_whole_damage_family',
             'a full snout set in lava - snout is keyed on the fire TAG',
             'snout answers to the whole fire damage family (the IS_FIRE tag), not to being on fire alone - the same tag its quartz material twin reads eleven lines further down',
             kind="server"),
    Mutation('trim-combat-damage-clamp',
             'common/src/shared/java/com/simplebuilding/util/TrimMultiplierLogic.java',
             '        int damageSinceDeath = Math.max(0, curDamage - baseDamage);',
             '        int damageSinceDeath = curDamage - baseDamage;',
             'trim_wiring_game_test_the_combat_factor_weighs_kills_and_damage_by_mob_category',
             'the clamp on the damage counter is gone',
             'a damage baseline that outruns the DAMAGE_TAKEN statistic (a rolled-back stats file) holds the combat factor at its floor instead of driving it negative',
             kind="server"),
    Mutation('trim-rib-cadence-forty',
             'common/src/shared/java/com/simplebuilding/mixin/LivingEntityMixin.java',
             '        if (!entity.level().isClientSide() && entity.tickCount % 20 == 0) {',
             '        if (!entity.level().isClientSide() && entity.tickCount % 40 == 0) {',
             'trim_wiring_game_test_the_tick_driven_trim_effects_fire_on_their_own_cadence',
             'survived tick 220, which is on the 20 tick cadence',
             'the rib trim works against a wither every 20 ticks, not every 40',
             kind="server"),
    Mutation('trim-land-speed-under-elytra',
             'common/src/shared/java/com/simplebuilding/mixin/PlayerEntityMixin.java',
             '        if (!player.isSwimming() && !player.isFallFlying()) {',
             '        if (!player.isSwimming()) {',
             'trim_wiring_game_test_the_player_mixin_delivers_speed_hunger_and_experience_behind_its_guards',
             'the land speed bonus of a bolt trim kept running under an elytra',
             'the bolt/redstone walking bonus stands down while the wearer glides - it is a land bonus',
             kind="server"),
    Mutation('trim-material-colour-stale',
             'common/src/shared/java/com/simplebuilding/trim/ModTrimMaterials.java',
             '        register(context, ASTRALIT, Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF55)));',
             '        register(context, ASTRALIT, Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF56)));',
             'trim_wiring_game_test_the_three_trim_materials_keep_their_colours_and_their_tags',
             'the generated JSON on disk is older than the bootstrap that is supposed to produce it',
             'the colour ModTrimMaterials.bootstrap declares is the colour the running datapack registry carries - datagen output and bootstrap are one thing, not two copies',
             kind="server"),
    # --- trades-loot -------------------------------------------------------------------------
    Mutation('weighted-enchant-second-chance-default',
             'common/src/shared/java/com/simplebuilding/loot/WeightedEnchantFunction.java',
             '.optionalFieldOf("second_chance", 0.0F)',
             '.optionalFieldOf("second_chance", 1.0F)',
             'trade_offer_game_test_weighted_enchant_honours_its_second_chance_setting',
             'a pool without second_chance must put exactly one enchantment on the item',
             'a pool that declares no second_chance hands out exactly one enchantment - the codec default is never, not always',
             kind="server"),
    Mutation('weighted-enchant-level-dropped',
             'common/src/shared/java/com/simplebuilding/loot/WeightedEnchantFunction.java',
             '            enchantments.set(first.enchantment(), first.level());',
             '            enchantments.set(first.enchantment(), 1);',
             'trade_offer_game_test_master_book_trade_draws_every_enchantment_in_its_pool',
             'these pool entries were never drawn, so their weight or their level is being ignored',
             'the level a pool entry declares is the level that lands on the item - the first pick is not flattened to level one',
             kind="server"),
    Mutation('loot-config-guard-dropped',
             'common/src/shared/java/com/simplebuilding/loot/ModLootTableModifications.java',
             'if (!Simplebuilding.getConfig().worldGen.enableLootTableChanges) {\n                return;\n            }',
             '',
             'config_option_game_test_loot_table_changes_stop_when_the_option_is_switched_off',
             'although the loot table option is switched off',
             'worldGen.enableLootTableChanges is checked before any pool is handed to the loader, on both editor paths',
             kind="server"),
    Mutation('migration-inventory-loop-off-by-one',
             'common/src/shared/java/com/simplebuilding/util/LegacySpatulaMigration.java',
             '        for (int slotIndex = 0; slotIndex < inventory.getContainerSize(); slotIndex++) {',
             '        for (int slotIndex = 0; slotIndex < inventory.getContainerSize() - 2; slotIndex++) {',
             'trade_and_migration_game_test_legacy_spatulas_in_player_inventory_become_chisels',
             'the inventory loop no longer walks the whole container',
             "migratePlayer's inventory loop walks every container slot, including the one no menu slot covers",
             kind="server"),
    Mutation('migration-scan-ceiling-at-zero',
             'common/src/shared/java/com/simplebuilding/util/LegacySpatulaMigration.java',
             '    private static final AABB WORLD_SCAN_BOX = new AABB(-30000000.0, -64.0, -30000000.0, 30000000.0, 320.0, 30000000.0);',
             '    private static final AABB WORLD_SCAN_BOX = new AABB(-30000000.0, -64.0, -30000000.0, 30000000.0, 0.0, 30000000.0);',
             'trade_and_migration_game_test_legacy_spatula_item_entity_is_rewritten_in_place',
             'the scan box no longer covers the height players build at',
             'the world scan box reaches from y=-64 up to y=320, so a spatula lying on the surface is rewritten too',
             kind="server"),
    Mutation('config-wandering-switch-static',
             'common/src/shared/java/com/simplebuilding/config/SimplebuildingConfig.java',
             '        public boolean enableWanderingTrades = true;',
             '        public static boolean enableWanderingTrades = true;',
             'config_option_game_test_trade_switch_conditions_still_name_real_config_fields_on_both_loaders',
             'became static, so it is no longer part of the saved config',
             'both trade switches are instance options of WorldGen, so the Gson serializer persists them and the toggle survives a restart',
             kind="server"),
    # --- detector-magnet-rangefinder ---------------------------------------------------------
    Mutation('ore-detector-nearest-guard',
             'common/src/shared/java/com/simplebuilding/items/custom/OreDetectorItem.java',
             '                    if (distanceSq > maxScanDistanceSq || distanceSq >= bestDistanceSq) continue;',
             '                    if (distanceSq > maxScanDistanceSq) continue;',
             'ore_detector_game_test_detector_reports_the_nearest_target_inside_its_budget',
             'the detector was supposed to report the nearer of two iron ores',
             'the detector reports the nearest reachable match, not the last one the cube scan happens to visit',
             kind="server"),
    Mutation('magnet-filter-path-only',
             'common/src/shared/java/com/simplebuilding/items/custom/MagnetItem.java',
             '        return filterId.equals(itemId);',
             '        return itemId.endsWith(filterId);',
             'magnet_game_test_magnet_filter_matches_the_full_registry_id_and_nothing_else',
             'a magnet whose filter is missing the namespace should not have pulled the',
             'the magnet filter compares the full registry id, a bare path does not match',
             kind="server"),
    Mutation('octant-air-click-sneak-guard',
             'common/src/shared/java/com/simplebuilding/items/custom/OctantItem.java',
             '        if (!world.isClientSide() && user.isShiftKeyDown()) {',
             '        if (!world.isClientSide()) {',
             'octant_game_test_air_clicks_only_reset_an_unlocked_octant_while_sneaking',
             'a plain right click in the air cleared the selection; only the sneak click may',
             'an air click only throws the selection away while sneaking',
             kind="server"),
    Mutation('chisel-preview-spatula-sneak',
             'common/src/shared/java/com/simplebuilding/items/custom/ChiselItem.java',
             '        if (this.isDedicatedSpatula) {\n            if (isSneaking) {\n                currentMap = hasConstructorsTouch ? this.touchForwardMap : this.forwardMap;',
             '        if (this.isDedicatedSpatula) {\n            if (isSneaking) {\n                currentMap = hasConstructorsTouch ? this.touchBackwardMap : this.backwardMap;',
             'chisel_game_test_spatula_runs_forward_while_sneaking_and_chisel_runs_backward',
             'canChisel and the actual click disagree for a sneaking spatula',
             "canChisel (the highlight predicate) selects the same map as tryChiselBlock, including the sneaking spatula's forward table",
             kind="server"),
    Mutation('chisel-iron-merge-dropped',
             'common/src/shared/java/com/simplebuilding/items/custom/ChiselItem.java',
             '        FINAL_IRON_FWD = merge(FINAL_STONE_FWD, IRON_CHISEL_MAP);',
             '        FINAL_IRON_FWD = Map.copyOf(IRON_CHISEL_MAP);',
             'chisel_game_test_tier_tables_are_inherited_upwards_and_shared_in_pairs',
             "copper_chisel lost the stone tier's transformation, so the table merge is gone",
             "every tier's forward table is the union of its own entries and all lower tiers, so a copper chisel still performs the stone tier's stone -> chiseled stone bricks",
             kind="server"),
    Mutation('rotator-x-table-north',
             'common/src/shared/java/com/simplebuilding/items/custom/RotatorItem.java',
             '            if (dir == Direction.NORTH) return counterClockwise ? Direction.UP : Direction.DOWN;',
             '            if (dir == Direction.NORTH) return counterClockwise ? Direction.DOWN : Direction.UP;',
             'rotator_game_test_facing_blocks_turn_one_quarter_around_the_clicked_axis_or_jump_to_its_start',
             'X lap step 2',
             'a centre click on a side face turns a facing block one quarter clockwise around X: UP -> NORTH -> DOWN -> SOUTH',
             kind="server"),
    # --- light-frames-world ------------------------------------------------------------------
    Mutation('dyn-light-update-copy-drops-waterlogged',
             'common/src/shared/java/com/simplebuilding/util/DynamicLightHandler.java',
             '                    if (currentLightInBlock != lightLevel) {\n                        world.setBlock(currentPos, Blocks.LIGHT.defaultBlockState()\n                                .setValue(LightBlock.LEVEL, lightLevel)\n                                .setValue(LightBlock.WATERLOGGED, isWater), 3);',
             '                    if (currentLightInBlock != lightLevel) {\n                        world.setBlock(currentPos, Blocks.LIGHT.defaultBlockState()\n                                .setValue(LightBlock.LEVEL, lightLevel), 3);',
             'dynamic_light_game_test_the_light_block_only_replaces_air_or_water_sources_and_puts_the_water_back',
             'a waterlogged light block whose level was raised while the diver stood still',
             'the update-in-place copy of the setBlock carries the waterlogged flag exactly like the first-placement copy, so raising the level under water does not drain it',
             kind="server"),
    Mutation('dyn-light-standing-block-not-rewritten',
             'common/src/shared/java/com/simplebuilding/util/DynamicLightHandler.java',
             '            if (currentState.isAir() || currentState.is(Blocks.LIGHT)\n                    || (isWater && currentState.getFluidState().isSource())) {',
             '            if (currentState.isAir()\n                    || (isWater && currentState.getFluidState().isSource())) {',
             'dynamic_light_game_test_worn_emission_levels_add_up_into_the_light_block_over_the_players_head',
             'a helmet and a chestplate at emission level I, put on without moving',
             'a dry light block that is already standing over the player is rewritten in place when the armour changes, not only after the next step',
             kind="server"),
    Mutation('frame-lock-branch-ignores-locked',
             'common/src/shared/java/com/simplebuilding/mixin/ItemFrameEntityMixin.java',
             '            if (handStack.is(Items.GLASS_PANE) && !this.getItem().isEmpty() && !this.simplebuilding$locked) {',
             '            if (handStack.is(Items.GLASS_PANE) && !this.getItem().isEmpty()) {',
             'ore_gen_and_item_frame_game_test_glass_pane_locks_the_frame_and_the_lock_survives_the_save_round_trip',
             'sneaking with a glass pane on an already locked frame locked it again instead of',
             'the lock branch asks whether the frame is already locked, so a player holding glass panes can still reach the unlock branch behind it',
             kind="server"),
    Mutation('frame-magnet-behind-lock',
             'common/src/shared/java/com/simplebuilding/mixin/ItemFrameEntityMixin.java',
             '            if (hasEnchantment && !this.getItem().isEmpty()) {',
             '            if (hasEnchantment && !this.getItem().isEmpty() && !this.simplebuilding$locked) {',
             'world_and_player_game_test_locked_frames_still_answer_the_magnet_while_other_sneak_clicks_fall_through',
             'an enchanted magnet on a locked frame was answered with',
             "the Constructor's Touch magnet branch runs before the lock's FAIL, so a locked display frame can still be used as a filter template",
             kind="server"),
    Mutation('void-rescue-threshold-deeper',
             'common/src/shared/java/com/simplebuilding/mixin/EnderiteItemMixin.java',
             '            if (this.getY() < minY - 10) {',
             '            if (this.getY() < minY - 12) {',
             'protection_and_range_game_test_void_protection_lifts_enderite_back_into_the_world_while_other_items_are_lost',
             'world floor stayed at y=',
             'the rescue starts exactly ten blocks below the world floor; an enderite item eleven blocks down is lifted, not left hanging',
             kind="server"),
    Mutation('void-rescue-target-nudged',
             'common/src/shared/java/com/simplebuilding/mixin/EnderiteItemMixin.java',
             '                this.setPos(this.getX(), minY + 5, this.getZ());',
             '                this.setPos(this.getX(), minY + 6, this.getZ());',
             'protection_and_range_game_test_void_protection_lifts_enderite_back_into_the_world_while_other_items_are_lost',
             'the rescue put the enderite ingot at y=',
             'the rescue lifts the item to exactly five blocks above the world floor',
             kind="server"),
    # --- wand-enchant-sledge -----------------------------------------------------------------
    Mutation('wand-tick-search-reaches-backpack',
             'common/src/shared/java/com/simplebuilding/items/custom/BuildingWandItem.java',
             '        if (wandHasMasterBuilder) {\n            for (int i = 9; i < player.getInventory().getNonEquipmentItems().size(); i++) {',
             '        if (true) {\n            for (int i = 9; i < player.getInventory().getNonEquipmentItems().size(); i++) {',
             'building_wand_game_test_material_search_prefers_the_off_hand_and_only_master_builder_reaches_the_backpack',
             'a wand without Master Builder built more than the one block its off hand paid for',
             "only a Master Builder wand pays for a block out of the backpack once it is building (findSpecificMaterial's own copy of the guard)",
             kind="server"),
    Mutation('wand-palette-offhand-after-hotbar',
             'common/src/shared/java/com/simplebuilding/items/custom/BuildingWandItem.java',
             '        // Offhand\n        collectBlocksFromStack(player.getOffhandItem(), world, hasMasterBuilder, blocks);\n        // Main Inventory\n        int limit = hasMasterBuilder ? player.getInventory().getNonEquipmentItems().size() : 9;\n        for (int i = 0; i < limit; i++) {\n            collectBlocksFromStack(player.getInventory().getItem(i), world, hasMasterBuilder, blocks);\n        }',
             '        // Main Inventory\n        int limit = hasMasterBuilder ? player.getInventory().getNonEquipmentItems().size() : 9;\n        for (int i = 0; i < limit; i++) {\n            collectBlocksFromStack(player.getInventory().getItem(i), world, hasMasterBuilder, blocks);\n        }\n        // Offhand\n        collectBlocksFromStack(player.getOffhandItem(), world, hasMasterBuilder, blocks);',
             'building_enchantment_game_test_color_palette_spreads_the_carried_blocks_over_the_wand_preview',
             'findAllBuildingBlocks is collecting the hotbar',
             'the Color Palette palette lists the off hand block first, the same order findFirstBlockStateClient and the placement search use',
             kind="server"),
    Mutation('wand-linear-delay-flattened',
             'common/src/shared/java/com/simplebuilding/items/custom/BuildingWandItem.java',
             '            nbt.putInt("Timer", isLinePlace ? DELAY_TICKS_LINE : DELAY_TICKS);',
             '            nbt.putInt("Timer", DELAY_TICKS);',
             'building_enchantment_game_test_linear_only_shortens_the_wand_step_delay',
             'Linear did not speed the wand up at all',
             'Linear picks the shorter DELAY_TICKS_LINE pause between two rings',
             kind="server"),
    Mutation('wand-face-axis-default',
             'common/src/shared/java/com/simplebuilding/items/custom/BuildingWandItem.java',
             '        else buildAxis = face.getAxis(); // Default: Achse der Blickrichtung',
             '        else buildAxis = Direction.Axis.Y; // Default: Achse der Blickrichtung',
             'building_wand_game_test_clicked_face_sets_the_plane_until_an_axis_mode_overrides_it',
             'clicking the east face did not fill the upright plane one block east of the',
             'with axis mode 0 the plane stands perpendicular to the clicked face, not always flat',
             kind="server"),
    Mutation('versatility-level-one-searches-everything',
             'common/src/shared/java/com/simplebuilding/util/VersatilityUsageEvent.java',
             '        int searchRange = (level >= 2) ? 36 : 9;',
             '        int searchRange = (level >= 1) ? 36 : 9;',
             'enchantment_effect_game_test_versatility_swaps_in_the_better_tool_while_sneaking',
             "Versatility I reached past the hotbar, which is level II's job",
             "Versatility I searches the hotbar only; the whole inventory is level II's job",
             kind="server"),
    Mutation('hammer-stairs-to-slab-branch',
             'common/src/shared/java/com/simplebuilding/items/custom/SledgehammerItem.java',
             '            // 2. Stairs -> Slab\n            if (block instanceof StairBlock) {',
             '            // 2. Stairs -> Slab\n            if (block instanceof SlabBlock) {',
             'sledgehammer_game_test_sledgehammer_reshapes_full_blocks_stairs_and_slabs',
             'Expected block Stone Slab: got Stone Stairs',
             'the forward ladder has a second step: stairs become a slab',
             kind="server"),
]

#: Mutations whose "old" text is not one contiguous block. Each takes the file text and
#: returns it mutated, or raises if the anchor is gone.
PATCH_FUNCTIONS = {
    "wand-ghost-centred": lambda text: _drop_lines(text, [
        "poseStack.translate(0.5, 0.5, 0.5);",
        "poseStack.translate(-0.5, -0.5, -0.5);"]),
    "hammer-should-break": lambda text: _drop_block(text,
        "if (!SledgehammerUtils.shouldBreak(client.level, pos, centerPos, stack)) {",
        "continue;", "}"),
}


def _drop_lines(text: str, needles: list[str]) -> str:
    lines = text.split("\n")
    kept = []
    dropped = 0
    for line in lines:
        if any(n in line for n in needles):
            dropped += 1
            continue
        kept.append(line)
    if dropped != len(needles):
        raise RuntimeError(f"expected to drop {len(needles)} lines, dropped {dropped}")
    return "\n".join(kept)


def _drop_block(text: str, head: str, body: str, tail: str) -> str:
    lines = text.split("\n")
    for i in range(len(lines) - 2):
        if head in lines[i] and body in lines[i + 1] and lines[i + 2].strip() == tail:
            return "\n".join(lines[:i] + lines[i + 3:])
    raise RuntimeError(f"block starting with {head!r} not found")


def force_utf8_stdout() -> None:
    for name in ("stdout", "stderr"):
        stream = getattr(sys, name)
        if isinstance(stream, io.TextIOWrapper) and (stream.encoding or "").lower() != "utf-8":
            stream.reconfigure(encoding="utf-8", errors="replace")


def plan(selected: list[Mutation]) -> list[list[Mutation]]:
    """Rounds with at most one mutation per script."""
    remaining = list(selected)
    rounds: list[list[Mutation]] = []
    while remaining:
        this_round: list[Mutation] = []
        used: set[str] = set()
        for m in list(remaining):
            if m.script in used:
                continue
            this_round.append(m)
            used.add(m.script)
            remaining.remove(m)
        rounds.append(this_round)
    return rounds


#: The 1.21.11 line keeps its own copy of the shared mod sources (mc1_21_11/shared/java, mirrored
#: by hand) and its own Fabric module. A server mutation proved on 26.2 says nothing about
#: whether the TRANSLATED test body on 1.21.11 bites - the bodies are within the drift tolerance,
#: which is a statement about text, not about teeth. --line 1.21.11 re-points every server
#: mutation at the copy: same anchor where the copy is word-identical (28 of 30 are), a line
#: specific anchor where the API differs (the two below).
LINE_1_21_11 = "1.21.11"

#: What differs on the 1.21.11 copy, per mutation id: "old"/"new" where the anchor reads
#: differently there, "script"/"expect" where the test that sees the mutation is another one
#: (the trade tests of that line are code registered, not data driven - see LINE_DIFFERENCES
#: in run.py). Keys that are absent keep the 26.2 value.
ON_1_21_11: dict[str, dict[str, str]] = {
    "vein-ore-list-emerald": {
        "old": "state.is(BlockTags.DIAMOND_ORES) ||\n                state.is(BlockTags.EMERALD_ORES);",
        "new": "state.is(BlockTags.DIAMOND_ORES);"},
    "hopper-pickup-only": {
        "old": "                if (actionType == ClickType.PICKUP) {\n                    blockEntity.setGhostItem(slotIndex, cursor.isEmpty() ? ItemStack.EMPTY : cursor);\n                    // Abbrechen, damit Item nicht wirklich reingelegt wird\n                    return; \n                }",
        "new": "                blockEntity.setGhostItem(slotIndex, cursor.isEmpty() ? ItemStack.EMPTY : cursor);\n                return;"},
    # The loot function hands the draw to WeightedPicker there and applies every pick in one
    # loop; and the master book TRADE of that line never goes through the loot function (it is
    # an EnchantmentPool listing), so the test that drives the function directly is the one
    # that sees a dropped level.
    "weighted-enchant-level-dropped": {
        "old": "                enchantments.set(pick.enchantment(), pick.level());",
        "new": "                enchantments.set(pick.enchantment(), 1);",
        # Its two entry pool sees the level first, with the same sentence the 26.2 test uses.
        "script": "trade_offer_game_test_weighted_enchant_honours_its_second_chance_setting"},
    # The trade switch test is 26.2 only (it reads the shipped trade jsons); on 1.21.11 the
    # reflection over every config option is what notices a field that went static.
    "config-wandering-switch-static": {
        "script": "config_option_game_test_every_config_option_keeps_its_persisted_name_and_default",
        "expect": "the set of config options (name, group, type, default)"},
}


def on_line(m: Mutation, line: str) -> Mutation:
    """The same mutation, addressed at the copy of its file on the given Minecraft line."""
    if line != LINE_1_21_11:
        return m
    file = (m.file.replace("common/src/shared/java", "mc1_21_11/shared/java")
            .replace("src/main/java", "mc1_21_11/fabric/src/main/java"))
    diff = ON_1_21_11.get(m.id, {})
    return Mutation(m.id, file, diff.get("old", m.old), diff.get("new", m.new),
                    diff.get("script", m.script), diff.get("expect", m.expect), m.claim, m.kind)


def mutate(m: Mutation, text: str) -> str:
    """The mutated file text, or a RuntimeError naming the missing anchor."""
    if m.id in PATCH_FUNCTIONS:
        mutated = PATCH_FUNCTIONS[m.id](text)
    else:
        if m.old not in text:
            raise RuntimeError(f"{m.id}: anchor not found in {m.file}")
        mutated = text.replace(m.old, m.new, 1)
    if mutated == text:
        raise RuntimeError(f"{m.id}: mutation changed nothing")
    return mutated


def apply(m: Mutation) -> None:
    path = REPO / m.file
    path.write_text(mutate(m, path.read_text(encoding="utf-8")), encoding="utf-8")


def check_anchors(selected: list[Mutation]) -> int:
    """Every anchor has to be in its file, or the mutation could never be applied."""
    problems = 0
    for m in selected:
        try:
            mutate(m, (REPO / m.file).read_text(encoding="utf-8"))
            print(f"  ok   {m.id}")
        except (RuntimeError, FileNotFoundError) as e:
            problems += 1
            print(f"  FEHLT {m.id}: {e}")
    return problems


def restore(files: set[str]) -> None:
    subprocess.run(["git", "checkout", "--", *sorted(files)], cwd=REPO, check=True)


def working_tree_clean(files: set[str]) -> bool:
    out = subprocess.run(["git", "status", "--porcelain", "--", *sorted(files)],
                         cwd=REPO, capture_output=True, text=True).stdout
    return out.strip() == ""


LOG_FOR_TARGET = {
    "client-fabric-262": "build/run/clientGameTest/logs/latest.log",
    "client-neoforge-262": "neoforge/build/run/clientGameTest/logs/latest.log",
    "client-fabric-12111": "mc1_21_11/fabric/build/run/clientGameTest/logs/latest.log",
    "client-neoforge-12111": "mc1_21_11/neoforge/build/run/clientGameTest/logs/latest.log",
}

FAILED_LINE = re.compile(r"\[(?P<script>[a-z-]+)\] FAILED: (?P<message>.*)|FAILED in (?P<script2>[a-z-]+) at step '(?P<step>[^']*)': (?P<message2>.*)")


def failures_in_log(target: str) -> dict[str, str]:
    """script -> failure message, from the client log of the last run."""
    text = (REPO / LOG_FOR_TARGET[target]).read_text(encoding="utf-8", errors="replace")
    found: dict[str, str] = {}
    for line in text.splitlines():
        m = FAILED_LINE.search(line)
        if not m:
            continue
        script = m.group("script") or m.group("script2")
        message = m.group("message") or m.group("message2") or ""
        found.setdefault(script, message)
    return found


SERVER_REPORT = {
    "fabric-262": "build/junit.xml",
    "neoforge-262": "neoforge/build/neoforge-junit.xml",
    "fabric-12111": "mc1_21_11/fabric/build/junit.xml",
    "neoforge-12111": "mc1_21_11/neoforge/build/neoforge-junit.xml",
}


def server_failure(target: str, test_id: str) -> str | None:
    """The failure message of one test in the last JUnit report, or None when it passed."""
    from xml.etree import ElementTree
    root = ElementTree.parse(REPO / SERVER_REPORT[target]).getroot()
    for case in root.iter("testcase"):
        if (case.get("name") or "").endswith(test_id):
            problem = case.find("failure")
            if problem is None:
                problem = case.find("error")
            return None if problem is None else (problem.get("message") or problem.text or "")
    return "TEST NICHT IM BERICHT"


def run_server_mutation(number: int, m: Mutation, target: str, timeout: int) -> dict:
    """One server mutation, one filtered server run. Cheap enough to do one at a time."""
    if not working_tree_clean({m.file}):
        raise SystemExit(f"{m.file} is not clean in git; commit or stash first")

    print(f"\nServer-Runde {number}: {m.id} auf {target}")
    apply(m)
    print(f"  eingespielt  {m.id}  ({m.file})")
    try:
        # The selector wants the namespace; without it the server says "found no tests" and the
        # report has no such case, which reads as "not in the report", not as "still green".
        selector = m.script if ":" in m.script else f"simplebuilding:{m.script}"
        subprocess.run([sys.executable, "tools/testrunner/run.py", "--targets", target,
                        "--filter", selector, "--timeout", str(timeout),
                        "--trigger", f"mutation-{m.id}"],
                       cwd=REPO, capture_output=True, text=True)
        message = server_failure(target, m.script)
    finally:
        restore({m.file})
        print("  zurueckgenommen")

    if message is None:
        ok, verdict = False, "GRUEN GEBLIEBEN - die Schaerfung beisst nicht"
    elif m.expect in message:
        ok, verdict = True, "rot, mit der erwarteten Meldung"
    else:
        ok, verdict = False, "rot, aber mit einer ANDEREN Meldung: " + message[:200]
    print(f"  {'OK ' if ok else 'XX '} {m.id}: {verdict}")
    return {"round": number, "target": target,
            "mutations": [{"id": m.id, "script": m.script, "ok": ok, "verdict": verdict,
                           "message": message}],
            "collateral": {}}


def run_round(number: int, mutations: list[Mutation], target: str, timeout: int) -> dict:
    files = {m.file for m in mutations}
    if not working_tree_clean(files):
        raise SystemExit("the files to mutate are not clean in git; commit or stash first: "
                         + ", ".join(sorted(files)))

    print(f"\nRunde {number}: " + ", ".join(m.id for m in mutations))
    for m in mutations:
        apply(m)
        print(f"  eingespielt  {m.id}  ({m.file})")

    try:
        subprocess.run([sys.executable, "tools/testrunner/run.py", "--targets", target,
                        "--timeout", str(timeout), "--trigger", f"mutation-round-{number}"],
                       cwd=REPO, capture_output=True, text=True)
        failures = failures_in_log(target)
    finally:
        restore(files)
        print("  zurueckgenommen")

    verdicts = []
    mutated_scripts = {m.script for m in mutations}
    for m in mutations:
        message = failures.get(m.script)
        if message is None:
            verdict = "GRUEN GEBLIEBEN - die Schaerfung beisst nicht"
            ok = False
        elif m.expect in message:
            verdict = "rot, mit der erwarteten Meldung"
            ok = True
        else:
            verdict = "rot, aber mit einer ANDEREN Meldung: " + message[:200]
            ok = False
        verdicts.append({"id": m.id, "script": m.script, "ok": ok, "verdict": verdict,
                         "message": message})
        print(f"  {'OK ' if ok else 'XX '} {m.id}: {verdict}")

    collateral = {s: msg for s, msg in failures.items() if s not in mutated_scripts}
    for script, message in collateral.items():
        print(f"  !! Nebenschaden in {script}: {message[:200]}")

    return {"round": number, "target": target, "mutations": verdicts,
            "collateral": collateral}


def main(argv: list[str] | None = None) -> int:
    force_utf8_stdout()
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[1])
    parser.add_argument("--list", action="store_true")
    parser.add_argument("--check", action="store_true", help="nur pruefen, ob jeder Anker noch da ist")
    parser.add_argument("--plan", action="store_true")
    parser.add_argument("--run", action="store_true")
    parser.add_argument("--only", default="", help="comma separated mutation ids")
    parser.add_argument("--target", default="client-fabric-262")
    parser.add_argument("--server-target", default="fabric-262",
                        help="the server target the server side mutations are proved on")
    parser.add_argument("--timeout", type=int, default=1200)
    parser.add_argument("--p6", action="store_true",
                        help="the P6 core-area round (server side) instead of the false-green counter-checks")
    parser.add_argument("--p6b", action="store_true",
                        help="the P6b round over the remaining areas (server side)")
    parser.add_argument("--line", default="26.2", choices=["26.2", LINE_1_21_11],
                        help="which Minecraft line's copy of the mod to mutate; 1.21.11 takes the server "
                             "mutations only and proves them on fabric-12111 unless --server-target says otherwise")
    args = parser.parse_args(argv)

    catalogue = P6B_MUTATIONS if args.p6b else P6_MUTATIONS if args.p6 else MUTATIONS
    if args.line == LINE_1_21_11:
        catalogue = [on_line(m, args.line) for m in catalogue if m.kind == "server"]
        if args.server_target == "fabric-262":
            args.server_target = "fabric-12111"
    selected = catalogue
    if args.only:
        wanted = set(args.only.split(","))
        unknown = wanted - {m.id for m in catalogue}
        if unknown:
            raise SystemExit("unbekannte Mutationen: " + ", ".join(sorted(unknown)))
        selected = [m for m in catalogue if m.id in wanted]

    if args.list:
        for m in catalogue:
            print(f"  {m.id:32s} {m.script:22s} {m.claim}")
        return 0

    if args.check:
        return 1 if check_anchors(selected) else 0

    rounds = plan([m for m in selected if m.kind == "client"])
    if args.plan or not args.run:
        for m in (m for m in selected if m.kind == "server"):
            print(f"Server: {m.id} [{m.script}]")
        for i, r in enumerate(rounds, 1):
            print(f"Runde {i}: " + ", ".join(f"{m.id} [{m.script}]" for m in r))
        print(f"{len(rounds)} Client-Runden fuer {len(selected)} Mutationen")
        return 0

    results = []
    server_mutations = [m for m in selected if m.kind == "server"]
    client_rounds = plan([m for m in selected if m.kind == "client"])

    for i, m in enumerate(server_mutations, 1):
        results.append(run_server_mutation(i, m, args.server_target, args.timeout))
    for i, r in enumerate(client_rounds, 1):
        results.append(run_round(i, r, args.target, args.timeout))

    out_dir = REPO / "testing" / "mutations"
    out_dir.mkdir(parents=True, exist_ok=True)
    stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%dT%H-%M-%SZ")
    out = out_dir / f"{stamp}.json"
    # The header names the target the rounds actually ran on: the client target for the client
    # catalogue, the server target for a --p6 run, which has no client rounds at all.
    ran_on = args.server_target if (args.p6 or args.p6b) else args.target
    out.write_text(json.dumps({"target": ran_on, "line": args.line,
                               "catalogue": "p6b" if args.p6b else "p6" if args.p6 else "false-greens",
                               "rounds": results}, indent=2,
                              ensure_ascii=False), encoding="utf-8")

    total = sum(len(r["mutations"]) for r in results)
    bitten = sum(1 for r in results for v in r["mutations"] if v["ok"])
    collateral = sum(len(r["collateral"]) for r in results)
    print(f"\n{bitten} von {total} Mutationen wurden rot mit der erwarteten Meldung, "
          f"{collateral} Nebenschaeden. Datensatz: {out.relative_to(REPO)}")
    return 0 if bitten == total and collateral == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
