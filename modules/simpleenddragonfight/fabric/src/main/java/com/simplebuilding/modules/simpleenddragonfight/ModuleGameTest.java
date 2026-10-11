package com.simplebuilding.modules.simpleenddragonfight;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
/** Fabric runs only @GameTest methods: one wrapper per shared test body in {@link DragonFightTests}. */
public final class ModuleGameTest {
    @GameTest(structure = "simpleenddragonfight:empty", maxTicks = 100) public void registered(GameTestHelper helper) { DragonFightTests.ALL.get("registered").accept(helper); }
    @GameTest(structure = "simpleenddragonfight:empty", maxTicks = 100) public void scalingMath(GameTestHelper helper) { DragonFightTests.ALL.get("scaling_math").accept(helper); }
    @GameTest(structure = "simpleenddragonfight:empty", maxTicks = 100) public void scaleKeepsShare(GameTestHelper helper) { DragonFightTests.ALL.get("scale_keeps_share").accept(helper); }
    @GameTest(structure = "simpleenddragonfight:empty", maxTicks = 100) public void raiseBaseOff(GameTestHelper helper) { DragonFightTests.ALL.get("raise_base_off").accept(helper); }
    @GameTest(structure = "simpleenddragonfight:empty", maxTicks = 100) public void speedFactor(GameTestHelper helper) { DragonFightTests.ALL.get("speed_factor").accept(helper); }
    @GameTest(structure = "simpleenddragonfight:empty", maxTicks = 100) public void halfAndWaves(GameTestHelper helper) { DragonFightTests.ALL.get("half_and_waves").accept(helper); }
    @GameTest(structure = "simpleenddragonfight:empty", maxTicks = 100) public void miteCap(GameTestHelper helper) { DragonFightTests.ALL.get("mite_cap").accept(helper); }
    @GameTest(structure = "simpleenddragonfight:empty", maxTicks = 100) public void unboundMiteStays(GameTestHelper helper) { DragonFightTests.ALL.get("unbound_mite_stays").accept(helper); }
    @GameTest(structure = "simpleenddragonfight:empty", maxTicks = 100) public void entryQueue(GameTestHelper helper) { DragonFightTests.ALL.get("entry_queue").accept(helper); }
    @GameTest(structure = "simpleenddragonfight:empty", maxTicks = 100) public void switchOffIsVanilla(GameTestHelper helper) { DragonFightTests.ALL.get("switch_off_is_vanilla").accept(helper); }
    @GameTest(structure = "simpleenddragonfight:empty", maxTicks = 100) public void summonCounter(GameTestHelper helper) { DragonFightTests.ALL.get("summon_counter").accept(helper); }
    @GameTest(structure = "simpleenddragonfight:empty", maxTicks = 100) public void summonScaling(GameTestHelper helper) { DragonFightTests.ALL.get("summon_scaling").accept(helper); }
    @GameTest(structure = "simpleenddragonfight:empty", maxTicks = 100) public void noSimplebuildingNeeded(GameTestHelper helper) { DragonFightTests.ALL.get("no_simplebuilding_needed").accept(helper); }
}
