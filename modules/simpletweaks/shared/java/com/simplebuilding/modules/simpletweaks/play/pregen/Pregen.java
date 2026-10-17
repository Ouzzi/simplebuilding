package com.simplebuilding.modules.simpletweaks.play.pregen;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.simplebuilding.modules.simpletweaks.play.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.ChunkPos;

/**
 * T33: /pregen start &lt;radius&gt; [dimension] generates all chunks in a square spiral around the world border center
 * (radius in blocks, at most {@code maxRadius}); pause, resume, stop, status. Requests are asynchronous (the main thread
 * never waits): at most {@code parallel} open requests and {@code perTick} new ones per tick. Progress is a boss bar for
 * operators and a chat line every 10 %. Each chunk is loaded with the Vanilla asynchronous ticket load
 * ({@code addTicketAndLoadWithRadius}, short-lived spawn ticket, so finished chunks unload again); the job is saved in the world state and continues after a restart (paused).
 */
public final class Pregen {
    static final AtomicInteger OPEN = new AtomicInteger();
    static final AtomicLong DONE = new AtomicLong();
    static final Deque<long[]> RETRY = new ArrayDeque<>();
    static ServerBossEvent bar;
    static int lastTenth = -1;

    public static boolean active() { return Play.on() && Play.server() != null && Play.config().pregen().enabled(); }
    public static PlayState.PregenJob job() { return Play.state().pregen; }

    /** Chunk offset of spiral index {@code i} (0 = center, then ring by ring). */
    public static int[] spiral(long i) {
        if (i == 0) return new int[]{0, 0};
        long k = (long) Math.ceil((Math.sqrt(i + 1) - 1) / 2);
        long t = 2 * k + 1, m = t * t;
        t--;
        if (i >= m - t) return new int[]{(int) (k - (m - i)), (int) -k};
        m -= t;
        if (i >= m - t) return new int[]{(int) -k, (int) (-k + (m - i))};
        m -= t;
        if (i >= m - t) return new int[]{(int) (-k + (m - i)), (int) k};
        return new int[]{(int) k, (int) (k - (m - i - t))};
    }
    public static long total(int radiusBlocks) { long r = (radiusBlocks + 15) / 16; return (2 * r + 1) * (2 * r + 1); }

    public static void register(CommandDispatcher<CommandSourceStack> d, PlayConfig c) {
        if (!c.pregen().enabled()) return;
        int perm = c.pregen().permission();
        d.register(Commands.literal("pregen").requires(s -> PlayPerms.has(s, perm))
                .then(Commands.literal("start").then(Commands.argument("radius", IntegerArgumentType.integer(16, c.pregen().maxRadius()))
                        .executes(x -> start(x.getSource(), x.getSource().getLevel(), IntegerArgumentType.getInteger(x, "radius")))
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                .executes(x -> start(x.getSource(), DimensionArgument.getDimension(x, "dimension"), IntegerArgumentType.getInteger(x, "radius"))))))
                .then(Commands.literal("pause").executes(x -> pause(x.getSource(), true)))
                .then(Commands.literal("resume").executes(x -> pause(x.getSource(), false)))
                .then(Commands.literal("stop").executes(x -> { boolean had = job() != null; stop(x.getSource().getServer(), true); x.getSource().sendSuccess(() -> PlayText.t(had ? "pregen.stopped" : "pregen.none"), true); return had ? 1 : 0; }))
                .then(Commands.literal("status").executes(x -> { x.getSource().sendSuccess(() -> status(), false); return job() == null ? 0 : 1; })));
    }
    static net.minecraft.network.chat.MutableComponent status() {
        var j = job();
        if (j == null) return PlayText.t("pregen.none");
        return PlayText.t(j.paused ? "pregen.status_paused" : "pregen.status", j.done, j.total, j.total == 0 ? 100 : j.done * 100 / j.total, j.dimension);
    }

    static int start(CommandSourceStack source, ServerLevel level, int radius) {
        if (!active()) return 0;
        if (job() != null) { source.sendFailure(PlayText.t("pregen.busy")); return 0; }
        start(level, radius);
        source.sendSuccess(() -> PlayText.t("pregen.started", total(radius), radius, level.dimension().identifier().toString()), true);
        return 1;
    }
    /** Starts a job around the border center of {@code level}. */
    public static PlayState.PregenJob start(ServerLevel level, int radius) {
        var j = new PlayState.PregenJob();
        j.dimension = level.dimension().identifier().toString();
        var b = level.getWorldBorder();
        j.centerX = net.minecraft.util.Mth.floor(b.getCenterX()) >> 4;
        j.centerZ = net.minecraft.util.Mth.floor(b.getCenterZ()) >> 4;
        j.radius = radius;
        j.total = total(radius);
        Play.state().pregen = j;
        DONE.set(0); OPEN.set(0); RETRY.clear(); lastTenth = -1;
        Play.state().save();
        return j;
    }
    static int pause(CommandSourceStack source, boolean value) {
        var j = job();
        if (j == null) { source.sendFailure(PlayText.t("pregen.none")); return 0; }
        j.paused = value;
        Play.state().save();
        source.sendSuccess(() -> PlayText.t(value ? "pregen.paused" : "pregen.resumed"), true);
        return 1;
    }
    public static void setPaused(boolean value) { var j = job(); if (j != null) { j.paused = value; Play.state().save(); } }

    /** Server start: a saved job continues paused (an operator resumes it). */
    public static void start(MinecraftServer server) {
        OPEN.set(0); RETRY.clear(); lastTenth = -1;
        var j = job();
        if (j != null) { j.paused = true; DONE.set(j.done); }
    }
    public static void stop(MinecraftServer server) { hideBar(); }
    public static void stop(MinecraftServer server, boolean cancel) {
        if (cancel) { Play.state().pregen = null; Play.state().save(); }
        hideBar();
    }
    static void hideBar() { if (bar != null) { bar.removeAllPlayers(); bar = null; } }

    public static void tick(MinecraftServer server) {
        var j = job();
        if (j == null || !active()) { hideBar(); return; }
        j.done = DONE.get();
        if (j.done >= j.total && OPEN.get() == 0) { finish(server, j); return; }
        if (j.paused) { hideBar(); return; }
        var level = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(j.dimension)));
        if (level == null) { stop(server, true); return; }
        var c = Play.config().pregen();
        var cache = level.getChunkSource();
        for (int n = 0; n < c.perTick() && OPEN.get() < c.parallel(); n++) {
            int x, z;
            long[] r; synchronized (RETRY) { r = RETRY.poll(); }
            if (r != null) { x = (int) r[0]; z = (int) r[1]; }
            else if (j.next < j.total) { var o = spiral(j.next++); x = j.centerX + o[0]; z = j.centerZ + o[1]; }
            else break;
            OPEN.incrementAndGet();
            final int fx = x, fz = z;
            java.util.concurrent.CompletableFuture<?> future;
            try { future = cache.addTicketAndLoadWithRadius(TicketType.PLAYER_SPAWN, new ChunkPos(x, z), 0); }
            catch (RuntimeException e) {
                OPEN.decrementAndGet();
                org.slf4j.LoggerFactory.getLogger("simpletweaks-play").warn("Pregen request for chunk {} {} failed, retried later", x, z, e);
                synchronized (RETRY) { if (RETRY.size() < 4096) RETRY.add(new long[]{x, z}); }
                break;
            }
            future.whenComplete((result, error) -> {
                OPEN.decrementAndGet();
                if (error == null) DONE.incrementAndGet();
                else { org.slf4j.LoggerFactory.getLogger("simpletweaks-play").warn("Pregen chunk {} {} failed, retried later", fx, fz, error); }
                if (error != null) synchronized (RETRY) { if (RETRY.size() < 4096) RETRY.add(new long[]{fx, fz}); }
            });
        }
        progress(server, j);
        if (server.getTickCount() % 200 == 0) Play.state().save();
    }
    static void progress(MinecraftServer server, PlayState.PregenJob j) {
        float f = j.total == 0 ? 1f : Math.min(1f, j.done / (float) j.total);
        if (bar == null) bar = new ServerBossEvent(UUID.randomUUID(), PlayText.t("pregen.bar"), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
        bar.setProgress(f);
        for (var p : server.getPlayerList().getPlayers()) {
            boolean op = PlayPerms.has(p, Play.config().pregen().permission());
            if (op && !bar.getPlayers().contains(p)) bar.addPlayer(p); else if (!op && bar.getPlayers().contains(p)) bar.removePlayer(p);
        }
        int tenth = (int) (f * 10);
        if (tenth != lastTenth) { lastTenth = tenth; if (tenth > 0 && tenth < 10) tellOps(server, status()); }
    }
    static void finish(MinecraftServer server, PlayState.PregenJob j) {
        tellOps(server, PlayText.t("pregen.done", j.done, j.dimension).withStyle(ChatFormatting.GREEN));
        Play.state().pregen = null;
        Play.state().save();
        hideBar();
    }
    static void tellOps(MinecraftServer server, net.minecraft.network.chat.Component msg) {
        for (var p : server.getPlayerList().getPlayers()) if (PlayPerms.has(p, Play.config().pregen().permission())) p.sendSystemMessage(msg);
        org.slf4j.LoggerFactory.getLogger("simpletweaks-play").info(msg.getString());
    }
    private Pregen() {}
}
