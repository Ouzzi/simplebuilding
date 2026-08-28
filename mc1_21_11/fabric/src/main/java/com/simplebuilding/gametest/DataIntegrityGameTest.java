package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the data integrity tests (MC 1.21.11 line). No logic here; see
 * {@link DataIntegrityTests}.
 */
public final class DataIntegrityGameTest {

    @GameTest
    public void everyModItemIsInTheItemRegistry(GameTestHelper helper) {
        DataIntegrityTests.everyModItemIsInTheItemRegistry(helper);
    }

    @GameTest
    public void everyModBlockIsRegisteredAndHasItsBlockItem(GameTestHelper helper) {
        DataIntegrityTests.everyModBlockIsRegisteredAndHasItsBlockItem(helper);
    }

    @GameTest
    public void modRecipesOnlyReferenceRegisteredItems(GameTestHelper helper) {
        DataIntegrityTests.modRecipesOnlyReferenceRegisteredItems(helper);
    }

    @GameTest
    public void everyModBlockLootTableLoads(GameTestHelper helper) {
        DataIntegrityTests.everyModBlockLootTableLoads(helper);
    }

    @GameTest(maxTicks = DataIntegrityTests.BLOCK_DROP_MAX_TICKS)
    public void brokenModBlocksDropTheirExpectedItem(GameTestHelper helper) {
        DataIntegrityTests.brokenModBlocksDropTheirExpectedItem(helper);
    }

    @GameTest
    public void modEnchantmentsArePresentInTheDatapackRegistry(GameTestHelper helper) {
        DataIntegrityTests.modEnchantmentsArePresentInTheDatapackRegistry(helper);
    }

    @GameTest
    public void modEnchantmentTagsResolveToTheExpectedEntries(GameTestHelper helper) {
        DataIntegrityTests.modEnchantmentTagsResolveToTheExpectedEntries(helper);
    }

    @GameTest
    public void voidProtectedTagIsLanguageIndependent(GameTestHelper helper) {
        DataIntegrityTests.voidProtectedTagIsLanguageIndependent(helper);
    }
}
