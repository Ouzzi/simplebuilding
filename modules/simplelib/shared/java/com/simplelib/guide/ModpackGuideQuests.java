package com.simplelib.guide;

import com.simplebuilding.framework.api.ModuleQuestDefaults;
import com.simplelib.SimpleLib;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * FTB Quests start chapter of the Simple Modpack guide (b-guide01): one quest "craft or pick up a crafting table", reward the
 * guide. Files under {@code data/simplelib/ftbquests/}; the loader adapter installs them only when FTB Quests is loaded and
 * SimpleBuilding is not (SimpleBuilding's own first quest gives the guide). The single guides' start chapters are gone.
 */
public final class ModpackGuideQuests {
    private ModpackGuideQuests() {}

    /** Returns the number of files written, -1 on failure (logged). */
    public static int install(Path configDir) {
        try {
            return ModuleQuestDefaults.install(configDir.resolve("ftbquests").resolve("quests"), SimpleLib.MOD_ID, ModpackGuideQuests::resource);
        } catch (IOException | RuntimeException e) {
            SimpleLib.LOG.warn("Simple Modpack guide: FTB Quests start chapter not installed ({})", e.toString());
            return -1;
        }
    }

    public static String resource(String relative) {
        try (InputStream in = ModpackGuideQuests.class.getResourceAsStream("/data/simplelib/ftbquests/" + relative)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}
