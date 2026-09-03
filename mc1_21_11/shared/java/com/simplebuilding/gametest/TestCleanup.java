package com.simplebuilding.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Stand-in for {@code GameTestHelper#runBeforeTestEnd}, which only exists on MC 26.2.
 *
 * <p>The 26.2 copies of the test bodies register their clean-up -- handing a mock player back to
 * the player list, discarding an entity that was spawned outside the test structure -- with that
 * hook and let the framework fire it however the test ends. MC 1.21.11's {@code GameTestHelper}
 * has no such hook: it offers only {@code runAtTickTime} / {@code runAfterDelay} /
 * {@code onEachTick}, and all three stop firing the moment the test is done.
 *
 * <p>So the clean-up is collected here and run explicitly at the point every body already has:
 * right before it reports success. That means it runs on the success path only. A test that fails
 * half way through leaks whatever it registered -- the same trade-off {@link MockPlayers} already
 * documents for this line, and acceptable because a failing test fails the build anyway.
 *
 * <p>The map is concurrent because the whole suite runs as one batch in a single world and several
 * tests hold registrations at the same time. Each entry is keyed by the helper, i.e. by the test,
 * so no test can ever run another test's clean-up.
 */
final class TestCleanup {

    private static final Map<GameTestHelper, List<Runnable>> PENDING = new ConcurrentHashMap<>();

    private TestCleanup() {
    }

    /** Registers an action to run just before this test reports success. */
    static void before(GameTestHelper helper, Runnable action) {
        PENDING.computeIfAbsent(helper, key -> new ArrayList<>()).add(action);
    }

    /**
     * Runs everything this test registered, most recent first, and forgets it. Later registrations
     * usually depend on earlier ones (a player is created, then something is given to it), so they
     * are undone in the opposite order.
     */
    static void run(GameTestHelper helper) {
        List<Runnable> actions = PENDING.remove(helper);
        if (actions == null) {
            return;
        }
        for (int i = actions.size() - 1; i >= 0; i--) {
            actions.get(i).run();
        }
    }

    /** Runs the clean-up, then reports the test as passed. */
    static void succeed(GameTestHelper helper) {
        run(helper);
        helper.succeed();
    }
}
