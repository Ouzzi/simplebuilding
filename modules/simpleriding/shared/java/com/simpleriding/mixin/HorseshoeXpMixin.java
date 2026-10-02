package com.simpleriding.mixin;

import com.simpleriding.Horseshoes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** XP that Vanilla Mending leaves over repairs Mending horseshoes of the horse the player rides. */
@Mixin(ExperienceOrb.class)
public abstract class HorseshoeXpMixin {
 @Inject(method="repairPlayerItems",at=@At("RETURN"),cancellable=true)
 private void simpleriding$mendShoes(ServerPlayer player,int amount,CallbackInfoReturnable<Integer> cir){
  cir.setReturnValue(Horseshoes.mend(player,cir.getReturnValue()));
 }
}
