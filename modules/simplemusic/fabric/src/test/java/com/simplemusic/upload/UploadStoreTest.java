package com.simplemusic.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Chunked upload assembly and the track store (concept §5/§6). <b>What breaks this test:</b> a gap, repeat, oversize
 * chunk or a too fast client accepted; a hash that is not the server's own; dedup lost; a deleted track still on disk;
 * the quota counting rejected tracks; the index not surviving a restart; a file name built from anything but a hash.
 */
class UploadStoreTest {
    private static byte[] bytes(int n) {
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++) b[i] = (byte) (i * 13);
        return b;
    }

    @Test
    void chunksInOrderOnly() {
        UploadSession s = new UploadSession(70_000, "Song", 256 * 1024, 0);
        byte[] data = bytes(70_000);
        assertNull(s.accept(0, Arrays.copyOfRange(data, 0, 32768), 0));
        assertEquals("duplicate_chunk", s.accept(0, Arrays.copyOfRange(data, 0, 32768), 0));
        assertEquals("missing_chunk", s.accept(2, Arrays.copyOfRange(data, 0, 10), 0));
        assertEquals("bad_chunk", s.accept(1, new byte[UploadSession.MAX_CHUNK + 1], 0));
        assertNull(s.accept(1, Arrays.copyOfRange(data, 32768, 65536), 0));
        assertFalse(s.complete());
        assertEquals("too_many_bytes", s.accept(2, new byte[5000], 0));
        assertNull(s.accept(2, Arrays.copyOfRange(data, 65536, 70000), 0));
        assertTrue(s.complete());
        assertEquals(UploadSession.sha256(data), UploadSession.sha256(s.bytes()));
    }

    @Test
    void rateLimit() {
        UploadSession s = new UploadSession(1 << 20, "Fast", 64 * 1024, 0);
        int index = 0;
        String problem = null;
        // four chunks of 32 KiB in the same millisecond: one second (64 KiB) plus one chunk of burst allows three
        for (; index < 4 && problem == null; index++) problem = s.accept(index, new byte[32768], 0);
        assertEquals("rate", problem);
        UploadSession paced = new UploadSession(1 << 20, "Paced", 64 * 1024, 0);
        for (int i = 0; i < 20; i++) assertNull(paced.accept(i, new byte[32768], 500L * i), "chunk " + i);
        assertTrue(new UploadSession(1, "x", 1, 0).expired(UploadSession.TIMEOUT_MILLIS + 1));
    }

    @Test
    void hashOnlyFileNames() {
        assertTrue(UploadSession.isHash("a".repeat(64)));
        assertFalse(UploadSession.isHash("../" + "a".repeat(61)));
        assertFalse(UploadSession.isHash("A".repeat(64)));
        assertFalse(UploadSession.isHash("a".repeat(63)));
        TrackStore store = new TrackStore(Path.of("unused"));
        assertNull(store.file("../../server.properties"));
    }

    @Test
    void storeDedupStatusQuotaAndRestart() throws Exception {
        Path dir = Files.createTempDirectory("simplemusic-store");
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        TrackStore store = new TrackStore(dir);
        byte[] one = OggFiles.mono().build();
        OggFiles other = OggFiles.mono();
        other.serial = 4;
        byte[] two = other.build();
        TrackStore.Track a = store.add(one, "One", 10, alice, "Alice", false, 1000);
        assertEquals(UploadSession.sha256(one), a.hash());
        assertEquals(TrackStore.Status.PENDING, a.status());
        assertTrue(Files.isRegularFile(store.file(a.hash())));
        assertEquals(a, store.add(one, "Again", 10, bob, "Bob", true, 2000), "same hash = same entry");
        assertTrue(a.audibleFor(alice));
        assertFalse(a.audibleFor(bob), "pending: only the uploader hears it");
        TrackStore.Track b = store.add(two, "Two", 10, alice, "Alice", true, 3000);
        assertEquals(2, store.countBy(alice));
        assertEquals(0, store.countBy(bob));
        assertEquals(one.length + two.length, store.totalBytes());
        assertTrue(store.setStatus(a.hash(), TrackStore.Status.APPROVED));
        assertTrue(store.get(a.hash()).audibleFor(bob));
        assertTrue(store.setStatus(b.hash(), TrackStore.Status.DELETED));
        assertFalse(Files.exists(store.file(b.hash())), "deleted = file gone");
        assertEquals(1, store.countBy(alice), "deleted tracks free the quota");
        store.ban(bob, true);
        assertFalse(store.setStatus("f".repeat(64), TrackStore.Status.APPROVED));
        Files.writeString(dir.resolve("tmp").resolve("crashed.part"), "x");
        TrackStore again = new TrackStore(dir);
        again.cleanTmp();
        assertEquals(TrackStore.Status.APPROVED, again.get(a.hash()).status());
        assertEquals(TrackStore.Status.DELETED, again.get(b.hash()).status());
        assertEquals("Alice", again.get(a.hash()).uploaderName());
        assertTrue(again.banned(bob));
        assertEquals(0, again.pending().size());
        assertFalse(Files.exists(dir.resolve("tmp").resolve("crashed.part")));
        assertEquals(one.length, again.read(a.hash()).length);
    }

    /** REV2-B: a damaged entry in index.json (hand edit) skips only itself; the others keep their status, the index stays. */
    @Test
    void damagedIndexEntrySkipsOnlyItself() throws Exception {
        Path dir = Files.createTempDirectory("simplemusic-index");
        UUID alice = UUID.randomUUID();
        TrackStore store = new TrackStore(dir);
        TrackStore.Track a = store.add(OggFiles.mono().build(), "One", 10, alice, "Alice", true, 1000);
        store.ban(alice, true);
        Path index = dir.resolve("index.json");
        String text = Files.readString(index);
        text = text.replace("\"tracks\": [", "\"tracks\": [{\"hash\": \"" + "e".repeat(64) + "\", \"status\": \"APPROVED\"},");
        text = text.replace("\"banned\": [", "\"banned\": [\"not-a-uuid\",");
        Files.writeString(index, text);
        TrackStore again = new TrackStore(dir);
        assertEquals(TrackStore.Status.APPROVED, again.get(a.hash()).status(), "the good entry keeps its status");
        assertNull(again.get("e".repeat(64)), "the damaged entry is skipped");
        assertTrue(again.banned(alice), "the good ban stays");
        assertTrue(Files.isRegularFile(index), "the index is not moved aside");
    }
}
