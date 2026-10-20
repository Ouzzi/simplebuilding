package com.simplemusic.upload;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * One upload in progress (concept §5): the client announces the size, then sends the file in order in chunks of at
 * most {@link #MAX_CHUNK} bytes; a gap, a repeat, a chunk too big, more bytes than announced or a rate above the limit
 * aborts the upload. At the end the server computes the SHA-256 itself (never trusts the client). Plain Java.
 */
public final class UploadSession {
    public static final int MAX_CHUNK = 32 * 1024;
    /** Abandoned uploads are dropped after five minutes. */
    public static final long TIMEOUT_MILLIS = 5 * 60 * 1000L;

    private final int declared;
    private final String title;
    private final long bytesPerSecond;
    private final ByteArrayOutputStream data;
    private int next;
    private long lastMillis;
    private double tokens;
    private final long started;

    public UploadSession(int declared, String title, long bytesPerSecond, long nowMillis) {
        this.declared = declared;
        this.title = title;
        this.bytesPerSecond = Math.max(1, bytesPerSecond);
        this.data = new ByteArrayOutputStream(Math.max(0, declared));
        this.lastMillis = nowMillis;
        this.started = nowMillis;
        // one second of burst plus one chunk, so a client that paces at the limit never trips it
        this.tokens = this.bytesPerSecond + MAX_CHUNK;
    }

    public String title() {
        return title;
    }

    public int declared() {
        return declared;
    }

    public int received() {
        return data.size();
    }

    public boolean complete() {
        return data.size() == declared;
    }

    public boolean expired(long nowMillis) {
        return nowMillis - lastMillis > TIMEOUT_MILLIS;
    }

    public long started() {
        return started;
    }

    /** Adds chunk {@code index}; returns null when accepted, otherwise the problem key (the upload is then dead). */
    public String accept(int index, byte[] bytes, long nowMillis) {
        if (index != next) return index < next ? "duplicate_chunk" : "missing_chunk";
        if (bytes.length == 0 || bytes.length > MAX_CHUNK) return "bad_chunk";
        if ((long) data.size() + bytes.length > declared) return "too_many_bytes";
        double capacity = bytesPerSecond + MAX_CHUNK;
        tokens = Math.min(capacity, tokens + (nowMillis - lastMillis) * bytesPerSecond / 1000.0);
        lastMillis = nowMillis;
        if (tokens < bytes.length) return "rate";
        tokens -= bytes.length;
        data.write(bytes, 0, bytes.length);
        next++;
        return null;
    }

    public byte[] bytes() {
        return data.toByteArray();
    }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A lowercase 64 digit hex SHA-256 (the only file names the store ever builds, no path from a client). */
    public static boolean isHash(String text) {
        return text != null && text.length() == 64 && text.chars().allMatch(c -> (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'));
    }
}
