package com.simplebuilding.gametest;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class Mech23GameTest {
    @GameTest(maxTicks = 100) public void chipsFlyAndHurtALittle(GameTestHelper helper) { Mech23Tests.chipsFlyAndHurtALittle(helper); }
    @GameTest public void coresAndWardenTendrilAreRare(GameTestHelper helper) { Mech23Tests.coresAndWardenTendrilAreRare(helper); }
}
