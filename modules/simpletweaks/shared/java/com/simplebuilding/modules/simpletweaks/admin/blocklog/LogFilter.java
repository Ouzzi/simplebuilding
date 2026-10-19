package com.simplebuilding.modules.simpletweaks.admin.blocklog;

import com.simplebuilding.modules.simpletweaks.admin.AdminPlayers;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

/**
 * Filter of /tweaks lookup and /tweaks rollback: {@code player:} (name, UUID or {@code #actor}, comma list), {@code action:},
 * {@code item:}, {@code block:}, {@code dim:}, {@code time:} (30m, 2h, 1d), {@code radius:} (around the source, its dimension).
 * {@code item:} matches container entries (T D I O), {@code block:} block entries (B P S X F).
 */
public record LogFilter(Set<UUID> players, Set<String> actors, Set<Character> actions, Set<String> items, Set<String> blocks,
        Set<String> dims, long since, BlockPos center, int radius) {
    /** Action names; the letters work too. */
    public static final Map<String, Character> ACTIONS = new LinkedHashMap<>();
    static {
        ACTIONS.put("break", 'B'); ACTIONS.put("place", 'P'); ACTIONS.put("take", 'T'); ACTIONS.put("put", 'D'); ACTIONS.put("sign", 'S');
        ACTIONS.put("explode", 'X'); ACTIONS.put("burn", 'F'); ACTIONS.put("hopperin", 'I'); ACTIONS.put("hopperout", 'O');
    }
    public static final List<String> KEYS = List.of("player:", "action:", "item:", "block:", "dim:", "time:", "radius:");
    static final String ITEM_ACTIONS = "TDIO", BLOCK_ACTIONS = "BPSXF";

    /** Parse result: a filter or the translation key + argument of the error. */
    public record Parsed(LogFilter filter, boolean hasTime, boolean preview, String error, String errorArg) {}

    public static LogFilter of(UUID player, long since, String dim, BlockPos center, int radius) {
        return new LogFilter(player == null ? Set.of() : Set.of(player), Set.of(), Set.of(), Set.of(), Set.of(), dim == null ? null : Set.of(dim), since, center, radius);
    }

    /**
     * Parses {@code key:value} words (and {@code --preview}). Without any {@code key:} word the old form
     * {@code <player> <time> [radius]} is read. {@code here}/{@code hereDim}: the source's position and dimension for radius.
     */
    public static Parsed parse(MinecraftServer server, String text, BlockPos here, String hereDim, long now) {
        var words = new ArrayList<>(Arrays.asList(text.trim().split("\\s+")));
        words.removeIf(String::isEmpty);
        boolean preview = words.removeIf(w -> w.equalsIgnoreCase("--preview") || w.equalsIgnoreCase("-p"));
        if (!words.isEmpty() && words.stream().noneMatch(w -> w.contains(":"))) {
            // legacy: <player> <time> [radius]
            if (words.size() < 2 || words.size() > 3) return err("simpletweaks.admin.blocklog.filter.bad", text);
            var legacy = new ArrayList<String>();
            legacy.add("player:" + words.get(0)); legacy.add("time:" + words.get(1));
            if (words.size() == 3) legacy.add("radius:" + words.get(2));
            words = legacy;
        }
        var players = new HashSet<UUID>(); var actors = new HashSet<String>(); var actions = new HashSet<Character>();
        var items = new HashSet<String>(); var blocks = new HashSet<String>(); Set<String> dims = null;
        long since = 0; boolean hasTime = false; int radius = -1;
        for (String w : words) {
            int c = w.indexOf(':');
            if (c <= 0) return err("simpletweaks.admin.blocklog.filter.bad", w);
            String key = w.substring(0, c).toLowerCase(Locale.ROOT), value = w.substring(c + 1);
            if (value.isEmpty()) return err("simpletweaks.admin.blocklog.filter.bad", w);
            switch (key) {
                case "player", "p", "user", "u" -> {
                    for (String v : value.split(",")) {
                        if (v.startsWith("#")) { actors.add(v.toLowerCase(Locale.ROOT)); continue; }
                        var profile = AdminPlayers.resolve(server, v);
                        if (profile == null) return err("simpletweaks.admin.unknown_player", v);
                        players.add(profile.id());
                    }
                }
                case "action", "a" -> {
                    for (String v : value.split(",")) {
                        Character a = ACTIONS.get(v.toLowerCase(Locale.ROOT));
                        if (a == null && v.length() == 1 && (ITEM_ACTIONS + BLOCK_ACTIONS).indexOf(Character.toUpperCase(v.charAt(0))) >= 0) a = Character.toUpperCase(v.charAt(0));
                        if (a == null) return err("simpletweaks.admin.blocklog.filter.action", v);
                        actions.add(a);
                    }
                }
                case "item", "i" -> { for (String v : value.split(",")) items.add(id(v)); }
                case "block", "b" -> { for (String v : value.split(",")) blocks.add(id(v)); }
                case "dim", "dimension", "d" -> { dims = new HashSet<>(); for (String v : value.split(",")) dims.add(dim(v)); }
                case "time", "t" -> {
                    long span = Inspect.parseDuration(value);
                    if (span <= 0) return err("simpletweaks.admin.blocklog.badtime", value);
                    since = now - span; hasTime = true;
                }
                case "radius", "r" -> {
                    try { radius = Integer.parseInt(value); } catch (NumberFormatException e) { radius = -1; }
                    if (radius < 1 || radius > 4096) return err("simpletweaks.admin.blocklog.filter.radius", value);
                }
                default -> { return err("simpletweaks.admin.blocklog.filter.bad", w); }
            }
        }
        BlockPos center = null;
        if (radius > 0) {
            if (here == null || hereDim == null) return err("simpletweaks.admin.blocklog.filter.radius", String.valueOf(radius));
            center = here.immutable();
            if (dims == null) dims = Set.of(hereDim);
        }
        return new Parsed(new LogFilter(Set.copyOf(players), Set.copyOf(actors), Set.copyOf(actions), Set.copyOf(items), Set.copyOf(blocks),
                dims == null ? null : Set.copyOf(dims), since, center, radius), hasTime, preview, null, null);
    }
    private static Parsed err(String key, String arg) { return new Parsed(null, false, false, key, arg); }
    static String id(String v) { v = v.toLowerCase(Locale.ROOT); return v.contains(":") ? v : "minecraft:" + v; }
    static String dim(String v) {
        v = v.toLowerCase(Locale.ROOT);
        return switch (v) { case "nether" -> "minecraft:the_nether"; case "end" -> "minecraft:the_end"; default -> id(v); };
    }

    /** True when only a player or actor and a radius narrow the search (rollback needs one of them). */
    public boolean narrowed() { return !players.isEmpty() || !actors.isEmpty() || center != null; }

    public boolean test(BlockLog.Entry e) {
        if (e.time() < since) return false;
        if (!players.isEmpty() || !actors.isEmpty()) {
            boolean who = players.contains(e.player()) || !actors.isEmpty() && e.name().startsWith("#") && actors.contains(e.name().toLowerCase(Locale.ROOT));
            if (!who) return false;
        }
        if (!actions.isEmpty() && !actions.contains(e.action())) return false;
        if (!items.isEmpty() || !blocks.isEmpty()) {
            boolean itemAction = ITEM_ACTIONS.indexOf(e.action()) >= 0;
            if (!(itemAction ? items.contains(e.id()) : blocks.contains(e.id()))) return false;
        }
        if (center != null && (Math.abs(e.x() - center.getX()) > radius || Math.abs(e.z() - center.getZ()) > radius)) return false;
        return true;
    }
}
