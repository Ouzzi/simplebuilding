package com.simpleriding.mixin;
import com.simpleriding.Riding;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Enchantment.class) public abstract class RidingEnchantmentMixin {
 @Inject(method={"canEnchant","isSupportedItem"},at=@At("HEAD"),cancellable=true)
 private void simpleriding$armor(ItemStack stack,CallbackInfoReturnable<Boolean> cir){
  if(stack.is(Riding.ARMOR))cir.setReturnValue(Riding.allowed((Enchantment)(Object)this));
 }
}
