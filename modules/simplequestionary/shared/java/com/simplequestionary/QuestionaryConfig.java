package com.simplequestionary;

import com.simplebuilding.framework.api.ConfigFiles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Config {@code config/simplequestionary.json}: {@code autoFacts} switches the recipe line under the "Use" page.
 * {@link #enabled}: standalone always on; bundled in SimpleBuilding the host's switch "Enable Simple Questionary"
 * ({@code enableSimpleQuestionary} in {@code simplebuilding.json}, read once at load, restart to change) can turn it
 * off - the block stays registered (stable registries) but does nothing and is not offered.
 */
public final class QuestionaryConfig {
    public static final String FILE = "simplequestionary.json";
    public static final String HOST_FILE = "simplebuilding.json", HOST_KEY = "enableSimpleQuestionary";

    public static boolean autoFacts = true;
    public static boolean enabled = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load(Path dir) {
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleQuestionary.LOG::warn);
        autoFacts = read == null || !read.has("autoFacts") || !read.get("autoFacts").isJsonPrimitive() || read.get("autoFacts").getAsBoolean();
        enabled = hostAllows(dir);
        // CMD24: /simplequestionary config autoFacts.
        com.simplelib.api.config.ConfigCommands.server("simplequestionary").json(() -> {
            JsonObject o = new JsonObject();
            o.addProperty("autoFacts", autoFacts);
            return o;
        }, j -> autoFacts = !j.has("autoFacts") || !j.get("autoFacts").isJsonPrimitive() || j.get("autoFacts").getAsBoolean()).file(file).register();
        if (file == null) return;
        try {
            Files.createDirectories(dir);
            JsonObject out = new JsonObject();
            out.addProperty("autoFacts", autoFacts);
            ConfigFiles.writeAtomic(file, GSON.toJson(out));
        } catch (Exception e) {
            SimpleQuestionary.LOG.warn("Could not write {}: {}", FILE, e.toString());
        }
    }

    /** False only when a SimpleBuilding config next to ours explicitly says {@code enableSimpleQuestionary: false}. */
    public static boolean hostAllows(Path dir) {
        try {
            Path host = dir == null ? null : dir.resolve(HOST_FILE);
            if (host == null || !Files.isRegularFile(host)) return true;
            JsonObject json = GSON.fromJson(Files.readString(host), JsonObject.class);
            return json == null || !json.has(HOST_KEY) || !json.get(HOST_KEY).isJsonPrimitive() || json.get(HOST_KEY).getAsBoolean();
        } catch (Exception e) {
            return true;
        }
    }

    private QuestionaryConfig() {}
}
