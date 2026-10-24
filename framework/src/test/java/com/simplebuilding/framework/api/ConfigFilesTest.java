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
}
