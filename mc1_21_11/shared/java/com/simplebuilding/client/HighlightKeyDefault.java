package com.simplebuilding.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * N78 (owner 16.11.): "Toggle Highlights" is unbound by default (was H). Once (marker {@code n78} in
 * {@code config/simplebuilding-keydefaults.txt}, same file format as the framework's {@code ConfigFiles.migrateKeyDefaults}) an
 * {@code options.txt} line still on the old default H is unbound; a key the player picked stays. Runs before Minecraft reads the options.
 */
public final class HighlightKeyDefault {
    static final String LINE = "key_key.simplebuilding.toggle_highlight:";
    private static final Pattern OLD = Pattern.compile("^" + Pattern.quote(LINE + "key.keyboard.h") + "(?=\\r?$)", Pattern.MULTILINE);

    private HighlightKeyDefault() {}

    public static String options(String text) {
        return OLD.matcher(text).replaceAll(Matcher.quoteReplacement(LINE + "key.keyboard.unknown"));
    }

    public static void run(Path gameDir) {
        try {
            Path done = gameDir.resolve("config").resolve("simplebuilding-keydefaults.txt");
            String doneText = Files.isRegularFile(done) ? Files.readString(done) : "";
            if (doneText.lines().anyMatch("n78"::equals)) return;
            Path options = gameDir.resolve("options.txt");
            if (Files.isRegularFile(options) && Files.size(options) <= 1 << 20) {
                String text = Files.readString(options);
                String moved = options(text);
                if (!moved.equals(text)) Files.writeString(options, moved, StandardCharsets.UTF_8);
            }
            Files.createDirectories(done.getParent());
            Files.writeString(done, doneText + (doneText.isEmpty() || doneText.endsWith("\n") ? "" : "\n") + "n78\n", StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger("simplebuilding").warn("Cannot move the highlight key default", e);
        }
    }
}
