package com.simplebuilding.modules.simpletweaks.play;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/**
 * Server-only settings of the player/world/event features (plan docs/ai/PLAN-TWEAKS-PLAY-2026-10-17.md), one section
 * per feature, in {@code config/simpletweaks-play.json}. Every value is clamped (AGENTS.md 3.8); a missing key takes its
 * default, a broken file is kept and the defaults are used (logged). Owner rule 17.10.: what only acts on a command is on
 * by default, automatic behaviour changes keep the owner's choice.
 */
public record PlayConfig(Coords coords, DeathHint deathHint, Mentions mentions, Playtime playtime, MobGriefing mobGriefing,
        Pvp pvp, Welcome welcome, LoginReward loginReward, Events events, Leaderboard leaderboard, Vote vote, Afk afk,
        Border border, NetherRoof netherRoof, KeepInventory keepInventory, DragonEgg dragonEgg, Phantoms phantoms,
        EntityLimit entityLimit, Cleanup cleanup, Pregen pregen, Map<String, List<String>> autoOffWith) {

    /** T4 /coords. */
    public record Coords(boolean enabled, boolean ping, int pingSeconds, int cooldownSeconds) {
        public Coords { pingSeconds = Math.clamp(pingSeconds, 1, 60); cooldownSeconds = Math.clamp(cooldownSeconds, 0, 600); }
    }
    /** T5 private death hint. */
    public record DeathHint(boolean enabled) {}
    /** T6 chat mentions. */
    public record Mentions(boolean enabled, boolean sound, float volume, int everyonePermission) {
        public Mentions { volume = Math.clamp(volume, 0f, 1f); everyonePermission = level(everyonePermission); }
    }
    /** T7 /playtime and /seen. */
    public record Playtime(boolean enabled) {}
    /** T8 mob griefing per mob (only forbids, never allows more than the Vanilla rule). */
    public record MobGriefing(boolean enabled, List<String> blocked) {
        public MobGriefing { blocked = ids(blocked); }
    }
    /** T9 personal PvP. */
    public record Pvp(boolean enabled, boolean defaultOn, int cooldownSeconds) {
        public Pvp { cooldownSeconds = Math.clamp(cooldownSeconds, 0, 3600); }
    }
    /** T19 welcome, rotating tips, own join/leave lines (empty = Vanilla). */
    public record Welcome(boolean enabled, String message, List<String> tips, int tipMinutes, String joinMessage, String leaveMessage) {
        public Welcome {
            message = text(message); joinMessage = text(joinMessage); leaveMessage = text(leaveMessage);
            tips = tips == null ? List.of() : tips.stream().filter(t -> t != null && !t.isBlank()).map(PlayConfig::text).limit(64).toList();
            tipMinutes = Math.clamp(tipMinutes, 1, 1440);
        }
    }
    /** T20 daily login reward; items as "namespace:id count". */
    public record LoginReward(boolean enabled, List<String> items, int billsPerDay, int maxBills, int bonusEvery, List<String> bonusItems) {
        public LoginReward {
            items = stacks(items); bonusItems = stacks(bonusItems);
            billsPerDay = Math.clamp(billsPerDay, 0, 64); maxBills = Math.clamp(maxBills, 0, 64); bonusEvery = Math.clamp(bonusEvery, 0, 365);
        }
    }
    /** T21 server events; the command is on, the automatic schedule ("SAT 20:00 double_xp 60") is off. */
    public record Events(boolean enabled, int permission, boolean scheduleEnabled, List<String> schedule, int defaultMinutes,
            float xpMultiplier, int extraLootRolls, int bloodMoonWaveSeconds, int bloodMoonMobs) {
        public Events {
            permission = level(permission);
            schedule = schedule == null ? List.of() : schedule.stream().filter(s -> s != null && ServerEventsSchedule.parse(s) != null).limit(64).toList();
            defaultMinutes = Math.clamp(defaultMinutes, 1, 1440); xpMultiplier = Math.clamp(xpMultiplier, 1f, 10f);
            extraLootRolls = Math.clamp(extraLootRolls, 1, 5); bloodMoonWaveSeconds = Math.clamp(bloodMoonWaveSeconds, 5, 600);
            bloodMoonMobs = Math.clamp(bloodMoonMobs, 1, 8);
        }
    }
    /** T22 leaderboard: the command is on, the always-visible sidebar is off. */
    public record Leaderboard(boolean enabled, boolean sidebar, String sidebarStat, int refreshSeconds) {
        public Leaderboard {
            if (sidebarStat == null || !List.of("playtime", "deaths", "kills", "mined").contains(sidebarStat)) sidebarStat = "playtime";
            refreshSeconds = Math.clamp(refreshSeconds, 10, 3600);
        }
    }
    /** T23 votes; the day/night option is off. */
    public record Vote(boolean enabled, int seconds, int percent, int cooldownSeconds, boolean weather, boolean events, boolean dayNight) {
        public Vote { seconds = Math.clamp(seconds, 10, 300); percent = Math.clamp(percent, 1, 100); cooldownSeconds = Math.clamp(cooldownSeconds, 0, 86400); }
    }
    /** T24 AFK; the kick is off (Vanilla has player-idle-timeout too). */
    public record Afk(boolean enabled, int minutes, boolean excludeFromSleep, boolean kick, int kickMinutes) {
        public Afk { minutes = Math.clamp(minutes, 1, 120); kickMinutes = Math.clamp(kickMinutes, 2, 1440); }
    }
    /** T25 one border per dimension, applied to the Vanilla per-dimension world border at start. */
    public record Border(boolean enabled, Map<String, Dimension> dimensions) {
        public record Dimension(double size, double centerX, double centerZ) {
            public Dimension { size = Math.clamp(size, 16, 59_999_968); centerX = Math.clamp(centerX, -29_999_984, 29_999_984); centerZ = Math.clamp(centerZ, -29_999_984, 29_999_984); }
        }
        public Border {
            var out = new LinkedHashMap<String, Dimension>();
            if (dimensions != null) dimensions.forEach((k, v) -> { if (k != null && v != null && net.minecraft.resources.Identifier.tryParse(k) != null && out.size() < 64) out.put(k, v); });
            dimensions = Collections.unmodifiableMap(out);
        }
    }
    /** T26 Nether roof lock. */
    public record NetherRoof(boolean enabled, boolean opBypass, int bypassPermission) {
        public NetherRoof { bypassPermission = level(bypassPermission); }
    }
    /** T27 partial keep inventory: hotbar_armor, xp or custom. */
    public record KeepInventory(boolean enabled, String mode, boolean keepHotbar, boolean keepArmor, boolean keepOffhand, boolean keepMain, int keepXpPercent) {
        public KeepInventory {
            if (mode == null || !List.of("hotbar_armor", "xp", "custom").contains(mode)) mode = "hotbar_armor";
            keepXpPercent = Math.clamp(keepXpPercent, 0, 100);
        }
    }
    /** T28 a dragon egg after every dragon fight. */
    public record DragonEgg(boolean enabled) {}
    /** T30 /phantoms per player. */
    public record Phantoms(boolean enabled) {}
    /** T31 mobs per chunk: notify and block new mobs, never delete. */
    public record EntityLimit(boolean enabled, int perChunk, int scanSeconds, boolean blockSpawns) {
        public EntityLimit { perChunk = Math.clamp(perChunk, 4, 1000); scanSeconds = Math.clamp(scanSeconds, 2, 600); }
    }
    /** T32 ground item cleanup with countdown. */
    public record Cleanup(boolean enabled, int permission, int intervalMinutes, List<Integer> warnSeconds, int minAgeSeconds, String exemptRarity, List<String> exempt) {
        public Cleanup {
            permission = level(permission); intervalMinutes = Math.clamp(intervalMinutes, 1, 1440); minAgeSeconds = Math.clamp(minAgeSeconds, 0, 3600);
            warnSeconds = warnSeconds == null ? List.of() : warnSeconds.stream().filter(Objects::nonNull).map(s -> Math.clamp(s, 1, 600)).distinct().sorted(Comparator.reverseOrder()).limit(16).toList();
            if (exemptRarity == null || !List.of("uncommon", "rare", "epic", "none").contains(exemptRarity)) exemptRarity = "rare";
            exempt = ids(exempt);
        }
    }
    /** T33 chunk pregeneration. */
    public record Pregen(boolean enabled, int permission, int parallel, int perTick, int maxRadius) {
        public Pregen { permission = level(permission); parallel = Math.clamp(parallel, 1, 64); perTick = Math.clamp(perTick, 1, 64); maxRadius = Math.clamp(maxRadius, 1, 100_000); }
    }

    public static final PlayConfig DEFAULT = new PlayConfig(
            new Coords(true, true, 10, 5),
            new DeathHint(true),
            new Mentions(true, true, 0.6f, 2),
            new Playtime(true),
            new MobGriefing(false, List.of("minecraft:creeper", "minecraft:enderman", "minecraft:ghast", "minecraft:wither", "minecraft:ravager")),
            new Pvp(true, true, 30),
            new Welcome(false, "Welcome, {player}!", List.of("Share where you are with /coords.", "Vote for clear weather with /vote weather clear.", "Check your play time with /playtime."), 15, "", ""),
            new LoginReward(false, List.of("minecraft:bread 4"), 1, 7, 7, List.of("minecraft:diamond 1")),
            new Events(true, 2, false, List.of(), 60, 2f, 1, 30, 2),
            new Leaderboard(true, false, "playtime", 60),
            new Vote(true, 30, 50, 300, true, true, false),
            new Afk(true, 5, true, false, 30),
            new Border(true, Map.of()),
            new NetherRoof(false, true, 2),
            new KeepInventory(true, "hotbar_armor", true, true, true, false, 0),
            new DragonEgg(true),
            new Phantoms(true),
            new EntityLimit(false, 50, 10, true),
            new Cleanup(false, 2, 15, List.of(60, 30, 10, 5, 4, 3, 2, 1), 60, "rare", List.of("minecraft:nether_star", "minecraft:elytra", "minecraft:dragon_egg", "minecraft:totem_of_undying")),
            new Pregen(true, 3, 8, 4, 10_000),
            // Chunky pregenerates chunks itself (T33); two pregeneration jobs at once only fight over the server thread
            AutoOff.clean(Map.of("pregen", List.of("chunky"))));

    /** The settings with every feature off whose partner mod is loaded (logged); unknown keys are ignored. */
    public PlayConfig withAutoOff(java.util.function.Predicate<String> loaded, org.slf4j.Logger log) {
        var hits = AutoOff.match(autoOffWith, loaded);
        var r = this;
        if (hits.containsKey("pregen")) r = new PlayConfig(coords, deathHint, mentions, playtime, mobGriefing, pvp, welcome, loginReward, events, leaderboard, vote, afk, border,
                netherRoof, keepInventory, dragonEgg, phantoms, entityLimit, cleanup, new Pregen(false, pregen.permission, pregen.parallel, pregen.perTick, pregen.maxRadius), autoOffWith);
        AutoOff.log(log, "config/simpletweaks-play.json", hits, Set.of());
        return r;
    }

    public PlayConfig {
        var d = DEFAULT;
        if (d != null) {
            if (coords == null) coords = d.coords; if (deathHint == null) deathHint = d.deathHint; if (mentions == null) mentions = d.mentions;
            if (playtime == null) playtime = d.playtime; if (mobGriefing == null) mobGriefing = d.mobGriefing; if (pvp == null) pvp = d.pvp;
            if (welcome == null) welcome = d.welcome; if (loginReward == null) loginReward = d.loginReward; if (events == null) events = d.events;
            if (leaderboard == null) leaderboard = d.leaderboard; if (vote == null) vote = d.vote; if (afk == null) afk = d.afk;
            if (border == null) border = d.border; if (netherRoof == null) netherRoof = d.netherRoof; if (keepInventory == null) keepInventory = d.keepInventory;
            if (dragonEgg == null) dragonEgg = d.dragonEgg; if (phantoms == null) phantoms = d.phantoms; if (entityLimit == null) entityLimit = d.entityLimit;
            if (cleanup == null) cleanup = d.cleanup; if (pregen == null) pregen = d.pregen;
            if (autoOffWith == null) autoOffWith = d.autoOffWith;
            autoOffWith = AutoOff.clean(autoOffWith);
        }
    }

    static int level(int value) { return Math.clamp(value, 0, 4); }
    static String text(String value) { return value == null ? "" : value.length() > 256 ? value.substring(0, 256) : value; }
    static List<String> ids(List<String> values) {
        return values == null ? List.of() : values.stream().filter(s -> s != null && net.minecraft.resources.Identifier.tryParse(s) != null).distinct().limit(256).toList();
    }
    static List<String> stacks(List<String> values) {
        return values == null ? List.of() : values.stream().filter(s -> s != null && LoginRewardStacks.valid(s)).limit(16).toList();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final Path PATH = Path.of("config/simpletweaks-play.json");

    /** Writes the defaults when the file is missing; fills missing keys from the defaults without rewriting the file. */
    public static PlayConfig load(Path path) {
        var log = org.slf4j.LoggerFactory.getLogger("simpletweaks-play");
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.toAbsolutePath().getParent());
                Files.writeString(path, GSON.toJson(DEFAULT));
                return DEFAULT;
            }
            if (Files.isSymbolicLink(path) || Files.size(path) > 65536) throw new java.io.IOException("Unsafe play config");
            var merged = GSON.toJsonTree(DEFAULT).getAsJsonObject();
            merge(merged, JsonParser.parseString(Files.readString(path)).getAsJsonObject());
            return GSON.fromJson(merged, PlayConfig.class);
        } catch (Exception e) {
            log.error("Invalid {}; the file is kept and the defaults are used until it is fixed", path, e);
            return DEFAULT;
        }
    }
    /** These defaults with the given JSON merged over them (tests: {@code of("{\"pvp\":{\"cooldownSeconds\":0}}")}). */
    public static PlayConfig of(String override) {
        var merged = GSON.toJsonTree(DEFAULT).getAsJsonObject();
        merge(merged, JsonParser.parseString(override).getAsJsonObject());
        return GSON.fromJson(merged, PlayConfig.class);
    }
    private static void merge(JsonObject into, JsonObject from) {
        for (var e : from.entrySet()) {
            var old = into.get(e.getKey());
            // maps (border.dimensions) are taken as a whole; known sections merge key by key
            if (old != null && old.isJsonObject() && e.getValue().isJsonObject() && !e.getKey().equals("dimensions")) merge(old.getAsJsonObject(), e.getValue().getAsJsonObject());
            else if (old != null) into.add(e.getKey(), e.getValue());
        }
    }
    public String json() { return GSON.toJson(this); }
}
