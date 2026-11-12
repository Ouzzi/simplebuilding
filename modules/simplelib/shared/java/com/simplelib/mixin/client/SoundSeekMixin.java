package com.simplelib.mixin.client;

import com.simplelib.api.client.SoundSeek;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Opens {@code simplelib:sounds/seek/<s>/<ns>/<path>.ogg} as the resource {@code <ns>:sounds/<path>.ogg} from second
 * {@code s} on, looping seamlessly when asked ({@link SoundSeek}). Every other location stays Vanilla's.
 */
@Mixin(SoundBufferLibrary.class)
public abstract class SoundSeekMixin {
    @Shadow
    @Final
    private ResourceProvider resourceManager;

    @Inject(method = "getStream", at = @At("HEAD"), cancellable = true)
    private void simplelib$seek(Identifier location, boolean looping, CallbackInfoReturnable<CompletableFuture<AudioStream>> cir) {
        if (!SoundSeek.handles(location)) return;
        Identifier resource = SoundSeek.resource(location);
        if (resource == null) return;
        ResourceProvider resources = this.resourceManager;
        cir.setReturnValue(SoundSeek.open(() -> resources.open(resource), SoundSeek.seconds(location), looping));
    }
}
