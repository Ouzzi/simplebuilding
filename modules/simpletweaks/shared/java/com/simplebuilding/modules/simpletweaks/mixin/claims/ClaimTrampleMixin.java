package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Trampling claimed farmland needs rights (players) or happens only on the mob's own land. */
@Mixin(net.minecraft.world.level.block.FarmlandBlock.class)
public abstract class ClaimTrampleMixin {
 @Inject(method="turnToBaseBlock",at=@At("HEAD"),cancellable=true)
 private void claims$trample(Entity source,BlockState state,Level level,BlockPos pos,CallbackInfo ci){
  if(source!=null&&level instanceof ServerLevel server&&!Claims.mob(source,server,pos))ci.cancel();
 }
}
