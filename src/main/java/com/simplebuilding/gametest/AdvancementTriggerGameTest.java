package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for {@link AdvancementTriggerTests}. No logic here; class and method names are load bearing,
 * Fabric derives the test id from them. Registered via the {@code fabric-gametest} entrypoint in fabric.mod.json.
 */
public final class AdvancementTriggerGameTest {

    @GameTest
    public void theCounterGrantsAtItsThresholdAndSurvivesSaveAndRespawn(GameTestHelper helper) {
        AdvancementTriggerTests.theCounterGrantsAtItsThresholdAndSurvivesSaveAndRespawn(helper);
    }

    @GameTest
    public void aSledgehammerSwingCountsTheBlocksItTookAlong(GameTestHelper helper) {
        AdvancementTriggerTests.aSledgehammerSwingCountsTheBlocksItTookAlong(helper);
    }

    @GameTest
    public void theMiningEnchantmentsEarnTheirAdvancementsOnFirstUse(GameTestHelper helper) {
        AdvancementTriggerTests.theMiningEnchantmentsEarnTheirAdvancementsOnFirstUse(helper);
    }

    @GameTest
    public void airJumpAndKineticProtectionEarnTheirAdvancementsOnFirstUse(GameTestHelper helper) {
        AdvancementTriggerTests.airJumpAndKineticProtectionEarnTheirAdvancementsOnFirstUse(helper);
    }

    @GameTest
    public void radianceDyedStorageAndAllOctantColorsFollowTheInventory(GameTestHelper helper) {
        AdvancementTriggerTests.radianceDyedStorageAndAllOctantColorsFollowTheInventory(helper);
    }

    @GameTest
    public void aFullBonusTrimSetIsNoticedByThePlayerTick(GameTestHelper helper) {
        AdvancementTriggerTests.aFullBonusTrimSetIsNoticedByThePlayerTick(helper);
    }

    @GameTest
    public void settingDownTheBackpackEarnsPitchingCamp(GameTestHelper helper) {
        AdvancementTriggerTests.settingDownTheBackpackEarnsPitchingCamp(helper);
    }

    @GameTest
    public void repairingTheBreakerPistonEarnsGoodAsNew(GameTestHelper helper) {
        AdvancementTriggerTests.repairingTheBreakerPistonEarnsGoodAsNew(helper);
    }

    @GameTest
    public void theVoidCatchesAThrownEnderiteItemForItsThrower(GameTestHelper helper) {
        AdvancementTriggerTests.theVoidCatchesAThrownEnderiteItemForItsThrower(helper);
    }

    @GameTest
    public void theCrackedEchoSounderShattersAndEarnsBrokenRecord(GameTestHelper helper) {
        AdvancementTriggerTests.theCrackedEchoSounderShattersAndEarnsBrokenRecord(helper);
    }

    @GameTest
    public void theLensBeamPrimingTntEarnsRemoteDetonation(GameTestHelper helper) {
        AdvancementTriggerTests.theLensBeamPrimingTntEarnsRemoteDetonation(helper);
    }
}
