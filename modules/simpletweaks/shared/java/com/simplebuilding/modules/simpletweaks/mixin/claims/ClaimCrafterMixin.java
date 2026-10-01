package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(CrafterBlock.class)
public abstract class ClaimCrafterMixin {
 @Inject(method="dispenseFrom",at=@At("HEAD"),cancellable=true)
 private void claims$craft(BlockState state,ServerLevel level,BlockPos pos,CallbackInfo ci){
  if(!Claims.enabled(level.getServer()))return;
  var target=pos.relative(state.getValue(BlockStateProperties.ORIENTATION).front());
  if(!Claims.transfer(level,pos,target)){ci.cancel();return;}
  var into=HopperBlockEntity.getContainerAt(level,target);
  // Stop before crafting consumes inputs; a refused insertion otherwise ejects items.
  if(into!=null&&!ClaimAutomation.transfer(level.getBlockEntity(pos),into))ci.cancel();
 }
}
