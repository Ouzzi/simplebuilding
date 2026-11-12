package com.simplebuilding.modules.simplelivemap.web;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Self-host is strictly ONE live map per installation (owner N34 addition 1): one web server, one world. The first server that
 * starts its web map holds an operating-system file lock on {@code config/simplelivemap-web.lock}; a second game or server of the
 * same installation (another world, a second process) finds the lock taken and keeps its web map off. Who holds it (world id,
 * port, process) stands in {@code simplelivemap-web.owner.json} next to it, for the status command and the log. The lock ends
 * with the process (also on a crash), so a stale file never blocks.
 */
public final class SelfHostLock implements AutoCloseable {
    public static final String FILE = "simplelivemap-web.lock", OWNER = "simplelivemap-web.owner.json";

    private final FileChannel channel;
    private final FileLock lock;
    private final Path owner;

    private SelfHostLock(FileChannel channel, FileLock lock, Path owner) {
        this.channel = channel;
        this.lock = lock;
        this.owner = owner;
    }

    /** Who holds the lock: world id, port, pid (empty strings / 0 when unknown). */
    public record Holder(String world, int port, long pid) {}

    /**
     * Takes the lock in {@code dir} for {@code world}; null when another live map of this installation holds it (then
     * {@link #holder(Path)} tells which). An unwritable folder counts as "not ours" as well (never two maps by accident).
     */
    public static SelfHostLock acquire(Path dir, String world, int port) {
        FileChannel ch = null;
        try {
            Files.createDirectories(dir);
            ch = FileChannel.open(dir.resolve(FILE), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock l;
            try {
                l = ch.tryLock();
            } catch (OverlappingFileLockException e) {
                l = null; // this JVM already holds it (a second server in the same process)
            }
            if (l == null) {
                ch.close();
                return null;
            }
            SelfHostLock out = new SelfHostLock(ch, l, dir.resolve(OWNER));
            out.port(world, port);
            return out;
        } catch (IOException | RuntimeException e) {
            if (ch != null) {
                try {
                    ch.close();
                } catch (IOException ignored) {
                }
            }
            return null;
        }
    }

    /** Updates the holder note (the port can change on a restart of the web server). */
    public void port(String world, int port) {
        JsonObject o = new JsonObject();
        o.addProperty("world", world == null ? "" : world);
        o.addProperty("port", port);
        o.addProperty("pid", ProcessHandle.current().pid());
        try {
            Path tmp = owner.resolveSibling(OWNER + ".tmp");
            Files.writeString(tmp, o.toString(), StandardCharsets.UTF_8);
            Files.move(tmp, owner, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException ignored) {
            // only a note for humans; the lock itself is what counts
        }
    }

    /** The holder note in {@code dir}, or null when there is none or it is unreadable. */
    public static Holder holder(Path dir) {
        try {
            JsonObject o = JsonParser.parseString(Files.readString(dir.resolve(OWNER), StandardCharsets.UTF_8)).getAsJsonObject();
            return new Holder(o.has("world") ? o.get("world").getAsString() : "", o.has("port") ? o.get("port").getAsInt() : 0,
                    o.has("pid") ? o.get("pid").getAsLong() : 0);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    public boolean valid() {
        return lock.isValid();
    }

    @Override
    public void close() {
        try {
            Files.deleteIfExists(owner);
        } catch (IOException ignored) {
        }
        try {
            lock.release();
        } catch (IOException | RuntimeException ignored) {
        }
        try {
            channel.close();
        } catch (IOException ignored) {
        }
    }
}
