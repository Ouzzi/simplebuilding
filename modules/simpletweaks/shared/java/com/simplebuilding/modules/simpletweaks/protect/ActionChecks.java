package com.simplebuilding.modules.simpletweaks.protect;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;

/**
 * Reach / fast-break sanity logging for admins (default off, never punishes). Only what the client asks for by packet is
 * measured: block break actions, using an item on a block, attacking or using an entity. The limit is the player's own
 * server-side value – {@code isWithinBlockInteractionRange}/{@code isWithinEntityInteractionRange} (attributes: Range
 * enchantment, trims, any mod's modifier) plus Vanilla's tolerance and {@code reachSlack}; break speed is Vanilla's own
 * {@code getDestroyProgress} (tool, enchantments, effects, attributes, our mining mixins, loader break-speed events).
 * Area breaks of hammers/vein/strip miners, wands, blueprints, chisels and saws run server-side without these packets and
 * are therefore never counted.
 */
public final class ActionChecks {
    public enum Kind { REACH_BLOCK, REACH_ENTITY, FAST_BREAK }
    public record Hit(long time, UUID player, String name, Kind kind, String detail) {}

    private static final Map<UUID, EnumMap<Kind, Integer>> COUNTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NOTIFIED = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LOGGED = new ConcurrentHashMap<>();
    private static final ArrayDeque<Hit> RECENT = new ArrayDeque<>();
    /** Tests read every hit. */
    public static volatile java.util.function.Consumer<Hit> listener = h -> { };

    static void reset() { COUNTS.clear(); NOTIFIED.clear(); LOGGED.clear(); synchronized (RECENT) { RECENT.clear(); } }

    static ProtectConfig.ActionChecks cfg(ServerPlayer p) {
        if (p == null || !Protect.on()) return null;
        var c = Protect.config().actionChecks();
        return c.enabled() && p.gameMode.getGameModeForPlayer() != GameType.SPECTATOR ? c : null;
    }

    /** A block break action or use-on-block packet at {@code pos}. */
    public static void block(ServerPlayer p, BlockPos pos, String what) {
        var c = cfg(p);
        if (c == null || p.isWithinBlockInteractionRange(pos, 1.0 + c.reachSlack())) return;
        double d = Math.sqrt(p.getEyePosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)));
        hit(p, Kind.REACH_BLOCK, String.format(Locale.ROOT, "%s %d %d %d distance %.1f limit %.1f", what, pos.getX(), pos.getY(), pos.getZ(), d, p.blockInteractionRange()));
    }
    /** An attack or interaction packet for {@code target}. */
    public static void entity(ServerPlayer p, Entity target, String what) {
        var c = cfg(p);
        if (c == null || target == null || p.isWithinEntityInteractionRange(target.getBoundingBox(), 3.0 + c.reachSlack())) return;
        double d = Math.sqrt(target.getBoundingBox().distanceToSqr(p.getEyePosition()));
        hit(p, Kind.REACH_ENTITY, String.format(Locale.ROOT, "%s %s distance %.1f limit %.1f", what, net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()), d, p.entityInteractionRange()));
    }
    /** STOP_DESTROY_BLOCK after {@code ticks} ticks: {@code progressPerTick * (ticks + 1)} is Vanilla's own completion. */
    public static void stop(ServerPlayer p, BlockPos pos, float progressPerTick, int ticks) {
        var c = cfg(p);
        if (c == null || progressPerTick <= 0f) return;
        double done = progressPerTick * (ticks + 1.0);
        if (done >= c.fastBreakRatio()) return;
        hit(p, Kind.FAST_BREAK, String.format(Locale.ROOT, "%s at %d %d %d after %d ticks, %.0f%% of the real break time",
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(p.level().getBlockState(pos).getBlock()), pos.getX(), pos.getY(), pos.getZ(), ticks, done * 100));
    }

    static void hit(ServerPlayer p, Kind kind, String detail) {
        var c = Protect.config().actionChecks();
        var h = new Hit(System.currentTimeMillis(), p.getUUID(), p.getGameProfile().name(), kind, detail);
        COUNTS.computeIfAbsent(p.getUUID(), k -> new EnumMap<>(Kind.class)).merge(kind, 1, Integer::sum);
        synchronized (RECENT) { RECENT.addLast(h); while (RECENT.size() > 200) RECENT.removeFirst(); }
        listener.accept(h);
        var server = p.level().getServer();
        long now = System.currentTimeMillis(), last = NOTIFIED.getOrDefault(p.getUUID(), 0L);
        // at most one file line per player and second, so a spamming client cannot flood the disk
        if (c.log() && server != null && now - LOGGED.getOrDefault(p.getUUID(), 0L) >= 1000L) { LOGGED.put(p.getUUID(), now); write(server, h); }
        if (server != null && now - last >= c.notifyCooldownSeconds() * 1000L) {
            NOTIFIED.put(p.getUUID(), now);
            var msg = Protect.t("checks.notify", h.name(), Protect.t("checks.kind." + kind.name().toLowerCase(Locale.ROOT)), detail);
            for (var admin : server.getPlayerList().getPlayers()) if (admin != p && ProtectPerms.has(admin, c.notifyPermission())) admin.sendSystemMessage(msg);
            org.slf4j.LoggerFactory.getLogger("simpletweaks-protect").info("Check {} {}: {}", kind, h.name(), detail);
        }
    }
    private static void write(net.minecraft.server.MinecraftServer server, Hit h) {
        try {
            var dir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("simpletweaks");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("protect-checks.log"), java.time.Instant.ofEpochMilli(h.time()) + "\t" + h.player() + "\t" + h.name() + "\t" + h.kind() + "\t" + h.detail() + "\n",
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (java.io.IOException e) {
            org.slf4j.LoggerFactory.getLogger("simpletweaks-protect").warn("Could not write protect-checks.log", e);
        }
    }

    public static Map<Kind, Integer> counts(UUID player) { var m = COUNTS.get(player); return m == null ? Map.of() : Map.copyOf(m); }
    public static Map<UUID, EnumMap<Kind, Integer>> all() { return Map.copyOf(COUNTS); }
    public static List<Hit> recent() { synchronized (RECENT) { return List.copyOf(RECENT); } }
    private ActionChecks() {}
}
