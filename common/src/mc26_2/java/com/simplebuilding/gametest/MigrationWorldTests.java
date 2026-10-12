package com.simplebuilding.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * 26.2 twin of the migration world tests: the id moves live in SimpleLib's {@code IdMoves}, which exists from 26.3 on
 * (on 26.2 the renamed ids are covered by {@code TradeAndMigrationTests}). Both tests pass without doing anything here.
 */
public final class MigrationWorldTests {
    private MigrationWorldTests() {
    }

    public static void fixturesAreBuildable(GameTestHelper helper) {
        helper.succeed();
    }

    public static void worldLoadsUnderMovedIds(GameTestHelper helper) {
        helper.succeed();
    }
}
