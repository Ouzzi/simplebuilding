package com.simplebuilding.framework.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Since 0.1.7. One policy for a broken server config in every module: never crash, move the unreadable file to
 * {@code <name>.broken-<yyyyMMdd-HHmmss>}, warn, and let the caller fall back to defaults (and write them, since the
 * original name is free again). Only claims stay fail-fast on purpose (a wrong default is a security risk there).
 */
public final class ConfigFiles {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ConfigFiles() {}

    /**
     * Reads {@code file} and hands its text to {@code parser} (e.g. {@code text -> gson.fromJson(text, X.class)}). Returns {@code null} when the file is missing, or when it is
     * unreadable/unparsable/empty (then it is quarantined first and {@code warn} gets one line). The caller uses its
     * defaults for {@code null}.
     */
    public static <T> T readOrQuarantine(Path file, Function<String, T> parser, Consumer<String> warn) {
        if (file == null || !Files.isRegularFile(file)) return null;
        try {
            T value = parser.apply(Files.readString(file));
            if (value != null) return value;
            throw new IllegalStateException("file is empty");
        } catch (Exception e) {
            warn.accept("Unreadable " + file.getFileName() + ", using defaults (" + e + "); old file kept as " + quarantine(file));
            return null;
        }
    }

    /** Renames {@code file} to {@code <name>.broken-<time>} and returns the new file name ({@code "(not moved)"} if that failed). */
    public static String quarantine(Path file) {
        Path target = file.resolveSibling(file.getFileName() + ".broken-" + LocalDateTime.now().format(STAMP));
        try {
            Files.move(file, target);
            return target.getFileName().toString();
        } catch (IOException | RuntimeException e) {
            return "(not moved)";
        }
    }
}
