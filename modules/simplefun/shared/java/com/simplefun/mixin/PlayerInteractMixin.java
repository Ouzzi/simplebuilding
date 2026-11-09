package com.simplefun.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** FUN11-3/4 (b-fun14): sneak + empty hand on a player (high five) or an own tamed animal (petting), server side only. */
@Mixin(Player.class)
public class PlayerInteractMixin {
  @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
  private void fun$toyGesture(
      Entity entity, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
    if ((Object) this instanceof ServerPlayer player
        && com.simplefun.toys.FunToys.interact(player, entity, hand))
      cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
  }
}
