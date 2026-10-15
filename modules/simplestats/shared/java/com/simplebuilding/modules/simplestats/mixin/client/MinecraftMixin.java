package com.simplebuilding.modules.simplestats.mixin.client;

import com.simplebuilding.modules.simplestats.client.StatsClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The key opens the statistics of the held item while no screen is open (key presses reach the game, not a screen). */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "handleKeybinds", at = @At("TAIL"))
    private void simplestats$key(CallbackInfo ci) {
        Minecraft self = (Minecraft) (Object) this;
        if (StatsClient.openKey == null) return;
        while (StatsClient.openKey.consumeClick()) {
            if (self.player != null && self.gui.screen() == null) StatsClient.open(self.player.getMainHandItem(), null);
        }
    }
}
