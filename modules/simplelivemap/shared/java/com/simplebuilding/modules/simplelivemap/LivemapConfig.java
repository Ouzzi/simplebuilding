package com.simplebuilding.modules.simplelivemap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.framework.api.ConfigFiles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Server config {@code config/simplelivemap.json}, four groups (categories): {@code general}, {@code record} (what counts as
 * explored), {@code render} (tile budget), {@code web} (built-in web server, off by default). Missing or broken keys use the
 * defaults and the file is rewritten complete; every number is clamped (server authority, A9). Plan
 * {@code docs/ai/PLAN-B-LIVE32-2026-10-31.md}.
 */
public final class LivemapConfig {
    public static final String FILE = SimpleLivemap.MOD_ID + ".json";
    public static final String PRIVATE = "private", PUBLIC = "public";
    public static final int PORT_DEFAULT = 8125, RADIUS_MAX = 8, RATE_DEFAULT = 1200, RATE_MIN = 30, RATE_MAX = 20000;

    // general
    public static boolean enabled;
    // record
    /** Chunks around a player that count as visited (0 = only the own chunk, 2 = 5 x 5). */
    public static int radius;
    /** How often player positions are checked. */
    public static int intervalTicks;
    /** A held filled Vanilla map counts its drawn area as explored. */
    public static boolean maps;
    // render
    public static int chunksPerTick;
    /** A visited chunk is drawn again on a later visit when its picture is older than this. */
    public static int refreshMinutes;
    /** Vanilla map relief (brighter/darker by height to the north): the 2.5D look of the tiles. */
    public static boolean shading;
    // web
    public static boolean webEnabled;
    public static String bind;
    public static int port;
    /** {@link #PRIVATE}: tiles and live data only with the token; {@link #PUBLIC}: for everyone who reaches the port. */
    public static String visibility;
    /** Secret of the private map (32 hex characters, made on first start); never logged, {@code ***} in the config command. */
    public static String token;
    public static boolean showPlayers;
    public static int requestsPerMinute;
    public static int liveSeconds;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final SecureRandom RANDOM = new SecureRandom();

    static {
        apply(new JsonObject());
    }

    private LivemapConfig() {}

    public static boolean isPublic() {
        return PUBLIC.equals(visibility);
    }

    /** A fresh random token (128 bit, hex). */
    public static String newToken() {
        byte[] b = new byte[16];
        RANDOM.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    /** Reads (or creates) the file; a missing token is made here so a private map is never open by accident. */
    public static void load(Path dir) {
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = file == null ? null : ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleLivemap.LOG::warn);
        apply(read == null ? new JsonObject() : read);
        if (token.isEmpty()) token = newToken();
        save(dir);
    }

    public static void save(Path dir) {
        if (dir == null) return;
        try {
            Files.createDirectories(dir);
            ConfigFiles.writeAtomic(dir.resolve(FILE), GSON.toJson(toJson()));
        } catch (Exception e) {
            SimpleLivemap.LOG.warn("Simple Livemap: could not write {}: {}", FILE, e.toString());
        }
    }

    public static void apply(JsonObject json) {
        JsonObject g = group(json, "general"), r = group(json, "record"), d = group(json, "render"), w = group(json, "web");
        enabled = bool(g, "enabled", true);
        radius = integer(r, "radius", 2, 0, RADIUS_MAX);
        intervalTicks = integer(r, "intervalTicks", 20, 5, 200);
        maps = bool(r, "maps", true);
        chunksPerTick = integer(d, "chunksPerTick", 2, 1, 32);
        refreshMinutes = integer(d, "refreshMinutes", 10, 1, 1440);
        shading = bool(d, "shading", true);
        webEnabled = bool(w, "enabled", false);
        String b = string(w, "bind", "127.0.0.1").trim();
        bind = b.isEmpty() || b.length() > 64 ? "127.0.0.1" : b;
        port = integer(w, "port", PORT_DEFAULT, 1, 65535);
        String v = string(w, "visibility", PRIVATE).trim().toLowerCase(java.util.Locale.ROOT);
        visibility = PUBLIC.equals(v) ? PUBLIC : PRIVATE;
        String t = string(w, "token", "").trim();
        token = t.matches("[A-Za-z0-9_-]{16,128}") ? t : "";
        showPlayers = bool(w, "showPlayers", false);
        requestsPerMinute = integer(w, "requestsPerMinute", RATE_DEFAULT, RATE_MIN, RATE_MAX);
        liveSeconds = integer(w, "liveSeconds", 2, 1, 60);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        JsonObject g = new JsonObject();
        g.addProperty("enabled", enabled);
        o.add("general", g);
        JsonObject r = new JsonObject();
        r.addProperty("radius", radius);
        r.addProperty("intervalTicks", intervalTicks);
        r.addProperty("maps", maps);
        o.add("record", r);
        JsonObject d = new JsonObject();
        d.addProperty("chunksPerTick", chunksPerTick);
        d.addProperty("refreshMinutes", refreshMinutes);
        d.addProperty("shading", shading);
        o.add("render", d);
        JsonObject w = new JsonObject();
        w.addProperty("enabled", webEnabled);
        w.addProperty("bind", bind);
        w.addProperty("port", port);
        w.addProperty("visibility", visibility);
        w.addProperty("token", token);
        w.addProperty("showPlayers", showPlayers);
        w.addProperty("requestsPerMinute", requestsPerMinute);
        w.addProperty("liveSeconds", liveSeconds);
        o.add("web", w);
        return o;
    }

    private static JsonObject group(JsonObject o, String key) {
        JsonElement e = o == null ? null : o.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
    }

    private static boolean bool(JsonObject o, String key, boolean def) {
        try {
            return o.has(key) ? o.get(key).getAsBoolean() : def;
        } catch (RuntimeException e) {
            return def;
        }
    }

    private static int integer(JsonObject o, String key, int def, int min, int max) {
        try {
            return o.has(key) ? Math.max(min, Math.min(max, o.get(key).getAsInt())) : def;
        } catch (RuntimeException e) {
            return def;
        }
    }

    private static String string(JsonObject o, String key, String def) {
        try {
            return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : def;
        } catch (RuntimeException e) {
            return def;
        }
    }
}
