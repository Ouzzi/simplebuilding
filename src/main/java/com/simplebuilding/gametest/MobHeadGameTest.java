package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the mob heads ({@link MobHeadTests}): drops, the flypad recipe and every secret ability. No
 * logic here; class and method names are load bearing, Fabric derives the test id from them. Registered via the
 * {@code fabric-gametest} entrypoint in fabric.mod.json.
 */
public final class MobHeadGameTest {

    @GameTest
    public void chargedCreepersDropTheNewHeadsAndNoOtherDeathDoes(GameTestHelper helper) {
        MobHeadTests.chargedCreepersDropTheNewHeadsAndNoOtherDeathDoes(helper);
    }

    @GameTest
    public void everyModHeadIsWearableLikeVanillaSkullsAndKeepsItsSecret(GameTestHelper helper) {
        MobHeadTests.everyModHeadIsWearableLikeVanillaSkullsAndKeepsItsSecret(helper);
    }

    @GameTest
    public void flypadOneNeedsTheShulkerHeadAndAnElytraWithMending(GameTestHelper helper) {
        MobHeadTests.flypadOneNeedsTheShulkerHeadAndAnElytraWithMending(helper);
    }

    @GameTest
    public void blazeHeadWearersIgnoreMagmaBlocksAndCampfires(GameTestHelper helper) {
        MobHeadTests.blazeHeadWearersIgnoreMagmaBlocksAndCampfires(helper);
    }

    @GameTest
    public void endermanHeadWearersTakeNoEnderPearlDamage(GameTestHelper helper) {
        MobHeadTests.endermanHeadWearersTakeNoEnderPearlDamage(helper);
    }

    @GameTest
    public void huskHeadSkipsTheHungerOfFood(GameTestHelper helper) {
        MobHeadTests.huskHeadSkipsTheHungerOfFood(helper);
    }

    @GameTest
    public void boggedSkullSkipsThePoisonOfFood(GameTestHelper helper) {
        MobHeadTests.boggedSkullSkipsThePoisonOfFood(helper);
    }

    @GameTest
    public void spiderHeadWearersAreNotSlowedByCobwebs(GameTestHelper helper) {
        MobHeadTests.spiderHeadWearersAreNotSlowedByCobwebs(helper);
    }

    @GameTest
    public void caveSpiderHeadCutsCobwebsAsFastAsSwords(GameTestHelper helper) {
        MobHeadTests.caveSpiderHeadCutsCobwebsAsFastAsSwords(helper);
    }

    @GameTest
    public void straySkullKeepsPowderSnowFromFreezing(GameTestHelper helper) {
        MobHeadTests.straySkullKeepsPowderSnowFromFreezing(helper);
    }

    @GameTest
    public void slimeHeadAddsOneBlockOfSafeFall(GameTestHelper helper) {
        MobHeadTests.slimeHeadAddsOneBlockOfSafeFall(helper);
    }

    @GameTest
    public void silverfishHeadShrinksTheWearerToHalfSize(GameTestHelper helper) {
        MobHeadTests.silverfishHeadShrinksTheWearerToHalfSize(helper);
    }

    @GameTest
    public void breezeHeadWearersDoNotTrampleFarmland(GameTestHelper helper) {
        MobHeadTests.breezeHeadWearersDoNotTrampleFarmland(helper);
    }

    @GameTest
    public void shulkerHeadOpensBlockedShulkerBoxes(GameTestHelper helper) {
        MobHeadTests.shulkerHeadOpensBlockedShulkerBoxes(helper);
    }

    @GameTest
    public void drownedHeadResistsDownwardBubbleColumns(GameTestHelper helper) {
        MobHeadTests.drownedHeadResistsDownwardBubbleColumns(helper);
    }
}
