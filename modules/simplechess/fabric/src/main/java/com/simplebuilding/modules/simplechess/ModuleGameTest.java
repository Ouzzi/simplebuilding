package com.simplebuilding.modules.simplechess;

import com.simplechess.test.ChessOctetTests;
import com.simplechess.test.ChessTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter: ids module_game_test_<name>, bodies in {@link ChessTests} and (only with Simple Octets, its classes are named
 * there) {@link ChessOctetTests}. The default empty structure, as the tests had in SimpleBuilding.
 */
public final class ModuleGameTest {
    private static boolean noOctets(GameTestHelper helper) {
        if (FabricLoader.getInstance().isModLoaded("simpleoctets")) return false;
        helper.succeed();
        return true;
    }

    @GameTest
    public void piecesStandOnQuartersAndSwap(GameTestHelper helper) {
        ChessTests.piecesStandOnQuartersAndSwap(helper);
    }

    @GameTest
    public void piecesBreakOnlyTheLookedAtPiece(GameTestHelper helper) {
        ChessTests.piecesBreakOnlyTheLookedAtPiece(helper);
    }

    @GameTest
    public void chessRecipesCutFromCheckersAndOctets(GameTestHelper helper) {
        ChessTests.chessRecipesCutFromCheckersAndOctets(helper);
    }

    @GameTest
    public void paletteColoursFollowTheirSource(GameTestHelper helper) {
        ChessTests.paletteColoursFollowTheirSource(helper);
    }

    @GameTest
    public void oldSimplebuildingIdsMigrate(GameTestHelper helper) {
        ChessTests.oldSimplebuildingIdsMigrate(helper);
    }

    @GameTest
    public void octetsStackToTheEngineMaximum(GameTestHelper helper) {
        if (noOctets(helper)) return;
        ChessOctetTests.octetsStackToTheEngineMaximum(helper);
    }

    @GameTest
    public void octetsFillTheSubGridByHitPoint(GameTestHelper helper) {
        if (noOctets(helper)) return;
        ChessOctetTests.octetsFillTheSubGridByHitPoint(helper);
    }

    @GameTest
    public void octetsHoldWaterUntilFull(GameTestHelper helper) {
        if (noOctets(helper)) return;
        ChessOctetTests.octetsHoldWaterUntilFull(helper);
    }
}
