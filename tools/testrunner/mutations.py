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
             "multi-block-breaking", "iron Strip Miner pickaxe produced breaking cracks",
             "sneaking alone is not enough; the tool has to fit"),
    Mutation("hopper-ordinal-bounds",
             f"{SHARED}/screen/NetheriteHopperScreenHandler.java",
             "        if (ordinal >= 0 && ordinal < HopperFilterMode.values().length) {\n            return HopperFilterMode.values()[ordinal];\n        }\n        return HopperFilterMode.NONE;",
             "        return HopperFilterMode.values()[ordinal];",
             "hopper_game_test_the_mode_delegate_reads_and_writes_the_filter_mode",
             "ArrayIndexOutOfBounds",
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
#: (port_tests_to_1_21_11.py --drift keeps them within twelve normalised lines), so the same
#: sentence is there; what differs is the Minecraft underneath, which a mutation of the mod's
#: source does not touch.
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
             "Expected block Stone: got Air",
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
             "0.124 from the west edge, still inside the rim",
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
    args = parser.parse_args(argv)

    catalogue = P6_MUTATIONS if args.p6 else MUTATIONS
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
    out.write_text(json.dumps({"target": args.target, "catalogue": "p6" if args.p6 else "false-greens",
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
