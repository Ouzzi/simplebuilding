package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the mixed octet cells ({@link OctetMixTests}); the chess tests themselves moved to Simple Chess (b-chess47).
 * The class keeps its name: Fabric derives the test ids ({@code chess_game_test_octets_*}) from class and method names.
 */
public final class ChessGameTest {

    @GameTest
    public void octetsMixMaterialsInOneCell(GameTestHelper helper) {
        OctetMixTests.octetsMixMaterialsInOneCell(helper);
    }

    @GameTest
    public void octetsBreakOnlyTheLookedAtOctet(GameTestHelper helper) {
        OctetMixTests.octetsBreakOnlyTheLookedAtOctet(helper);
    }

    @GameTest
    public void octetsMixedCellDropsEveryMaterial(GameTestHelper helper) {
        OctetMixTests.octetsMixedCellDropsEveryMaterial(helper);
    }

    @GameTest
    public void octetsOldCellsMigrateAndMixedCellsPersist(GameTestHelper helper) {
        OctetMixTests.octetsOldCellsMigrateAndMixedCellsPersist(helper);
    }

    @GameTest
    public void octetsNormalBreakTakesTheWholeCell(GameTestHelper helper) {
        OctetMixTests.octetsNormalBreakTakesTheWholeCell(helper);
    }

    @GameTest
    public void octetsSledgehammerFollowsTheSneakRule(GameTestHelper helper) {
        OctetMixTests.octetsSledgehammerFollowsTheSneakRule(helper);
    }
}
