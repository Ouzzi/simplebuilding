package com.simplebuilding.modules.simpletweaks.play.events;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.simplebuilding.modules.simpletweaks.play.*;
import java.time.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * T21 server events: double XP (every experience orb x {@code xpMultiplier}), more loot (mobs killed by players roll
 * their loot table {@code extraLootRolls} more times) and blood moon (at night in the Overworld every
 * {@code bloodMoonWaveSeconds} up to {@code bloodMoonMobs} monsters near each survival player; it ends at daybreak).
 * /tweaks event start|stop|status (operators) is on; the automatic schedule ({@code scheduleEnabled}) is off.
 * One event at a time; announced in chat, shown as a boss bar.
 */
public final class ServerEvents {
    public static final List<String> EVENTS = ServerEventsSchedule.EVENTS;
    static String current;
    static long until = -1, length = 1;
    static boolean nightSeen;
    static ServerBossEvent bar;
    static int lastMinute = -1;
    static Clock clock = Clock.systemDefaultZone();
    static final ThreadLocal<Boolean> EXTRA = ThreadLocal.withInitial(() -> false);

    public static void useClock(Clock value) { clock = value; }
    public static boolean enabled() { return Play.on() && Play.server() != null && Play.config().events().enabled(); }
    public static String current() { return enabled() && current != null ? current : null; }
    public static boolean is(String event) { return event.equals(current()); }

    public static void register(CommandDispatcher<CommandSourceStack> d, PlayConfig c) {
        if (!c.events().enabled()) return;
        int perm = c.events().permission();
        var start = Commands.literal("start");
        for (var e : EVENTS) start.then(Commands.literal(e)
                .executes(x -> start(x.getSource().getServer(), e, Play.config().events().defaultMinutes()) ? 1 : 0)
                .then(Commands.argument("minutes", IntegerArgumentType.integer(1, 1440))
                        .executes(x -> start(x.getSource().getServer(), e, IntegerArgumentType.getInteger(x, "minutes")) ? 1 : 0)));
        d.register(Commands.literal("tweaks").then(Commands.literal("event").requires(s -> PlayPerms.has(s, perm))
                .then(start)
                .then(Commands.literal("stop").executes(x -> stop(x.getSource().getServer()) ? 1 : 0))
                .then(Commands.literal("status").executes(x -> {
                    var cur = current();
                    x.getSource().sendSuccess(() -> cur == null ? PlayText.t("event.none") : PlayText.t("event.running", name(cur), PlayText.duration(until - clock.millis())), false);
                    return cur == null ? 0 : 1;
                }))));
    }
    public static Component name(String event) { return PlayText.t("event." + event); }

    public static boolean start(MinecraftServer server, String event, int minutes) {
        if (!enabled() || !EVENTS.contains(event)) return false;
        if (current != null) stopQuietly();
        current = event;
        length = minutes * 60_000L;
        until = clock.millis() + length;
        nightSeen = false;
        bar = new ServerBossEvent(UUID.randomUUID(), name(event), event.equals("blood_moon") ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
        for (var p : server.getPlayerList().getPlayers()) bar.addPlayer(p);
        server.getPlayerList().broadcastSystemMessage(PlayText.t("event.started", name(event), minutes).withStyle(ChatFormatting.GOLD), false);
        return true;
    }
    public static boolean stop(MinecraftServer server) {
        if (current == null) return false;
        var was = current;
        stopQuietly();
        server.getPlayerList().broadcastSystemMessage(PlayText.t("event.ended", name(was)).withStyle(ChatFormatting.GOLD), false);
        return true;
    }
    static void stopQuietly() {
        current = null; until = -1;
        if (bar != null) { bar.removeAllPlayers(); bar = null; }
    }
    /** Server stop: nothing carries over. */
    public static void shutdown() { stopQuietly(); lastMinute = -1; }

    /** Experience of an orb while double XP runs. */
    public static int xp(int amount) {
        return amount > 0 && is("double_xp") ? Math.round(amount * Play.config().events().xpMultiplier()) : amount;
    }
    /** Extra loot table rolls for a mob killed by a player (0 when no "more loot" event runs). */
    public static int extraLootRolls(LivingEntity victim, boolean playerKilled) {
        if (EXTRA.get() || !playerKilled || victim instanceof net.minecraft.world.entity.player.Player || !is("more_loot")) return 0;
        return Play.config().events().extraLootRolls();
    }
    /** Runs {@code roll} once per extra roll without counting the extra rolls again. */
    public static void extra(int rolls, Runnable roll) {
        if (rolls <= 0) return;
        EXTRA.set(true);
        try { for (int i = 0; i < rolls; i++) roll.run(); } finally { EXTRA.set(false); }
    }

    public static void tick(MinecraftServer server) {
        if (!enabled()) { if (current != null) stopQuietly(); return; }
        var c = Play.config().events();
        if (c.scheduleEnabled() && server.getTickCount() % 20 == 0) {
            var now = LocalDateTime.now(clock);
            int minute = now.getHour() * 60 + now.getMinute();
            if (minute != lastMinute) {
                lastMinute = minute;
                for (var line : c.schedule()) {
                    var s = ServerEventsSchedule.parse(line);
                    if (s != null && s.startsAt(now)) { start(server, s.event(), s.minutes()); break; }
                }
            }
        }
        if (current == null) return;
        if (bar != null) {
            for (var p : server.getPlayerList().getPlayers()) if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
            if (server.getTickCount() % 20 == 0) bar.setProgress(Math.clamp((until - clock.millis()) / (float) length, 0f, 1f));
        }
        if (clock.millis() >= until) { stop(server); return; }
        if (current.equals("blood_moon")) bloodMoon(server, c);
    }
    static void bloodMoon(MinecraftServer server, PlayConfig.Events c) {
        ServerLevel level = server.overworld();
        boolean night = level.isDarkOutside();
        if (night) nightSeen = true;
        else if (nightSeen) { stop(server); return; }
        if (!night || server.getTickCount() % (c.bloodMoonWaveSeconds() * 20) != 0) return;
        for (var p : level.players()) if (!p.isSpectator() && !p.isCreative()) wave(level, p, c.bloodMoonMobs());
    }
    /** Spawns up to {@code count} monsters 12–28 blocks around the player on dark ground; the number spawned. */
    public static int wave(ServerLevel level, ServerPlayer p, int count) {
        var random = level.getRandom();
        EntityType<?>[] types = {EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.SPIDER, EntityTypes.CREEPER};
        int spawned = 0;
        for (int tries = 0; tries < count * 6 && spawned < count; tries++) {
            double angle = random.nextDouble() * Math.PI * 2, dist = 12 + random.nextDouble() * 16;
            var column = BlockPos.containing(p.getX() + Math.cos(angle) * dist, p.getY(), p.getZ() + Math.sin(angle) * dist);
            var pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            var type = types[random.nextInt(types.length)];
            if (!NaturalSpawner.isValidEmptySpawnBlock(level, pos, level.getBlockState(pos), level.getFluidState(pos), type)) continue;
            if (level.getBrightness(LightLayer.BLOCK, pos) > 7 || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) continue;
            if (type.spawn(level, pos, EntitySpawnReason.EVENT) != null) spawned++;
        }
        return spawned;
    }
    private ServerEvents() {}
}
