package com.simplefun.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rechtsklick auf einen Spieler mit Tierkopf: melken, scheren ({@link com.simplefun.heads.HeadAbilities#interact}). */
@Mixin(Entity.class)
public class HeadInteractMixin {
  @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
  private void fun$headInteract(Player user, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
    if ((Object) this instanceof Player target) {
      InteractionResult result = com.simplefun.heads.HeadAbilities.interact(target, user, hand);
      if (result != null) cir.setReturnValue(result);
    }
  }
}
