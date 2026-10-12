package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(net.minecraft.world.level.block.TurtleEggBlock.class)
public abstract class ClaimTurtleEggMixin {
 @Inject(method="canDestroyEgg",at=@At("RETURN"),cancellable=true)
 private void claims$egg(ServerLevel level,BlockPos pos,Entity entity,CallbackInfoReturnable<Boolean> cir){
  if(cir.getReturnValueZ()&&!Claims.mob(entity,level,pos))cir.setReturnValue(false);
 }
}
