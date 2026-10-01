package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.world.level.block.LiquidBlock.class)
public abstract class ClaimLiquidMixin {
 @Inject(method="shouldSpreadLiquid",at=@At("HEAD"),cancellable=true)
 private void claims$conversion(Level level,BlockPos pos,BlockState state,CallbackInfoReturnable<Boolean> cir){
  if(!(level instanceof ServerLevel server)||!Claims.enabled(server.getServer())||!state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA))return;
  boolean basalt=level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.SOUL_SOIL);
  for(var direction:Direction.values()){var neighbor=pos.relative(direction);if((level.getFluidState(neighbor).is(net.minecraft.tags.FluidTags.WATER)||(basalt&&level.getBlockState(neighbor).is(net.minecraft.world.level.block.Blocks.BLUE_ICE)))&&!Claims.transfer(server,pos,neighbor)){cir.setReturnValue(false);return;}}
 }
}
