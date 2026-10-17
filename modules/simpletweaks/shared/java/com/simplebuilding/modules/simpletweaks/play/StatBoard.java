package com.simplebuilding.modules.simpletweaks.play;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.*;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Reads the Vanilla player statistics of online players (live) and of offline players (their {@code stats/<uuid>.json},
 * read with the Vanilla counter, data fixers included). Used by /playtime (T7) and the leaderboard (T22).
 */
public final class StatBoard {
    public static final List<String> KINDS = List.of("playtime", "deaths", "kills", "mined");
    public record Entry(UUID id, String name, long value) {}

    public static StatsCounter counter(MinecraftServer server, UUID id) {
        var online = server.getPlayerList().getPlayer(id);
        if (online != null) return online.getStats();
        return new ServerStatsCounter(server, dir(server).resolve(id + ".json"));
    }
    static Path dir(MinecraftServer server) { return server.getWorldPath(LevelResource.PLAYER_STATS_DIR); }

    /** The value of one kind: play time in ticks, deaths, kills (mobs + players), blocks mined (all blocks). */
    public static long value(StatsCounter stats, String kind) {
        return switch (kind) {
            case "playtime" -> stats.getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
            case "deaths" -> stats.getValue(Stats.CUSTOM.get(Stats.DEATHS));
            case "kills" -> (long) stats.getValue(Stats.CUSTOM.get(Stats.MOB_KILLS)) + stats.getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS));
            case "mined" -> { long sum = 0; for (var b : BuiltInRegistries.BLOCK) sum += stats.getValue(Stats.BLOCK_MINED.get(b)); yield sum; }
            default -> 0;
        };
    }
    /** Every player that has a statistics file or is online. */
    public static Set<UUID> known(MinecraftServer server) {
        var out = new LinkedHashSet<UUID>();
        for (var p : server.getPlayerList().getPlayers()) out.add(p.getUUID());
        try (var files = Files.list(dir(server))) {
            files.forEach(f -> {
                String n = f.getFileName().toString();
                if (n.endsWith(".json")) { try { out.add(UUID.fromString(n.substring(0, n.length() - 5))); } catch (IllegalArgumentException ignored) {} }
            });
        } catch (java.io.IOException | RuntimeException ignored) {}
        return out;
    }
    public static String name(MinecraftServer server, UUID id) {
        var online = server.getPlayerList().getPlayer(id);
        if (online != null) return online.nameAndId().name();
        var known = Play.state().names.get(id.toString());
        if (known != null) return known;
        return server.services().nameToIdCache().get(id).map(net.minecraft.server.players.NameAndId::name).orElse(id.toString().substring(0, 8));
    }
    /** The top {@code limit} players of one kind, highest first (ties by name). */
    public static List<Entry> top(MinecraftServer server, String kind, int limit) {
        var list = new ArrayList<Entry>();
        for (var id : known(server)) list.add(new Entry(id, name(server, id), value(counter(server, id), kind)));
        list.sort(Comparator.comparingLong(Entry::value).reversed().thenComparing(Entry::name));
        return list.subList(0, Math.min(limit, list.size()));
    }
    private StatBoard() {}
}
