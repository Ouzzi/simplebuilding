package com.simplelib.api;

import com.simplelib.SimpleLib;
import com.simplelib.config.LibConfig;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import net.minecraft.server.MinecraftServer;

/**
 * Hang diagnosis (BS27-1): a daemon thread checks once a second whether the server tick still advances. When it stands for
 * {@code hangWatchdogSeconds} (default 30; a paused singleplayer game does not count, saving on quit does), it writes a dump
 * of every thread of the JVM - server thread, render thread, chunk workers - with full stacks, held locks and deadlocks to
 * {@code logs/simplebuilding-hang-<time>.txt}, and once more after four times the limit. It only reads; the game is never
 * touched. Switch {@code hangWatchdog} in {@code config/simplelib-server.json}.
 */
public final class HangWatchdog {
    public static final String PREFIX = "simplebuilding-hang-";
    /** Dumps per stall: at the limit and at four times the limit. */
    public static final int MAX_DUMPS = 2;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    private static volatile MinecraftServer server;
    private static volatile Thread watcher;
    private static volatile long lastBeat = System.nanoTime();
    private static volatile boolean stopping;
    private static volatile int dumps;
    /** Where dumps go ({@code logs} next to latest.log, the game directory is the working directory). */
    static volatile Path dir = Path.of("logs");

    private HangWatchdog() {}

    /** Server started: watch it (a running watcher of an earlier world ends). */
    public static synchronized void start(MinecraftServer s) {
        stop();
        server = s;
        stopping = false;
        beat();
        Thread t = new Thread(() -> run(s), "SimpleLib hang watchdog");
        t.setDaemon(true);
        t.setPriority(Thread.MAX_PRIORITY);
        watcher = t;
        t.start();
    }

    /** Server thread, end of every tick. */
    public static void beat() {
        long now = System.nanoTime();
        if (dumps > 0) SimpleLib.LOG.warn("Hang watchdog: the server tick runs again after {} s", (now - lastBeat) / 1_000_000_000L);
        dumps = 0;
        lastBeat = now;
    }

    /** Server stopping (save and quit): the watch goes on, a save that never ends is exactly what we want to see. */
    public static void stopping() {
        beat();
        stopping = true;
    }

    public static synchronized void stop() {
        server = null;
        Thread t = watcher;
        watcher = null;
        if (t != null) t.interrupt();
    }

    /** Is a dump due? {@code stalledNanos} since the last tick, {@code done} dumps of this stall so far. */
    public static boolean due(long stalledNanos, int limitSeconds, int done) {
        if (done >= MAX_DUMPS) return false;
        long limit = limitSeconds * 1_000_000_000L * (done == 0 ? 1 : 4);
        return stalledNanos >= limit;
    }

    private static void run(MinecraftServer s) {
        while (server == s && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }
            if (server != s) return;
            long now = System.nanoTime();
            // off, or a paused singleplayer game (the integrated server skips its ticks then): no stall
            if (!LibConfig.hangWatchdog || !stopping && s.isPaused()) {
                lastBeat = now;
                continue;
            }
            long stalled = now - lastBeat;
            if (due(stalled, LibConfig.hangWatchdogSeconds, dumps)) {
                dumps++;
                Path file = write(dir, (stopping ? "Server stopping (save and quit) for " : "Server tick stalled for ") + stalled / 1_000_000_000L
                        + " s (limit " + LibConfig.hangWatchdogSeconds + " s), dump " + dumps + " of " + MAX_DUMPS + ", tick " + s.getTickCount()
                        + ", running " + s.isRunning() + ", paused " + s.isPaused(), s.getRunningThread());
                if (file != null) SimpleLib.LOG.error("Hang watchdog: the server tick stands for {} s - thread dump written to {}. Please send this file.",
                        stalled / 1_000_000_000L, file.toAbsolutePath());
            }
        }
    }

    /** Writes a dump of all threads (the server thread first) to {@code dir}; null when it could not be written. */
    public static Path write(Path dir, String reason, Thread serverThread) {
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(PREFIX + LocalDateTime.now().format(STAMP) + ".txt");
            for (int n = 2; Files.exists(file); n++) file = dir.resolve(PREFIX + LocalDateTime.now().format(STAMP) + "-" + n + ".txt");
            Files.writeString(file, dump(reason, serverThread), StandardCharsets.UTF_8);
            return file;
        } catch (Throwable e) {
            SimpleLib.LOG.warn("Hang watchdog: could not write the thread dump: {}", e.toString());
            return null;
        }
    }

    /** The dump text: reason, deadlocks, then every thread with its full stack and locks. */
    public static String dump(String reason, Thread serverThread) {
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        boolean monitors = mx.isObjectMonitorUsageSupported(), synchronizers = mx.isSynchronizerUsageSupported();
        ThreadInfo[] infos = mx.dumpAllThreads(monitors, synchronizers);
        long serverId = serverThread == null ? -1 : serverThread.threadId();
        Arrays.sort(infos, Comparator.comparing((ThreadInfo i) -> i.getThreadId() != serverId).thenComparing(ThreadInfo::getThreadName));
        StringBuilder out = new StringBuilder();
        out.append("SimpleBuilding hang watchdog (Simple Lib, BS27-1)\n");
        out.append("Time: ").append(LocalDateTime.now()).append('\n');
        out.append("Reason: ").append(reason).append('\n');
        out.append("Java: ").append(System.getProperty("java.version")).append(", ").append(System.getProperty("os.name")).append(", ")
                .append(Runtime.getRuntime().availableProcessors()).append(" cores, heap ").append(Runtime.getRuntime().totalMemory() >> 20)
                .append(" of ").append(Runtime.getRuntime().maxMemory() >> 20).append(" MiB\n");
        long[] dead = mx.findDeadlockedThreads();
        out.append("Deadlocked threads: ").append(dead == null ? "none" : Arrays.toString(dead)).append("\n\n");
        for (ThreadInfo info : infos) {
            if (info == null) continue;
            out.append('"').append(info.getThreadName()).append("\" id=").append(info.getThreadId());
            if (info.isDaemon()) out.append(" daemon");
            out.append(" prio=").append(info.getPriority()).append(' ').append(info.getThreadState());
            if (info.getLockName() != null) out.append(" on ").append(info.getLockName());
            if (info.getLockOwnerName() != null) out.append(" owned by \"").append(info.getLockOwnerName()).append("\" id=").append(info.getLockOwnerId());
            out.append('\n');
            StackTraceElement[] stack = info.getStackTrace();
            MonitorInfo[] locked = info.getLockedMonitors();
            for (int d = 0; d < stack.length; d++) {
                out.append("\tat ").append(stack[d]).append('\n');
                for (MonitorInfo m : locked) if (m.getLockedStackDepth() == d) out.append("\t- locked ").append(m).append('\n');
            }
            LockInfo[] syncs = info.getLockedSynchronizers();
            if (syncs.length > 0) {
                out.append("\tLocked synchronizers:\n");
                for (LockInfo l : syncs) out.append("\t- ").append(l).append('\n');
            }
            out.append('\n');
        }
        return out.toString();
    }
}
