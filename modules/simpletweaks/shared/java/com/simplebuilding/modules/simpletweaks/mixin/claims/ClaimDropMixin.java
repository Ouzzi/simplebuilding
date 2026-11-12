package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Death drops of a player carry no thrower in Vanilla; the claim rules need it to let the player pick them up again (E1ANS F2). */
@Mixin(net.minecraft.world.entity.LivingEntity.class)
public abstract class ClaimDropMixin {
 @Inject(method="createItemStackToDrop",at=@At("RETURN"))
 private void claims$dropper(net.minecraft.world.item.ItemStack stack,boolean randomly,boolean thrownFromHand,CallbackInfoReturnable<net.minecraft.world.entity.item.ItemEntity> cir){
  if(cir.getReturnValue()!=null&&(Object)this instanceof ServerPlayer player)Claims.droppedBy(cir.getReturnValue(),player);
 }
}
