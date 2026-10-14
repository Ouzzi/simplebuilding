package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/**
 * A click that did not open a door or container falls through to the item. Everything except placing a block
 * (checked where it is placed, with B) is strict: an axe, hoe or flint and steel can change blocks without a place hook.
 */
@Mixin(ItemStack.class)
public abstract class ClaimItemUseMixin {
 @Inject(method="useOn",at=@At("HEAD"),cancellable=true)
 private void claims$use(UseOnContext context,CallbackInfoReturnable<InteractionResult> cir){
  var stack=(ItemStack)(Object)this;
  if(stack.isEmpty()||stack.getItem() instanceof BlockItem||!(context.getPlayer() instanceof ServerPlayer player)||!(context.getLevel() instanceof ServerLevel level)||!Claims.enabled(level.getServer()))return;
  var pos=context.getClickedPos();
  if(!Claims.allowBlock(player,level,pos)||!Claims.allow(player,level,pos.relative(context.getClickedFace())))cir.setReturnValue(InteractionResult.FAIL);
 }
}
