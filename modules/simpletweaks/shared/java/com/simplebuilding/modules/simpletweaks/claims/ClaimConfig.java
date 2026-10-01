package com.simplebuilding.modules.simpletweaks.claims;

import com.google.gson.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.resources.Identifier;

/** Server-only, restart-bound configuration. No client can change these values. */
public record ClaimConfig(boolean enabled, int maxClaimsPerPlayer, int maxTrustedPlayers,
        int globalCap, int cooldownTicks, boolean opBypass, int spawnBuffer, List<String> dimensions) {
    public static final int MAX_CLAIMS = 256, MAX_TRUSTED = 64, MAX_GLOBAL = 10000;
    public static final int MAX_COOLDOWN = 72000, MAX_SPAWN_BUFFER = 256, MAX_DIMENSIONS = 32;
    public static final ClaimConfig DEFAULT = new ClaimConfig(false, 16, 8, 4096, 100, false, 16,
            List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"));
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
    }
    static boolean validDimension(String id) {
        if (id==null || id.length()>128 || Identifier.tryParse(id)==null || !id.contains(":")) return false;
        String path=Identifier.parse(id).getPath();
        return !path.startsWith("/") && Arrays.stream(path.split("/",-1)).noneMatch(s->s.isEmpty()||s.equals(".")||s.equals(".."));
    }
    public static ClaimConfig load(Path path) {
        try {
            if (!Files.exists(path)) {
                ClaimStore.atomicWrite(path, new GsonBuilder().setPrettyPrinting().create().toJson(DEFAULT));
                return DEFAULT;
            }
            if (Files.isSymbolicLink(path) || Files.size(path) > 16384) throw new IOException("Unsafe claim config");
            var json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            var d = DEFAULT;
            return new ClaimConfig(bool(json,"enabled",d.enabled), integer(json,"maxClaimsPerPlayer",d.maxClaimsPerPlayer),
                    integer(json,"maxTrustedPlayers",d.maxTrustedPlayers), integer(json,"globalCap",d.globalCap),
                    integer(json,"cooldownTicks",d.cooldownTicks), bool(json,"opBypass",d.opBypass),
                    integer(json,"spawnBuffer",d.spawnBuffer), json.has("dimensions")
                    ? json.getAsJsonArray("dimensions").asList().stream().map(JsonElement::getAsString).toList() : d.dimensions);
        } catch (Exception e) {
            // Never turn protection off silently because a server administrator made a typo.
            throw new IllegalStateException("Invalid claim config; original file preserved: " + path, e);
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
