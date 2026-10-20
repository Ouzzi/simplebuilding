package com.simplemusic.upload;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The server's uploaded tracks (concept §5): {@code <world>/simplemusic/tracks/<sha256>.ogg} plus {@code index.json}
 * (hash, title, seconds, uploader, date, status) and the upload ban list. The same hash is stored once (dedup). A file
 * is first written to {@code tmp/} and then moved into place. File names are only ever built from a checked hash, never
 * from client text (no path traversal). Plain Java with Gson; one instance per world, used on the server thread.
 */
public final class TrackStore {
    public enum Status { PENDING, APPROVED, REJECTED, DELETED }

    public record Track(String hash, String title, int seconds, int bytes, UUID uploader, String uploaderName, long date, Status status) {
        Track with(Status s) {
            return new Track(hash, title, seconds, bytes, uploader, uploaderName, date, s);
        }

        /** Whether this player may hear it: approved, or still pending and their own. */
        public boolean audibleFor(UUID player) {
            return status == Status.APPROVED || (status == Status.PENDING && uploader.equals(player));
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path root;
    private final Map<String, Track> tracks = new LinkedHashMap<>();
    private final Set<UUID> banned = new LinkedHashSet<>();

    public TrackStore(Path root) {
        this.root = root;
        load();
    }

    public Path root() {
        return root;
    }

    private Path index() {
        return root.resolve("index.json");
    }

    /** The audio file of a hash; null for anything that is not a hash. */
    public Path file(String hash) {
        return UploadSession.isHash(hash) ? root.resolve("tracks").resolve(hash + ".ogg") : null;
    }

    public Track get(String hash) {
        return hash == null ? null : tracks.get(hash);
    }

    public List<Track> all() {
        return List.copyOf(tracks.values());
    }

    public List<Track> pending() {
        return tracks.values().stream().filter(t -> t.status() == Status.PENDING).toList();
    }

    /** Tracks of an uploader that still count against the quota (pending or approved). */
    public int countBy(UUID uploader) {
        return (int) tracks.values().stream().filter(t -> t.uploader().equals(uploader)
                && (t.status() == Status.PENDING || t.status() == Status.APPROVED)).count();
    }

    public long totalBytes() {
        return tracks.values().stream().filter(t -> t.status() == Status.PENDING || t.status() == Status.APPROVED)
                .mapToLong(Track::bytes).sum();
    }

    public boolean banned(UUID player) {
        return banned.contains(player);
    }

    public void ban(UUID player, boolean on) {
        if (on) banned.add(player);
        else banned.remove(player);
        save();
    }

    /**
     * Stores a checked file under its own hash. A hash already present keeps its entry (dedup): an approved one stays
     * approved, a deleted/rejected one is not revived.
     */
    public Track add(byte[] data, String title, int seconds, UUID uploader, String uploaderName, boolean approved, long now) throws IOException {
        String hash = UploadSession.sha256(data);
        Track known = tracks.get(hash);
        if (known != null) return known;
        Path tmp = root.resolve("tmp");
        Files.createDirectories(tmp);
        Files.createDirectories(root.resolve("tracks"));
        Path part = tmp.resolve(hash + ".part");
        Files.write(part, data);
        try {
            Files.move(part, file(hash), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(part, file(hash), StandardCopyOption.REPLACE_EXISTING);
        }
        Track track = new Track(hash, title, seconds, data.length, uploader, uploaderName, now, approved ? Status.APPROVED : Status.PENDING);
        tracks.put(hash, track);
        save();
        return track;
    }

    /** Sets a status; REJECTED and DELETED also remove the audio file. Returns false for an unknown hash. */
    public boolean setStatus(String hash, Status status) {
        Track track = tracks.get(hash);
        if (track == null) return false;
        tracks.put(hash, track.with(status));
        if (status == Status.REJECTED || status == Status.DELETED) {
            try {
                Files.deleteIfExists(file(hash));
            } catch (IOException ignored) {
                // the index entry is what counts; a left-over file is never served (status check)
            }
        }
        save();
        return true;
    }

    /** Deletes left-over partial files (crashed uploads). */
    public void cleanTmp() {
        Path tmp = root.resolve("tmp");
        if (!Files.isDirectory(tmp)) return;
        try (var files = Files.list(tmp)) {
            for (Path p : files.toList()) Files.deleteIfExists(p);
        } catch (IOException ignored) {
            // retried at the next start
        }
    }

    public byte[] read(String hash) throws IOException {
        Path file = file(hash);
        if (file == null) throw new IOException("not a hash");
        return Files.readAllBytes(file);
    }

    private void load() {
        Path index = index();
        if (!Files.isRegularFile(index)) return;
        try {
            JsonObject json = GSON.fromJson(Files.readString(index), JsonObject.class);
            if (json == null) return;
            JsonArray list = json.has("tracks") ? json.getAsJsonArray("tracks") : new JsonArray();
            for (JsonElement e : list) {
                JsonObject o = e.getAsJsonObject();
                String hash = o.get("hash").getAsString();
                if (!UploadSession.isHash(hash)) continue;
                Status status;
                try {
                    status = Status.valueOf(o.get("status").getAsString());
                } catch (RuntimeException ex) {
                    status = Status.PENDING;
                }
                tracks.put(hash, new Track(hash, o.get("title").getAsString(), o.get("seconds").getAsInt(), o.get("bytes").getAsInt(),
                        UUID.fromString(o.get("uploader").getAsString()), o.has("uploaderName") ? o.get("uploaderName").getAsString() : "",
                        o.get("date").getAsLong(), status));
            }
            if (json.has("banned")) for (JsonElement e : json.getAsJsonArray("banned")) banned.add(UUID.fromString(e.getAsString()));
        } catch (Exception e) {
            // a broken index is kept aside, the store starts empty (files stay on disk)
            try {
                Files.move(index, index.resolveSibling("index.broken-" + System.currentTimeMillis() + ".json"));
            } catch (IOException ignored) {
                // nothing else to do
            }
        }
    }

    private void save() {
        JsonObject json = new JsonObject();
        JsonArray list = new JsonArray();
        for (Track t : tracks.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("hash", t.hash());
            o.addProperty("title", t.title());
            o.addProperty("seconds", t.seconds());
            o.addProperty("bytes", t.bytes());
            o.addProperty("uploader", t.uploader().toString());
            o.addProperty("uploaderName", t.uploaderName());
            o.addProperty("date", t.date());
            o.addProperty("status", t.status().name());
            list.add(o);
        }
        json.add("tracks", list);
        JsonArray bans = new JsonArray();
        for (UUID b : banned) bans.add(b.toString());
        json.add("banned", bans);
        try {
            Files.createDirectories(root);
            Path tmp = root.resolve("index.json.tmp");
            Files.writeString(tmp, GSON.toJson(json));
            Files.move(tmp, index(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("could not write " + index(), e);
        }
    }

    /** All hashes (for listings). */
    public List<String> hashes() {
        return new ArrayList<>(tracks.keySet());
    }
}
