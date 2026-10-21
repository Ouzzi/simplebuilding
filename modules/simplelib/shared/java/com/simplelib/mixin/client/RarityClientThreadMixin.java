package com.simplelib.mixin.client;

import com.simplelib.api.SimpleRarity;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remembers the render thread: only there are mythic names coloured letter by letter ({@link SimpleRarity#onClient()}). */
@Mixin(Minecraft.class)
public abstract class RarityClientThreadMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void simplelib$markRenderThread(CallbackInfo ci) {
        SimpleRarity.markClientThread();
    }
}
