package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin({net.minecraft.world.level.material.FlowingFluid.class,net.minecraft.world.level.material.LavaFluid.class})
public abstract class ClaimFluidMixin {
 @Inject(method="spreadTo",at=@At("HEAD"),cancellable=true)
 private void claims$flow(LevelAccessor level,BlockPos pos,BlockState state,Direction direction,net.minecraft.world.level.material.FluidState fluid,CallbackInfo ci){
  if(level instanceof ServerLevel server&&!Claims.transfer(server,pos.relative(direction.getOpposite()),pos))ci.cancel();
 }
}
