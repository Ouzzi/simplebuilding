package com.simplepads;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplebuilding.framework.api.ConfigFiles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simplepads-server.json} (b-pads01, 04.11.2026): the sections {@code pads} (on/off per family),
 * {@code padTuning} (warm-up, launch strength, potion pad timing), {@code spawn} (Spawn Elytra, first-join gifts, teleporter
 * target) and {@code tuning} (stranger break time, blocked dimensions, chunk loader owner rule). Until 04.11. these were
 * SimpleBuilding's {@code tweaks.pads}, {@code tweaks.padTuning}, {@code tweaks.spawn} and {@code server.pads},
 * {@code server.dimensionLocks}, {@code server.chunkLoaders}; a missing file takes them over once from {@code simplebuilding.json}.
 * Missing or broken keys use the defaults, the file is rewritten complete.
 */
public class PadsConfig {
    public static final String FILE = "simplepads-server.json";
    public static final String LEGACY_FILE = "simplebuilding.json";
    public static final int MAX_TELEPORTER_WARMUP_TICKS = 12000;
    public static final double MAX_LAUNCHPAD_STRENGTH_MULTIPLIER = 2.0;
    public static final int MAX_POTION_CHARGE_STEP_TICKS = 1200;
    public static final double MAX_POTION_COOLDOWN_FACTOR = 10.0;
    public static final int MAX_SPAWN_ELYTRA_RADIUS = 256;
    public static final float MAX_BOOST_STRENGTH = 1.2f;
    public static final int MAX_BREAK_SECONDS = 600;
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("pads.enableChunkLoaders", "pads.enableElytraPads", "pads.enableFlypads",
            "pads.enableSpawnTeleporters", "pads.enableLaunchpads", "pads.enableTimedCopperPlates", "pads.enableFilterPlates",
            "pads.enablePotionPads", "padTuning.teleporterTier1WarmupTicks", "padTuning.teleporterTier2WarmupTicks",
            "padTuning.teleporterTier3WarmupTicks", "padTuning.launchpadStrengthMultiplier", "padTuning.potionPadChargeStepTicks",
            "padTuning.potionPadCooldownFactor", "spawn.disableFallDamageInSpawn", "spawn.firstJoinTeleporterCount",
            "spawn.firstJoinElytraPadCount", "spawn.giveElytraOnSpawn", "spawn.spawnElytraRadius", "spawn.useWorldSpawnAsCenter",
            "spawn.customSpawnElytraX", "spawn.customSpawnElytraZ", "spawn.flightTimeSeconds", "spawn.maxBoosts", "spawn.boostStrength",
            "spawn.spawn1X", "spawn.spawn1Y", "spawn.spawn1Z", "tuning.strangerPadBreakSeconds", "tuning.strangerPlateBreakSeconds",
            "tuning.chunkLoaderBlockedDimensions", "tuning.flypadBlockedDimensions", "tuning.chunkLoaderRequireOwnerOnline");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public Pads pads = new Pads();
    public PadTuning padTuning = new PadTuning();
    public Spawn spawn = new Spawn();
    public Tuning tuning = new Tuning();

    /** Clamps hand-edited values; missing groups come back. */
    public PadsConfig validate() {
        if (pads == null) pads = new Pads();
        if (padTuning == null) padTuning = new PadTuning();
        if (spawn == null) spawn = new Spawn();
        if (tuning == null) tuning = new Tuning();
        padTuning.teleporterTier1WarmupTicks = (int) bounded("padTuning.teleporterTier1WarmupTicks", padTuning.teleporterTier1WarmupTicks, 1, MAX_TELEPORTER_WARMUP_TICKS, 1000);
        padTuning.teleporterTier2WarmupTicks = (int) bounded("padTuning.teleporterTier2WarmupTicks", padTuning.teleporterTier2WarmupTicks, 1, MAX_TELEPORTER_WARMUP_TICKS, 400);
        padTuning.teleporterTier3WarmupTicks = (int) bounded("padTuning.teleporterTier3WarmupTicks", padTuning.teleporterTier3WarmupTicks, 1, MAX_TELEPORTER_WARMUP_TICKS, 100);
        padTuning.launchpadStrengthMultiplier = bounded("padTuning.launchpadStrengthMultiplier", padTuning.launchpadStrengthMultiplier, 0, MAX_LAUNCHPAD_STRENGTH_MULTIPLIER, 1.0);
        padTuning.potionPadChargeStepTicks = (int) bounded("padTuning.potionPadChargeStepTicks", padTuning.potionPadChargeStepTicks, 1, MAX_POTION_CHARGE_STEP_TICKS, 20);
        padTuning.potionPadCooldownFactor = bounded("padTuning.potionPadCooldownFactor", padTuning.potionPadCooldownFactor, 0, MAX_POTION_COOLDOWN_FACTOR, 1.5);
        spawn.spawnElytraRadius = (int) bounded("spawn.spawnElytraRadius", spawn.spawnElytraRadius, 1, MAX_SPAWN_ELYTRA_RADIUS, 25);
        spawn.boostStrength = (float) bounded("spawn.boostStrength", spawn.boostStrength, 0.1, MAX_BOOST_STRENGTH, 0.6);
        spawn.firstJoinTeleporterCount = Math.max(0, Math.min(64, spawn.firstJoinTeleporterCount));
        spawn.firstJoinElytraPadCount = Math.max(0, Math.min(64, spawn.firstJoinElytraPadCount));
        spawn.migrateLegacyFirstJoinCount();
        tuning.strangerPadBreakSeconds = (int) bounded("tuning.strangerPadBreakSeconds", tuning.strangerPadBreakSeconds, 1, MAX_BREAK_SECONDS, 60);
        tuning.strangerPlateBreakSeconds = (int) bounded("tuning.strangerPlateBreakSeconds", tuning.strangerPlateBreakSeconds, 1, MAX_BREAK_SECONDS, 10);
        if (tuning.chunkLoaderBlockedDimensions == null) tuning.chunkLoaderBlockedDimensions = "";
        if (tuning.flypadBlockedDimensions == null) tuning.flypadBlockedDimensions = "";
        return this;
    }

    /** Clamp in memory only; never save an invalid file as a side effect. */
    public static double bounded(String option, double value, double min, double max, double fallback) {
        double checked = Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
        if (Double.compare(value, checked) != 0) {
            SimplePads.LOG.warn("Invalid {}: {}; using {} (range {}..{})", option, value, checked, min, max);
        }
        return checked;
    }

    /** Runtime guard without per-packet warning spam. */
    public static double capped(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }

    // ---------------------------------------------------------------------------------------------
    // File
    // ---------------------------------------------------------------------------------------------

    /** Reads {@code dir/FILE} (first: takes SimpleBuilding's old sections over once), clamps and writes it back complete. */
    public static PadsConfig load(Path dir) {
        if (dir == null) return new PadsConfig().validate();
        Path file = dir.resolve(FILE);
        ConfigFiles.adoptLegacy(file, dir.resolve(LEGACY_FILE), PadsConfig::fromLegacy, SimplePads.LOG::info);
        PadsConfig read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, PadsConfig.class), SimplePads.LOG::warn);
        PadsConfig config = (read == null ? new PadsConfig() : read).validate();
        save(dir, config);
        return config;
    }

    public static void save(Path dir, PadsConfig config) {
        if (dir == null) return;
        try {
            Files.createDirectories(dir);
            ConfigFiles.writeAtomic(dir.resolve(FILE), GSON.toJson(config));
        } catch (Exception e) {
            SimplePads.LOG.warn("Could not write {}: {}", FILE, e.toString());
        }
    }

    /**
     * The pad parts of SimpleBuilding's {@code simplebuilding.json}: {@code tweaks.pads|padTuning|spawn} keep their names,
     * {@code server.pads.*}, {@code server.dimensionLocks.{chunkLoader,flypad}BlockedDimensions} and
     * {@code server.chunkLoaders.requireOwnerOnline} go to {@code tuning}. Null when there is nothing to take.
     */
    static String fromLegacy(String text) {
        JsonElement parsed = JsonParser.parseString(text);
        if (!parsed.isJsonObject()) return null;
        JsonObject old = parsed.getAsJsonObject();
        JsonObject out = new JsonObject();
        JsonObject tweaks = object(old, "tweaks");
        if (tweaks != null) {
            for (String key : List.of("pads", "padTuning", "spawn")) {
                if (tweaks.get(key) instanceof JsonObject section) out.add(key, section.deepCopy());
            }
        }
        JsonObject server = object(old, "server");
        if (server != null) {
            JsonObject tuning = new JsonObject();
            JsonObject pads = object(server, "pads");
            if (pads != null) {
                copy(pads, "strangerPadBreakSeconds", tuning, "strangerPadBreakSeconds");
                copy(pads, "strangerPlateBreakSeconds", tuning, "strangerPlateBreakSeconds");
            }
            JsonObject locks = object(server, "dimensionLocks");
            if (locks != null) {
                copy(locks, "chunkLoaderBlockedDimensions", tuning, "chunkLoaderBlockedDimensions");
                copy(locks, "flypadBlockedDimensions", tuning, "flypadBlockedDimensions");
            }
            JsonObject loaders = object(server, "chunkLoaders");
            if (loaders != null) copy(loaders, "requireOwnerOnline", tuning, "chunkLoaderRequireOwnerOnline");
            if (!tuning.isEmpty()) out.add("tuning", tuning);
        }
        return out.isEmpty() ? null : GSON.toJson(out);
    }

    /** {@link #fromLegacy} for the game tests. */
    public static String fromLegacyForTests(String text) {
        return fromLegacy(text);
    }

    private static JsonObject object(JsonObject parent, String key) {
        return parent.get(key) instanceof JsonObject o ? o : null;
    }

    private static void copy(JsonObject from, String key, JsonObject to, String as) {
        if (from.has(key)) to.add(as, from.get(key).deepCopy());
    }

    // ---------------------------------------------------------------------------------------------
    // Sections (field names as in SimpleBuilding's TweaksConfig, Gson reads by name)
    // ---------------------------------------------------------------------------------------------

    /**
     * Aus = der Block bleibt platzierbar und abbaubar, tut aber nichts: Chunk-Loader geben ihre
     * Chunks frei, Flypads nehmen den Flug zurueck, Elytra-Pads verteilen keine Elytren, Teleporter
     * und Launchpads zaehlen nicht, Platten senden kein Signal.
     */
    public static class Pads {
        public boolean enableChunkLoaders = true;
        public boolean enableElytraPads = true;
        public boolean enableFlypads = true;
        public boolean enableSpawnTeleporters = true;
        public boolean enableLaunchpads = true;
        public boolean enableTimedCopperPlates = true;
        public boolean enableFilterPlates = true;
        /** Aus: Trank-Pads geben keine Wirkungen mehr (Config-Umbau 2026-09-28). */
        public boolean enablePotionPads = true;
    }

    /**
     * Zeiten und Staerken der Pads (Config-Umbau 2026-09-28; Standard = das bisherige feste
     * Verhalten). Nur der Server liest sie; die Zugriffe unten begrenzen handeditierte Werte.
     */
    public static class PadTuning {
        /*
         * Stillstehen bis zum Sprung je Spawn-Teleporter-Stufe (Besitzer 2026-09-28: 50/20/5 s). Neue
         * Schluessel statt teleporterWarmupTicks/enderiteTeleporterWarmupTicks (100/60 fuer die alten fuenf
         * Stufen): eine damit gespeicherte Datei soll nicht still die alten Zeiten behalten.
         */
        /** Spawn-Teleporter I (Ticks; 1000 = 50 s). */
        public int teleporterTier1WarmupTicks = 1000;
        /** Spawn-Teleporter II (Ticks; 400 = 20 s). */
        public int teleporterTier2WarmupTicks = 400;
        /** Spawn-Teleporter III, Enderit (Ticks; 100 = 5 s). */
        public int teleporterTier3WarmupTicks = 100;
        /** Faktor auf den Schub der Launchpads (1,5 + 0,8 je Windkugel-Ladung). */
        public double launchpadStrengthMultiplier = 1.0;
        /** Dauer eines Aufladeschritts der Trank-Pads (Ticks; drei Schritte: 25/50/100 %). */
        public int potionPadChargeStepTicks = 20;
        /** Abklingzeit der Trank-Pads als Vielfaches der Wirkdauer; 0 = keine Abklingzeit. */
        public double potionPadCooldownFactor = 1.5;

        /** Wartezeit des Spawn-Teleporters der Stufe 1..3 (hoeher = 3), mindestens 1 Tick. */
        public int teleporterWarmup(int tier) {
            int ticks = tier >= 3 ? teleporterTier3WarmupTicks : tier == 2 ? teleporterTier2WarmupTicks : teleporterTier1WarmupTicks;
            return (int) capped(ticks, 1, MAX_TELEPORTER_WARMUP_TICKS, 1000);
        }

        public double launchpadStrengthFactor() {
            return capped(launchpadStrengthMultiplier, 0, MAX_LAUNCHPAD_STRENGTH_MULTIPLIER, 1.0);
        }

        public int potionPadStepTicks() {
            return (int) capped(potionPadChargeStepTicks, 1, MAX_POTION_CHARGE_STEP_TICKS, 20);
        }

        public double potionPadCooldown() {
            return capped(potionPadCooldownFactor, 0, MAX_POTION_COOLDOWN_FACTOR, 1.5);
        }
    }

    public static class Spawn {
        /** Obergrenze der Flugzeit (24 h): {@code flightTimeSeconds * 20} lief frueher ueber (Audit #51). */
        public static final int MAX_FLIGHT_SECONDS = 86_400;
        /** Obergrenze der Boosts; mehr Teilstriche passen ohnehin nicht in die 182-Pixel-Leiste. */
        public static final int MAX_BOOSTS = 100;
        public boolean disableFallDamageInSpawn = true;

        /** 0..64 Spawn-Teleporter beim ersten Betreten; 0 = keine (Besitzer-Wunsch 2026-09-26). */
        public int firstJoinTeleporterCount = 0;
        /** 0..64 Elytra-Pads beim ersten Betreten; 0 = keine. */
        public int firstJoinElytraPadCount = 0;
        /**
         * Alter Schluessel aus Simple Tweaks: EIN Wert fuer Teleporter und Pads, Standard 1. Wird nur
         * noch gelesen und von {@link #migrateLegacyFirstJoinCount()} auf die zwei neuen Werte
         * verteilt; {@code null} laesst Gson beim Speichern weg, der Schluessel verschwindet also
         * mit dem naechsten Speichern aus der Datei.
         */
        public Integer spawnTeleporterCount = null;
        public boolean giveElytraOnSpawn = false;
        public int spawnElytraRadius = 25;
        public boolean useWorldSpawnAsCenter = false;
        public int customSpawnElytraX = 0;
        public int customSpawnElytraZ = 0;
        public int flightTimeSeconds = 300;
        public int maxBoosts = 3;
        public float boostStrength = 0.6f;

        /** Flugzeit in Ticks, auf 1 s .. {@link #MAX_FLIGHT_SECONDS} begrenzt (auch fuer handeditierte Dateien). */
        public int flightTicks() {
            return Math.max(1, Math.min(MAX_FLIGHT_SECONDS, flightTimeSeconds)) * 20;
        }

        /** Boosts je Ladung, auf 1 .. {@link #MAX_BOOSTS} begrenzt. */
        public int boostCount() {
            return Math.max(1, Math.min(MAX_BOOSTS, maxBoosts));
        }

        /**
         * Ziel aller Spawn-Teleporter (per {@code worldspawn setspawn1}); y = -1000 heisst "nicht gesetzt"
         * (dann Weltspawn). Die Ziele 2-4 der frueheren Stufen II-IV gibt es seit 2026-09-28 nicht mehr
         * (drei Stufen, die sich nur in der Wartezeit unterscheiden); alte Schluessel in einer
         * Config-Datei werden beim Laden ignoriert.
         */
        public int spawn1X = 0;
        public int spawn1Y = -1000;
        public int spawn1Z = 0;

        /**
         * Uebernimmt einen alten {@code spawnTeleporterCount} aus einer bestehenden Config-Datei.
         * Ein vom Spieler geaenderter Wert (nicht der alte Standard 1) gilt wie frueher fuer beide
         * Geschenke; der alte Standard 1 wird zum neuen Standard 0, sonst bekaeme jede bestehende
         * Installation die Geschenke weiter, obwohl sie nie jemand eingestellt hat.
         *
         * @return ob ein alter Wert da war
         */
        public boolean migrateLegacyFirstJoinCount() {
            if (spawnTeleporterCount == null) {
                return false;
            }
            int legacy = Math.max(0, Math.min(64, spawnTeleporterCount));
            if (legacy != 1) {
                firstJoinTeleporterCount = legacy;
                firstJoinElytraPadCount = legacy;
            }
            spawnTeleporterCount = null;
            return true;
        }
    }

    /** Until 04.11. SimpleBuilding's "Server & Modpack Tuning" values of the pads. */
    public static class Tuning {
        /** Seconds a stranger needs to break someone else's pad (owner: normal speed). */
        public int strangerPadBreakSeconds = 60;
        /** Seconds a stranger needs to break someone else's chunk loader or copper/filter plate. */
        public int strangerPlateBreakSeconds = 10;
        /** Dimensions (comma separated ids) where chunk loaders stay idle. */
        public String chunkLoaderBlockedDimensions = "";
        /** Dimensions (comma separated ids) where flypads give no flight. */
        public String flypadBlockedDimensions = "";
        /** Chunk loaders only work while their owner is online. */
        public boolean chunkLoaderRequireOwnerOnline = true;

        /** Break progress per tick for strangers ({@code plate} = chunk loader, copper/filter plates). */
        public float strangerBreakProgress(boolean plate) {
            int seconds = (int) capped(plate ? strangerPlateBreakSeconds : strangerPadBreakSeconds, 1, MAX_BREAK_SECONDS, plate ? 10 : 60);
            return 1.0f / (seconds * 20.0f);
        }

        public boolean chunkLoaderBlockedIn(net.minecraft.resources.Identifier dimension) {
            return listed(chunkLoaderBlockedDimensions, dimension);
        }

        public boolean flypadBlockedIn(net.minecraft.resources.Identifier dimension) {
            return listed(flypadBlockedDimensions, dimension);
        }

        private static boolean listed(String list, net.minecraft.resources.Identifier dimension) {
            if (list == null || list.isBlank() || dimension == null) return false;
            for (String entry : list.split("[,;\\s]+")) {
                String id = entry.trim().toLowerCase(java.util.Locale.ROOT);
                if (id.isEmpty()) continue;
                if (!id.contains(":")) id = "minecraft:" + id;
                if (id.equals(dimension.toString())) return true;
            }
            return false;
        }

    }
}
