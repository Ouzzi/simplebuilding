package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the per-effect potion pad rules ({@link PotionPadRuleTests}). No logic here; class
 * and method names are load bearing, Fabric derives the test id from them. Registered via the
 * {@code fabric-gametest} entrypoint in fabric.mod.json.
 */
public final class PotionPadRuleGameTest {

    @GameTest
    public void harmfulEffectsReachOnlyTheOwnerAndStrangersCannotChargeThePad(GameTestHelper helper) {
        PotionPadRuleTests.harmfulEffectsReachOnlyTheOwnerAndStrangersCannotChargeThePad(helper);
    }

    @GameTest
    public void blockedEffectsAreNeverGivenAndStartNoCooldown(GameTestHelper helper) {
        PotionPadRuleTests.blockedEffectsAreNeverGivenAndStartNoCooldown(helper);
    }

    @GameTest
    public void levelsAndDurationsNeverExceedVanillaOrThePotion(GameTestHelper helper) {
        PotionPadRuleTests.levelsAndDurationsNeverExceedVanillaOrThePotion(helper);
    }

    @GameTest
    public void theCooldownFollowsTheGrantedDurationAndTheEffectMultiplier(GameTestHelper helper) {
        PotionPadRuleTests.theCooldownFollowsTheGrantedDurationAndTheEffectMultiplier(helper);
    }

    @GameTest
    public void healingLocksThePlayerOutOfEveryPadForOneMinute(GameTestHelper helper) {
        PotionPadRuleTests.healingLocksThePlayerOutOfEveryPadForOneMinute(helper);
    }

    @GameTest
    public void mobsNeverReceiveAnyPadEffect(GameTestHelper helper) {
        PotionPadRuleTests.mobsNeverReceiveAnyPadEffect(helper);
    }

    @GameTest
    public void everyVanillaEffectHasDeliberateRules(GameTestHelper helper) {
        PotionPadRuleTests.everyVanillaEffectHasDeliberateRules(helper);
    }
}
