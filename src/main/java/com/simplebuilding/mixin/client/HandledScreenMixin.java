package com.simplebuilding.mixin.client;

import com.simplebuilding.client.gui.tooltip.ReinforcedBundleTooltipSubmenuHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.TooltipSubmenuHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow protected abstract void addTooltipSubmenuHandler(TooltipSubmenuHandler handler);

    @Inject(method = "init", at = @At("TAIL"))
    private void simplebuilding$addReinforcedBundleHandler(CallbackInfo ci) {
        // Fügt unseren Handler zur Liste hinzu. Minecraft prüft diese Liste beim Scrollen.
        this.addTooltipSubmenuHandler(new ReinforcedBundleTooltipSubmenuHandler(MinecraftClient.getInstance()));
    }
}