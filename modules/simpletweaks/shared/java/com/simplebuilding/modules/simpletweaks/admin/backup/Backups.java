package com.simplebuilding.modules.simpletweaks.admin.backup;

import com.mojang.brigadier.CommandDispatcher;
import com.simplebuilding.modules.simpletweaks.admin.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

/**
 * T17 world backups: every {@code intervalHours} and with /backup now. Like save-off / save-all flush / save-on:
 * saving is paused on every level, everything is flushed, the world folder is zipped on a worker thread (without
 * {@code session.lock} and without the target folder), then saving resumes. The newest {@code keep} archives stay.
 */
public final class Backups {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("simpletweaks-backup");
    private static ExecutorService worker;
    private static volatile boolean running;
    private static long last;

    public static boolean active() { return Admin.on() && Admin.config().backups().enabled(); }
    public static boolean running() { return running; }
    public static Path directory(MinecraftServer server) { return server.getServerDirectory().resolve(Admin.config().backups().directory()).normalize(); }
    private static Path marker(MinecraftServer server) { return directory(server).resolve(".simpletweaks-last"); }

    public static void start(MinecraftServer server) {
        running = false;
        if (!Admin.config().backups().enabled()) return;
        worker = Executors.newSingleThreadExecutor(r -> { var t = new Thread(r, "simpletweaks-backup"); t.setDaemon(true); return t; });
        try { last = Long.parseLong(Files.readString(marker(server)).strip()); }
        catch (Exception e) { last = System.currentTimeMillis(); } // first start: the first backup comes one interval later
    }
    public static void stop() {
        if (worker != null) worker.shutdown();
        worker = null;
    }
    public static void tick(MinecraftServer server) {
        if (worker == null || running || !active() || server.getTickCount() % 1200 != 0) return;
        if (System.currentTimeMillis() - last >= Admin.config().backups().intervalHours() * 3_600_000L) begin(server, null);
    }

    /** Starts a backup; {@code done} gets the archive (or null on failure) on the server thread. False if one is running. */
    public static boolean begin(MinecraftServer server, java.util.function.Consumer<Path> done) {
        if (running || worker == null) return false;
        running = true;
        var paused = new ArrayList<ServerLevel>();
        for (var level : server.getAllLevels()) if (!level.noSave) { level.noSave = true; paused.add(level); }
        server.saveEverything(true, true, true);
        Path world = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        Path dir = directory(server).toAbsolutePath().normalize();
        String name = world.getFileName() + "-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()) + ".zip";
        int keep = Admin.config().backups().keep();
        broadcast(server, Component.translatable("simpletweaks.admin.backup.started").withStyle(ChatFormatting.GRAY));
        worker.submit(() -> {
            Path archive = null;
            try { archive = zip(world, dir, name); prune(dir, world.getFileName() + "-", keep); }
            catch (Exception e) { LOG.error("World backup failed", e); }
            final Path result = archive;
            server.execute(() -> {
                for (var level : paused) level.noSave = false;
                running = false;
                last = System.currentTimeMillis();
                try { Files.createDirectories(dir); Files.writeString(marker(server), Long.toString(last)); } catch (IOException ignored) {}
                broadcast(server, result == null ? Component.translatable("simpletweaks.admin.backup.failed").withStyle(ChatFormatting.RED)
                        : Component.translatable("simpletweaks.admin.backup.done", result.getFileName().toString(), size(result)).withStyle(ChatFormatting.GREEN));
                if (done != null) done.accept(result);
            });
        });
        return true;
    }
    private static String size(Path p) {
        try { return String.format(Locale.ROOT, "%.1f MB", Files.size(p) / 1048576.0); } catch (IOException e) { return "?"; }
    }
    /** Server log + every player allowed to make backups. */
    private static void broadcast(MinecraftServer server, Component text) {
        LOG.info(text.getString());
        for (var p : server.getPlayerList().getPlayers()) if (AdminPerms.has(p, Admin.config().backups().permission())) p.sendSystemMessage(text);
    }

    static Path zip(Path world, Path dir, String name) throws IOException {
        Files.createDirectories(dir);
        Path part = dir.resolve(name + ".part"), out = dir.resolve(name);
        try (var zip = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(part)));
             var walk = Files.walk(world)) {
            for (var file : (Iterable<Path>) walk::iterator) {
                if (!Files.isRegularFile(file) || file.startsWith(dir) || file.getFileName().toString().equals("session.lock")) continue;
                String entry = world.getFileName() + "/" + world.relativize(file).toString().replace('\\', '/');
                try (var in = Files.newInputStream(file)) {
                    zip.putNextEntry(new ZipEntry(entry));
                    in.transferTo(zip);
                    zip.closeEntry();
                } catch (IOException e) { LOG.warn("Skipped {} in the backup: {}", file, e.toString()); }
            }
        }
        Files.move(part, out, StandardCopyOption.REPLACE_EXISTING);
        return out;
    }
    static void prune(Path dir, String prefix, int keep) throws IOException {
        List<Path> archives;
        try (var list = Files.list(dir)) {
            archives = list.filter(p -> { var n = p.getFileName().toString(); return n.startsWith(prefix) && n.endsWith(".zip"); })
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed()).toList();
        }
        for (int i = keep; i < archives.size(); i++) Files.deleteIfExists(archives.get(i));
    }

    public static void register(CommandDispatcher<CommandSourceStack> d, AdminConfig config) {
        if (!config.backups().enabled()) return;
        d.register(Commands.literal("backup").requires(s -> Admin.on() && AdminPerms.has(s, Admin.config().backups().permission()))
                .then(Commands.literal("now").executes(c -> {
                    if (begin(c.getSource().getServer(), null)) return 1;
                    c.getSource().sendFailure(Component.translatable("simpletweaks.admin.backup.busy"));
                    return 0;
                })));
    }
    private Backups() {}
}
