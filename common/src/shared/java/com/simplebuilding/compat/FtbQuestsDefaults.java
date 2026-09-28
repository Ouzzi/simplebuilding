package com.simplebuilding.compat;

import com.simplebuilding.platform.ModEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ships the SimpleBuilding quest chapters to FTB Quests - optional, without any FTB class.
 *
 * <p>FTB Quests reads its whole quest book from {@code config/ftbquests/quests} and has no way to
 * load quests from a data pack. When FTB Quests is installed, this copies the chapters the mod
 * carries under {@code data/simplebuilding/ftbquests/} (written by
 * {@code tools/quests/generate_quests.py}) into that folder once, before any world starts:
 * <ul>
 *   <li>{@code file <path>} - copied only when the book has no such file yet (chapter files carry
 *       a {@code simplebuilding_} prefix, so they never clash with a modpack's own chapters);</li>
 *   <li>{@code list <path> <key> <entry> <marker>} - the chapter group: the whole file for a new
 *       book, otherwise the entry is inserted at the start of the list {@code <key>};</li>
 *   <li>{@code map <path> <entries> <marker>} - the 1.21.11 lang file: the whole file for a new
 *       book, otherwise the entries are inserted at the start of its compound.</li>
 * </ul>
 * Nothing that exists is overwritten, and a marker file ({@code simplebuilding.installed}) with the
 * data version stops a second install: deleted chapters stay deleted until the mod ships a newer
 * version. Every failure is logged and swallowed - without FTB Quests, or with a book this cannot
 * read, the game starts as before.
 *
 * <p>Loader-neutral; each loader calls {@link #installIfPresent} from its entry point with its
 * config directory. 1.21.11 keeps an identical copy (the shipped files are SNBT there, JSON5 on 26.x).
 */
public final class FtbQuestsDefaults {
    private static final Logger LOGGER = LoggerFactory.getLogger("simplebuilding");
    public static final String RESOURCE_ROOT = "data/simplebuilding/ftbquests/";
    public static final String MARKER = "simplebuilding.installed";

    private FtbQuestsDefaults() {
    }

    /** Installs the default chapters when FTB Quests is loaded; never throws. */
    public static void installIfPresent(Path configDir) {
        if (!ModEnvironment.isModLoaded("ftbquests")) {
            return;
        }
        try {
            int changed = install(configDir.resolve("ftbquests").resolve("quests"));
            if (changed > 0) {
                LOGGER.info("Installed the SimpleBuilding quest chapters for FTB Quests ({} file(s))", changed);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not install the SimpleBuilding quest chapters for FTB Quests", e);
        }
    }

    /**
     * Installs into {@code questsDir}; returns how many files were written or changed. Public for
     * the game test, which runs it against a temporary folder.
     */
    public static int install(Path questsDir) throws IOException {
        List<String> lines = readResource("install.txt").lines().map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#")).toList();
        String version = "0";
        for (String line : lines) {
            String[] parts = line.split(" ");
            if (parts[0].equals("version")) {
                version = parts[1];
            }
        }
        Path marker = questsDir.resolve(MARKER);
        if (Files.exists(marker) && parseVersion(Files.readString(marker, StandardCharsets.UTF_8)) >= parseVersion(version)) {
            return 0;
        }
        int changed = 0;
        for (String line : lines) {
            String[] parts = line.split(" ");
            switch (parts[0]) {
                case "file" -> changed += copyIfAbsent(questsDir, parts[1]) ? 1 : 0;
                case "list" -> changed += insert(questsDir, parts[1], parts[4],
                        Pattern.compile("\"?" + Pattern.quote(parts[2]) + "\"?\\s*[:=]\\s*\\["), parts[3]) ? 1 : 0;
                case "map" -> changed += insert(questsDir, parts[1], parts[3], Pattern.compile("\\{"), parts[2]) ? 1 : 0;
                default -> {
                    // format, version
                }
            }
        }
        Files.createDirectories(questsDir);
        Files.writeString(marker, version + "\n", StandardCharsets.UTF_8);
        return changed;
    }

    private static boolean copyIfAbsent(Path questsDir, String relative) throws IOException {
        Path target = safeResolve(questsDir, relative);
        if (Files.exists(target)) {
            return false;
        }
        Files.createDirectories(target.getParent());
        Files.writeString(target, readResource(relative), StandardCharsets.UTF_8);
        return true;
    }

    /**
     * Puts {@code entryResource} right behind the first match of {@code anchor} in an existing
     * file, or copies the whole resource file when there is none. Skips a file that already
     * contains {@code marker} (the group id / the first lang key).
     */
    private static boolean insert(Path questsDir, String relative, String marker, Pattern anchor, String entryResource)
            throws IOException {
        Path target = safeResolve(questsDir, relative);
        if (!Files.exists(target)) {
            return copyIfAbsent(questsDir, relative);
        }
        String text = Files.readString(target, StandardCharsets.UTF_8);
        if (text.contains(marker)) {
            return false;
        }
        Matcher matcher = anchor.matcher(text);
        if (!matcher.find()) {
            LOGGER.warn("FTB Quests file {} has an unexpected layout; SimpleBuilding left it alone", target);
            return false;
        }
        String entry = readResource(entryResource).strip();
        String rest = text.substring(matcher.end());
        boolean empty = rest.stripLeading().startsWith("]") || rest.stripLeading().startsWith("}");
        // JSON5 needs a comma between entries; FTB's SNBT takes a comma or a line break, so a
        // comma is right for both (and keeps a comma-separated SNBT file valid for vanilla, too).
        String separator = !empty && !entry.endsWith(",") ? "," : "";
        Files.writeString(target, text.substring(0, matcher.end()) + "\n" + entry + separator + "\n" + rest, StandardCharsets.UTF_8);
        return true;
    }

    private static Path safeResolve(Path questsDir, String relative) throws IOException {
        Path target = questsDir.resolve(relative).normalize();
        if (!target.startsWith(questsDir.normalize()) || relative.contains("..")) {
            throw new IOException("refusing to write outside the quest book: " + relative);
        }
        return target;
    }

    /** A file of the shipped quest book, as text; public for the game test. */
    public static String readResource(String relative) throws IOException {
        try (InputStream in = FtbQuestsDefaults.class.getClassLoader().getResourceAsStream(RESOURCE_ROOT + relative)) {
            if (in == null) {
                throw new IOException("missing resource " + RESOURCE_ROOT + relative);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Every file the install list names (paths relative to the quest book), for the game test. */
    public static List<String> shippedFiles() throws IOException {
        List<String> out = new ArrayList<>();
        for (String line : readResource("install.txt").lines().map(String::strip).toList()) {
            String[] parts = line.split(" ");
            if (parts[0].equals("file") || parts[0].equals("list") || parts[0].equals("map")) {
                out.add(parts[1]);
            }
        }
        return out;
    }

    private static int parseVersion(String text) {
        try {
            return Integer.parseInt(text.strip());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
