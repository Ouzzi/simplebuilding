package com.simplemusic.mixin.client;

import com.simplemusic.SimpleMusic;
import com.simplemusic.client.TrackCache;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Streams custom discs from the client cache: {@code simplemusic:sounds/upload/<sha256>/<seconds>.ogg} is the cached
 * file (never a resource pack path), the first {@code seconds} are skipped so a late listener hears the same moment as
 * everyone else. The Pocket Jukebox resumes Vanilla discs the same way: {@code simplemusic:sounds/seek/<seconds>/<ns>/<path>.ogg}
 * is the resource {@code <ns>:sounds/<path>.ogg}, from {@code seconds} on.
 */
@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin {
    @Shadow
    @Final
    private ResourceProvider resourceManager;

    @Inject(method = "getStream", at = @At("HEAD"), cancellable = true)
    private void simplemusic$streamUpload(Identifier location, boolean looping, CallbackInfoReturnable<CompletableFuture<AudioStream>> cir) {
        if (!location.getNamespace().equals(SimpleMusic.MOD_ID)) return;
        String path = location.getPath();
        if (path.startsWith("sounds/seek/")) {
            String[] parts = path.substring("sounds/seek/".length()).split("/", 3);
            if (parts.length < 3) return;
            Identifier resource = Identifier.tryBuild(parts[1], "sounds/" + parts[2]);
            if (resource == null) return;
            ResourceProvider resources = resourceManager;
            cir.setReturnValue(simplemusic$open(() -> resources.open(resource), simplemusic$seconds(parts[0])));
            return;
        }
        if (!path.startsWith("sounds/upload/")) return;
        String[] parts = path.substring("sounds/upload/".length()).replace(".ogg", "").split("/");
        Path file = TrackCache.file(parts[0]);
        if (file == null) return;
        cir.setReturnValue(simplemusic$open(() -> Files.newInputStream(file), parts.length > 1 ? simplemusic$seconds(parts[1]) : 0));
    }

    @Unique
    private static int simplemusic$seconds(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Unique
    private static CompletableFuture<AudioStream> simplemusic$open(java.util.concurrent.Callable<InputStream> opener, int skip) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                InputStream in = opener.call();
                JOrbisAudioStream stream = new JOrbisAudioStream(in);
                long bytes = (long) (skip * stream.getFormat().getFrameRate()) * stream.getFormat().getFrameSize();
                while (bytes > 0) {
                    ByteBuffer read = stream.read((int) Math.min(bytes, 1 << 16));
                    if (read == null || !read.hasRemaining()) break;
                    bytes -= read.remaining();
                }
                return (AudioStream) stream;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, Util.nonCriticalIoPool());
    }
}
