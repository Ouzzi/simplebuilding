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
            if (name.endsWith(".nbs")) return fit(NbsReader.read(Files.readAllBytes(file)).code());
            return fit(Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n"));
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /**
     * Code that fits the editor and the packet ({@link NoteCode#MAX_CODE_LENGTH}): longer text is cut after the last whole bar
     * (blank line, as the .nbs import writes them), else after the last line. simplemusic-P2-1: a long .nbs song wrote more than
     * the limit; closing the editor then sent a string the packet codec refuses, which drops the player from the server.
     */
    public static String fit(String code) {
        int max = NoteCode.MAX_CODE_LENGTH;
        if (code.length() <= max) return code;
        int cut = code.lastIndexOf("\n\n", max);
        if (cut <= 0) cut = code.lastIndexOf('\n', max);
        return code.substring(0, cut > 0 ? cut : max);
    }
}
