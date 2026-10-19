package com.simplebuilding.modules.simpleoptmemory;

import com.simplebuilding.framework.api.OptCompat;
import com.simplebuilding.framework.api.OptSettings;
import java.nio.file.Files;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;

/** Simple Opt Memory checks: boots without areas, writes its config, framework engine reachable alone. */
public final class MemoryOptTests {
    private MemoryOptTests() {}

    public static void bootsWithoutAreas(GameTestHelper h) {
        h.assertTrue(MemoryOpt.AREAS.isEmpty() && OptCompat.decisions(MemoryOpt.MOD_ID).isEmpty(), "no memory area in stage 1");
        h.assertTrue(MemoryOpt.WATCHED.contains("ferritecore") && MemoryOpt.WATCHED.contains("modernfix"), "watched mods");
        try {
            var dir = Files.createTempDirectory("simpleoptmemory-test");
            OptSettings.load(dir, MemoryOpt.FILE, Map.of(), s -> {});
            h.assertTrue(Files.readString(dir.resolve(MemoryOpt.FILE)).contains("conflictScan"), "config written");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        h.succeed();
    }
}
