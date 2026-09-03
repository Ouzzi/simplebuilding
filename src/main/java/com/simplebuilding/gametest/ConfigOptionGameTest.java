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
    public void tradeSwitchConditionsStillNameRealConfigFieldsOnBothLoaders(GameTestHelper helper) {
        ConfigOptionTests.tradeSwitchConditionsStillNameRealConfigFieldsOnBothLoaders(helper);
    }

    @GameTest
    public void everyConfigOptionKeepsItsPersistedNameAndDefault(GameTestHelper helper) {
        ConfigOptionTests.everyConfigOptionKeepsItsPersistedNameAndDefault(helper);
    }
}
