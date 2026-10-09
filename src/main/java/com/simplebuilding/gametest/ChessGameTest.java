package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for chess (octets, pieces, checker stairs/slabs). No test logic here: every method delegates to
 * {@link ChessTests}. Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class ChessGameTest {

    @GameTest
    public void octetsFillTheSubGridByHitPoint(GameTestHelper helper) {
        ChessTests.octetsFillTheSubGridByHitPoint(helper);
    }

    @GameTest
    public void octetsHoldWaterUntilFull(GameTestHelper helper) {
        ChessTests.octetsHoldWaterUntilFull(helper);
    }

    @GameTest
    public void piecesStandOnQuartersAndSwap(GameTestHelper helper) {
        ChessTests.piecesStandOnQuartersAndSwap(helper);
    }

    @GameTest
    public void chessRecipesCutFromCheckersAndOctets(GameTestHelper helper) {
        ChessTests.chessRecipesCutFromCheckersAndOctets(helper);
    }

    @GameTest
    public void octetsStackToTheEngineMaximum(GameTestHelper helper) {
        ChessTests.octetsStackToTheEngineMaximum(helper);
    }
}
