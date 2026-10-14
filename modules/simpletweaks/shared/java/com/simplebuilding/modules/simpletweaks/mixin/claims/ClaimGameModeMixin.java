package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.framework.api.Protection.Action;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerPlayerGameMode.class)
public abstract class ClaimGameModeMixin {
 @Shadow @Final protected ServerPlayer player;
 @Inject(method="destroyBlock",at=@At("HEAD"),cancellable=true)
 private void claims$break(BlockPos pos,CallbackInfoReturnable<Boolean> cir){if(!Claims.allowBlock(player,player.level(),pos,Action.DESTROY))cir.setReturnValue(false);}
 @Inject(method="useItemOn",at=@At("HEAD"),cancellable=true)
 private void claims$use(ServerPlayer player,Level level,ItemStack stack,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> cir){
  if(!(level instanceof ServerLevel server))return;
  // The right depends on what the click does (door, chest, lever, placing, anything else = strict); only placing and strict clicks reach the neighbor cell.
  var action=Claims.useAction(player,hit.getBlockPos(),stack);
  boolean reachesNeighbor=action==Action.BUILD||action==Action.ANY;
  if(!Claims.allowBlock(player,server,hit.getBlockPos(),action) || (reachesNeighbor && !Claims.allow(player,server,hit.getBlockPos().relative(hit.getDirection()),action)))cir.setReturnValue(InteractionResult.FAIL);
 }
}
