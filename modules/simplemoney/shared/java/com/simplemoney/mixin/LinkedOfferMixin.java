package com.simplemoney.mixin;

import com.simplemoney.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantOffer.class)
public abstract class LinkedOfferMixin {
    // No reputation/Hero discounts or negative demand on linked currency prices.
    // The authoritative base price is created by LinkedTradeMixin and synced by Vanilla.
    @Inject(method = "getCostA", at = @At("HEAD"), cancellable = true)
    private void price(CallbackInfoReturnable<ItemStack> cir) {
        var price = MoneyLinks.price((MerchantOffer)(Object)this);
        if (price != null) cir.setReturnValue(((MerchantOffer)(Object)this).getBaseCostA());
    }
}
