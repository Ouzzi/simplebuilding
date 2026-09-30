package com.simplefun.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(net.minecraft.world.item.enchantment.EnchantmentHelper.class)public class KnockbackCapMixin {
 @Inject(method="modifyKnockback",at=@At("RETURN"),cancellable=true)private static void fun$cap(CallbackInfoReturnable<Float> c){var cfg=com.simplefun.SimplefunCommon.getConfig().fun;float max=cfg.enableHigherKnockback?cfg.maxKnockback:2;float n=c.getReturnValue();c.setReturnValue(Float.isFinite(n)?Math.max(0,Math.min(max,n)):0f);}
}
