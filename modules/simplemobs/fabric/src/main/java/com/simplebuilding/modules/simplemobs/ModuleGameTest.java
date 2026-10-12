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
    @GameTest(structure = "simplemobs:empty") public void silenceDampens(GameTestHelper helper) { MobsTests.ALL.get("silence_dampens").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void endnetherRegistered(GameTestHelper helper) { MobsTests.ALL.get("endnether_registered").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void endnetherRules(GameTestHelper helper) { MobsTests.ALL.get("endnether_rules").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void shellkerShell(GameTestHelper helper) { MobsTests.ALL.get("shellker_shell").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void shellkerConvert(GameTestHelper helper) { MobsTests.ALL.get("shellker_convert").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void shellkerGateway(GameTestHelper helper) { MobsTests.ALL.get("shellker_gateway").accept(helper); }
    @GameTest(structure = "simplemobs:empty") public void highGravity(GameTestHelper helper) { MobsTests.ALL.get("high_gravity").accept(helper); }
    @GameTest(structure = "simplemobs:empty", maxTicks = 100) public void soulfireBlaze(GameTestHelper helper) { MobsTests.ALL.get("soulfire_blaze").accept(helper); }
}
