package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the armour trim bonus arithmetic.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link TrimBonusTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class TrimBonusGameTest {

    @GameTest
    public void tagKeyedPatternsCoverTheWholeDamageFamily(GameTestHelper helper) {
        TrimBonusTests.tagKeyedPatternsCoverTheWholeDamageFamily(helper);
    }

    @GameTest
    public void exactlyKeyedPatternsIgnoreTheirNeighbours(GameTestHelper helper) {
        TrimBonusTests.exactlyKeyedPatternsIgnoreTheirNeighbours(helper);
    }

    @GameTest
    public void magicIsSoftenedByTheVexPatternAndTheGoldAndLapisMaterials(GameTestHelper helper) {
        TrimBonusTests.magicIsSoftenedByTheVexPatternAndTheGoldAndLapisMaterials(helper);
    }

    @GameTest
    public void wildAndSilenceRideOnTheDamageMessageId(GameTestHelper helper) {
        TrimBonusTests.wildAndSilenceRideOnTheDamageMessageId(helper);
    }

    @GameTest
    public void flowReadsTheTypeNameOfTheProjectileThatLanded(GameTestHelper helper) {
        TrimBonusTests.flowReadsTheTypeNameOfTheProjectileThatLanded(helper);
    }

    @GameTest
    public void armourBypassingHitsSkipTheThreePhysicalMaterials(GameTestHelper helper) {
        TrimBonusTests.armourBypassingHitsSkipTheThreePhysicalMaterials(helper);
    }

    @GameTest
    public void ironAndQuartzMaterialsAddToTheirOwnPatterns(GameTestHelper helper) {
        TrimBonusTests.ironAndQuartzMaterialsAddToTheirOwnPatterns(helper);
    }

    @GameTest
    public void attackerKeyedMaterialsReadTheEntityBehindTheHit(GameTestHelper helper) {
        TrimBonusTests.attackerKeyedMaterialsReadTheEntityBehindTheHit(helper);
    }
}
