package com.simplemusic.client;

import com.simplemusic.SimpleMusic;
import com.simplemusic.net.TrackDataPayload;
import com.simplemusic.upload.UploadSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;

/**
 * Client cache of downloaded tracks (concept §5): {@code <game dir>/simplemusic-cache/<sha256>.ogg}, content
 * addressed (the hash is checked after each download, so files from different servers never mix up), oldest files
 * dropped above {@link #LIMIT_BYTES}.
 */
public final class TrackCache {
    public static final long LIMIT_BYTES = 512L << 20;
    private static final Map<String, ByteArrayOutputStream> INCOMING = new HashMap<>();

    private TrackCache() {}

    public static Path dir() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("simplemusic-cache");
    }

    public static Path file(String hash) {
        return UploadSession.isHash(hash) ? dir().resolve(hash + ".ogg") : null;
    }

    public static boolean has(String hash) {
        Path f = file(hash);
        return f != null && Files.isRegularFile(f);
    }

    /** Collects a chunk; returns true when the file is complete, checked and stored. */
    public static boolean receive(TrackDataPayload chunk) {
        if (!UploadSession.isHash(chunk.sha256()) || chunk.total() <= 0 || chunk.total() > (64 << 20)) return false;
        ByteArrayOutputStream buffer = INCOMING.computeIfAbsent(chunk.sha256(), k -> new ByteArrayOutputStream());
        if (chunk.index() == 0) buffer.reset();
        buffer.writeBytes(chunk.bytes());
        if (buffer.size() < chunk.total()) return false;
        INCOMING.remove(chunk.sha256());
        byte[] data = buffer.toByteArray();
        if (data.length != chunk.total() || !UploadSession.sha256(data).equals(chunk.sha256())) {
            SimpleMusic.LOG.warn("Dropped a track download with a wrong hash");
            return false;
        }
        try {
            Files.createDirectories(dir());
            Path tmp = dir().resolve(chunk.sha256() + ".part");
            Files.write(tmp, data);
            Files.move(tmp, file(chunk.sha256()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            trim();
            return true;
        } catch (IOException e) {
            SimpleMusic.LOG.warn("Could not cache a track: {}", e.toString());
            return false;
        }
    }

    private static void trim() throws IOException {
        try (var files = Files.list(dir())) {
            List<Path> all = files.filter(p -> p.toString().endsWith(".ogg")).sorted(Comparator.comparingLong(p -> {
                try {
                    return Files.getLastModifiedTime(p).toMillis();
                } catch (IOException e) {
                    return 0L;
                }
            })).toList();
            long total = 0;
            for (Path p : all) total += Files.size(p);
            for (Path p : all) {
                if (total <= LIMIT_BYTES) break;
                total -= Files.size(p);
                Files.deleteIfExists(p);
            }
        }
    }
}
