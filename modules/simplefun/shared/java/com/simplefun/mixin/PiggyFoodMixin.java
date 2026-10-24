package com.simplefun.mixin;

import com.simplefun.SimplefunCommon;
import com.simplefun.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class PiggyFoodMixin {

  @Shadow protected ItemStack useItem;

  @Inject(method = "completeUsingItem", at = @At("HEAD"))
  private void simplefun$onCompleteUsingItem(CallbackInfo ci) {
    if ((Object) this instanceof net.minecraft.server.level.ServerPlayer player)
      com.simplefun.FunDelights.food(player, this.useItem);
    // Super-mod switch off = inert (REV2-A: the piggy effect still came with every porkchop).
    if (!SimplefunCommon.on() || !SimplefunCommon.getConfig().fun.enablePiggyEffect) return;

    LivingEntity entity = (LivingEntity) (Object) this;
    if (this.useItem.is(Items.PORKCHOP) || this.useItem.is(Items.COOKED_PORKCHOP)) {
      if (!entity.level().isClientSide()) {
        entity.addEffect(new MobEffectInstance(ModEffects.holder(), com.simplefun.config.SimplefunConfig.PIGGY_DURATION, 0));
      }
    }
  }
}
