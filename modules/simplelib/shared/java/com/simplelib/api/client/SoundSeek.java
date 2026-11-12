package com.simplelib.api.client;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.LoopingAudioStream;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * Streamed sounds that start part-way (a-shopmusic37, shared building block): the sound file
 * {@code simplelib:seek/<seconds>/<ns>/<path>} is the resource {@code <ns>:sounds/<path>.ogg} from {@code seconds} on
 * ({@code SoundSeekMixin} opens it). A looping sound instance gets a seamless loop (Vanilla's {@link LoopingAudioStream})
 * whose first pass starts at {@code seconds}. Use it in a sound instance whose {@code Sound} is a streamed
 * {@code FILE} with this id (see {@code simpleshops} {@code ShopMusicSound}). Simple Music's Pocket Jukebox still has its
 * own copy ({@code simplemusic:seek/...}).
 */
public final class SoundSeek {
    public static final String NAMESPACE = "simplelib";
    private static final String PATH = "sounds/seek/";

    /**
     * The seek file of sound event {@code event} (its first file; music discs have one), starting at {@code offsetSeconds},
     * or {@code null} when the client does not know the event.
     */
    public static @Nullable Identifier file(SoundManager sounds, Identifier event, int offsetSeconds) {
        WeighedSoundEvents events = sounds.getSoundEvent(event);
        if (events == null) return null;
        Identifier file = events.getSound(SoundInstance.createUnseededRandom()).getLocation();
        if (file == null || file.equals(SoundManager.EMPTY_SOUND.getLocation())) return null;
        return file(file, offsetSeconds);
    }

    /** {@code simplelib:seek/<s>/<ns>/<path>} for the sound file {@code ns:path} (as in sounds.json, without "sounds/" and ".ogg"). */
    public static Identifier file(Identifier soundFile, int offsetSeconds) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, "seek/" + Math.max(0, offsetSeconds) + "/" + soundFile.getNamespace() + "/" + soundFile.getPath());
    }

    /** A stream location this class serves: {@code simplelib:sounds/seek/<s>/<ns>/<path>.ogg}. */
    public static boolean handles(Identifier location) {
        return location.getNamespace().equals(NAMESPACE) && location.getPath().startsWith(PATH);
    }

    /** The resource behind a served location, or {@code null} when the location is malformed. */
    public static @Nullable Identifier resource(Identifier location) {
        String[] parts = location.getPath().substring(PATH.length()).split("/", 3);
        return parts.length < 3 ? null : Identifier.tryBuild(parts[1], "sounds/" + parts[2]);
    }

    /** The start second of a served location (0 when unreadable). */
    public static int seconds(Identifier location) {
        String[] parts = location.getPath().substring(PATH.length()).split("/", 2);
        try {
            return Math.max(0, Integer.parseInt(parts[0]));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Opens an Ogg stream from {@code opener}, skips {@code skipSeconds}; {@code looping} restarts at 0 at the end. */
    public static CompletableFuture<AudioStream> open(Callable<InputStream> opener, int skipSeconds, boolean looping) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                InputStream in = opener.call();
                if (!looping) return skip(new JOrbisAudioStream(in), skipSeconds);
                boolean[] first = {true};
                return (AudioStream) new LoopingAudioStream(stream -> {
                    JOrbisAudioStream ogg = new JOrbisAudioStream(stream);
                    if (!first[0]) return ogg;
                    first[0] = false;
                    return skip(ogg, skipSeconds);
                }, in);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, Util.nonCriticalIoPool());
    }

    private static AudioStream skip(JOrbisAudioStream stream, int seconds) throws java.io.IOException {
        long bytes = (long) (Math.max(0, seconds) * stream.getFormat().getFrameRate()) * stream.getFormat().getFrameSize();
        while (bytes > 0) {
            ByteBuffer read = stream.read((int) Math.min(bytes, 1 << 16));
            if (read == null || !read.hasRemaining()) break;
            bytes -= read.remaining();
        }
        return stream;
    }

    private SoundSeek() {}
}
