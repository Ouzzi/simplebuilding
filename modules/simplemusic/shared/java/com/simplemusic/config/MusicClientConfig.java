package com.simplemusic.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplemusic.SimpleMusic;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Client config {@code config/simplemusic-client.json} (concept docs/ai/KONZEPT-MUSIKPLATTE-UPLOAD-2026-10-19.md §4,
 * AUD-7): "Load foreign music". The server already sends only tracks it vouches for (approved, or your own pending
 * ones); with the switch off this client additionally never downloads or decodes any track it did not upload itself
 * in this session (Vorbis decoding of foreign files is the remaining client risk). Vanilla songs are not affected.
 */
public final class MusicClientConfig {
    public static final String FILE = "simplemusic-client.json";
    /** The only key of the file (wiki and check_data read it). */
    public static final String KEY = "loadForeignMusic";

    /** On by default: the server's own approval already filters what reaches a client. */
    public static boolean loadForeignMusic = true;
    /** Hashes this client uploaded (always allowed, they are our own files). */
    private static final Set<String> OWN = new HashSet<>();
    private static final Set<String> HINTED = new HashSet<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private MusicClientConfig() {}

    public static void load(Path dir) {
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject json = new JsonObject();
        if (file != null) {
            JsonObject read = com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleMusic.LOG::warn);
            if (read != null) json = read;
        }
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                Files.writeString(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleMusic.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    public static void apply(JsonObject json) {
        JsonElement v = json.get(KEY);
        loadForeignMusic = v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() ? v.getAsBoolean() : true;
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty(KEY, loadForeignMusic);
        return o;
    }

    public static void reset() {
        apply(new JsonObject());
        OWN.clear();
        HINTED.clear();
    }

    /** Remember a file this client is uploading (called with its hash before sending). */
    public static void own(String hash) {
        OWN.add(hash);
    }

    /** Whether a track may be downloaded, cached or played here. */
    public static boolean allowed(String hash) {
        return loadForeignMusic || OWN.contains(hash);
    }

    /** True once per hash and session: the caller shows the "switched off" hint then. */
    public static boolean firstRefusal(String hash) {
        return HINTED.add(hash);
    }
}
