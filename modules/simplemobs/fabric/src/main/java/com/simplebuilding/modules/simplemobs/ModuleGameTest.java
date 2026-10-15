package com.simplebuilding.modules.simplemobs;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class ModuleGameTest {
    @GameTest public void registered(GameTestHelper helper) { MobsTests.ALL.get("registered").accept(helper); }
    @GameTest public void subModSwitch(GameTestHelper helper) { MobsTests.ALL.get("sub_mod_switch").accept(helper); }
    @GameTest public void phases(GameTestHelper helper) { MobsTests.ALL.get("phases").accept(helper); }
    @GameTest public void waveSizes(GameTestHelper helper) { MobsTests.ALL.get("wave_sizes").accept(helper); }
    @GameTest public void waveKinds(GameTestHelper helper) { MobsTests.ALL.get("wave_kinds").accept(helper); }
    @GameTest public void provocation(GameTestHelper helper) { MobsTests.ALL.get("provocation").accept(helper); }
    @GameTest public void pulseAndHeal(GameTestHelper helper) { MobsTests.ALL.get("pulse_and_heal").accept(helper); }
    @GameTest public void spawnRules(GameTestHelper helper) { MobsTests.ALL.get("spawn_rules").accept(helper); }
    @GameTest public void fight(GameTestHelper helper) { MobsTests.ALL.get("fight").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void disguiseHitbox(GameTestHelper helper) { MobsTests.ALL.get("disguise_hitbox").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void howlerRegistered(GameTestHelper helper) { MobsTests.ALL.get("howler_registered").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void howlerRules(GameTestHelper helper) { MobsTests.ALL.get("howler_rules").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void howlerPattern(GameTestHelper helper) { MobsTests.ALL.get("howler_pattern").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void howlerJawTag(GameTestHelper helper) { MobsTests.ALL.get("howler_jaw_tag").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void howlerHornRule(GameTestHelper helper) { MobsTests.ALL.get("howler_horn_rule").accept(helper); }
    @GameTest(structure = "simplemobs:empty", maxTicks = 100) public void howlerAbortLight(GameTestHelper helper) { MobsTests.ALL.get("howler_abort_light").accept(helper); }
    @GameTest(structure = "simplemobs:empty", maxTicks = 100) public void howlerAbortSensor(GameTestHelper helper) { MobsTests.ALL.get("howler_abort_sensor").accept(helper); }
    @GameTest(structure = "simplemobs:empty", maxTicks = 100) public void howlerSummon(GameTestHelper helper) { MobsTests.ALL.get("howler_summon").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void howlerFight(GameTestHelper helper) { MobsTests.ALL.get("howler_fight").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void howlerAttackChoice(GameTestHelper helper) { MobsTests.ALL.get("howler_attack_choice").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void howlerShockwave(GameTestHelper helper) { MobsTests.ALL.get("howler_shockwave").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void howlerShriekCover(GameTestHelper helper) { MobsTests.ALL.get("howler_shriek_cover").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void howlerTendrils(GameTestHelper helper) { MobsTests.ALL.get("howler_tendrils").accept(helper); }
    @GameTest(structure = "simplemobs:empty", maxTicks = 100) public void howlerChargeHit(GameTestHelper helper) { MobsTests.ALL.get("howler_charge_hit").accept(helper); }
    @GameTest(structure = "simplemobs:empty", maxTicks = 100) public void howlerChargeWall(GameTestHelper helper) { MobsTests.ALL.get("howler_charge_wall").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void howlerHeartbeat(GameTestHelper helper) { MobsTests.ALL.get("howler_heartbeat").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void howlerRagePulseRules(GameTestHelper helper) { MobsTests.ALL.get("howler_rage_pulse_rules").accept(helper); }
    @GameTest(structure = "simplemobs:empty", maxTicks = 100) public void howlerPhaseSounds(GameTestHelper helper) { MobsTests.ALL.get("howler_phase_sounds").accept(helper); }
    @GameTest(structure = "simplemobs:empty", maxTicks = 100) public void howlerRagePulse(GameTestHelper helper) { MobsTests.ALL.get("howler_rage_pulse").accept(helper); }
    @GameTest(structure = "simplemobs:empty", maxTicks = 100) public void howlerNoPulseBeforeRage(GameTestHelper helper) { MobsTests.ALL.get("howler_no_pulse_before_rage").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void silenceDampens(GameTestHelper helper) { MobsTests.ALL.get("silence_dampens").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void cloakRegistered(GameTestHelper helper) { MobsTests.ALL.get("cloak_registered").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void veiledInvisibility(GameTestHelper helper) { MobsTests.ALL.get("veiled_invisibility").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void cloakDetection(GameTestHelper helper) { MobsTests.ALL.get("cloak_detection").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void cloakMorph(GameTestHelper helper) { MobsTests.ALL.get("cloak_morph").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void cloakPlayerMorph(GameTestHelper helper) { MobsTests.ALL.get("cloak_player_morph").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void cloakReveal(GameTestHelper helper) { MobsTests.ALL.get("cloak_reveal").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void trickMirror(GameTestHelper helper) { MobsTests.ALL.get("trick_mirror").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void trickMirrorBalance(GameTestHelper helper) { MobsTests.ALL.get("trick_mirror_balance").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void cloakHeadSlot(GameTestHelper helper) { MobsTests.ALL.get("cloak_head_slot").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void cloakAirSwing(GameTestHelper helper) { MobsTests.ALL.get("cloak_air_swing").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void deceiverDrops(GameTestHelper helper) { MobsTests.ALL.get("deceiver_drops").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void doubleChance(GameTestHelper helper) { MobsTests.ALL.get("double_chance").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void doubleCap(GameTestHelper helper) { MobsTests.ALL.get("double_cap").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void endnetherRegistered(GameTestHelper helper) { MobsTests.ALL.get("endnether_registered").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void endnetherRules(GameTestHelper helper) { MobsTests.ALL.get("endnether_rules").accept(helper); }
    @GameTest(structure = "simplemobs:empty", maxTicks = 60) public void shellkerN45(GameTestHelper helper) { MobsTests.ALL.get("shellker_n45").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void shellkerShell(GameTestHelper helper) { MobsTests.ALL.get("shellker_shell").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void shellkerConvert(GameTestHelper helper) { MobsTests.ALL.get("shellker_convert").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void shellkerGateway(GameTestHelper helper) { MobsTests.ALL.get("shellker_gateway").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void highGravity(GameTestHelper helper) { MobsTests.ALL.get("high_gravity").accept(helper); }
    @GameTest(structure = "simplemobs:empty", maxTicks = 100) public void soulfireCharge(GameTestHelper helper) { MobsTests.ALL.get("soulfire_charge").accept(helper); }
    @GameTest(structure = "simplemobs:empty", maxTicks = 100) public void soulfireBlaze(GameTestHelper helper) { MobsTests.ALL.get("soulfire_blaze").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void fearRegistered(GameTestHelper helper) { MobsTests.ALL.get("fear_registered").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void fearDreadSummon(GameTestHelper helper) { MobsTests.ALL.get("fear_dread_summon").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void fearAuraLevels(GameTestHelper helper) { MobsTests.ALL.get("fear_aura_levels").accept(helper); }
    @GameTest(structure = "simplemobs:empty", maxTicks = 100) public void fearDreadLight(GameTestHelper helper) { MobsTests.ALL.get("fear_dread_light").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void fearDreadShield(GameTestHelper helper) { MobsTests.ALL.get("fear_dread_shield").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void fearDreadStomp(GameTestHelper helper) { MobsTests.ALL.get("fear_dread_stomp").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void fearWatcherGaze(GameTestHelper helper) { MobsTests.ALL.get("fear_watcher_gaze").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void fearWatcherHitTeleport(GameTestHelper helper) { MobsTests.ALL.get("fear_watcher_hit_teleport").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void fearWatcherSpyglass(GameTestHelper helper) { MobsTests.ALL.get("fear_watcher_spyglass").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void fearAuraDecay(GameTestHelper helper) { MobsTests.ALL.get("fear_aura_decay").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void fearAuraTriggered(GameTestHelper helper) { MobsTests.ALL.get("fear_aura_triggered").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void fearWatcherScream(GameTestHelper helper) { MobsTests.ALL.get("fear_watcher_scream").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void fearWatcherTeleport(GameTestHelper helper) { MobsTests.ALL.get("fear_watcher_teleport").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void fearWatcherZones(GameTestHelper helper) { MobsTests.ALL.get("fear_watcher_zones").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void fearWatcherZoneLogic(GameTestHelper helper) { MobsTests.ALL.get("fear_watcher_zone_logic").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void fearWatcherMultiFreeze(GameTestHelper helper) { MobsTests.ALL.get("fear_watcher_multi_freeze").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void fearWatcherHitNewVictim(GameTestHelper helper) { MobsTests.ALL.get("fear_watcher_hit_new_victim").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void fearWatcherBoat(GameTestHelper helper) { MobsTests.ALL.get("fear_watcher_boat").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void fearWatcherSpawnRules(GameTestHelper helper) { MobsTests.ALL.get("fear_watcher_spawn_rules").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void fearDrops(GameTestHelper helper) { MobsTests.ALL.get("fear_drops").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void fearImmunity(GameTestHelper helper) { MobsTests.ALL.get("fear_immunity").accept(helper); }
    @GameTest public void discoverRecipeUnlocks(GameTestHelper h) { com.simplebuilding.modules.simplemobs.discover.MobsDiscover.recipeUnlocks(h); }
    @GameTest public void discoverBehaviourHints(GameTestHelper h) { com.simplebuilding.modules.simplemobs.discover.MobsDiscover.behaviourHints(h); }
    @GameTest public void discoverFirstPickupTips(GameTestHelper h) { com.simplebuilding.modules.simplemobs.discover.MobsDiscover.firstPickupTips(h); }
    @GameTest(structure = "simplemobs:empty") public void witherValues(GameTestHelper helper) { MobsTests.ALL.get("wither_values").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void witherSummon(GameTestHelper helper) { MobsTests.ALL.get("wither_summon").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void witherDrop(GameTestHelper helper) { MobsTests.ALL.get("wither_drop").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void witherAttacks(GameTestHelper helper) { MobsTests.ALL.get("wither_attacks").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void witherWilt(GameTestHelper helper) { MobsTests.ALL.get("wither_wilt").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void witherOrder(GameTestHelper helper) { MobsTests.ALL.get("wither_order").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void witherMinionArmor(GameTestHelper helper) { MobsTests.ALL.get("wither_minion_armor").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void witherNovaPush(GameTestHelper helper) { MobsTests.ALL.get("wither_nova_push").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void biomeBrushItem(GameTestHelper helper) { MobsTests.ALL.get("biome_brush_item").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void biomeBrushPaint(GameTestHelper helper) { MobsTests.ALL.get("biome_brush_paint").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void guideBook(GameTestHelper helper) { MobsTests.ALL.get("guide_book").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void biomeBrushClaims(GameTestHelper helper) { MobsTests.ALL.get("biome_brush_claims").accept(helper); }
}
