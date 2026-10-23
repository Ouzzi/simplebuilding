package com.simplebuilding.gametest;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class Mech23GameTest {
    @GameTest(maxTicks = 100) public void chipsFlyAndHurtSlightly(GameTestHelper helper) { Mech23Tests.chipsFlyAndHurtSlightly(helper); }
    @GameTest public void coresAndWardenTendrilAreRare(GameTestHelper helper) { Mech23Tests.coresAndWardenTendrilAreRare(helper); }
}
