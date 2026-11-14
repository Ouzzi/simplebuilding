package com.simplebuilding.modules.simplechallenges.test;

import com.simplebuilding.modules.simplechallenges.core.PresetRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.gametest.framework.GameTestHelper;

/** PLAN40 simplechallenges-2: follow-up checks (kept apart from ChallengeTests). */
public final class ChallengeFollowUpTests {
    /** A preset file above the limit is skipped, a normal one is read. */
    public static void presetFileLimit(GameTestHelper h) {
        try {
            Path dir = Files.createTempDirectory("sc-preset-limit");
            Path small = dir.resolve("small.json");
            Files.writeString(small, "{}");
            Path big = dir.resolve("big.json");
            Files.write(big, new byte[(int) PresetRegistry.MAX_PRESET_BYTES + 1]);
            h.assertTrue(!PresetRegistry.tooLarge(small), "a small preset file is read");
            h.assertTrue(PresetRegistry.tooLarge(big), "a file over the limit is skipped");
            h.assertTrue(PresetRegistry.tooLarge(dir.resolve("missing.json")), "an unreadable size counts as too large");
            Files.deleteIfExists(small);
            Files.deleteIfExists(big);
            Files.deleteIfExists(dir);
        } catch (java.io.IOException e) {
            h.assertTrue(false, "temp files: " + e);
        }
        h.succeed();
    }

    private ChallengeFollowUpTests() {}
}
