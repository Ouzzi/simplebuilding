package com.simplebuilding.modules.simpleoptimization;

import com.simplebuilding.framework.api.SubMods;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;

/** Super-mod checks shared by all loaders (Fabric wrapper ModuleGameTest, NeoForge ModuleNeoTests). */
public final class OptimizationTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    static {
        ALL.put("sub_mod_switch", OptimizationTests::subModSwitch);
        ALL.put("config_file", OptimizationTests::configFile);
    }

    private OptimizationTests() {}

    /** "Enable Simple X" reaches exactly its sub-mod through framework SubMods; defaults on; restored afterwards. */
    public static void subModSwitch(GameTestHelper h) {
        var defaults = new SimpleOptimization.Config();
        for (String subMod : SimpleOptimization.SUB_MODS) h.assertTrue(defaults.enabled(subMod), "Default on: " + subMod);
        try {
            for (String off : SimpleOptimization.SUB_MODS) {
                var c = new SimpleOptimization.Config();
                c.enableSimpleOptRender = !off.equals("simpleoptrender");
                c.enableSimpleOptMemory = !off.equals("simpleoptmemory");
                c.enableSimpleOptServer = !off.equals("simpleoptserver");
                SimpleOptimization.apply(c);
                for (String subMod : SimpleOptimization.SUB_MODS)
                    h.assertTrue(SubMods.enabled(subMod) == !subMod.equals(off), "Switch for " + off + " reaches only it (" + subMod + ")");
            }
        } finally {
            SimpleOptimization.apply(SimpleOptimization.config());
        }
        for (String subMod : SimpleOptimization.SUB_MODS)
            h.assertTrue(SubMods.enabled(subMod) == SimpleOptimization.config().enabled(subMod), "Switch restored: " + subMod);
        h.succeed();
    }

    /** The config file is created complete, a broken file is quarantined, and an edited switch is read back. */
    public static void configFile(GameTestHelper h) {
        var live = SimpleOptimization.config();
        try {
            var dir = java.nio.file.Files.createTempDirectory("simpleoptimization-test");
            SimpleOptimization.load(dir);
            var file = dir.resolve(SimpleOptimization.FILE);
            h.assertTrue(java.nio.file.Files.readString(file).contains("enableSimpleOptServer"), "File written with every switch");
            java.nio.file.Files.writeString(file, "{\"enableSimpleOptMemory\": false}");
            SimpleOptimization.load(dir);
            h.assertTrue(!SimpleOptimization.config().enableSimpleOptMemory && SimpleOptimization.config().enableSimpleOptRender, "Edited switch read, others default");
            h.assertTrue(!SubMods.enabled("simpleoptmemory"), "Loaded switch applied");
            java.nio.file.Files.writeString(file, "{ broken");
            SimpleOptimization.load(dir);
            h.assertTrue(SimpleOptimization.config().enableSimpleOptMemory, "Broken file falls back to defaults");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        } finally {
            SimpleOptimization.restore(live);
        }
        h.succeed();
    }
}
