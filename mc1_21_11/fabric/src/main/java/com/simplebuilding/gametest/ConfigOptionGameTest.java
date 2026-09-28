package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the config options.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link ConfigOptionTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class ConfigOptionGameTest {

    @GameTest
    public void bundleClickInversionFollowsTheConfiguredOption(GameTestHelper helper) {
        ConfigOptionTests.bundleClickInversionFollowsTheConfiguredOption(helper);
    }

    @GameTest
    public void lootTableChangesStopWhenTheOptionIsSwitchedOff(GameTestHelper helper) {
        ConfigOptionTests.lootTableChangesStopWhenTheOptionIsSwitchedOff(helper);
    }

    @GameTest
    public void lootBalanceKeepsEveryChestWithinItsBudget(GameTestHelper helper) {
        ConfigOptionTests.lootBalanceKeepsEveryChestWithinItsBudget(helper);
    }

    @GameTest
    public void buildingCoresAreVeryRareInLootChests(GameTestHelper helper) {
        ConfigOptionTests.buildingCoresAreVeryRareInLootChests(helper);
    }

    @GameTest
    public void everyConfigOptionKeepsItsPersistedNameAndDefault(GameTestHelper helper) {
        ConfigOptionTests.everyConfigOptionKeepsItsPersistedNameAndDefault(helper);
    }

    @GameTest
    public void everyOptionHasNameTooltipAndTab(GameTestHelper helper) {
        ConfigOptionTests.everyOptionHasNameTooltipAndTab(helper);
    }

    @GameTest
    public void theConfigCommandReachesEveryOption(GameTestHelper helper) {
        ConfigOptionTests.theConfigCommandReachesEveryOption(helper);
    }

    @GameTest
    public void newToolOptionsChangeWhatTheToolsDo(GameTestHelper helper) {
        ConfigOptionTests.newToolOptionsChangeWhatTheToolsDo(helper);
    }

    @GameTest
    public void newPadOptionsChangeWhatThePadsDo(GameTestHelper helper) {
        ConfigOptionTests.newPadOptionsChangeWhatThePadsDo(helper);
    }

    @GameTest
    public void newTweakOptionsChangeWhatTheTweaksDo(GameTestHelper helper) {
        ConfigOptionTests.newTweakOptionsChangeWhatTheTweaksDo(helper);
    }

    @GameTest
    public void coreLootChanceFollowsItsMultiplier(GameTestHelper helper) {
        ConfigOptionTests.coreLootChanceFollowsItsMultiplier(helper);
    }

    @GameTest
    public void theAirJumpCooldownTravelsFromServerToClient(GameTestHelper helper) {
        ConfigOptionTests.theAirJumpCooldownTravelsFromServerToClient(helper);
    }
}
