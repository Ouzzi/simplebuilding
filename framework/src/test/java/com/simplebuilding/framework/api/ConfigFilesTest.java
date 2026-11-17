package com.simplebuilding.framework.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Atomic write, collision-free quarantine and the options.txt key rename (review F-27, N-3, N-4). */
class ConfigFilesTest {
    @Test
    void writeAtomicReplacesAndLeavesNoTempFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("a.json");
        ConfigFiles.writeAtomic(file, "one");
        ConfigFiles.writeAtomic(file, "two");
        assertEquals("two", Files.readString(file));
        assertFalse(Files.exists(dir.resolve("a.json.tmp")));
    }

    @Test
    void quarantineKeepsBothFilesWithinOneSecond(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("c.json");
        Files.writeString(file, "x");
        String first = ConfigFiles.quarantine(file);
        Files.writeString(file, "y");
        String second = ConfigFiles.quarantine(file);
        assertNotEquals("(not moved)", first);
        assertNotEquals("(not moved)", second);
        assertNotEquals(first, second);
        assertEquals("x", Files.readString(dir.resolve(first)));
        assertEquals("y", Files.readString(dir.resolve(second)));
    }

    @Test
    void renameOptionKeyMatchesLineStartAndKeepsNewLine() {
        String old = "key_key.a.x:key.keyboard.g\nother:key_key.a.x:\n";
        assertEquals("key_key.b.x:key.keyboard.g\nother:key_key.a.x:\n", ConfigFiles.renameOptionKey(old, "key_key.a.x:", "key_key.b.x:"));
        String both = old + "key_key.b.x:key.keyboard.r\n";
        assertEquals(both, ConfigFiles.renameOptionKey(both, "key_key.a.x:", "key_key.b.x:"));
        assertTrue(ConfigFiles.renameOptionKey("", "a", "b").isEmpty());
    }

    @Test
    void moveKeyDefaultsOnlyTouchesTheOldDefault() {
        String[][] moves = {{"key_key.m.crawl:", "key.keyboard.c", "key.keyboard.down"}, {"key_key.m.walk:", "key.keyboard.unknown", "key.keyboard.up"}};
        String text = "key_key.m.crawl:key.keyboard.c\nkey_key.m.walk:key.keyboard.r\nkey_key.m.crawl2:key.keyboard.c\n";
        String moved = ConfigFiles.moveKeyDefaults(text, moves);
        assertTrue(moved.contains("key_key.m.crawl:key.keyboard.down\n"), moved);
        assertTrue(moved.contains("key_key.m.walk:key.keyboard.r\n"), "a player's own key stays");
        assertTrue(moved.contains("key_key.m.crawl2:key.keyboard.c\n"), "another key with the same value stays");
        assertEquals("key_key.m.crawl:key.keyboard.comma\n", ConfigFiles.moveKeyDefaults("key_key.m.crawl:key.keyboard.comma\n", moves), "prefix of a longer value");
        assertEquals("key_key.m.walk:key.keyboard.up\r\n", ConfigFiles.moveKeyDefaults("key_key.m.walk:key.keyboard.unknown\r\n", moves));
    }

    @Test
    void migrateKeyDefaultsRunsOncePerMarker(@TempDir Path dir) throws IOException {
        String[][] moves = {{"key_key.m.crawl:", "key.keyboard.c", "key.keyboard.down"}};
        Path options = dir.resolve("options.txt");
        Files.writeString(options, "version:1\nkey_key.m.crawl:key.keyboard.c\n");
        ConfigFiles.migrateKeyDefaults(dir, "m", "n78", moves);
        assertEquals("version:1\nkey_key.m.crawl:key.keyboard.down\n", Files.readString(options));
        assertEquals("n78\n", Files.readString(dir.resolve("config/m-keydefaults.txt")));
        Files.writeString(options, "key_key.m.crawl:key.keyboard.c\n");   // the player chose C again later
        ConfigFiles.migrateKeyDefaults(dir, "m", "n78", moves);
        assertEquals("key_key.m.crawl:key.keyboard.c\n", Files.readString(options));
        Path fresh = dir.resolve("fresh");
        Files.createDirectories(fresh);
        ConfigFiles.migrateKeyDefaults(fresh, "m", "n78", moves);
        assertFalse(Files.exists(fresh.resolve("options.txt")));
        assertEquals("n78\n", Files.readString(fresh.resolve("config/m-keydefaults.txt")));
    }
}
