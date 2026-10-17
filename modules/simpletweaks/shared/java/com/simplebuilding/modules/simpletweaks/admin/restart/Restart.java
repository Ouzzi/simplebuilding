package com.simplebuilding.modules.simpletweaks.admin.restart;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.simplebuilding.modules.simpletweaks.admin.*;
import java.time.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;

/**
 * T16 restart announcements: scheduled times ("HH:mm") and /tweaks restart &lt;minutes&gt;. Chat at the warning
 * minutes and at 30, 10…1 seconds, a boss bar with the time left; at zero everything is saved and the server stops
 * cleanly. The wrapper restarts it; a configured exit code is passed on (dedicated servers only).
 */
public final class Restart {
    /** Seconds before the stop that get a chat line besides the warning minutes. */
    static final int[] SECONDS = {30, 10, 5, 4, 3, 2, 1};
    private static Clock clock = Clock.systemDefaultZone();
    private static long deadline = -1;
    private static final Set<Integer> announced = new HashSet<>();
    private static ServerBossEvent bar;
    /** Tests replace the stop. */
    static java.util.function.Consumer<MinecraftServer> stopAction = Restart::stopServer;

    /** Tests: a fixed clock (game test servers may tick faster than real time). */
    public static void useClock(Clock value) { clock = value; }
    public static boolean active() { return Admin.on() && Admin.config().restart().enabled(); }
    public static long deadline() { return deadline; }

    public static void start(MinecraftServer server) { deadline = -1; announced.clear(); scheduleNext(); }
    public static void stop(MinecraftServer server) { hideBar(); deadline = -1; }

    /** The next configured time after now, in ms; -1 if none. */
    public static long nextScheduled(List<String> times, ZonedDateTime now) {
        long best = -1;
        for (String t : times) {
            var parts = t.split(":");
            var at = now.withHour(Integer.parseInt(parts[0])).withMinute(Integer.parseInt(parts[1])).withSecond(0).withNano(0);
            if (!at.isAfter(now)) at = at.plusDays(1);
            long ms = at.toInstant().toEpochMilli();
            if (best < 0 || ms < best) best = ms;
        }
        return best;
    }
    private static void scheduleNext() {
        if (!active()) return;
        long next = nextScheduled(Admin.config().restart().times(), ZonedDateTime.now(clock));
        if (next > 0) { deadline = next; announced.clear(); }
    }
    public static void schedule(long millisFromNow) { deadline = clock.millis() + millisFromNow; announced.clear(); }
    public static boolean cancel(MinecraftServer server) {
        if (deadline < 0) return false;
        deadline = -1; announced.clear(); hideBar();
        server.getPlayerList().broadcastSystemMessage(Component.translatable("simpletweaks.admin.restart.cancelled").withStyle(ChatFormatting.GREEN), false);
        return true;
    }

    public static void tick(MinecraftServer server) {
        if (deadline < 0 || !active()) return;
        long left = deadline - clock.millis();
        int seconds = (int) Math.ceil(left / 1000.0);
        if (seconds <= 0) {
            deadline = -1; hideBar();
            server.getPlayerList().broadcastSystemMessage(Component.translatable("simpletweaks.admin.restart.now").withStyle(ChatFormatting.RED), false);
            stopAction.accept(server);
            return;
        }
        Integer mark = mark(seconds, Admin.config().restart().warnMinutes());
        if (mark != null && announced.add(mark)) {
            var text = mark >= 60 ? Component.translatable("simpletweaks.admin.restart.minutes", mark / 60) : Component.translatable("simpletweaks.admin.restart.seconds", mark);
            server.getPlayerList().broadcastSystemMessage(text.withStyle(ChatFormatting.GOLD), false);
        }
        int first = Admin.config().restart().warnMinutes().isEmpty() ? 60 : Admin.config().restart().warnMinutes().getFirst() * 60;
        if (seconds <= first && server.getTickCount() % 10 == 0) showBar(server, seconds, first);
    }
    /** The announcement due at {@code seconds} left (a warning minute or one of {@link #SECONDS}), else null. */
    static Integer mark(int seconds, List<Integer> warnMinutes) {
        for (int m : warnMinutes) if (seconds == m * 60) return seconds;
        for (int s : SECONDS) if (seconds == s) return s;
        return null;
    }
    private static void showBar(MinecraftServer server, int seconds, int total) {
        if (bar == null) bar = new ServerBossEvent(java.util.UUID.randomUUID(), Component.empty(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        bar.setName(Component.translatable("simpletweaks.admin.restart.bar", String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)));
        bar.setProgress(Math.clamp(seconds / (float) total, 0f, 1f));
        for (var p : server.getPlayerList().getPlayers()) bar.addPlayer(p);
    }
    private static void hideBar() { if (bar != null) { bar.removeAllPlayers(); bar = null; } }
    /** Tests. */
    public static boolean barShown() { return bar != null; }

    static void stopServer(MinecraftServer server) {
        server.saveEverything(false, true, true);
        int code = Admin.config().restart().exitCode();
        if (code != 0 && server.isDedicatedServer()) {
            Thread serverThread = server.getRunningThread();
            // Not a daemon: the JVM waits for it, and it calls exit only after the server thread ended, so the Vanilla
            // shutdown hook (which waits for the server thread) cannot deadlock with it.
            var exit = new Thread(() -> {
                try { serverThread.join(120_000); } catch (InterruptedException ignored) {}
                Runtime.getRuntime().exit(code);
            }, "simpletweaks-restart-exit");
            exit.setDaemon(false);
            exit.start();
        }
        server.halt(false);
    }

    public static void register(CommandDispatcher<CommandSourceStack> d, AdminConfig config) {
        if (!config.restart().enabled()) return;
        d.register(Commands.literal("tweaks").then(Commands.literal("restart").requires(s -> Admin.on() && AdminPerms.has(s, Admin.config().restart().permission()))
                .executes(c -> {
                    long left = deadline < 0 ? -1 : (deadline - clock.millis()) / 1000;
                    c.getSource().sendSuccess(() -> left < 0 ? Component.translatable("simpletweaks.admin.restart.none")
                            : Component.translatable("simpletweaks.admin.restart.pending", String.format(Locale.ROOT, "%d:%02d", left / 60, left % 60)), false);
                    return 1;
                })
                .then(Commands.literal("cancel").executes(c -> {
                    if (cancel(c.getSource().getServer())) return 1;
                    c.getSource().sendFailure(Component.translatable("simpletweaks.admin.restart.none")); return 0;
                }))
                .then(Commands.argument("minutes", IntegerArgumentType.integer(1, 1440)).executes(c -> {
                    int m = IntegerArgumentType.getInteger(c, "minutes");
                    schedule(m * 60_000L);
                    c.getSource().getServer().getPlayerList().broadcastSystemMessage(Component.translatable("simpletweaks.admin.restart.minutes", m).withStyle(ChatFormatting.GOLD), false);
                    announced.add(m * 60);
                    return 1;
                }))));
    }
    private Restart() {}
}
