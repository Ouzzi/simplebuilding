package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the Pulsating Armor Trim and the template/attractor names ({@link PulsatingTrimTests}).
 * No logic here; class and method names are load bearing, Fabric derives the test id from them.
 * Registered via the {@code fabric-gametest} entrypoint in fabric.mod.json.
 */
public final class PulsatingTrimGameTest {

    @GameTest
    public void thePulsatingTemplateIsCraftedFromAnEchoShardAndAnySledgehammerThatStays(GameTestHelper helper) {
        PulsatingTrimTests.thePulsatingTemplateIsCraftedFromAnEchoShardAndAnySledgehammerThatStays(helper);
    }

    @GameTest
    public void thePulsatingUpgradeMakesTheTrimPulseOnceAndCombinesWithGlowing(GameTestHelper helper) {
        PulsatingTrimTests.thePulsatingUpgradeMakesTheTrimPulseOnceAndCombinesWithGlowing(helper);
    }

    @GameTest
    public void templatesAndTheAttractorCarryTheirNewNames(GameTestHelper helper) {
        PulsatingTrimTests.templatesAndTheAttractorCarryTheirNewNames(helper);
    }
}
