package com.simplebuilding.modules.simplerespawn.mixin.client;

import com.simplebuilding.modules.simplerespawn.client.RespawnClient;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** "Spectate" while downed: the camera circles the own lying body at the third-person distance, any angle. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private Entity entity;
    @Shadow private boolean detached;

    @Shadow protected abstract void setRotation(float yRot, float xRot);

    @Shadow protected abstract void setPosition(Vec3 position);

    @Shadow protected abstract void move(float forwards, float up, float right);

    @Shadow protected abstract float getMaxZoom(float cameraDist);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void simplerespawn$orbit(float partialTicks, CallbackInfo ci) {
        if (!RespawnClient.orbiting() || entity == null || entity != net.minecraft.client.Minecraft.getInstance().player) return;
        Vec3 pivot = new Vec3(Mth.lerp(partialTicks, entity.xo, entity.getX()), Mth.lerp(partialTicks, entity.yo, entity.getY()) + 0.4,
                Mth.lerp(partialTicks, entity.zo, entity.getZ()));
        setRotation(RespawnClient.orbitYaw(), RespawnClient.orbitPitch());
        setPosition(pivot);
        move(-getMaxZoom(4.0f), 0f, 0f);
        detached = true;
    }
}
