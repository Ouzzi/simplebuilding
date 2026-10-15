package com.simplebuilding.modules.simpletweaks.claims;

import com.google.gson.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.resources.Identifier;

/** Server-only, restart-bound configuration. No client can change these values. */
public record ClaimConfig(boolean enabled, int maxClaimsPerPlayer, int maxTrustedPlayers,
        int globalCap, int cooldownTicks, boolean opBypass, int spawnBuffer, List<String> dimensions, String claimVisibility) {
    /** The settings before the claim table: visibility {@code all}. */
    public ClaimConfig(boolean enabled, int maxClaimsPerPlayer, int maxTrustedPlayers, int globalCap, int cooldownTicks, boolean opBypass, int spawnBuffer, List<String> dimensions) {
        this(enabled, maxClaimsPerPlayer, maxTrustedPlayers, globalCap, cooldownTicks, opBypass, spawnBuffer, dimensions, "all");
    }
    /** Same settings with another claim table visibility (tests). */
    public ClaimConfig withVisibility(String value) {
        return new ClaimConfig(enabled, maxClaimsPerPlayer, maxTrustedPlayers, globalCap, cooldownTicks, opBypass, spawnBuffer, dimensions, value);
    }
    public com.simplebuilding.modules.simpletweaks.table.TableVisibility visibility() { return com.simplebuilding.modules.simpletweaks.table.TableVisibility.parse(claimVisibility); }
    /** Written into the file; files without it (or lower) were written by the old "default off" build. */
    public static final int CONFIG_VERSION = 2;
    public static final int MAX_CLAIMS = 256, MAX_TRUSTED = 64, MAX_GLOBAL = 10000;
    public static final int MAX_COOLDOWN = 72000, MAX_SPAWN_BUFFER = 256, MAX_DIMENSIONS = 32;
    /** On by default since 2026-10-13 (owner release). An existing file with "enabled": false stays off. */
    public static final ClaimConfig DEFAULT = new ClaimConfig(true, 16, 8, 4096, 100, false, 16,
            List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"));
    /** The defaults with the main switch off (server administrators' opt-out, disabled-path tests). */
    public static final ClaimConfig OFF = new ClaimConfig(false, DEFAULT.maxClaimsPerPlayer, DEFAULT.maxTrustedPlayers,
            DEFAULT.globalCap, DEFAULT.cooldownTicks, DEFAULT.opBypass, DEFAULT.spawnBuffer, DEFAULT.dimensions, DEFAULT.claimVisibility);
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
                    json.has("claimVisibility") ? json.get("claimVisibility").getAsString() : d.claimVisibility);
        } catch (Exception e) {
            // Never turn protection off silently because a server administrator made a typo.
            throw new IllegalStateException("Invalid claim config; original file preserved: " + path, e);
        }
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
