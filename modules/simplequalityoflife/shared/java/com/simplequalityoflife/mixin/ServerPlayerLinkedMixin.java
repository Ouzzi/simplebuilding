package com.simplequalityoflife.mixin;

import com.simplequalityoflife.container.LinkedContainers;
import com.simplequalityoflife.container.PortableContainers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Linked GUIs: append the marked container before a menu's first sync; tick checks and pending opens. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerLinkedMixin {
    @Inject(method = "initMenu", at = @At("HEAD"))
    private void qol$initMenu(AbstractContainerMenu menu, CallbackInfo ci) {
        LinkedContainers.onInitMenu((ServerPlayer) (Object) this, menu);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void qol$tick(CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        LinkedContainers.tick(self);
        PortableContainers.tick(self);
    }
}
