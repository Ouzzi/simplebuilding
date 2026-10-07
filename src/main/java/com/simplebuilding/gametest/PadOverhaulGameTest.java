package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the owner's pad overhaul of 2026-09-28 ({@link PadOverhaulTests}). No logic here;
 * class and method names are load bearing, Fabric derives the test id from them. Registered via the
 * {@code fabric-gametest} entrypoint in fabric.mod.json.
 */
public final class PadOverhaulGameTest {

    @GameTest(maxTicks = 60)
    public void padEffectsFadeInAndStopSchedulingWhenSettled(GameTestHelper helper) {
        PadOverhaulTests.padEffectsFadeInAndStopSchedulingWhenSettled(helper);
    }

    @GameTest
    public void spawnTeleporterSignalChangesResetTheEntireWarmup(GameTestHelper helper) {
        PadOverhaulTests.spawnTeleporterSignalChangesResetTheEntireWarmup(helper);
    }

    @GameTest(maxTicks = PadOverhaulTests.WAIT_MAX_TICKS)
    public void spawnTeleporterTiersWaitFiftyTwentyAndFiveSeconds(GameTestHelper helper) {
        PadOverhaulTests.spawnTeleporterTiersWaitFiftyTwentyAndFiveSeconds(helper);
    }

    @GameTest
    public void oldSpawnTeleportersBecomeTheirNewTierInTheWorldAndTheInventory(GameTestHelper helper) {
        PadOverhaulTests.oldSpawnTeleportersBecomeTheirNewTierInTheWorldAndTheInventory(helper);
    }

    @GameTest
    public void oldElytraPadsBecomeTheirNewTierInTheWorldAndTheInventory(GameTestHelper helper) {
        PadOverhaulTests.oldElytraPadsBecomeTheirNewTierInTheWorldAndTheInventory(helper);
    }

    @GameTest
    public void tierOneOfEveryPadFamilyIsSmithedFromItsPlateAndUnlockItem(GameTestHelper helper) {
        PadOverhaulTests.tierOneOfEveryPadFamilyIsSmithedFromItsPlateAndUnlockItem(helper);
    }

    @GameTest(maxTicks = PadOverhaulTests.NO_TEXT_MAX_TICKS)
    public void padsAndGadgetsWriteNoTextOnTheScreen(GameTestHelper helper) {
        PadOverhaulTests.padsAndGadgetsWriteNoTextOnTheScreen(helper);
    }

    @GameTest
    public void theEchoSounderCooldownIsFourTimesLonger(GameTestHelper helper) {
        PadOverhaulTests.theEchoSounderCooldownIsFourTimesLonger(helper);
    }

    @GameTest
    public void theEchoSounderDoesNotRelinkTheLodestoneItIsLinkedTo(GameTestHelper helper) {
        PadOverhaulTests.theEchoSounderDoesNotRelinkTheLodestoneItIsLinkedTo(helper);
    }

    @GameTest
    public void redstoneSwitchesLaunchpadsAndFlypadsOffAndComparatorsReadThem(GameTestHelper helper) {
        PadOverhaulTests.redstoneSwitchesLaunchpadsAndFlypadsOffAndComparatorsReadThem(helper);
    }

    @GameTest(maxTicks = PadOverhaulTests.HOPPER_MAX_TICKS)
    public void hoppersFillOnlyWindChargesIntoTheLaunchpad(GameTestHelper helper) {
        PadOverhaulTests.hoppersFillOnlyWindChargesIntoTheLaunchpad(helper);
    }

    @GameTest(maxTicks = PadOverhaulTests.DESTINATION_MAX_TICKS)
    public void theSpawnTeleporterTakesPlayersToTheirBedAndWithRedstoneToTheWorldSpawn(GameTestHelper helper) {
        PadOverhaulTests.theSpawnTeleporterTakesPlayersToTheirBedAndWithRedstoneToTheWorldSpawn(helper);
    }

    @GameTest
    public void theEchoSounderLocksForUpToFiveSecondsAfterAnAttemptDependingOnTheDistance(GameTestHelper helper) {
        PadOverhaulTests.theEchoSounderLocksForUpToFiveSecondsAfterAnAttemptDependingOnTheDistance(helper);
    }

    @GameTest
    public void aWrongNuggetOnTheSledgehammerWritesNothingAndDoesNotTilt(GameTestHelper helper) {
        PadOverhaulTests.aWrongNuggetOnTheSledgehammerWritesNothingAndDoesNotTilt(helper);
    }

    @GameTest(maxTicks = PadOverhaulTests.STATE_MAX_TICKS)
    public void everyPadFamilyShowsWhetherItIsWorking(GameTestHelper helper) {
        PadOverhaulTests.everyPadFamilyShowsWhetherItIsWorking(helper);
    }
}
