package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for SimpleBuilding's uses of the nature variants (chisel, cracked ice fields; the blocks and their own tests
 * are Simple Blocks' since b-blocksmove). No test logic here: every method delegates to {@link NatureBlockTests}. Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class NatureBlockGameTest {

    @GameTest
    public void chiselMakesCrackedIce(GameTestHelper helper) {
        NatureBlockTests.chiselMakesCrackedIce(helper);
    }

    @GameTest
    public void crackedIceFieldsGenerateInIceSpikesAndFrozenOceans(GameTestHelper helper) {
        NatureBlockTests.crackedIceFieldsGenerateInIceSpikesAndFrozenOceans(helper);
    }
}
