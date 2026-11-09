package com.simplefun.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class DelightTickMixin {
  @Inject(method = "tick", at = @At("TAIL"))
  private void fun$tick(CallbackInfo c) {
    com.simplefun.FunDelights.tick((ServerPlayer) (Object) this);
    // FUN11-10 snow angel watch and live angels (b-fun14).
    com.simplefun.toys.FunToys.tick((ServerPlayer) (Object) this);
  }
}
