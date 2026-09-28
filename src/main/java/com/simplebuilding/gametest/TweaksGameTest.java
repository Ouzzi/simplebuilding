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
    public void theStellarFlypadIsSmithedFromTwoReinforcedFlypads(GameTestHelper helper) {
        TweaksTests.theStellarFlypadIsSmithedFromTwoReinforcedFlypads(helper);
    }

    @GameTest
    public void theEchoSounderIsCraftedFromTheRecoveryCompassTheEnderiteCoreAndSevenEnderiteNuggets(GameTestHelper helper) {
        TweaksTests.theEchoSounderIsCraftedFromTheRecoveryCompassTheEnderiteCoreAndSevenEnderiteNuggets(helper);
    }

    @GameTest
    public void theVelocityGaugeIsCraftedWithQuartzCornersCopperNuggetsAndTheCopperCore(GameTestHelper helper) {
        TweaksTests.theVelocityGaugeIsCraftedWithQuartzCornersCopperNuggetsAndTheCopperCore(helper);
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
    public void theEchoSounderLinksToTheLodestoneAndTeleportsWithoutAnyPearl(GameTestHelper helper) {
        TweaksTests.theEchoSounderLinksToTheLodestoneAndTeleportsWithoutAnyPearl(helper);
    }

    @GameTest
    public void theEchoSounderKeepsItsIdButIsNamedEchoSounder(GameTestHelper helper) {
        TweaksTests.theEchoSounderKeepsItsIdButIsNamedEchoSounder(helper);
    }

    @GameTest
    public void unbreakingLowersHowMuchTheJumpEmptiesTheEchoCompass(GameTestHelper helper) {
        TweaksTests.unbreakingLowersHowMuchTheJumpEmptiesTheEchoCompass(helper);
    }

    @GameTest
    public void theEchoCompassChargesForThreeSecondsAndReleasingEarlyCostsNothing(GameTestHelper helper) {
        TweaksTests.theEchoCompassChargesForThreeSecondsAndReleasingEarlyCostsNothing(helper);
    }

    @GameTest
    public void aFullChargeJumpsAndLeavesTheEchoCompassEmpty(GameTestHelper helper) {
        TweaksTests.aFullChargeJumpsAndLeavesTheEchoCompassEmpty(helper);
    }

    @GameTest
    public void theEchoCompassIsOnlyChargedAgainAfterFifteenHundredRepairPoints(GameTestHelper helper) {
        TweaksTests.theEchoCompassIsOnlyChargedAgainAfterFifteenHundredRepairPoints(helper);
    }

    @GameTest
    public void aCrackedEchoCompassChargesTwiceAsLongAndShattersAfterTheJump(GameTestHelper helper) {
        TweaksTests.aCrackedEchoCompassChargesTwiceAsLongAndShattersAfterTheJump(helper);
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

    @GameTest
    public void spawnElytrasVanishAsSoonAsTheyLeaveTheChestSlot(GameTestHelper helper) {
        TweaksTests.spawnElytrasVanishAsSoonAsTheyLeaveTheChestSlot(helper);
    }

    @GameTest
    public void theSpawnAreaLiesOnlyInTheSpawnDimensionAndFallProtectionCoversAllOfIt(GameTestHelper helper) {
        TweaksTests.theSpawnAreaLiesOnlyInTheSpawnDimensionAndFallProtectionCoversAllOfIt(helper);
    }

    @GameTest
    public void chunkLoadersReleaseOnSetblockAndHandOverSharedChunks(GameTestHelper helper) {
        TweaksTests.chunkLoadersReleaseOnSetblockAndHandOverSharedChunks(helper);
    }

    @GameTest
    public void overlappingFlypadsKeepThePlayerFlyingAndTakeOnlyTheirOwnFlight(GameTestHelper helper) {
        TweaksTests.overlappingFlypadsKeepThePlayerFlyingAndTakeOnlyTheirOwnFlight(helper);
    }

    @GameTest
    public void theLaserRelayChecksTheSenderAndOnlyReachesNearbyPlayers(GameTestHelper helper) {
        TweaksTests.theLaserRelayChecksTheSenderAndOnlyReachesNearbyPlayers(helper);
    }

    @GameTest
    public void theClientRelevantTweaksValuesAreSentAtLoginAndOnEveryChange(GameTestHelper helper) {
        TweaksTests.theClientRelevantTweaksValuesAreSentAtLoginAndOnEveryChange(helper);
    }

    @GameTest
    public void aBlockedEchoCompassJumpCostsNothing(GameTestHelper helper) {
        TweaksTests.aBlockedEchoCompassJumpCostsNothing(helper);
    }

    @GameTest
    public void theAmethystLensIsCraftedAroundAnIronCore(GameTestHelper helper) {
        TweaksTests.theAmethystLensIsCraftedAroundAnIronCore(helper);
    }

    @GameTest
    public void theLensBeamMeltsIceAndSnow(GameTestHelper helper) {
        TweaksTests.theLensBeamMeltsIceAndSnow(helper);
    }

    @GameTest
    public void theLensBeamIgnitesFlammableBlocksOnlyAfterDwelling(GameTestHelper helper) {
        TweaksTests.theLensBeamIgnitesFlammableBlocksOnlyAfterDwelling(helper);
    }

    @GameTest
    public void theLensBeamLightsSoulFireCampfiresAndCandles(GameTestHelper helper) {
        TweaksTests.theLensBeamLightsSoulFireCampfiresAndCandles(helper);
    }

    @GameTest
    public void theLensBeamNeverLightsNetherPortals(GameTestHelper helper) {
        TweaksTests.theLensBeamNeverLightsNetherPortals(helper);
    }

    @GameTest
    public void theLensBeamPrimesTntAfterDwellingButRespectsTheRules(GameTestHelper helper) {
        TweaksTests.theLensBeamPrimesTntAfterDwellingButRespectsTheRules(helper);
    }

    @GameTest
    public void theLensBeamDriesWetSponges(GameTestHelper helper) {
        TweaksTests.theLensBeamDriesWetSponges(helper);
    }

    @GameTest
    public void theLensBeamRespectsAdventureModeAndTheFireSpreadRule(GameTestHelper helper) {
        TweaksTests.theLensBeamRespectsAdventureModeAndTheFireSpreadRule(helper);
    }

    @GameTest
    public void theLensChargeRunsDownButTheLensNeverBreaks(GameTestHelper helper) {
        TweaksTests.theLensChargeRunsDownButTheLensNeverBreaks(helper);
    }

    @GameTest
    public void theLensDrainsChargeEvenWhenItPointsIntoTheAir(GameTestHelper helper) {
        TweaksTests.theLensDrainsChargeEvenWhenItPointsIntoTheAir(helper);
    }

    @GameTest
    public void theLensRecordsTheLastMeasurementOnlyWithConstructorsTouch(GameTestHelper helper) {
        TweaksTests.theLensRecordsTheLastMeasurementOnlyWithConstructorsTouch(helper);
    }

    @GameTest
    public void theLensDwellTimeGrowsModeratelyWithDistance(GameTestHelper helper) {
        TweaksTests.theLensDwellTimeGrowsModeratelyWithDistance(helper);
    }

    @GameTest
    public void theLensSetsLivingEntitiesOnFireTakingTwiceAsLong(GameTestHelper helper) {
        TweaksTests.theLensSetsLivingEntitiesOnFireTakingTwiceAsLong(helper);
    }

    @GameTest
    public void theLensOnlyIgnitesPlayersWhenPvpAllowsIt(GameTestHelper helper) {
        TweaksTests.theLensOnlyIgnitesPlayersWhenPvpAllowsIt(helper);
    }

    @GameTest
    public void theLensHumsOnAnySurfaceAndSizzlesOrCracklesWhileHeating(GameTestHelper helper) {
        TweaksTests.theLensHumsOnAnySurfaceAndSizzlesOrCracklesWhileHeating(helper);
    }

    @GameTest
    public void anvilRechargeWithRedstoneCostsNoLevels(GameTestHelper helper) {
        TweaksTests.anvilRechargeWithRedstoneCostsNoLevels(helper);
    }

    @GameTest
    public void theWorldSpawnCommandTakesEffectImmediately(GameTestHelper helper) {
        TweaksTests.theWorldSpawnCommandTakesEffectImmediately(helper);
    }

    @GameTest
    public void killBoatsAllDropsTheContentsOfChestBoats(GameTestHelper helper) {
        TweaksTests.killBoatsAllDropsTheContentsOfChestBoats(helper);
    }

    @GameTest
    public void flightTimeAndBoostsAreCappedAndBrokenLaunchpadsDropTheirCharges(GameTestHelper helper) {
        TweaksTests.flightTimeAndBoostsAreCappedAndBrokenLaunchpadsDropTheirCharges(helper);
    }

    @GameTest
    public void forcedExactRespawnPutsThePlayerOnTheBedCentre(GameTestHelper helper) {
        TweaksTests.forcedExactRespawnPutsThePlayerOnTheBedCentre(helper);
    }

    @GameTest
    public void commandTeleportsPassTheDimensionLockAndTheLockMessageWaits(GameTestHelper helper) {
        TweaksTests.commandTeleportsPassTheDimensionLockAndTheLockMessageWaits(helper);
    }

    @GameTest
    public void aChunkLoaderReplacedByAnotherLoaderTypeReleasesItsChunks(GameTestHelper helper) {
        TweaksTests.aChunkLoaderReplacedByAnotherLoaderTypeReleasesItsChunks(helper);
    }

    @GameTest
    public void onlyWorldSpawnCommandsReapplyTheCustomWorldSpawn(GameTestHelper helper) {
        TweaksTests.onlyWorldSpawnCommandsReapplyTheCustomWorldSpawn(helper);
    }

    @GameTest
    public void theFirstJoinKeyMigrationSavesTheConfigOnce(GameTestHelper helper) {
        TweaksTests.theFirstJoinKeyMigrationSavesTheConfigOnce(helper);
    }

    @GameTest
    public void creativePlayersLoseTheStaleFlypadFlightTag(GameTestHelper helper) {
        TweaksTests.creativePlayersLoseTheStaleFlypadFlightTag(helper);
    }
}
