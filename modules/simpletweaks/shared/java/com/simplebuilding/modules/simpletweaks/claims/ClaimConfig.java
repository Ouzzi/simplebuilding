package com.simplebuilding.modules.simpletweaks.claims;

import com.google.gson.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.resources.Identifier;

/** Server-only, restart-bound configuration. No client can change these values. */
public record ClaimConfig(boolean enabled, int maxClaimsPerPlayer, int maxTrustedPlayers,
        int globalCap, int cooldownTicks, boolean opBypass, int spawnBuffer, List<String> dimensions, String claimVisibility,
        int startChunks, boolean expansionEnabled, List<Tier> expansionTiers) {
    /** One material of an expansion step: item id and count. Existence of the item is checked when the step is bought. */
    public record Cost(String id, int count) {
        public Cost { if (id == null || id.length() > 128 || Identifier.tryParse(id) == null || !id.contains(":") || count < 1 || count > MAX_COST_COUNT) throw new IllegalArgumentException("Invalid expansion cost"); }
    }
    /** An expansion step: the land after buying it, the materials and the XP levels. */
    public record Tier(int chunks, List<Cost> items, int levels) {
        public Tier {
            if (chunks < 1 || chunks > MAX_CLAIMS || levels < 0 || levels > MAX_LEVELS || items == null || items.size() > MAX_COSTS) throw new IllegalArgumentException("Invalid expansion tier");
            items = List.copyOf(items);
        }
    }
    public static final int MAX_COST_COUNT = 576, MAX_COSTS = 8, MAX_LEVELS = 100, MAX_TIERS = 32, MAX_START = 256;
    /** The settings before the claim table: visibility {@code all}; no expansion (budget = {@code maxClaimsPerPlayer}). */
    public ClaimConfig(boolean enabled, int maxClaimsPerPlayer, int maxTrustedPlayers, int globalCap, int cooldownTicks, boolean opBypass, int spawnBuffer, List<String> dimensions) {
        this(enabled, maxClaimsPerPlayer, maxTrustedPlayers, globalCap, cooldownTicks, opBypass, spawnBuffer, dimensions, "all");
    }
    public ClaimConfig(boolean enabled, int maxClaimsPerPlayer, int maxTrustedPlayers, int globalCap, int cooldownTicks, boolean opBypass, int spawnBuffer, List<String> dimensions, String claimVisibility) {
        this(enabled, maxClaimsPerPlayer, maxTrustedPlayers, globalCap, cooldownTicks, opBypass, spawnBuffer, dimensions, claimVisibility, 4, false, List.of());
    }
    /** Same settings with another claim table visibility (tests). */
    public ClaimConfig withVisibility(String value) {
        return new ClaimConfig(enabled, maxClaimsPerPlayer, maxTrustedPlayers, globalCap, cooldownTicks, opBypass, spawnBuffer, dimensions, value, startChunks, expansionEnabled, expansionTiers);
    }
    /** Same settings with the land expansion (tests). */
    public ClaimConfig withExpansion(int start, List<Tier> tiers) {
        return new ClaimConfig(enabled, maxClaimsPerPlayer, maxTrustedPlayers, globalCap, cooldownTicks, opBypass, spawnBuffer, dimensions, claimVisibility, start, true, tiers);
    }
    /** The default curve of the owner-approved plan: 4 chunks to start, 64 at the end (copper ... nether star). */
    public static final List<Tier> DEFAULT_TIERS = List.of(
            new Tier(6, List.of(new Cost("minecraft:copper_ingot", 8)), 1), new Tier(8, List.of(new Cost("minecraft:iron_ingot", 8)), 2),
            new Tier(12, List.of(new Cost("minecraft:gold_ingot", 8)), 3), new Tier(16, List.of(new Cost("minecraft:diamond", 4)), 5),
            new Tier(24, List.of(new Cost("minecraft:diamond", 8), new Cost("minecraft:emerald", 8)), 8),
            new Tier(32, List.of(new Cost("minecraft:netherite_ingot", 2)), 10), new Tier(48, List.of(new Cost("minecraft:netherite_ingot", 4)), 15),
            new Tier(64, List.of(new Cost("minecraft:nether_star", 1)), 20));
    /**
     * Land a player may hold, from the stored budget (null = never bought: the start size). Never below what they already
     * hold (nobody loses land); without expansion it is the hard limit {@code maxClaimsPerPlayer}; never above that limit.
     */
    public int budgetOf(Integer stored, int held) {
        if (!expansionEnabled) return maxClaimsPerPlayer;
        return Math.min(maxClaimsPerPlayer, Math.max(Math.max(stored == null ? startChunks : stored, startChunks), held));
    }
    /** The next step above {@code budget} that the hard limit still allows, or null. */
    public Tier nextTier(int budget) {
        for (var t : expansionTiers) if (t.chunks() > budget) return t.chunks() <= maxClaimsPerPlayer ? t : null;
        return null;
    }
    public com.simplebuilding.modules.simpletweaks.table.TableVisibility visibility() { return com.simplebuilding.modules.simpletweaks.table.TableVisibility.parse(claimVisibility); }
    /** Written into the file; files without it (or lower) were written by the old "default off" build. */
    public static final int CONFIG_VERSION = 2;
    public static final int MAX_CLAIMS = 256, MAX_TRUSTED = 64, MAX_GLOBAL = 10000;
    public static final int MAX_COOLDOWN = 72000, MAX_SPAWN_BUFFER = 256, MAX_DIMENSIONS = 32;
    /** On by default since 2026-10-13 (owner release). An existing file with "enabled": false stays off. */
    public static final ClaimConfig DEFAULT = new ClaimConfig(true, 64, 8, 4096, 100, false, 16,
            List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"), "all", 4, true, DEFAULT_TIERS);
    /** The defaults with the main switch off (server administrators' opt-out, disabled-path tests). */
    public static final ClaimConfig OFF = new ClaimConfig(false, DEFAULT.maxClaimsPerPlayer, DEFAULT.maxTrustedPlayers,
            DEFAULT.globalCap, DEFAULT.cooldownTicks, DEFAULT.opBypass, DEFAULT.spawnBuffer, DEFAULT.dimensions, DEFAULT.claimVisibility, DEFAULT.startChunks, DEFAULT.expansionEnabled, DEFAULT.expansionTiers);
    public ClaimConfig {
        maxClaimsPerPlayer = Math.clamp(maxClaimsPerPlayer, 1, MAX_CLAIMS);
        maxTrustedPlayers = Math.clamp(maxTrustedPlayers, 0, MAX_TRUSTED);
        globalCap = Math.clamp(globalCap, 1, MAX_GLOBAL);
        cooldownTicks = Math.clamp(cooldownTicks, 20, MAX_COOLDOWN);
        spawnBuffer = Math.clamp(spawnBuffer, 0, MAX_SPAWN_BUFFER);
        if (dimensions == null || dimensions.size() > MAX_DIMENSIONS
                || dimensions.stream().anyMatch(x -> !validDimension(x)))
            throw new IllegalArgumentException("Invalid claim dimension allowlist");
        dimensions = List.copyOf(new LinkedHashSet<>(dimensions));
        if (com.simplebuilding.modules.simpletweaks.table.TableVisibility.parse(claimVisibility) == null) throw new IllegalArgumentException("claimVisibility");
        startChunks = Math.clamp(startChunks, 1, MAX_START);
        if (expansionTiers == null || expansionTiers.size() > MAX_TIERS) throw new IllegalArgumentException("Invalid expansion tiers");
        int last = startChunks;
        for (var t : expansionTiers) { if (t.chunks() <= last) throw new IllegalArgumentException("Expansion tiers must rise above the start size"); last = t.chunks(); }
        expansionTiers = List.copyOf(expansionTiers);
    }
    static boolean validDimension(String id) {
        if (id==null || id.length()>128 || Identifier.tryParse(id)==null || !id.contains(":")) return false;
        String path=Identifier.parse(id).getPath();
        return !path.startsWith("/") && Arrays.stream(path.split("/",-1)).noneMatch(s->s.isEmpty()||s.equals(".")||s.equals(".."));
    }
    public static ClaimConfig load(Path path) {
        try {
            if (!Files.exists(path)) {
                var gson = new GsonBuilder().setPrettyPrinting().create();
                var fresh = gson.toJsonTree(DEFAULT).getAsJsonObject();
                fresh.addProperty("configVersion", CONFIG_VERSION);
                ClaimStore.atomicWrite(path, gson.toJson(fresh));
                return DEFAULT;
            }
            if (Files.isSymbolicLink(path) || Files.size(path) > 16384) throw new IOException("Unsafe claim config");
            var json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            var d = DEFAULT;
            if (versionOf(json) < CONFIG_VERSION) migrate(path, json);
            return new ClaimConfig(bool(json,"enabled",d.enabled), integer(json,"maxClaimsPerPlayer",d.maxClaimsPerPlayer),
                    integer(json,"maxTrustedPlayers",d.maxTrustedPlayers), integer(json,"globalCap",d.globalCap),
                    integer(json,"cooldownTicks",d.cooldownTicks), bool(json,"opBypass",d.opBypass),
                    integer(json,"spawnBuffer",d.spawnBuffer), json.has("dimensions")
                    ? json.getAsJsonArray("dimensions").asList().stream().map(JsonElement::getAsString).toList() : d.dimensions,
                    json.has("claimVisibility") ? json.get("claimVisibility").getAsString() : d.claimVisibility,
                    integer(json,"startChunks",d.startChunks), bool(json,"expansionEnabled",d.expansionEnabled), tiers(json,d.expansionTiers));
        } catch (Exception e) {
            // Never turn protection off silently because a server administrator made a typo.
            throw new IllegalStateException("Invalid claim config; original file preserved: " + path, e);
        }
    }
    private static List<Tier> tiers(JsonObject j, List<Tier> fallback) {
        if (!j.has("expansionTiers")) return fallback;
        var out = new ArrayList<Tier>();
        for (var e : j.getAsJsonArray("expansionTiers")) {
            var t = e.getAsJsonObject();
            var items = new ArrayList<Cost>();
            if (t.has("items")) for (var i : t.getAsJsonArray("items")) { var c = i.getAsJsonObject(); items.add(new Cost(c.get("id").getAsString(), integer(c, "count", 1))); }
            out.add(new Tier(integer(t, "chunks", 0), items, integer(t, "levels", 0)));
        }
        return out;
    }
    private static int versionOf(JsonObject j) {
        return j.has("configVersion") && j.get("configVersion").isJsonPrimitive() ? j.get("configVersion").getAsInt() : 1;
    }
    /** One-time: the old build wrote "enabled": false on first start; that value was never a choice, so it turns on once. */
    private static void migrate(Path path, JsonObject json) {
        boolean wasOff = json.has("enabled") && json.get("enabled").isJsonPrimitive()
                && json.getAsJsonPrimitive("enabled").isBoolean() && !json.get("enabled").getAsBoolean();
        if (wasOff) json.addProperty("enabled", true);
        json.addProperty("configVersion", CONFIG_VERSION);
        try {
            ClaimStore.atomicWrite(path, new GsonBuilder().setPrettyPrinting().create().toJson(json));
            if (wasOff) org.slf4j.LoggerFactory.getLogger("simpletweaks-claims").info(
                    "Claim config migrated to version {}: \"enabled\": false from the old default was set to true once. Set it to false again to opt out.", CONFIG_VERSION);
        } catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger("simpletweaks-claims").warn("Could not write migrated claim config {}", path, e);
        }
    }
    private static int integer(JsonObject j,String k,int fallback) {
        if (!j.has(k)) return fallback;
        return j.get(k).getAsBigDecimal().max(java.math.BigDecimal.valueOf(Integer.MIN_VALUE))
                .min(java.math.BigDecimal.valueOf(Integer.MAX_VALUE)).intValueExact();
    }
    private static boolean bool(JsonObject j,String k,boolean fallback) {
        if (!j.has(k)) return fallback;
        if (!j.get(k).isJsonPrimitive() || !j.getAsJsonPrimitive(k).isBoolean()) throw new IllegalArgumentException(k);
        return j.get(k).getAsBoolean();
    }
}
