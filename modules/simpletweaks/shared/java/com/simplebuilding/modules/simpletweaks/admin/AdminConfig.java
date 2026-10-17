package com.simplebuilding.modules.simpletweaks.admin;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/**
 * Server-only settings of the admin and protection features, one section per feature, in
 * {@code config/simpletweaks-admin.json}. Every value is clamped (AGENTS.md 3.8); a missing key takes its default,
 * a broken file is kept and the defaults are used (logged). Permission values are Vanilla operator levels 0–4.
 */
public record AdminConfig(BlockLog blockLog, Deaths deathBackups, Invsee invsee, Vanish vanish, Freeze freeze,
        Restart restart, Backups backups, Maintenance maintenance, Lag lagFinder) {
    /** T10 block and container log. */
    public record BlockLog(boolean enabled, int permission, int rollbackPermission, int retentionDays, int rollbackMaxChanges, int flushSeconds) {
        public BlockLog {
            permission = level(permission); rollbackPermission = level(rollbackPermission);
            retentionDays = Math.clamp(retentionDays, 1, 365); rollbackMaxChanges = Math.clamp(rollbackMaxChanges, 1, 200000);
            flushSeconds = Math.clamp(flushSeconds, 1, 60);
        }
    }
    /** T11 inventory backups on death. */
    public record Deaths(boolean enabled, int permission, int keep) {
        public Deaths { permission = level(permission); keep = Math.clamp(keep, 1, 50); }
    }
    /** T13 /invsee and /endersee. */
    public record Invsee(boolean enabled, int permission, int editPermission) {
        public Invsee { permission = level(permission); editPermission = level(editPermission); }
    }
    /** T14 vanish. */
    public record Vanish(boolean enabled, int permission, int seePermission) {
        public Vanish { permission = level(permission); seePermission = level(seePermission); }
    }
    /** T15 freeze. */
    public record Freeze(boolean enabled, int permission) {
        public Freeze { permission = level(permission); }
    }
    /** T16 restart announcements; {@code times} as "HH:mm" in server time. */
    public record Restart(boolean enabled, int permission, List<String> times, List<Integer> warnMinutes, int exitCode) {
        public Restart {
            permission = level(permission); exitCode = Math.clamp(exitCode, 0, 255);
            times = times == null ? List.of() : times.stream().filter(t -> t != null && t.matches("([01]?\\d|2[0-3]):[0-5]\\d")).limit(24).toList();
            warnMinutes = warnMinutes == null ? List.of() : warnMinutes.stream().filter(Objects::nonNull).map(m -> Math.clamp(m, 1, 120)).distinct().sorted(Comparator.reverseOrder()).limit(12).toList();
        }
    }
    /** T17 automatic world backups. */
    public record Backups(boolean enabled, int permission, int intervalHours, int keep, String directory) {
        public Backups {
            permission = level(permission); intervalHours = Math.clamp(intervalHours, 1, 168); keep = Math.clamp(keep, 1, 100);
            if (directory == null || directory.isBlank() || directory.length() > 256) directory = "backups";
        }
    }
    /** T18 maintenance mode (the switch; the mode itself starts inactive and is turned on with /maintenance on). */
    public record Maintenance(boolean enabled, int permission, int joinPermission, List<String> allowed, String kickMessage, String motd, boolean kickOnline) {
        public Maintenance {
            permission = level(permission); joinPermission = level(joinPermission);
            allowed = allowed == null ? List.of() : allowed.stream().filter(s -> s != null && !s.isBlank() && s.length() <= 64).limit(256).toList();
            kickMessage = text(kickMessage, "The server is in maintenance. Please come back later.");
            motd = text(motd, "Maintenance - back soon");
        }
    }
    /** T34 lag finder (off by default). */
    public record Lag(boolean enabled, int permission, int defaultSeconds, int clockThreshold) {
        public Lag { permission = level(permission); defaultSeconds = Math.clamp(defaultSeconds, 1, 30); clockThreshold = Math.clamp(clockThreshold, 2, 1000); }
    }

    public static final AdminConfig DEFAULT = new AdminConfig(
            new BlockLog(true, 2, 3, 14, 20000, 5),
            new Deaths(true, 3, 5),
            new Invsee(true, 2, 2),
            new Vanish(true, 2, 2),
            new Freeze(true, 2),
            new Restart(true, 3, List.of(), List.of(15, 10, 5, 1), 0),
            new Backups(true, 3, 6, 5, "backups"),
            new Maintenance(true, 3, 2, List.of(), null, null, true),
            new Lag(false, 2, 5, 8));

    public AdminConfig {
        var d = DEFAULT;
        if (d != null) {
            if (blockLog == null) blockLog = d.blockLog; if (deathBackups == null) deathBackups = d.deathBackups;
            if (invsee == null) invsee = d.invsee; if (vanish == null) vanish = d.vanish; if (freeze == null) freeze = d.freeze;
            if (restart == null) restart = d.restart; if (backups == null) backups = d.backups;
            if (maintenance == null) maintenance = d.maintenance; if (lagFinder == null) lagFinder = d.lagFinder;
        }
    }

    static int level(int value) { return Math.clamp(value, 0, 4); }
    private static String text(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.length() > 256 ? value.substring(0, 256) : value;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final Path PATH = Path.of("config/simpletweaks-admin.json");

    /** Writes the defaults when the file is missing; fills missing keys from the defaults without rewriting the file. */
    public static AdminConfig load(Path path) {
        var log = org.slf4j.LoggerFactory.getLogger("simpletweaks-admin");
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.toAbsolutePath().getParent());
                Files.writeString(path, GSON.toJson(DEFAULT));
                return DEFAULT;
            }
            if (Files.isSymbolicLink(path) || Files.size(path) > 65536) throw new java.io.IOException("Unsafe admin config");
            var merged = GSON.toJsonTree(DEFAULT).getAsJsonObject();
            merge(merged, JsonParser.parseString(Files.readString(path)).getAsJsonObject());
            return GSON.fromJson(merged, AdminConfig.class);
        } catch (Exception e) {
            log.error("Invalid {}; the file is kept and the defaults are used until it is fixed", path, e);
            return DEFAULT;
        }
    }
    private static void merge(JsonObject into, JsonObject from) {
        for (var e : from.entrySet()) {
            var old = into.get(e.getKey());
            if (old != null && old.isJsonObject() && e.getValue().isJsonObject()) merge(old.getAsJsonObject(), e.getValue().getAsJsonObject());
            else if (old != null) into.add(e.getKey(), e.getValue());
        }
    }
    /** JSON of these settings (tests, documentation). */
    public String json() { return GSON.toJson(this); }
}
