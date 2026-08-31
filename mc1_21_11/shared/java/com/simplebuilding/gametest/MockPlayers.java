package com.simplebuilding.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * Life cycle of the fake server players the tests need.
 *
 * <p>MC 26.2 offers {@code GameTestHelper#runBeforeTestEnd}, which the 26.2 copy of this suite uses
 * to hand the mock player back to the player list no matter how the test ends. MC 1.21.11 has no
 * such hook - its {@code GameTestHelper} only offers {@code runAtTickTime} / {@code runAfterDelay}
 * / {@code onEachTick}, and all three stop firing the moment the test is done. So every test hands
 * its own player back on the way out instead.
 *
 * <p>Deliberately <em>not</em> done here: sweeping up stray mock players when a new one is created.
 * The whole suite runs as one concurrent batch in a single world, so several tests hold a mock
 * player at the same time and a sweep would tear down someone else's player mid-test. A test that
 * fails half way through therefore leaks its player - acceptable, because a failing test fails the
 * build anyway.
 */
final class MockPlayers {

    private MockPlayers() {
    }

    @SuppressWarnings("removal")
    static ServerPlayer create(GameTestHelper helper) {
        return helper.makeMockServerPlayerInLevel();
    }

    static void remove(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }
}
