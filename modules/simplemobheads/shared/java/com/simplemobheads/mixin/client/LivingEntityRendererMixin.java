package com.simplemobheads.mixin.client;

import com.simplemobheads.MobHeadType;
import com.simplemobheads.MobHeads;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A worn head shows its variant: the skull type follows the head item's {@code simplemobheads:variant}. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"))
    private void simplemobheads$wornVariant(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
        if (state.wornHeadType instanceof MobHeadType head) {
            String variant = entity.getItemBySlot(EquipmentSlot.HEAD).get(MobHeads.VARIANT);
            if (variant != null) {
                state.wornHeadType = MobHeads.type(head.head(), variant);
            }
        }
    }
}
