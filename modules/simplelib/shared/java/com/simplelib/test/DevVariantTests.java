package com.simplelib.test;

import com.simplelib.api.DevVariants;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.gametest.framework.GameTestHelper;

/** ABN30-9: the dev variants registry - defaults, selection, stale choices, persistence, the enable rule, and nothing client-side. */
final class DevVariantTests {
    private DevVariantTests() {}

    static void registryAndPersistence(GameTestHelper h) {
        String id = "test:scope_frame";
        try {
            Path dir = Files.createTempDirectory("simplelib-devvariants");
            DevVariants.load(dir);
            DevVariants.register(id, "Test Mod", "Fernrohr-Rahmen", "a", new DevVariants.Option("a", "A"), new DevVariants.Option("b", "B"));
            h.assertTrue(DevVariants.get(id).equals("a"), "default is not a");
            h.assertTrue(DevVariants.get("test:unknown").isEmpty(), "unknown id must give an empty string");
            int[] changes = {0};
            DevVariants.onChange(v -> changes[0]++);
            h.assertTrue(DevVariants.select(id, "b") && DevVariants.is(id, "b") && changes[0] == 1, "select b did not apply at once");
            h.assertTrue(!DevVariants.select(id, "zzz") && !DevVariants.select("test:unknown", "a") && DevVariants.is(id, "b"), "invalid selection must be refused");
            // persistence: a fresh load sees the choice, also for a variant that registers only afterwards
            DevVariants.load(dir);
            DevVariants.register(id, "Test Mod", "Fernrohr-Rahmen", "a", new DevVariants.Option("a", "A"), new DevVariants.Option("b", "B"));
            h.assertTrue(DevVariants.is(id, "b"), "choice not stored and loaded");
            // a stale stored option (variant lost it) falls back to the default
            DevVariants.register(id, "Test Mod", "Fernrohr-Rahmen", "a", new DevVariants.Option("a", "A"));
            h.assertTrue(DevVariants.is(id, "a"), "stale choice must fall back to the default");
            // enable rule: explicit config wins, else the development environment decides
            DevVariants.setEnabled(null);
            DevVariants.setDevEnvironment(false);
            h.assertTrue(!DevVariants.enabled(), "release must default to off");
            DevVariants.setDevEnvironment(true);
            h.assertTrue(DevVariants.enabled(), "development must default to on");
            DevVariants.setEnabled(false);
            h.assertTrue(!DevVariants.enabled(), "explicit false must win in development");
            DevVariants.load(dir);
            h.assertTrue(!DevVariants.enabled(), "devVariants=false not stored");
            // broken file: defaults, no exception
            Files.writeString(dir.resolve(DevVariants.FILE), "{ not json");
            DevVariants.load(dir);
            h.assertTrue(DevVariants.is(id, "a") && DevVariants.enabled(), "broken file must give the defaults");
            DevVariants.load(null);
            DevVariants.setDevEnvironment(false);
            h.succeed();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}
