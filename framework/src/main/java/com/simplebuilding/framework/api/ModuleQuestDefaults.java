package com.simplebuilding.framework.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * Copies a module's default FTB Quests files into {@code config/ftbquests/quests} - pure Java, no FTB class.
 *
 * <p>Since 0.1.3. A module ships its files under {@code data/<namespace>/ftbquests/} with an {@code install.txt}
 * ({@code version <n>} and {@code file <relative path>} lines, written by {@code tools/guides/module_guides.py}).
 * The loader adapter calls {@link #install} only when FTB Quests is loaded. Existing files are never overwritten;
 * a marker {@code <namespace>.installed} holding the data version stops a second install, so a deleted chapter
 * stays deleted until the module ships a newer version. Same semantics as SimpleBuilding's own installer.
 */
public final class ModuleQuestDefaults {
    private ModuleQuestDefaults() {
    }

    /**
     * @param questsDir the FTB Quests book folder ({@code <config>/ftbquests/quests})
     * @param namespace the module namespace, also the marker name
     * @param resource  reads a path relative to {@code data/<namespace>/ftbquests/}; {@code null} when missing
     * @return how many files were written
     */
    public static int install(Path questsDir, String namespace, Function<String, String> resource) throws IOException {
        String index = resource.apply("install.txt");
        if (index == null) {
            return 0;
        }
        String version = "0";
        for (String line : index.lines().map(String::strip).toList()) {
            if (line.startsWith("version ")) {
                version = line.substring(8).strip();
            }
        }
        Path marker = questsDir.resolve(namespace + ".installed");
        if (Files.exists(marker) && parse(Files.readString(marker, StandardCharsets.UTF_8)) >= parse(version)) {
            return 0;
        }
        int written = 0;
        for (String line : index.lines().map(String::strip).toList()) {
            if (!line.startsWith("file ")) {
                continue;
            }
            String relative = line.substring(5).strip();
            Path target = questsDir.resolve(relative).normalize();
            if (!target.startsWith(questsDir.normalize())) {
                throw new IOException("Quest file outside the quest folder: " + relative);
            }
            if (Files.exists(target)) {
                continue;
            }
            String text = resource.apply(relative);
            if (text == null) {
                throw new IOException("Missing quest resource: " + relative);
            }
            Files.createDirectories(target.getParent());
            Files.writeString(target, text, StandardCharsets.UTF_8);
            written++;
        }
        Files.createDirectories(questsDir);
        Files.writeString(marker, version + "\n", StandardCharsets.UTF_8);
        return written;
    }

    private static int parse(String text) {
        try {
            return Integer.parseInt(text.strip());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
