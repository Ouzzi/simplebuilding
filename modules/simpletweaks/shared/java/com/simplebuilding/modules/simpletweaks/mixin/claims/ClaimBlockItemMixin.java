package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.Direction;
import net.minecraft.server.level.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockItem.class)
public abstract class ClaimBlockItemMixin {
 @Inject(method="placeBlock",at=@At("HEAD"),cancellable=true)
 private void claims$place(BlockPlaceContext context,BlockState state,CallbackInfoReturnable<Boolean> cir){
  if(!(context.getLevel() instanceof ServerLevel level) || !Claims.enabled(level.getServer()))return;
  var p=context.getPlayer();var pos=context.getClickedPos();
  java.util.function.Predicate<net.minecraft.core.BlockPos> allowed=at->p instanceof ServerPlayer player?Claims.allow(player,level,at):Claims.allow(level,null,at);
  if(!allowed.test(pos)){cir.setReturnValue(false);return;}
  if(state.getBlock() instanceof BedBlock && !allowed.test(pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING)))){cir.setReturnValue(false);return;}
  if(state.getBlock() instanceof ChestBlock)for(var direction:Direction.Plane.HORIZONTAL){var neighbor=pos.relative(direction);if(level.getBlockState(neighbor).getBlock() instanceof ChestBlock&&!allowed.test(neighbor)){cir.setReturnValue(false);return;}}
 }
}
