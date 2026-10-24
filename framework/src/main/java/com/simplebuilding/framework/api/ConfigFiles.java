package com.simplebuilding.framework.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Since 0.1.7. One policy for a broken server config in every module: never crash, move the unreadable file to
 * {@code <name>.broken-<yyyyMMdd-HHmmss>}, warn, and let the caller fall back to defaults (and write them, since the
 * original name is free again). Only claims stay fail-fast on purpose (a wrong default is a security risk there).
 */
public final class ConfigFiles {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ConfigFiles() {}

    /**
     * Writes {@code text} to {@code file} through a sibling {@code <name>.tmp} and a move, so a crash while writing never leaves a
     * half-written file (the old content stays). Falls back to a plain replacing move where the file system has no atomic move.
     */
    public static void writeAtomic(Path file, String text) throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, text, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Runs {@code migrate} over {@code <gameDir>/options.txt} (skipped when missing or over 1 MiB) and writes the result back
     * atomically, only if it changed. Throws what the file access throws.
     */
    public static void migrateOptions(Path gameDir, UnaryOperator<String> migrate) throws IOException {
        Path options = gameDir.resolve("options.txt");
        if (!Files.isRegularFile(options) || Files.size(options) > 1 << 20) return;
        String text = Files.readString(options);
        String migrated = migrate.apply(text);
        if (!migrated.equals(text)) writeAtomic(options, migrated);
    }

    /**
     * Renames the options line {@code oldKey} (e.g. {@code "key_key.mod.x:"}) to {@code newKey}, matched at a line start only,
     * and not when a line {@code newKey} exists already.
     */
    public static String renameOptionKey(String text, String oldKey, String newKey) {
        var hasNew = java.util.regex.Pattern.compile("^" + java.util.regex.Pattern.quote(newKey), java.util.regex.Pattern.MULTILINE);
        if (hasNew.matcher(text).find()) return text;
        var old = java.util.regex.Pattern.compile("^" + java.util.regex.Pattern.quote(oldKey), java.util.regex.Pattern.MULTILINE);
        return old.matcher(text).replaceAll(java.util.regex.Matcher.quoteReplacement(newKey));
    }

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

    /**
     * Since 0.1.8. Config takeover when a sub-mod leaves a bigger mod (split plan 3.2 item 8): if {@code newFile} does
     * not exist but {@code oldFile} does, {@code extract} turns the old file's text into the new file's text (or
     * {@code null}: nothing to take over), which is written to {@code newFile}. The old file is never changed; an
     * unreadable old file or a failing {@code extract} leaves everything as it is. {@code log} gets one line when a file
     * was written or the takeover failed. Returns whether {@code newFile} was written.
     */
    public static boolean adoptLegacy(Path newFile, Path oldFile, UnaryOperator<String> extract, Consumer<String> log) {
        if (newFile == null || oldFile == null || Files.exists(newFile) || !Files.isRegularFile(oldFile)) return false;
        try {
            String text = extract.apply(Files.readString(oldFile));
            if (text == null) return false;
            if (newFile.getParent() != null) Files.createDirectories(newFile.getParent());
            Files.writeString(newFile, text);
            log.accept("Took over settings from " + oldFile.getFileName() + " into " + newFile.getFileName() + " (old file unchanged)");
            return true;
        } catch (Exception e) {
            log.accept("Could not take over settings from " + oldFile.getFileName() + " (" + e + "); using defaults");
            return false;
        }
    }

    /** Renames {@code file} to {@code <name>.broken-<time>} and returns the new file name ({@code "(not moved)"} if that failed). */
    public static String quarantine(Path file) {
        String base = file.getFileName() + ".broken-" + LocalDateTime.now().format(STAMP);
        try {
            // two broken files of one name within a second must not clash: count up instead of failing and losing the original
            Path target = file.resolveSibling(base);
            for (int n = 2; Files.exists(target); n++) {
                target = file.resolveSibling(base + "-" + n);
            }
            Files.move(file, target);
            return target.getFileName().toString();
        } catch (IOException | RuntimeException e) {
            return "(not moved)";
        }
    }
}
