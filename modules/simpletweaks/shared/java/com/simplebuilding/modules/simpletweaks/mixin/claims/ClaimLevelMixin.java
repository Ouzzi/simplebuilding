package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerLevel.class)
public abstract class ClaimLevelMixin {
 @Inject(method="mayInteract",at=@At("RETURN"),cancellable=true)
 private void claims$permission(Entity actor,BlockPos pos,CallbackInfoReturnable<Boolean> cir) {
  if(cir.getReturnValue() && actor instanceof ServerPlayer player && !Claims.allow(player,(ServerLevel)(Object)this,pos))cir.setReturnValue(false);
 }
}
