package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the part taken over from Simple Tweaks ({@link TweaksTests}). No logic here;
 * class and method names are load bearing, Fabric derives the test id from them. Registered via
 * the {@code fabric-gametest} entrypoint in fabric.mod.json.
 */
public final class TweaksGameTest {

    @GameTest
    public void padTiersGrowAndEnderiteSitsBetweenNetheriteAndTheNetherStarTier(GameTestHelper helper) {
        TweaksTests.padTiersGrowAndEnderiteSitsBetweenNetheriteAndTheNetherStarTier(helper);
    }

    @GameTest
    public void enderiteTiersAreSmithedFromTheNetheriteTierAndTheNetherStarTiersFromEnderite(GameTestHelper helper) {
        TweaksTests.enderiteTiersAreSmithedFromTheNetheriteTierAndTheNetherStarTiersFromEnderite(helper);
    }

    @GameTest
    public void theStellarFlypadIsCraftedFromEnderiteFlypads(GameTestHelper helper) {
        TweaksTests.theStellarFlypadIsCraftedFromEnderiteFlypads(helper);
    }

    @GameTest
    public void theEchoCompassIsCraftedFromTheRecoveryCompassTheEnderiteCoreAndNetheritePlates(GameTestHelper helper) {
        TweaksTests.theEchoCompassIsCraftedFromTheRecoveryCompassTheEnderiteCoreAndNetheritePlates(helper);
    }

    @GameTest
    public void elytraPadsEquipAnUnsafeSpawnElytraInTheirArea(GameTestHelper helper) {
        TweaksTests.elytraPadsEquipAnUnsafeSpawnElytraInTheirArea(helper);
    }

    @GameTest
    public void elytraPadsRechargeBoostsInTheColumnAndFromEnderiteOnInTheWholeArea(GameTestHelper helper) {
        TweaksTests.elytraPadsRechargeBoostsInTheColumnAndFromEnderiteOnInTheWholeArea(helper);
    }

    @GameTest
    public void theSpawnElytraBoostSpendsOneChargeAndOnlyWhileGliding(GameTestHelper helper) {
        TweaksTests.theSpawnElytraBoostSpendsOneChargeAndOnlyWhileGliding(helper);
    }

    @GameTest
    public void safeSpawnElytrasAndEnderiteLaunchesPreventFallDamage(GameTestHelper helper) {
        TweaksTests.safeSpawnElytrasAndEnderiteLaunchesPreventFallDamage(helper);
    }

    @GameTest
    public void padElytrasExpireEvenWithTheSpawnElytraSwitchedOff(GameTestHelper helper) {
        TweaksTests.padElytrasExpireEvenWithTheSpawnElytraSwitchedOff(helper);
    }

    @GameTest
    public void flypadsGrantFlightInsideAndTakeItBackOutside(GameTestHelper helper) {
        TweaksTests.flypadsGrantFlightInsideAndTakeItBackOutside(helper);
    }

    @GameTest
    public void enderiteFlypadsCatchFlyersLeavingTheirAreaWithSlowFalling(GameTestHelper helper) {
        TweaksTests.enderiteFlypadsCatchFlyersLeavingTheirAreaWithSlowFalling(helper);
    }

    @GameTest
    public void switchedOffFlypadsGrantNoFlight(GameTestHelper helper) {
        TweaksTests.switchedOffFlypadsGrantNoFlight(helper);
    }

    @GameTest(maxTicks = TweaksTests.TELEPORTER_MAX_TICKS)
    public void spawnTeleportersSendStillPlayersToTheirSpawnPoint(GameTestHelper helper) {
        TweaksTests.spawnTeleportersSendStillPlayersToTheirSpawnPoint(helper);
    }

    @GameTest
    public void spawnTeleporterTargetsFallBackToTheWorldSpawn(GameTestHelper helper) {
        TweaksTests.spawnTeleporterTargetsFallBackToTheWorldSpawn(helper);
    }

    @GameTest(maxTicks = TweaksTests.COPPER_MAX_TICKS)
    public void copperPlatesWaitLongerTheMoreTheyOxidized(GameTestHelper helper) {
        TweaksTests.copperPlatesWaitLongerTheMoreTheyOxidized(helper);
    }

    @GameTest
    public void copperPlatesOxidizeInOrderAndTheAxeScrapesThemBack(GameTestHelper helper) {
        TweaksTests.copperPlatesOxidizeInOrderAndTheAxeScrapesThemBack(helper);
    }

    @GameTest(maxTicks = TweaksTests.COPPER_MAX_TICKS)
    public void diamondPressurePlatesReactToPlayersOnly(GameTestHelper helper) {
        TweaksTests.diamondPressurePlatesReactToPlayersOnly(helper);
    }

    @GameTest
    public void netheritePlatesAdmitOnlyHoldersOfBarrelItems(GameTestHelper helper) {
        TweaksTests.netheritePlatesAdmitOnlyHoldersOfBarrelItems(helper);
    }

    @GameTest
    public void enderitePlatesLockToTheirOwnerAndNamedTags(GameTestHelper helper) {
        TweaksTests.enderitePlatesLockToTheirOwnerAndNamedTags(helper);
    }

    @GameTest
    public void ownersBreakTheirPadsFastAndStrangersSlowly(GameTestHelper helper) {
        TweaksTests.ownersBreakTheirPadsFastAndStrangersSlowly(helper);
    }

    @GameTest
    public void placingPadsMakesThePlacerTheOwner(GameTestHelper helper) {
        TweaksTests.placingPadsMakesThePlacerTheOwner(helper);
    }

    @GameTest
    public void chunkLoadersForceTheirChunksAndReleaseOnlyTheirOwn(GameTestHelper helper) {
        TweaksTests.chunkLoadersForceTheirChunksAndReleaseOnlyTheirOwn(helper);
    }

    @GameTest
    public void launchpadsHoldSixteenWindChargesAndTheEnderiteOneThirtyTwo(GameTestHelper helper) {
        TweaksTests.launchpadsHoldSixteenWindChargesAndTheEnderiteOneThirtyTwo(helper);
    }

    @GameTest
    public void theEchoCompassLinksToTheLodestoneAndTeleportsForOnePearl(GameTestHelper helper) {
        TweaksTests.theEchoCompassLinksToTheLodestoneAndTeleportsForOnePearl(helper);
    }

    @GameTest
    public void unbreakingProtectsTheEchoCompass(GameTestHelper helper) {
        TweaksTests.unbreakingProtectsTheEchoCompass(helper);
    }

    @GameTest
    public void theFirstJoinGiftComesOnceAndHonoursSimpleTweaksPlayers(GameTestHelper helper) {
        TweaksTests.theFirstJoinGiftComesOnceAndHonoursSimpleTweaksPlayers(helper);
    }

    @GameTest
    public void theNetherAndTheEndCanBeLockedByConfig(GameTestHelper helper) {
        TweaksTests.theNetherAndTheEndCanBeLockedByConfig(helper);
    }

    @GameTest
    public void xpOrbsClumpWithoutLosingExperience(GameTestHelper helper) {
        TweaksTests.xpOrbsClumpWithoutLosingExperience(helper);
    }

    @GameTest
    public void theRocketStackSizeFollowsTheConfig(GameTestHelper helper) {
        TweaksTests.theRocketStackSizeFollowsTheConfig(helper);
    }

    @GameTest
    public void killBoatsRemovesBoatsByMode(GameTestHelper helper) {
        TweaksTests.killBoatsRemovesBoatsByMode(helper);
    }

    @GameTest
    public void tweaksConfigKeepsItsNamesAndDefaults(GameTestHelper helper) {
        TweaksTests.tweaksConfigKeepsItsNamesAndDefaults(helper);
    }

    @GameTest
    public void theTweaksCommandsWriteTheConfig(GameTestHelper helper) {
        TweaksTests.theTweaksCommandsWriteTheConfig(helper);
    }
}
