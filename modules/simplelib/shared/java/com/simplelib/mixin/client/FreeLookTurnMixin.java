package com.simplelib.mixin.client;

import com.simplelib.api.client.FreeLook;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Free look (E29-4): while the key is held, the mouse turn of the local player goes to the camera offset instead. */
@Mixin(Entity.class)
public abstract class FreeLookTurnMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void simplelib$freeLook(double dYaw, double dPitch, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (FreeLook.active() && self == Minecraft.getInstance().player) {
            FreeLook.turn(dYaw, dPitch, self.getXRot());
            ci.cancel();
        }
    }
}
