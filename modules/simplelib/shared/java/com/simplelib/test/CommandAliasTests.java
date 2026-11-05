package com.simplelib.test;

import com.simplelib.api.CommandAliases;
import net.minecraft.gametest.framework.GameTestHelper;

/** IG31-44: every command of {@code mod} lives under {@code /<mod>} and each old root name leads to the very same node. */
public final class CommandAliasTests {
    private CommandAliasTests() {}

    public static void module(GameTestHelper h, String mod) {
        var d = h.getLevel().getServer().getCommands().getDispatcher();
        var problems = CommandAliases.verify(d, mod);
        h.assertTrue(problems.isEmpty(), "command aliases of " + mod + ": " + problems);
        h.succeed();
    }
}
