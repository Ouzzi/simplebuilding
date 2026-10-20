package com.simplemusic.mixin.client;

import com.simplemusic.SimpleMusic;
import com.simplemusic.client.TrackCache;
import java.io.IOException;
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
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Streams custom discs from the client cache: {@code simplemusic:sounds/upload/<sha256>/<seconds>.ogg} is the cached
 * file (never a resource pack path), the first {@code seconds} are skipped so a late listener hears the same moment as
 * everyone else.
 */
@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin {
    @Inject(method = "getStream", at = @At("HEAD"), cancellable = true)
    private void simplemusic$streamUpload(Identifier location, boolean looping, CallbackInfoReturnable<CompletableFuture<AudioStream>> cir) {
        if (!location.getNamespace().equals(SimpleMusic.MOD_ID) || !location.getPath().startsWith("sounds/upload/")) return;
        String[] parts = location.getPath().substring("sounds/upload/".length()).replace(".ogg", "").split("/");
        Path file = TrackCache.file(parts[0]);
        int seconds;
        try {
            seconds = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        } catch (NumberFormatException e) {
            seconds = 0;
        }
        if (file == null) return;
        int skip = seconds;
        cir.setReturnValue(CompletableFuture.supplyAsync(() -> {
            try {
                InputStream in = Files.newInputStream(file);
                JOrbisAudioStream stream = new JOrbisAudioStream(in);
                long bytes = (long) (skip * stream.getFormat().getFrameRate()) * stream.getFormat().getFrameSize();
                while (bytes > 0) {
                    ByteBuffer read = stream.read((int) Math.min(bytes, 1 << 16));
                    if (read == null || !read.hasRemaining()) break;
                    bytes -= read.remaining();
                }
                return (AudioStream) stream;
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, Util.nonCriticalIoPool()));
    }
}
