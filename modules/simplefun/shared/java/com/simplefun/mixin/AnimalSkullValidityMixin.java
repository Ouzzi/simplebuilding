package com.simplefun.mixin;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityType.class)
public class AnimalSkullValidityMixin {
  @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
  private void fun$valid(BlockState s, CallbackInfoReturnable<Boolean> c) {
    if ((Object) this == BlockEntityTypes.SKULL
        && s.getBlock() instanceof AbstractSkullBlock skull
        && skull.getType() instanceof com.simplefun.heads.AnimalHead) c.setReturnValue(true);
  }
}
