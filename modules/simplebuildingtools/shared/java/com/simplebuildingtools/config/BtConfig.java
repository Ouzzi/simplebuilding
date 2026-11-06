package com.simplebuildingtools.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplebuilding.framework.api.ConfigFiles;
import com.simplebuildingtools.SimpleBuildingTools;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simplebuildingtools-server.json} (b-btools07, 07.11.2026): {@code blueprint} (on/off, build speed,
 * picture import) and {@code wands} (hunger cost). Until 07.11. these were SimpleBuilding's {@code server.features.blueprint},
 * {@code server.blueprint.*} and {@code tools.buildingWandHungerCost}/{@code tools.wandHungerMultiplier}; a missing file takes
 * them over once from {@code simplebuilding.json}. Missing or broken keys use the defaults, the file is rewritten complete.
 * The server sends it to the clients (picture import button, blueprint switch).
 */
public class BtConfig {
    public static final String FILE = "simplebuildingtools-server.json";
    public static final String LEGACY_FILE = "simplebuilding.json";
    public static final int MAX_BLUEPRINT_BLOCKS_PER_TICK = 32768;
    public static final int MIN_PICTURE_SIDE = 16, MAX_PICTURE_SIDE = 128;
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("blueprint.enabled", "blueprint.maxBlocksPerTick", "blueprint.pictureImport",
            "blueprint.picturePermission", "blueprint.pictureMaxSide", "wands.hungerCost", "wands.hungerMultiplier");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public Blueprint blueprint = new Blueprint();
    public Wands wands = new Wands();

    /** Clamps hand-edited values; missing groups come back. */
    public BtConfig validate() {
        if (blueprint == null) blueprint = new Blueprint();
        if (wands == null) wands = new Wands();
        blueprint.maxBlocksPerTick = Math.max(1, Math.min(MAX_BLUEPRINT_BLOCKS_PER_TICK, blueprint.maxBlocksPerTick));
        blueprint.picturePermission = Math.max(0, Math.min(4, blueprint.picturePermission));
        blueprint.pictureMaxSide = Math.max(MIN_PICTURE_SIDE, Math.min(MAX_PICTURE_SIDE, blueprint.pictureMaxSide));
        if (!Double.isFinite(wands.hungerMultiplier)) wands.hungerMultiplier = 1.0;
        wands.hungerMultiplier = Math.max(0, Math.min(10, wands.hungerMultiplier));
        return this;
    }

    /** Reads {@code dir/FILE} (first: takes SimpleBuilding's old keys over once), clamps and writes it back complete. */
    public static BtConfig load(Path dir) {
        if (dir == null) return new BtConfig().validate();
        Path file = dir.resolve(FILE);
        ConfigFiles.adoptLegacy(file, dir.resolve(LEGACY_FILE), BtConfig::fromLegacy, SimpleBuildingTools.LOG::info);
        BtConfig read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, BtConfig.class), SimpleBuildingTools.LOG::warn);
        BtConfig config = (read == null ? new BtConfig() : read).validate();
        save(dir, config);
        return config;
    }

    public static void save(Path dir, BtConfig config) {
        if (dir == null) return;
        try {
            Files.createDirectories(dir);
            ConfigFiles.writeAtomic(dir.resolve(FILE), GSON.toJson(config));
        } catch (Exception e) {
            SimpleBuildingTools.LOG.warn("Could not write {}: {}", FILE, e.toString());
        }
    }

    /**
     * The tool parts of SimpleBuilding's {@code simplebuilding.json}: {@code server.features.blueprint} becomes
     * {@code blueprint.enabled}, {@code server.blueprint.*} keep their names, {@code tools.buildingWandHungerCost} and
     * {@code tools.wandHungerMultiplier} go to {@code wands}. Null when there is nothing to take.
     */
    static String fromLegacy(String text) {
        JsonElement parsed = JsonParser.parseString(text);
        if (!parsed.isJsonObject()) return null;
        JsonObject old = parsed.getAsJsonObject();
        JsonObject out = new JsonObject();
        JsonObject blueprint = new JsonObject();
        JsonObject server = object(old, "server");
        if (server != null) {
            JsonObject features = object(server, "features");
            if (features != null) copy(features, "blueprint", blueprint, "enabled");
            JsonObject bp = object(server, "blueprint");
            if (bp != null) {
                for (String key : List.of("maxBlocksPerTick", "pictureImport", "picturePermission", "pictureMaxSide")) copy(bp, key, blueprint, key);
            }
        }
        if (!blueprint.isEmpty()) out.add("blueprint", blueprint);
        JsonObject tools = object(old, "tools");
        if (tools != null) {
            JsonObject wands = new JsonObject();
            copy(tools, "buildingWandHungerCost", wands, "hungerCost");
            copy(tools, "wandHungerMultiplier", wands, "hungerMultiplier");
            if (!wands.isEmpty()) out.add("wands", wands);
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

    /** The blueprint: on/off, build speed and the picture import. */
    public static class Blueprint {
        /** Off: blueprints neither scan nor build (the items stay). */
        public boolean enabled = true;
        /** At most this many cells per tick while a blueprint builds (the build then takes longer). */
        public int maxBlocksPerTick = MAX_BLUEPRINT_BLOCKS_PER_TICK;
        /** Picture import (a picture becomes a pixel-art plan). */
        public boolean pictureImport = true;
        /** Permission level needed for the picture import (0 = everyone .. 4). */
        public int picturePermission = 0;
        /** Longest picture side in blocks (16..128). */
        public int pictureMaxSide = 128;
    }

    /** The building wands. */
    public static class Wands {
        /** Building with a wand costs hunger (beyond a free allowance; creative never). */
        public boolean hungerCost = true;
        /** Factor on the hunger per placed block (0..10). */
        public double hungerMultiplier = 1.0;
    }
}
