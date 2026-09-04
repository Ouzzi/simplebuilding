package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the storage and player enchantments.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link StorageEnchantmentTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class StorageEnchantmentGameTest {

    @GameTest
    public void funnelFilterDecidesWhatTheTouchSweepsUp(GameTestHelper helper) {
        StorageEnchantmentTests.funnelFilterDecidesWhatTheTouchSweepsUp(helper);
    }

    @GameTest
    public void drawerKindCapHoldsAgainstTheFunnelToo(GameTestHelper helper) {
        StorageEnchantmentTests.drawerKindCapHoldsAgainstTheFunnelToo(helper);
    }

    @GameTest
    public void funnelQuiverSweepsArrowsOnlyAndStopsAtItsBrim(GameTestHelper helper) {
        StorageEnchantmentTests.funnelQuiverSweepsArrowsOnlyAndStopsAtItsBrim(helper);
    }

    @GameTest
    public void drawerAndDeepPocketsMultiplyOnTheSameContainer(GameTestHelper helper) {
        StorageEnchantmentTests.drawerAndDeepPocketsMultiplyOnTheSameContainer(helper);
    }

    @GameTest
    public void storageBooksSitInTheChestsTheyAreMeantFor(GameTestHelper helper) {
        StorageEnchantmentTests.storageBooksSitInTheChestsTheyAreMeantFor(helper);
    }

    @GameTest
    public void lootQuiversCarryOneOfTheContainerEnchantments(GameTestHelper helper) {
        StorageEnchantmentTests.lootQuiversCarryOneOfTheContainerEnchantments(helper);
    }
}
