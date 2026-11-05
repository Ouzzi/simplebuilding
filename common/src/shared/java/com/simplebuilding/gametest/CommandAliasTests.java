package com.simplebuilding.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

/** IG31-44: the core commands live under {@code /simplebuilding}; {@code /killboats} and {@code /killcarts} lead to the same nodes. */
public final class CommandAliasTests {
    private CommandAliasTests() {}

    public static void coreCommandsLiveUnderSimplebuildingAndOldNamesLeadThere(GameTestHelper helper) {
        var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();
        var base = root.getChild("simplebuilding");
        helper.assertTrue(base != null, "/simplebuilding is not registered");
        for (String name : new String[] {"killboats", "killcarts"}) {
            var target = base.getChild(name);
            helper.assertTrue(target != null, "/simplebuilding " + name + " is not registered");
            var old = root.getChild(name);
            helper.assertTrue(old != null && old.getRedirect() == target, "/" + name + " does not lead to /simplebuilding " + name);
            helper.assertTrue(old.getRequirement() == target.getRequirement(), "/" + name + " carries another permission than its target");
        }
        var problems = com.simplelib.api.CommandAliases.verify(helper.getLevel().getServer().getCommands().getDispatcher(), "simplebuilding");
        helper.assertTrue(problems.isEmpty(), "command aliases of simplebuilding: " + problems);
        helper.succeed();
    }
}
