package com.simplemusic.client;

import com.simplemusic.SimpleMusic;
import com.simplemusic.sheet.NbsReader;
import com.simplemusic.sheet.NoteCode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Import folders (owner decisions N2 and U5: files from a fixed folder, like the blueprint images; the file never
 * leaves the client, the import only creates text): {@code config/simplemusic/sheets/} for {@code .txt} note code and
 * Note Block Studio {@code .nbs}, {@code config/simplemusic/upload/} for {@code .ogg} discs.
 */
public final class SheetFiles {
    public static final int MAX_FILES = 64;
    public static final int MAX_NBS_BYTES = 4 << 20;

    private SheetFiles() {}

    public static Path sheets() {
        return SimpleMusic.configDir.resolve("simplemusic").resolve("sheets");
    }

    public static Path uploads() {
        return SimpleMusic.configDir.resolve("simplemusic").resolve("upload");
    }

    /** Files of a folder with one of the endings (created if missing), sorted by name. */
    public static List<Path> list(Path dir, String... endings) {
        try {
            Files.createDirectories(dir);
            try (var files = Files.list(dir)) {
                return files.filter(Files::isRegularFile).filter(p -> {
                    String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                    for (String e : endings) if (name.endsWith(e)) return true;
                    return false;
                }).sorted().limit(MAX_FILES).toList();
            }
        } catch (IOException e) {
            return List.of();
        }
    }

    /** The note code of an import file (.nbs is converted), or null when unreadable. */
    public static String importCode(Path file) {
        try {
            if (Files.size(file) > MAX_NBS_BYTES) return null;
            String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (name.endsWith(".nbs")) return NbsReader.read(Files.readAllBytes(file)).code();
            String text = Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n");
            return text.length() > NoteCode.MAX_CODE_LENGTH ? text.substring(0, NoteCode.MAX_CODE_LENGTH) : text;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
