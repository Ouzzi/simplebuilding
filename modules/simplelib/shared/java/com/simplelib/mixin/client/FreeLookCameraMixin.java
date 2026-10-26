package com.simplelib.mixin.client;

import com.simplelib.api.client.FreeLook;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Free look (E29-4): the camera turns by the free-look offset on top of the player's view (first and third person). */
@Mixin(Camera.class)
public abstract class FreeLookCameraMixin {
    @ModifyVariable(method = "setRotation", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float simplelib$freeLookYaw(float yRot) {
        return yRot + FreeLook.yawOffset();
    }

    @ModifyVariable(method = "setRotation", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private float simplelib$freeLookPitch(float xRot) {
        return Math.max(-90, Math.min(90, xRot + FreeLook.pitchOffset()));
    }
}
