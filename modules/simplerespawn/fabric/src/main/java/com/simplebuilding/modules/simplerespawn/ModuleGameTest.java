package com.simplebuilding.modules.simplerespawn;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
/** Fabric runs only @GameTest methods: one wrapper per shared test body in {@link RespawnTests}. */
public final class ModuleGameTest {
    @GameTest(structure = "simplerespawn:empty", maxTicks = 100) public void configBounds(GameTestHelper helper) { RespawnTests.ALL.get("config_bounds").accept(helper); }
    @GameTest(structure = "simplerespawn:empty", maxTicks = 100) public void aloneIsNormalDeath(GameTestHelper helper) { RespawnTests.ALL.get("alone_is_normal_death").accept(helper); }
    @GameTest(structure = "simplerespawn:empty", maxTicks = 100) public void downedInsteadOfDeath(GameTestHelper helper) { RespawnTests.ALL.get("downed_instead_of_death").accept(helper); }
    @GameTest(structure = "simplerespawn:empty", maxTicks = 100) public void totemAndVoidStayVanilla(GameTestHelper helper) { RespawnTests.ALL.get("totem_and_void_stay_vanilla").accept(helper); }
    @GameTest(structure = "simplerespawn:empty", maxTicks = 100) public void bleedOutIsDeath(GameTestHelper helper) { RespawnTests.ALL.get("bleed_out_is_death").accept(helper); }
    @GameTest(structure = "simplerespawn:empty", maxTicks = 100) public void damageShortensBleedOut(GameTestHelper helper) { RespawnTests.ALL.get("damage_shortens_bleed_out").accept(helper); }
    @GameTest(structure = "simplerespawn:empty", maxTicks = 100) public void giveUpIsDeath(GameTestHelper helper) { RespawnTests.ALL.get("give_up_is_death").accept(helper); }
    @GameTest(structure = "simplerespawn:empty", maxTicks = 300) public void reviveGivesThreeHeartsNoFood(GameTestHelper helper) { RespawnTests.ALL.get("revive_gives_three_hearts_no_food").accept(helper); }
    @GameTest(structure = "simplerespawn:empty", maxTicks = 100) public void reviveBreaksOff(GameTestHelper helper) { RespawnTests.ALL.get("revive_breaks_off").accept(helper); }
    @GameTest(structure = "simplerespawn:empty", maxTicks = 100) public void logoutIsDeathWithoutDupe(GameTestHelper helper) { RespawnTests.ALL.get("logout_is_death_without_dupe").accept(helper); }
    @GameTest(structure = "simplerespawn:empty", maxTicks = 100) public void crashTagIsDeath(GameTestHelper helper) { RespawnTests.ALL.get("crash_tag_is_death").accept(helper); }
    @GameTest(structure = "simplerespawn:empty", maxTicks = 100) public void downedCannotAct(GameTestHelper helper) { RespawnTests.ALL.get("downed_cannot_act").accept(helper); }
    @GameTest(structure = "simplerespawn:empty", maxTicks = 100) public void guideBook(GameTestHelper helper) { RespawnTests.ALL.get("guide_book").accept(helper); }
    @GameTest(structure = "simplerespawn:empty", maxTicks = 100) public void noSimplebuildingNeeded(GameTestHelper helper) { RespawnTests.ALL.get("no_simplebuilding_needed").accept(helper); }
}
