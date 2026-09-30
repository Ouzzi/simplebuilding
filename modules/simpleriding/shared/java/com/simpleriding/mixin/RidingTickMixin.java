package com.simpleriding.mixin;
import com.simpleriding.RidingEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class) public abstract class RidingTickMixin {
 @Inject(method="tick",at=@At("HEAD")) private void simpleriding$tick(CallbackInfo ci){RidingEffects.tick((LivingEntity)(Object)this);}
}
