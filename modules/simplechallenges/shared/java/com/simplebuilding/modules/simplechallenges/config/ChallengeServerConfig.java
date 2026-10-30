package com.simplebuilding.modules.simplechallenges.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.simplebuilding.framework.api.ConfigFiles;
import com.simplebuilding.modules.simplechallenges.SimpleChallenges;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simplechallenges-server.json} (server authority, A9). Safety lock: the defaults change nothing -
 * no challenge is chosen ({@code defaultChallenge = none}) and the admin commands that switch a world over are off.
 * Missing or broken keys use the defaults and the file is rewritten complete.
 */
public final class ChallengeServerConfig {
    public static final String FILE = "simplechallenges-server.json";
    public static final List<String> KEYS = List.of("enabled", "allowAdminCommands", "defaultChallenge", "exemptCreative", "pityEnabled");

    public static boolean enabled;
    /** Whether /simplechallenges activate, deactivate, unlock-for-free and credits add work (R2: test worlds only). */
    public static boolean allowAdminCommands;
    /** A preset id applied once to a brand-new world (game time 0, never initialised); {@code none} = nothing. */
    public static String defaultChallenge;
    /** Creative and spectator players ignore the borders. */
    public static boolean exemptCreative;
    /** The emergency unlock when nothing can be paid for a while (concept 3, deadlock protection). */
    public static boolean pityEnabled;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static {
        reset();
    }

    public static void reset() {
        apply(new JsonObject());
    }

    public static void load(Path dir) {
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleChallenges.LOG::warn);
        apply(read == null ? new JsonObject() : read);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleChallenges.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    public static void apply(JsonObject json) {
        enabled = bool(json, "enabled", true);
        allowAdminCommands = bool(json, "allowAdminCommands", false);
        defaultChallenge = text(json, "defaultChallenge", "none");
        exemptCreative = bool(json, "exemptCreative", true);
        pityEnabled = bool(json, "pityEnabled", true);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("allowAdminCommands", allowAdminCommands);
        o.addProperty("defaultChallenge", defaultChallenge);
        o.addProperty("exemptCreative", exemptCreative);
        o.addProperty("pityEnabled", pityEnabled);
        return o;
    }

    private static boolean bool(JsonObject o, String key, boolean def) {
        try {
            return o.has(key) ? o.get(key).getAsBoolean() : def;
        } catch (RuntimeException e) {
            return def;
        }
    }

    private static String text(JsonObject o, String key, String def) {
        try {
            String s = o.has(key) ? o.get(key).getAsString().trim() : def;
            return s.isEmpty() || s.length() > 64 ? def : s;
        } catch (RuntimeException e) {
            return def;
        }
    }

    private ChallengeServerConfig() {}
}
