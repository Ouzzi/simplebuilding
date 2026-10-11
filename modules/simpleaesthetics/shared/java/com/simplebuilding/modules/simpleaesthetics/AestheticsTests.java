package com.simplebuilding.modules.simpleaesthetics;

import com.simplebuilding.framework.api.SubMods;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;

/** Super-mod checks shared by all loaders (Fabric wrapper ModuleGameTest, NeoForge ModuleNeoTests). */
public final class AestheticsTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    static {
        ALL.put("sub_mod_switch", AestheticsTests::subModSwitch);
        ALL.put("config_file", AestheticsTests::configFile);
    }

    private AestheticsTests() {}

    /** "Enable Simple X" reaches exactly its sub-mod through framework SubMods; defaults on; restored afterwards. */
    public static void subModSwitch(GameTestHelper h) {
        var defaults = new SimpleAesthetics.Config();
        for (String subMod : SimpleAesthetics.SUB_MODS) h.assertTrue(defaults.enabled(subMod), "Default on: " + subMod);
        try {
            for (String off : SimpleAesthetics.SUB_MODS) {
                var c = new SimpleAesthetics.Config();
                c.enableSimpleVisuals = !off.equals("simplevisuals");
                c.enableSimpleSounds = !off.equals("simplesounds");
                c.enableSimpleModels = !off.equals("simplemodels");
                SimpleAesthetics.apply(c);
                for (String subMod : SimpleAesthetics.SUB_MODS)
                    h.assertTrue(SubMods.enabled(subMod) == !subMod.equals(off), "Switch for " + off + " reaches only it (" + subMod + ")");
            }
        } finally {
            SimpleAesthetics.apply(SimpleAesthetics.config());
        }
        for (String subMod : SimpleAesthetics.SUB_MODS)
            h.assertTrue(SubMods.enabled(subMod) == SimpleAesthetics.config().enabled(subMod), "Switch restored: " + subMod);
        h.succeed();
    }

    /** The config file is created complete, a broken file is quarantined, and an edited switch is read back. */
    public static void configFile(GameTestHelper h) {
        var live = SimpleAesthetics.config();
        try {
            var dir = java.nio.file.Files.createTempDirectory("simpleaesthetics-test");
            SimpleAesthetics.load(dir);
            var file = dir.resolve(SimpleAesthetics.FILE);
            h.assertTrue(java.nio.file.Files.readString(file).contains("enableSimpleModels"), "File written with every switch");
            java.nio.file.Files.writeString(file, "{\"enableSimpleSounds\": false}");
            SimpleAesthetics.load(dir);
            h.assertTrue(!SimpleAesthetics.config().enableSimpleSounds && SimpleAesthetics.config().enableSimpleVisuals, "Edited switch read, others default");
            h.assertTrue(!SubMods.enabled("simplesounds"), "Loaded switch applied");
            java.nio.file.Files.writeString(file, "{ broken");
            SimpleAesthetics.load(dir);
            h.assertTrue(SimpleAesthetics.config().enableSimpleSounds, "Broken file falls back to defaults");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        } finally {
            SimpleAesthetics.restore(live);
        }
        h.succeed();
    }
}
