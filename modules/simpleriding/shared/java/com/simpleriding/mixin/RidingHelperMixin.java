package com.simpleriding.mixin;
import com.simpleriding.Riding;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(EnchantmentHelper.class) public abstract class RidingHelperMixin {
 @Inject(method="canStoreEnchantments",at=@At("HEAD"),cancellable=true)
 private static void simpleriding$store(ItemStack stack,CallbackInfoReturnable<Boolean> cir){if(stack.is(Riding.ARMOR)||stack.is(Riding.SADDLE))cir.setReturnValue(true);}
 @Redirect(method={"getEnchantmentCost","selectEnchantment"},at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
 private static Object simpleriding$enchantability(ItemStack stack,net.minecraft.core.component.DataComponentType<?> type){
  Object value=stack.get(type);
  return value==null&&type==net.minecraft.core.component.DataComponents.ENCHANTABLE&&stack.is(Riding.ARMOR)?new net.minecraft.world.item.enchantment.Enchantable(15):value;
 }
}
