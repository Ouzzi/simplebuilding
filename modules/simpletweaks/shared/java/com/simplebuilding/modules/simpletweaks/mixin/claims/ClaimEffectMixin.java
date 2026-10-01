package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.world.entity.LivingEntity.class)
public abstract class ClaimEffectMixin {
 @Inject(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",at=@At("HEAD"),cancellable=true)
 private void claims$effect(net.minecraft.world.effect.MobEffectInstance effect,net.minecraft.world.entity.Entity source,CallbackInfoReturnable<Boolean> cir){if(source!=null&&!effect.getEffect().value().isBeneficial()&&!Claims.action(source,(net.minecraft.world.entity.Entity)(Object)this))cir.setReturnValue(false);}
}
