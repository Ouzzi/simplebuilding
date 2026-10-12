package com.simplebuilding.modules.simplerespawn.mixin.client;

import com.simplebuilding.modules.simplerespawn.client.RespawnClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Client: a downed player sends no attack, use, pick, drop or swap; the inventory key opens the downed screen. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow @Final public Options options;

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void simplerespawn$noAttack(CallbackInfoReturnable<Boolean> cir) {
        if (RespawnClient.downed()) cir.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void simplerespawn$noMining(boolean down, CallbackInfo ci) {
        if (RespawnClient.downed()) ci.cancel();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void simplerespawn$noUse(CallbackInfo ci) {
        if (RespawnClient.downed()) ci.cancel();
    }

    @Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
    private void simplerespawn$noPick(CallbackInfo ci) {
        if (RespawnClient.downed()) ci.cancel();
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void simplerespawn$keys(CallbackInfo ci) {
        if (!RespawnClient.downed()) return;
        boolean menu = false;
        while (options.keyInventory.consumeClick()) menu = true;
        while (options.keyDrop.consumeClick()) { }
        while (options.keySwapOffhand.consumeClick()) { }
        if (menu) RespawnClient.openMenu((Minecraft) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void simplerespawn$tick(CallbackInfo ci) {
        RespawnClient.tick((Minecraft) (Object) this);
    }
}
