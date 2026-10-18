package com.simplebuilding.modules.simpletweaks.protect;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/**
 * Server-only settings of the protection features (plan docs/ai/PLAN-TWEAKS-PROTECT-2026-10-17.md) in
 * {@code config/simpletweaks-protect.json}. Every value is clamped (AGENTS.md 3.8); a missing key takes its default, a
 * broken file is kept and the defaults are used (logged). Owner 17.10.: anti-x-ray on, every other protection off.
 */
public record ProtectConfig(int permission, AntiXray antiXray, Freecam freecam, SeedHash seedHash, HideHealth hideHealth, ActionChecks actionChecks) {

    /** Ore hiding in chunk packets; {@code dimensions} keyed by dimension id, "*" = every other dimension. */
    public record AntiXray(boolean enabled, int engineMode, double fakeOreChance, boolean exemptCreative, boolean exemptSpectator,
            int exemptPermission, Map<String, Dimension> dimensions) {
        public record Dimension(List<String> hidden, String replacement, String deepslateReplacement, int maxY, List<String> fakeOres) {
            public Dimension {
                hidden = entries(hidden); fakeOres = entries(fakeOres);
                replacement = id(replacement, "minecraft:stone"); deepslateReplacement = deepslateReplacement == null || deepslateReplacement.isBlank() ? "" : id(deepslateReplacement, "");
                maxY = Math.clamp(maxY, -2048, 4096);
            }
        }
        public AntiXray {
            engineMode = Math.clamp(engineMode, 1, 2); fakeOreChance = Math.clamp(fakeOreChance, 0.01, 1.0);
            exemptPermission = Math.clamp(exemptPermission, -1, 4);
            var out = new LinkedHashMap<String, Dimension>();
            if (dimensions != null) dimensions.forEach((k, v) -> { if (k != null && v != null && (k.equals("*") || net.minecraft.resources.Identifier.tryParse(k) != null) && out.size() < 64) out.put(k, v); });
            dimensions = Collections.unmodifiableMap(out);
        }
    }
    /** "Freecam may not pass through blocks"; requireClient kicks clients without Simple Tweaks while the rule is on. */
    public record Freecam(boolean enabled, boolean requireClient, String kickMessage) {
        public Freecam { kickMessage = kickMessage == null || kickMessage.isBlank() ? "" : kickMessage.length() > 256 ? kickMessage.substring(0, 256) : kickMessage; }
    }
    /** A random hashed seed instead of the real one in login/respawn packets. */
    public record SeedHash(boolean enabled) {}
    /** Other players' health is sent as their maximum (0 stays 0). */
    public record HideHealth(boolean enabled) {}
    /** Reach / fast-break logging for admins (no automatic punishment). */
    public record ActionChecks(boolean enabled, double reachSlack, double fastBreakRatio, int notifyPermission, int notifyCooldownSeconds, boolean log) {
        public ActionChecks {
            reachSlack = Math.clamp(reachSlack, 0.0, 16.0); fastBreakRatio = Math.clamp(fastBreakRatio, 0.05, 0.7);
            notifyPermission = Math.clamp(notifyPermission, 0, 4); notifyCooldownSeconds = Math.clamp(notifyCooldownSeconds, 0, 3600);
        }
    }

    static final List<String> OVERWORLD_ORES = List.of("minecraft:coal_ore", "minecraft:deepslate_coal_ore", "minecraft:iron_ore", "minecraft:deepslate_iron_ore",
            "minecraft:copper_ore", "minecraft:deepslate_copper_ore", "minecraft:gold_ore", "minecraft:deepslate_gold_ore", "minecraft:redstone_ore",
            "minecraft:deepslate_redstone_ore", "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore", "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore",
            "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore", "#c:ores");
    static final List<String> NETHER_ORES = List.of("minecraft:ancient_debris", "minecraft:nether_gold_ore", "minecraft:nether_quartz_ore", "#c:ores");

    public static final ProtectConfig DEFAULT = new ProtectConfig(3,
            new AntiXray(true, 1, 0.15, true, true, -1, map(
                    "minecraft:overworld", new AntiXray.Dimension(OVERWORLD_ORES, "minecraft:stone", "minecraft:deepslate", 320,
                            List.of("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore", "minecraft:gold_ore", "minecraft:deepslate_gold_ore", "minecraft:iron_ore",
                                    "minecraft:deepslate_iron_ore", "minecraft:emerald_ore", "minecraft:redstone_ore", "minecraft:lapis_ore", "minecraft:coal_ore")),
                    "minecraft:the_nether", new AntiXray.Dimension(NETHER_ORES, "minecraft:netherrack", "", 128,
                            List.of("minecraft:ancient_debris", "minecraft:nether_gold_ore", "minecraft:nether_quartz_ore")),
                    "minecraft:the_end", new AntiXray.Dimension(List.of("#c:ores"), "minecraft:end_stone", "", 256, List.of()))),
            new Freecam(false, false, "This server requires Simple Tweaks on the client while the Freecam rule is on."),
            new SeedHash(false),
            new HideHealth(false),
            new ActionChecks(false, 1.0, 0.5, 2, 30, true));

    /** Base of a dimension entry that is not in the defaults (modded dimensions, "*"). */
    static final AntiXray.Dimension TEMPLATE = new AntiXray.Dimension(List.of("#c:ores"), "minecraft:stone", "", 4096, List.of());

    public ProtectConfig {
        permission = Math.clamp(permission, 0, 4);
        var d = DEFAULT;
        if (d != null) {
            if (antiXray == null) antiXray = d.antiXray; if (freecam == null) freecam = d.freecam; if (seedHash == null) seedHash = d.seedHash;
            if (hideHealth == null) hideHealth = d.hideHealth; if (actionChecks == null) actionChecks = d.actionChecks;
        }
    }

    private static Map<String, AntiXray.Dimension> map(Object... kv) {
        var m = new LinkedHashMap<String, AntiXray.Dimension>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], (AntiXray.Dimension) kv[i + 1]);
        return m;
    }
    static List<String> entries(List<String> values) {
        return values == null ? List.of() : values.stream().filter(s -> s != null && net.minecraft.resources.Identifier.tryParse(s.startsWith("#") ? s.substring(1) : s) != null).distinct().limit(512).toList();
    }
    static String id(String value, String fallback) { return value != null && net.minecraft.resources.Identifier.tryParse(value) != null ? value : fallback; }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final Path PATH = Path.of("config/simpletweaks-protect.json");

    /** Writes the defaults when the file is missing; fills missing keys from the defaults without rewriting the file. */
    public static ProtectConfig load(Path path) {
        var log = org.slf4j.LoggerFactory.getLogger("simpletweaks-protect");
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.toAbsolutePath().getParent());
                Files.writeString(path, GSON.toJson(DEFAULT));
                return DEFAULT;
            }
            if (Files.isSymbolicLink(path) || Files.size(path) > 65536) throw new java.io.IOException("Unsafe protect config");
            return of(Files.readString(path));
        } catch (Exception e) {
            log.error("Invalid {}; the file is kept and the defaults are used until it is fixed", path, e);
            return DEFAULT;
        }
    }
    /** These defaults with the given JSON merged over them (tests: {@code of("{\"freecam\":{\"enabled\":true}}")}). */
    public static ProtectConfig of(String override) {
        var merged = GSON.toJsonTree(DEFAULT).getAsJsonObject();
        merge(merged, JsonParser.parseString(override).getAsJsonObject());
        return GSON.fromJson(merged, ProtectConfig.class);
    }
    private static void merge(JsonObject into, JsonObject from) {
        for (var e : from.entrySet()) {
            var old = into.get(e.getKey());
            if (old != null && e.getKey().equals("dimensions") && e.getValue().isJsonObject()) {
                // the dimension map decides which dimensions are covered; each entry merges onto its default (or the template)
                var dims = new JsonObject();
                for (var d : e.getValue().getAsJsonObject().entrySet()) {
                    if (!d.getValue().isJsonObject()) continue;
                    var base = old.getAsJsonObject().has(d.getKey()) ? old.getAsJsonObject().get(d.getKey()).deepCopy() : GSON.toJsonTree(TEMPLATE);
                    merge(base.getAsJsonObject(), d.getValue().getAsJsonObject());
                    dims.add(d.getKey(), base);
                }
                into.add("dimensions", dims);
            } else if (old != null && old.isJsonObject() && e.getValue().isJsonObject()) merge(old.getAsJsonObject(), e.getValue().getAsJsonObject());
            else if (old != null) into.add(e.getKey(), e.getValue());
        }
    }
    public String json() { return GSON.toJson(this); }
}
