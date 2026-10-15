package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter for sulfur fire, copper fire and sulfur gunpowder (N33); the logic lives in {@link FireTests}. */
public final class FireGameTest {

    @GameTest(maxTicks = FireTests.FIRE_MAX_TICKS)
    public void sulfurFireBurnsOnSulfurAndGivesNausea(GameTestHelper helper) {
        FireTests.sulfurFireBurnsOnSulfurAndGivesNausea(helper);
    }

    @GameTest(maxTicks = FireTests.FIRE_MAX_TICKS)
    public void copperFireBurnsOnCopperWithoutEffect(GameTestHelper helper) {
        FireTests.copperFireBurnsOnCopperWithoutEffect(helper);
    }

    @GameTest
    public void firesKeepTheirBaseBlocks(GameTestHelper helper) {
        FireTests.firesKeepTheirBaseBlocks(helper);
    }

    @GameTest
    public void gunpowderCraftsFromSulfurAndCoal(GameTestHelper helper) {
        FireTests.gunpowderCraftsFromSulfurAndCoal(helper);
    }

    @GameTest(maxTicks = FireTests.FIRE_MAX_TICKS)
    public void sulfurLightsGlowAndCraft(GameTestHelper helper) {
        FireTests.sulfurLightsGlowAndCraft(helper);
    }
}
