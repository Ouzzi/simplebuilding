package com.simplemusic.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplemusic.SimpleMusic;
import com.simplemusic.upload.OggCheck;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Server config {@code config/simplemusic-server.json} (server authority, every number clamped to a hard range);
 * missing or broken keys use the defaults and the file is rewritten complete.
 */
public final class MusicConfig {
    public static final String FILE = "simplemusic-server.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("enabled", "sheetSounds", "blockedSounds", "maxTonesPerTick",
            "toneRange", "uploads", "maxUploadMiB", "maxUploadSeconds", "quotaPerPlayer",
            "storeLimitMiB", "uploadKiBPerSecond", "downloadKiBPerSecond", "jukeboxCustomDiscs");

    /** Which sounds a sheet may name (owner decision N4 a: all, with the blocklist). */
    public enum SheetSounds { INSTRUMENTS, HEADS, ALL }

    /** Who may burn a disc (owner decision U2 b: everyone within the quota, an OP approves). */
    public enum Uploads { EVERYONE, APPROVE, OP, OFF }

    /** Default blocklist: music, discs, UI and sounds that carry over the whole world (prefix match). */
    public static final List<String> DEFAULT_BLOCKED = List.of("minecraft:music.", "minecraft:music_disc.", "minecraft:ui.",
            "minecraft:ambient.cave", "minecraft:entity.wither.spawn", "minecraft:entity.ender_dragon.death",
            "minecraft:block.end_portal.spawn", "minecraft:event.raid.horn", "minecraft:item.goat_horn.");

    public static boolean enabled = true;
    public static SheetSounds sheetSounds = SheetSounds.ALL;
    public static final List<String> blockedSounds = new ArrayList<>();
    public static int maxTonesPerTick = 8;
    /** Blocks a composition disc's tones carry (B23-M9: was {@code orchestrionRange}, the old key is still read). */
    public static int toneRange = 48;
    public static Uploads uploads = Uploads.APPROVE;
    public static int maxUploadMiB = 4;
    public static int maxUploadSeconds = 300;
    public static int quotaPerPlayer = 5;
    public static int storeLimitMiB = 256;
    public static int uploadKiBPerSecond = 256;
    public static int downloadKiBPerSecond = 512;
    public static boolean jukeboxCustomDiscs = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static {
        reset();
    }

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleMusic.LOG::warn);
        if (read != null) json = read;
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
        enabled = bool(json, "enabled", true);
        sheetSounds = enumeration(json, "sheetSounds", SheetSounds.class, SheetSounds.ALL);
        blockedSounds.clear();
        if (json.has("blockedSounds") && json.get("blockedSounds").isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray("blockedSounds")) {
                if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() && !e.getAsString().isBlank()) blockedSounds.add(e.getAsString().strip());
            }
        } else {
            blockedSounds.addAll(DEFAULT_BLOCKED);
        }
        maxTonesPerTick = clamp(integer(json, "maxTonesPerTick", 8), 1, 8);
        toneRange = clamp(integer(json, "toneRange", integer(json, "orchestrionRange", 48)), 16, 64);
        uploads = enumeration(json, "uploads", Uploads.class, Uploads.APPROVE);
        maxUploadMiB = clamp(integer(json, "maxUploadMiB", 4), 1, 16);
        maxUploadSeconds = clamp(integer(json, "maxUploadSeconds", 300), 10, 900);
        quotaPerPlayer = clamp(integer(json, "quotaPerPlayer", 5), 0, 50);
        storeLimitMiB = clamp(integer(json, "storeLimitMiB", 256), 16, 2048);
        uploadKiBPerSecond = clamp(integer(json, "uploadKiBPerSecond", 256), 16, 1024);
        downloadKiBPerSecond = clamp(integer(json, "downloadKiBPerSecond", 512), 32, 4096);
        jukeboxCustomDiscs = bool(json, "jukeboxCustomDiscs", true);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("sheetSounds", sheetSounds.name().toLowerCase(Locale.ROOT));
        JsonArray blocked = new JsonArray();
        blockedSounds.forEach(blocked::add);
        o.add("blockedSounds", blocked);
        o.addProperty("maxTonesPerTick", maxTonesPerTick);
        o.addProperty("toneRange", toneRange);
        o.addProperty("uploads", uploads.name().toLowerCase(Locale.ROOT));
        o.addProperty("maxUploadMiB", maxUploadMiB);
        o.addProperty("maxUploadSeconds", maxUploadSeconds);
        o.addProperty("quotaPerPlayer", quotaPerPlayer);
        o.addProperty("storeLimitMiB", storeLimitMiB);
        o.addProperty("uploadKiBPerSecond", uploadKiBPerSecond);
        o.addProperty("downloadKiBPerSecond", downloadKiBPerSecond);
        o.addProperty("jukeboxCustomDiscs", jukeboxCustomDiscs);
        return o;
    }

    /** Upload limits for the Ogg check (sample rate 22.05-48 kHz is fixed by the concept). */
    public static OggCheck.Limits uploadLimits() {
        return new OggCheck.Limits(maxUploadMiB << 20, maxUploadSeconds, 22050, 48000);
    }

    /** Whether a sound id is on the blocklist (prefix match, a trailing '.' or '/' matches the whole group). */
    public static boolean blocked(String soundId) {
        for (String b : blockedSounds) {
            if (soundId.equals(b) || soundId.startsWith(b)) return true;
        }
        return false;
    }

    public static void reset() {
        apply(new JsonObject());
    }

    private static <E extends Enum<E>> E enumeration(JsonObject json, String key, Class<E> type, E fallback) {
        JsonElement v = json.get(key);
        if (v == null || !v.isJsonPrimitive()) return fallback;
        try {
            return Enum.valueOf(type, v.getAsString().strip().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement v = json.get(key);
        return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() ? v.getAsBoolean() : fallback;
    }

    private static int integer(JsonObject json, String key, int fallback) {
        JsonElement v = json.get(key);
        try {
            return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber() ? v.getAsInt() : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private MusicConfig() {}
}
