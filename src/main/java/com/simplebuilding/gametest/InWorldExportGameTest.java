package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the wiki export of the in-world transformations.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link InWorldExportTests}. Registered through the {@code fabric-gametest} entrypoint in
 * {@code fabric.mod.json}. Class and method names are load bearing: Fabric derives the test id
 * from them.
 */
public final class InWorldExportGameTest {

    @GameTest
    public void upgradeStepsNameTheWeakestHammerThatWorks(GameTestHelper helper) {
        InWorldExportTests.upgradeStepsNameTheWeakestHammerThatWorks(helper);
    }

    @GameTest
    public void reshapeTicksMatchTheUseDurationOfEveryHammer(GameTestHelper helper) {
        InWorldExportTests.reshapeTicksMatchTheUseDurationOfEveryHammer(helper);
    }

    @GameTest
    public void chiselTablesFollowTheToolTiers(GameTestHelper helper) {
        InWorldExportTests.chiselTablesFollowTheToolTiers(helper);
    }

    @GameTest
    public void jeiCatalogCoversEveryInWorldEntry(GameTestHelper helper) {
        InWorldExportTests.jeiCatalogCoversEveryInWorldEntry(helper);
    }
}
