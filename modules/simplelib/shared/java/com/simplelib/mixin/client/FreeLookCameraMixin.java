package com.simplelib.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simplelib.api.client.FreeLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Free look (E29-4, IG31-29): the camera turns by the free-look offset on top of the local player's view. Applied where
 * {@code alignWithEntity} reads the view angles - not on every {@code setRotation} as before: the mirrored third person
 * view and the bed view call {@code setRotation} a second time, which added the offset twice (yaw) or cancelled it (pitch).
 */
@Mixin(Camera.class)
public abstract class FreeLookCameraMixin {
    @WrapOperation(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float simplelib$freeLookYaw(Entity entity, float partialTicks, Operation<Float> original) {
        float yRot = original.call(entity, partialTicks);
        return entity == Minecraft.getInstance().player ? yRot + FreeLook.yawOffset() : yRot;
    }

    @WrapOperation(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private float simplelib$freeLookPitch(Entity entity, float partialTicks, Operation<Float> original) {
        float xRot = original.call(entity, partialTicks);
        return entity == Minecraft.getInstance().player ? Math.max(-90, Math.min(90, xRot + FreeLook.pitchOffset())) : xRot;
    }
}
