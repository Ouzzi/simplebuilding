package com.simplelib.mixin.client;

import com.simplelib.warm.Warm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Warm glow on dropped items (owner answer 41: everywhere, four steps): a warm item on the ground gets an orange
 * outline whose brightness follows the remaining warmth. Only within {@link #RANGE} blocks of the camera, so a
 * dropped sandwich does not shine through a whole base.
 */
@Mixin(EntityRenderer.class)
public abstract class WarmGlowEntityMixin {
    private static final double RANGE = 12.0;

    @Inject(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", at = @At("RETURN"))
    private void simplelib$warmOutline(Entity entity, float partialTicks, CallbackInfoReturnable<EntityRenderState> cir) {
        if (!(entity instanceof ItemEntity item) || cir.getReturnValue().outlineColor != 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.getCameraEntity() == null || entity.distanceToSqr(minecraft.getCameraEntity()) > RANGE * RANGE) return;
        float warmth = Warm.warmth(item.getItem(), minecraft.level.getGameTime());
        if (warmth <= 0) return;
        int step = Math.min(4, 1 + (int) (warmth * 4));
        cir.getReturnValue().outlineColor = com.simplelib.client.WarmGlowColors.outline(step);
    }
}
