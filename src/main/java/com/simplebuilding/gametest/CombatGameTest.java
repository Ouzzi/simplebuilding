package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the combat wave 2026-10-02 (arrow recovery, shulker boxes, resonance rod scan, Crafty Shulker).
 * No test logic here: every method delegates to {@link CombatTests}. Class and method names are load bearing.
 */
public final class CombatGameTest {

    @GameTest
    public void arrowsInMobsDropWhenTheyDie(GameTestHelper helper) {
        CombatTests.arrowsInMobsDropWhenTheyDie(helper);
    }

    @GameTest
    public void killingArrowsDropAtOnce(GameTestHelper helper) {
        CombatTests.killingArrowsDropAtOnce(helper);
    }

    @GameTest
    public void onlyPickableArrowsFromPlayersComeBack(GameTestHelper helper) {
        CombatTests.onlyPickableArrowsFromPlayersComeBack(helper);
    }

    @GameTest
    public void shulkerBoxesCannotBeEnchanted(GameTestHelper helper) {
        CombatTests.shulkerBoxesCannotBeEnchanted(helper);
    }

    @GameTest
    public void theResonanceRodScansCreaturesButNotPlayers(GameTestHelper helper) {
        CombatTests.theResonanceRodScansCreaturesButNotPlayers(helper);
    }

    @GameTest
    public void craftyShulkerPotionsAreRegisteredAndBrewable(GameTestHelper helper) {
        CombatTests.craftyShulkerPotionsAreRegisteredAndBrewable(helper);
    }

    @GameTest
    public void craftyShulkerOnlyTriggersOnHitsByCreatures(GameTestHelper helper) {
        CombatTests.craftyShulkerOnlyTriggersOnHitsByCreatures(helper);
    }

    @GameTest
    public void craftyShulkerLandsOnlyOnSafeGround(GameTestHelper helper) {
        CombatTests.craftyShulkerLandsOnlyOnSafeGround(helper);
    }

    @GameTest
    public void craftyShulkerTeleportsWhenHitAndRespectsTheCooldown(GameTestHelper helper) {
        CombatTests.craftyShulkerTeleportsWhenHitAndRespectsTheCooldown(helper);
    }
}
